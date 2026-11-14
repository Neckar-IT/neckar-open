# Floor-plan Viewer — webapp

Upload a DWG/DXF → the pipeline (`../tools/floorplan_pipeline.py`) runs every step (DWG→DXF conversion,
**room detection**, per-floor rendering in render worker processes) from the one pipeline thread →
you get an interactive **floor viewer** in the browser: floor tabs, a pan/zoom stage, per-layer
colour/visibility controls, and the detected rooms as named, clickable overlays. Thin Flask app; the
viewer (`app.py`, `_VIEWER`) composes the pipeline's transparent per-layer PNGs live from `manifest.json`.

Processing is asynchronous: the upload returns immediately, a progress page polls the job status, and the
same URL serves the viewer once rendering finishes. The progress page shows the current step and the count of
finished renders (`Erzeuge Vorschaubilder … 12 von 64`).

## Prerequisites

- **Room-extraction jar**: the pipeline runs the Kotlin `FloorPlanRoomExtractor` on each upload to
  detect floors + rooms and the `FloorBackgroundCli` of the same jar to draw each `background.png`, and finds the jar via
  `$FLOORPLAN_EXTRACTOR_JAR`; its manifest names the runtime classpath in `lib/` next to it. Without it, uploads
  still work but fall back to paper-space viewport floors (no room polygons), and `import.json` answers 409 for every job.
- `java` on PATH (to run the jar).
- **DWG** upload additionally needs `ODAFileConverter` on PATH, `xvfb` and the xcb libraries from [`tools/oda-file-converter/README.md`](../../../../../tools/oda-file-converter/README.md#conversion); DXF needs neither.

## Run

```bash
../tools/.venv/bin/pip install flask        # once, into the pipeline venv
../tools/.venv/bin/python app.py            # http://127.0.0.1:8090
```

## Flow / routes

Pages and files (HTML / downloads / render artifacts):
- `GET /` — upload page (drag-drop).
- `POST /upload` — save file + write `meta.json` (`state: queued`) → queue the job for the pipeline thread →
  redirect (~0.1 s). HTTP 507 with the page "Speicher voll" when the data directory has less than 2 GB or
  less than three times the upload size free; the file is not saved.
- `GET /j/<job>` — the viewer once done; otherwise the progress page (or an error page on failure).
- `GET /j/<job>/original` — download the exact uploaded file under its original name.
- `GET /j/<job>/import.json` — per floor with rooms: the rooms in the pixels of the floor's `background.png`, the image as a data URL (`floorplan_import.import_result`); 404 until the job is done, 409 for a job from a version before `background.png` (a new upload helps), a job whose room extraction failed, or a job with a floor with rooms that failed to render (the container log of the run names the cause).
- `GET /j/<job>/<path>` — serves the pipeline output (`manifest.json`, per-floor PNGs, `coordinates.json`).
- `GET /uploads/<token>` — archive of every stored upload (name, size, result counts, feedback badge,
  deletion date, links). Deep link only, deliberately unlinked from the UI; the token comes from
  `$FLOORPLAN_ARCHIVE_TOKEN` (set in the deployment compose file; the full link is listed in
  mea.neckar.it). Without the env var the archive is unreachable — locally run e.g.
  `FLOORPLAN_ARCHIVE_TOKEN=dev ../tools/.venv/bin/python app.py`.
- `GET /feedback-form` — standalone feedback page (used from the error page, where there is no viewer).

JSON API (monorepo REST conventions, template `internal/patterns`: kebab-case under `/api`, responses in
the `RestResponse` envelope `{"type": "good", "data": ...}` / `{"type": "bad/<Variant>"}`):
- `GET /api/floorplan/global-health` — anonymous liveness (`{"type": "Healthy"}`); Docker healthcheck.
  Below 2 GB free in the data directory: `{"type": "Degraded", "freeBytes": 1503238553}`, still HTTP 200,
  so the viewer of finished jobs stays routed; Gatus alerts on it.
- `GET /api/floorplan/meta-tags` — deploy metadata for deployment verification: `imageDigest` (null outside a
  pinned deploy), `gitHash`, `gitHashShort` and `gitCommitDateTime` always null.
- `GET /api/jobs/<job>/status` — `{state: queued|running|done|error|unknown, message}` read from
  `meta.json`, polled by the progress page.
- `POST /api/feedback` — store a user hint/bug report (`{job?, message, contact?, floor?}`); empty
  message → 400 `bad/EmptyMessage`.

Every response additionally carries the `X-App-Image-Digest` header when the `APP_IMAGE_DIGEST` env var holds a
pinned digest — the deploy sets it in the compose file.

Tests: the Gradle task `check` of the floorplan.neckar.it module runs `test_app.py` (`testWebapp`), every `../tools/test_*.py` (`testTools`), the import checks over a drawn square and four example DXFs (`testImport`) and one pipeline run over an example DXF (`testPipeline`).

## Storage & feedback
- **Job state** lives in `jobs/<job>/meta.json`: original filename, size, SHA-256, `uploaded_at`, `state`
  (`queued`, `running`, `done`, `error`), `message`, `started_at`, `finished_at`, `attempts`, floor/room
  counts. The original file stays at `jobs/<job>/input<ext>`.
- **Restart.** At start the webapp re-queues every `queued` or `running` job, oldest upload first; the
  progress page keeps polling and ends with the viewer. A job interrupted in three runs ends with `error`.
  At the interpreter exit the pipeline thread stops: the job it renders stays `running`, and the next start
  resumes it.
  A job directory without `meta.json` (an upload the last process did not finish saving) is removed.
- **One pipeline thread** takes the jobs from a queue, one at a time: each job starts its own render worker
  processes, each with a copy of the drawing in memory.
  gunicorn runs one worker process, because the queue lives in the process: a second process would
  resume and render the same jobs.
- **Free space.** `POST /upload` answers HTTP 507 below 2 GB or below three times the upload size free in
  `$FLOORPLAN_DATA_DIR`; `global-health` reports `Degraded` below 2 GB.
- **Retention.** A finished upload without `jobs/<job>/feedback.jsonl` is deleted on the 180th day after
  `uploaded_at` (input, render, `meta.json`); an upload with feedback stays. The first request after
  03:00 Europe/Berlin queues the run of the day for the pipeline thread; the run records its date in
  `retention.json` and logs `retention: deleted job e50e7f48e654 (uploaded 2026-10-05, no feedback)`.
  The archive at `/uploads/<token>` shows the deletion date of each upload.
- **Feedback** is collected via the "⚑ Problem melden" button in the viewer (carries the job + current
  floor) and the error page. Reports append to a global `feedback.jsonl` and the job's own
  `jobs/<job>/feedback.jsonl`, so a report stays next to the plan it is about.

`jobs/`, `feedback.jsonl` and `retention.json` are git-ignored (user data).
