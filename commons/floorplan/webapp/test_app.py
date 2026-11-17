#!/usr/bin/env python3
"""Standalone tests for app.py — no pytest in this repo.

Run:  the Gradle task `testWebapp` of the floorplan.neckar.it module (the render pipeline is stubbed)
Exit 0 = all pass; non-zero = a failure (the failing test is printed).
"""
import atexit
import datetime
import io
import json
import logging
import os
import queue
import runpy
import shutil
import sys
import tempfile
import threading
import time
import types

# app.py resolves the data dir and the deploy metadata at import time — configure the
# environment first, then import.
os.environ["FLOORPLAN_DATA_DIR"] = tempfile.mkdtemp(prefix="floorplan-webapp-test-")
atexit.register(shutil.rmtree, os.environ["FLOORPLAN_DATA_DIR"], ignore_errors=True)
os.environ["APP_IMAGE_DIGEST"] = "sha256:abababababababababababababababababababababababababababababababab"
os.environ["FLOORPLAN_ARCHIVE_TOKEN"] = "test-archive-token"

# The pipeline pulls in ezdxf/matplotlib; the tests replace `process` with their own stub.
sys.modules["floorplan_pipeline"] = types.ModuleType("floorplan_pipeline")
sys.modules["floorplan_pipeline"].RenderInterrupted = type("RenderInterrupted", (BaseException,), {})

import app  # noqa: E402

client = app.app.test_client()

GIGABYTE = 1024 ** 3
BERLIN_NOON = datetime.datetime(2026, 10, 6, 12, 0, tzinfo=app._BERLIN)
# Before 03:00 no retention run is due, so only a test that passes its own time deletes a job.
app._berlin_now = lambda: BERLIN_NOON.replace(hour=2)


def _process_writing_manifest(input_path, out_dir, **options):
    os.makedirs(out_dir)
    with open(os.path.join(out_dir, "manifest.json"), "w", encoding="utf-8") as handle:
        json.dump({"floors": [{"room_count": 4}, {"room_count": 3}]}, handle)


def _process_failing(input_path, out_dir, **options):
    os.makedirs(out_dir)
    raise SystemExit(f"cannot read {input_path}")


def _raise_no_space(*args, **fields):
    raise OSError(28, "No space left on device")


def _reset(free_bytes=10 * GIGABYTE, process=_process_writing_manifest):
    """A test that calls this starts from the same disk and pipeline stub, whatever the test before replaced."""
    app._free_bytes = lambda: free_bytes
    app.pipeline.process = process
    app.MIN_FREE_BYTES = 2 * GIGABYTE


def _wait_for_queue():
    """Queue.join() has no timeout; a dead pipeline thread fails the test here."""
    deadline = time.monotonic() + 30
    while app._QUEUE.unfinished_tasks:
        assert time.monotonic() < deadline, "the pipeline thread did not finish the queue"
        time.sleep(0.005)


def _job_on_volume(job, state, days_old=0, with_input=True, **fields):
    """A job directory as an earlier process left it on the volume."""
    job_dir = os.path.join(app.JOBS_DIR, job)
    os.makedirs(job_dir)
    if with_input:
        with open(os.path.join(job_dir, "input.dxf"), "w", encoding="utf-8") as handle:
            handle.write("0\nEOF\n")
    uploaded_at = datetime.datetime.now() - datetime.timedelta(days=days_old)
    app._write_meta(job, filename=f"{job}.dxf", ext=".dxf", size=6, state=state,
                    uploaded_at=uploaded_at.isoformat(timespec="seconds"), **fields)
    return job_dir


def _job_directories():
    return set(os.listdir(app.JOBS_DIR))


class _LogCapture(logging.Handler):
    def __init__(self):
        super().__init__()
        self.lines = []

    def emit(self, record):
        self.lines.append(record.getMessage())


def test_global_health_is_good_healthy():
    _reset(free_bytes=10 * GIGABYTE)
    response = client.get("/api/floorplan/global-health")
    assert response.status_code == 200
    assert response.get_json() == {"type": "good", "data": {"type": "Healthy"}}


def test_global_health_is_degraded_below_the_free_space_limit():
    _reset(free_bytes=1 * GIGABYTE)
    response = client.get("/api/floorplan/global-health")
    assert response.status_code == 200  # the Docker healthcheck stays green
    assert response.get_json() == {"type": "good", "data": {"type": "Degraded", "freeBytes": GIGABYTE}}


def test_status_is_read_from_meta_json_without_an_upload_in_this_process():
    _job_on_volume("statusdone01", "done", message="Fertig")
    response = client.get("/api/jobs/statusdone01/status")
    assert response.get_json() == {"type": "good", "data": {"state": "done", "message": "Fertig"}}
    assert 'id="floorsel"' in client.get("/j/statusdone01").get_data(as_text=True)  # the viewer


def test_result_page_of_a_failed_job_shows_the_message_from_meta_json():
    _job_on_volume("statuserror1", "error", message="Zeichnung ohne Geometrie")
    response = client.get("/j/statuserror1")
    assert response.status_code == 500
    page = response.get_data(as_text=True)
    assert "Zeichnung ohne Geometrie" in page
    assert "bleibt gespeichert" in page


def test_result_page_of_a_running_job_is_the_progress_page_with_the_filename():
    _job_on_volume("statusrunng1", "done")
    app._write_meta("statusrunng1", state="running")
    page = client.get("/j/statusrunng1").get_data(as_text=True)
    assert "Die Datei <b>statusrunng1.dxf</b> wird verarbeitet" in page
    assert "/api/jobs/statusrunng1/status" in page
    app._write_meta("statusrunng1", state="done")  # a later _resume_interrupted_jobs() would run it


def test_pipeline_thread_outlives_a_failing_queue_entry():
    _reset()
    delete_expired_jobs, app._delete_expired_jobs = app._delete_expired_jobs, _raise_no_space
    app.app.logger.disabled = True  # the expected failure writes a stack trace
    try:
        app._QUEUE.put(datetime.date(9999, 1, 1))  # a retention run no stamp has recorded
        _wait_for_queue()
    finally:
        app._delete_expired_jobs = delete_expired_jobs
        app.app.logger.disabled = False
    response = client.post("/upload", data={"file": (io.BytesIO(b"0\nEOF\n"), "plan.dxf")})
    _wait_for_queue()
    assert app._read_meta(response.headers["Location"].rsplit("/", 1)[1])["state"] == "done"


def test_status_of_a_running_job_carries_the_last_progress_message_of_the_pipeline():
    statuses = []

    def process_reporting_progress(input_path, out_dir, progress, **options):
        job = os.path.basename(os.path.dirname(input_path))
        progress("Erzeuge Vorschaubilder … 3 von 64")
        statuses.append(client.get(f"/api/jobs/{job}/status").get_json()["data"])
        _process_writing_manifest(input_path, out_dir)

    _reset(process=process_reporting_progress)
    client.post("/upload", data={"file": (io.BytesIO(b"0\nEOF\n"), "plan.dxf")})
    _wait_for_queue()
    assert statuses == [{"state": "running", "message": "Erzeuge Vorschaubilder … 3 von 64"}], statuses


def test_interrupted_job_stays_running_and_the_pipeline_thread_stops():
    def process_interrupted(input_path, out_dir, **options):
        os.makedirs(out_dir)
        raise app.pipeline.RenderInterrupted("cannot schedule new futures after interpreter shutdown")

    _wait_for_queue()  # an item left for the shared pipeline thread would stop it with the stub below
    _reset(process=process_interrupted)
    _job_on_volume("interrupted1", "queued", attempts=1)
    _job_on_volume("interrupted2", "queued")
    # A queue of its own: the shared pipeline thread keeps serving the other tests.
    shared_queue, app._QUEUE = app._QUEUE, queue.Queue()
    try:
        app._QUEUE.put("interrupted1")
        app._QUEUE.put("interrupted2")
        worker = threading.Thread(target=app._pipeline_worker, daemon=True)
        worker.start()
        worker.join(timeout=10)
        stopped, left = not worker.is_alive(), app._QUEUE.qsize()
        meta = app._read_meta("interrupted1")
        queued = app._read_meta("interrupted2")["state"]
    finally:
        app._QUEUE = shared_queue
        _reset()
        for job in ("interrupted1", "interrupted2"):
            app._write_meta(job, state="done")  # a later _resume_interrupted_jobs() would run it
    assert (stopped, meta["state"], meta["attempts"], left, queued) == (True, "running", 2, 1, "queued"), (
        stopped, meta, left, queued)


def test_app_imported_by_a_render_worker_starts_no_pipeline_thread():
    pipeline_threads = [thread for thread in threading.enumerate() if thread.name == "pipeline"]
    path = list(sys.path)
    try:
        runpy.run_path(app.__file__, run_name="__mp_main__")
    finally:
        sys.path[:] = path
    assert [thread for thread in threading.enumerate() if thread.name == "pipeline"] == pipeline_threads


def test_import_json_of_a_done_job_carries_the_rooms_and_the_background_of_each_floor_with_rooms():
    job_dir = _job_on_volume("importdone01", "done")
    floor_dir = os.path.join(job_dir, "out", "geschoss-1")
    os.makedirs(floor_dir)
    with open(os.path.join(job_dir, "out", "manifest.json"), "w", encoding="utf-8") as handle:
        json.dump({"room_floor_count": 1, "room_extraction_failed": False,
                   "floors": [{"label": "Geschoss 1", "dir": "geschoss-1", "room_count": 1, "background": "background.png"},
                              {"label": "Ansicht", "dir": "ansicht", "room_count": 0, "background": None}]}, handle)
    with open(os.path.join(floor_dir, "coordinates.json"), "w", encoding="utf-8") as handle:
        json.dump({"rooms": [{"name": "Wohnen", "area_m2": 16.0, "polygon_px": [[20, 20], [100, 20], [100, 100]]}]}, handle)
    with open(os.path.join(floor_dir, "background.png"), "wb") as handle:
        handle.write(b"PNG")

    response = client.get("/j/importdone01/import.json")

    assert response.status_code == 200, response.status_code
    assert response.get_json() == {"floors": [{
        "label": "Geschoss 1",
        "rooms": [{"name": "Wohnen", "statedAreaSquareMeters": 16.0,
                   "bounds": {"vertices": [{"x": 20, "y": 20}, {"x": 100, "y": 20}, {"x": 100, "y": 100}]}}],
        "background": "data:image/png;base64,UE5H",
    }]}, response.get_json()


def _done_job_with_manifest(job, manifest):
    job_dir = _job_on_volume(job, "done")
    os.makedirs(os.path.join(job_dir, "out"))
    with open(os.path.join(job_dir, "out", "manifest.json"), "w", encoding="utf-8") as handle:
        json.dump(manifest, handle)


def test_import_json_of_a_job_from_an_earlier_version_answers_409_and_asks_for_a_new_upload():
    _done_job_with_manifest("importold01", {"floors": [{"label": "Geschoss 1", "dir": "geschoss-1", "room_count": 2}]})

    response = client.get("/j/importold01/import.json")

    assert response.status_code == 409, response.status_code
    assert "neu hochladen" in response.get_json()["error"], response.get_json()


def test_import_json_of_a_job_with_a_failed_floor_answers_409_without_asking_for_a_new_upload():
    _done_job_with_manifest("importfailed01", {"room_floor_count": 2, "room_extraction_failed": False, "floors": [
        {"label": "Geschoss 1", "dir": "geschoss-1", "room_count": 2, "background": None}]})

    response = client.get("/j/importfailed01/import.json")

    assert response.status_code == 409, response.status_code
    assert "nicht zeichnen" in response.get_json()["error"], response.get_json()
    assert "neu hochladen" not in response.get_json()["error"], response.get_json()


def test_import_json_of_a_job_whose_room_extraction_failed_answers_409():
    _done_job_with_manifest("importnoroom01", {"room_floor_count": 0, "room_extraction_failed": True, "floors": []})

    response = client.get("/j/importnoroom01/import.json")

    assert response.status_code == 409, response.status_code
    assert "Raumerkennung" in response.get_json()["error"], response.get_json()


def test_import_json_of_a_running_failed_or_unknown_job_is_not_found():
    _job_on_volume("importrunning01", "running")
    _job_on_volume("importerror01", "error")
    statuses = [client.get(f"/j/{job}/import.json").status_code for job in ("importrunning01", "importerror01", "importunknown01")]
    assert statuses == [404, 404, 404], statuses


def test_result_page_of_a_job_directory_without_meta_json_is_not_found():
    os.makedirs(os.path.join(app.JOBS_DIR, "resultnometa"))
    try:
        assert client.get("/j/resultnometa").status_code == 404
    finally:
        os.rmdir(os.path.join(app.JOBS_DIR, "resultnometa"))


def test_start_removes_a_job_directory_without_meta_json():
    incomplete = os.path.join(app.JOBS_DIR, "incomplete01")
    os.makedirs(incomplete)
    open(os.path.join(incomplete, "input.dxf"), "w").close()
    _job_on_volume("completejob1", "done")
    app._remove_incomplete_uploads()
    remaining = _job_directories()
    assert "incomplete01" not in remaining
    assert "completejob1" in remaining


def test_resume_finishes_a_job_that_was_running_at_restart():
    _reset()
    job_dir = _job_on_volume("resumedone01", "running", attempts=1)
    os.makedirs(os.path.join(job_dir, "out"))
    leftover = os.path.join(job_dir, "out", "half-rendered.png")
    open(leftover, "w").close()
    app._resume_interrupted_jobs()
    assert app._read_meta("resumedone01")["state"] in ("queued", "running", "done")
    _wait_for_queue()
    meta = app._read_meta("resumedone01")
    assert (meta["state"], meta["message"], meta["attempts"]) == ("done", "Fertig", 2)
    assert (meta["floors"], meta["rooms"]) == (2, 7)
    assert meta["started_at"] and meta["finished_at"]
    assert not os.path.exists(leftover)


def test_resume_ends_a_failing_job_with_error_and_its_message():
    _reset(process=_process_failing)
    _job_on_volume("resumefail01", "queued")
    app._resume_interrupted_jobs()
    _wait_for_queue()
    meta = app._read_meta("resumefail01")
    assert meta["state"] == "error"
    assert not os.path.exists(os.path.join(app.JOBS_DIR, "resumefail01", "out"))
    assert meta["message"] == "cannot read resumefail01.dxf"  # the internal path is replaced
    status = client.get("/api/jobs/resumefail01/status").get_json()["data"]
    assert status == {"state": "error", "message": "cannot read resumefail01.dxf"}


def test_resume_ends_a_job_without_input_file_with_error():
    _reset()
    _job_on_volume("resumenoinp1", "running", with_input=False)
    app._resume_interrupted_jobs()
    _wait_for_queue()
    meta = app._read_meta("resumenoinp1")
    assert (meta["state"], meta["message"]) == ("error", "Die hochgeladene Datei fehlt.")


def test_resume_leaves_a_job_open_when_its_state_cannot_be_written():
    _reset()
    _job_on_volume("resumenospc1", "running", attempts=1)
    write_meta, app._write_meta = app._write_meta, _raise_no_space
    app.app.logger.disabled = True  # the expected failure writes a stack trace
    try:
        app._resume_interrupted_jobs()
    finally:
        app._write_meta = write_meta
        app.app.logger.disabled = False
    assert app._read_meta("resumenospc1")["state"] == "running"
    app._resume_interrupted_jobs()  # the next start
    _wait_for_queue()
    assert app._read_meta("resumenospc1")["state"] == "done"


def test_resume_gives_up_after_three_attempts_without_running_the_pipeline():
    calls = []
    _reset(process=lambda *args, **options: calls.append(args))
    job_dir = _job_on_volume("resumelimit1", "running", attempts=3)
    os.makedirs(os.path.join(job_dir, "out"))
    app._resume_interrupted_jobs()
    _wait_for_queue()
    meta = app._read_meta("resumelimit1")
    assert meta["state"] == "error"
    assert meta["message"] == "Verarbeitung nach 3 Versuchen abgebrochen."
    assert not os.path.exists(os.path.join(job_dir, "out"))
    assert calls == []


def test_upload_runs_the_job_and_records_its_state_in_meta_json():
    _reset()
    response = client.post("/upload", data={"file": (io.BytesIO(b"0\nEOF\n"), "plan.dxf")})
    assert response.status_code == 302
    job = response.headers["Location"].rsplit("/", 1)[1]
    assert app._read_meta(job)["state"] in ("queued", "running", "done")
    _wait_for_queue()
    meta = app._read_meta(job)
    assert (meta["state"], meta["filename"], meta["attempts"]) == ("done", "plan.dxf", 1)
    assert meta["started_at"] and meta["finished_at"]


def test_upload_that_fails_while_saving_leaves_no_job_directory():
    _reset()
    before = _job_directories()
    write_meta, app._write_meta = app._write_meta, _raise_no_space
    app.app.logger.disabled = True  # the expected failure writes a stack trace
    try:
        response = client.post("/upload", data={"file": (io.BytesIO(b"0\nEOF\n"), "plan.dxf")})
    finally:
        app._write_meta = write_meta
        app.app.logger.disabled = False
    assert response.status_code == 500
    page = response.get_data(as_text=True)
    assert "bleibt gespeichert" not in page
    assert app.JOBS_DIR not in page
    assert _job_directories() == before


def test_upload_answers_507_below_two_gigabytes_free():
    _reset(free_bytes=1 * GIGABYTE)
    before = _job_directories()
    response = client.post("/upload", data={"file": (io.BytesIO(b"0\nEOF\n"), "plan.dxf")})
    assert response.status_code == 507
    page = response.get_data(as_text=True)
    assert "Der Speicher des Dienstes ist voll; die Datei wurde nicht gespeichert." in page
    assert "bleibt gespeichert" not in page
    assert _job_directories() == before


def test_upload_answers_507_below_three_times_the_upload_size():
    _reset(free_bytes=299)
    app.MIN_FREE_BYTES = 10
    before = _job_directories()
    response = client.post("/upload", data={"file": (io.BytesIO(b"x" * 100), "plan.dxf")})
    assert response.status_code == 507
    assert _job_directories() == before
    _reset(free_bytes=300)
    app.MIN_FREE_BYTES = 10
    assert client.post("/upload", data={"file": (io.BytesIO(b"x" * 100), "plan.dxf")}).status_code == 302
    _wait_for_queue()


def test_retention_deletes_only_finished_uploads_without_feedback_after_180_days():
    _job_on_volume("retainold001", "done", days_old=181)
    uploaded = app._read_meta("retainold001")["uploaded_at"][:10]
    with_feedback = _job_on_volume("retainfeedb1", "done", days_old=181)
    open(os.path.join(with_feedback, "feedback.jsonl"), "w").close()
    _job_on_volume("retainyoung1", "error", days_old=179)
    _job_on_volume("retainlimit1", "error", days_old=180)
    _job_on_volume("retainrunng1", "running", days_old=181)
    capture = _LogCapture()
    app.app.logger.addHandler(capture)
    try:
        app._delete_expired_jobs(datetime.date.today())
    finally:
        app.app.logger.removeHandler(capture)
    remaining = _job_directories()
    assert "retainold001" not in remaining
    assert "retainlimit1" not in remaining
    assert {"retainfeedb1", "retainyoung1", "retainrunng1"} <= remaining
    assert f"retention: deleted job retainold001 (uploaded {uploaded}, no feedback)" in capture.lines
    assert len(capture.lines) == 2
    shutil.rmtree(os.path.join(app.JOBS_DIR, "retainrunng1"))


def test_retention_deletes_the_other_jobs_when_one_job_cannot_be_deleted():
    _job_on_volume("retainfail01", "done", days_old=181)
    _job_on_volume("retainfail02", "done", days_old=181)
    rmtree = shutil.rmtree

    def failing_for_the_first_job(path, **options):
        if path.endswith("retainfail01"):
            raise OSError(16, "Device or resource busy")
        rmtree(path, **options)

    app.app.logger.disabled = True  # the expected failure writes a stack trace
    app.shutil.rmtree = failing_for_the_first_job
    try:
        app._delete_expired_jobs(datetime.date.today())
    finally:
        app.shutil.rmtree = rmtree
        app.app.logger.disabled = False
    remaining = _job_directories()
    assert "retainfail01" in remaining
    assert "retainfail02" not in remaining
    rmtree(os.path.join(app.JOBS_DIR, "retainfail01"))


def test_retention_is_queued_once_per_berlin_day_and_only_after_three():
    _wait_for_queue()
    # The pipeline thread stays blocked on its own queue, so the queued dates can be counted here.
    pipeline_queue, app._QUEUE = app._QUEUE, queue.Queue()
    try:
        if os.path.exists(app.RETENTION_STAMP):
            os.remove(app.RETENTION_STAMP)
        app._retention_queued_for = None
        app._queue_retention_if_due(BERLIN_NOON.replace(hour=2, minute=59))
        assert app._QUEUE.qsize() == 0
        app._queue_retention_if_due(BERLIN_NOON)
        app._queue_retention_if_due(BERLIN_NOON.replace(hour=18))
        assert app._QUEUE.qsize() == 1
        assert app._QUEUE.get() == BERLIN_NOON.date()

        # The run of 2026-10-06 is recorded: a restarted process queues nothing more that day.
        app._run_retention(BERLIN_NOON.date())
        with open(app.RETENTION_STAMP, encoding="utf-8") as handle:
            assert json.load(handle) == {"last_run": "2026-10-06"}
        app._retention_queued_for = None
        app._queue_retention_if_due(BERLIN_NOON.replace(hour=18))
        assert app._QUEUE.qsize() == 0
        app._queue_retention_if_due(BERLIN_NOON + datetime.timedelta(days=1))
        assert app._QUEUE.qsize() == 1
    finally:
        app._QUEUE = pipeline_queue


def test_retention_run_deletes_once_per_berlin_date():
    _job_on_volume("retainstamp1", "done", days_old=181)
    berlin_date = app._deletion_date(app._read_meta("retainstamp1"))
    app._write_json(app.RETENTION_STAMP, {"last_run": berlin_date.isoformat()})
    app._run_retention(berlin_date)
    assert "retainstamp1" in _job_directories()
    berlin_date += datetime.timedelta(days=1)
    app._run_retention(berlin_date)
    assert "retainstamp1" not in _job_directories()
    with open(app.RETENTION_STAMP, encoding="utf-8") as handle:
        assert json.load(handle) == {"last_run": berlin_date.isoformat()}


def test_first_request_after_three_deletes_in_the_pipeline_thread():
    _reset()
    _job_on_volume("retainreqst1", "done", days_old=181)
    if os.path.exists(app.RETENTION_STAMP):
        os.remove(app.RETENTION_STAMP)
    app._retention_queued_for = None
    threads = []
    delete_expired_jobs = app._delete_expired_jobs

    def recording_the_thread(today):
        threads.append(threading.current_thread().name)
        delete_expired_jobs(datetime.date.today())  # the age of the job counts from the real date

    berlin_now, app._berlin_now = app._berlin_now, lambda: BERLIN_NOON
    app._delete_expired_jobs = recording_the_thread
    try:
        client.get("/api/floorplan/global-health")
        client.get("/api/floorplan/global-health")
        _wait_for_queue()
    finally:
        app._berlin_now = berlin_now
        app._delete_expired_jobs = delete_expired_jobs
    assert threads == ["pipeline"]
    assert "retainreqst1" not in _job_directories()


def test_uploads_archive_shows_the_deletion_date():
    _job_on_volume("archivenofb1", "done", days_old=10)
    with_feedback = _job_on_volume("archivefeed1", "done", days_old=10)
    open(os.path.join(with_feedback, "feedback.jsonl"), "w").close()
    page = client.get("/uploads/test-archive-token").get_data(as_text=True)
    uploaded_at = datetime.datetime.fromisoformat(app._read_meta("archivenofb1")["uploaded_at"])
    deletion = (uploaded_at + datetime.timedelta(days=180)).date().isoformat()
    assert "<th>Löschung</th>" in page
    rows = {row.split(".dxf")[0]: row for row in page.split('<tr><td class="fn">')}
    assert f'<td class="muted">{deletion}</td>' in rows["archivenofb1"]
    assert '<td class="muted">bleibt (Feedback)</td>' in rows["archivefeed1"]


def test_meta_tags_carry_deploy_metadata():
    response = client.get("/api/floorplan/meta-tags")
    assert response.status_code == 200
    body = response.get_json()
    assert body["type"] == "good"
    assert body["data"]["imageDigest"] == "sha256:abababababababababababababababababababababababababababababababab"
    assert body["data"]["gitHash"] is None


def test_every_response_carries_the_app_version_header():
    assert client.get("/").headers.get("X-App-Image-Digest") == "sha256:abababababababababababababababababababababababababababababababab"
    assert client.get("/nonexistent").headers.get("X-App-Image-Digest") == "sha256:abababababababababababababababababababababababababababababababab"


def test_an_unpinned_or_absent_digest_counts_as_absent():
    assert app.pinned_image_digest("unpinned") == ""
    assert app.pinned_image_digest("") == ""


def test_a_malformed_digest_fails_the_start():
    try:
        app.pinned_image_digest("sha256:not-hex")
    except ValueError as error:
        assert str(error) == "APP_IMAGE_DIGEST must be sha256:<64 hex characters> or unpinned but was <sha256:not-hex>"
    else:
        raise AssertionError("a malformed digest must fail")


def test_status_of_unknown_job_is_unknown():
    response = client.get("/api/jobs/nonexistent/status")
    assert response.status_code == 200
    assert response.get_json() == {"type": "good", "data": {"state": "unknown", "message": ""}}


def test_feedback_is_stored_and_acknowledged():
    response = client.post("/api/feedback", json={"message": "Testhinweis", "contact": "test@neckar.it"})
    assert response.status_code == 200
    assert response.get_json() == {"type": "good", "data": None}
    with open(app.FEEDBACK_LOG, encoding="utf-8") as handle:
        assert "Testhinweis" in handle.read()


def test_feedback_for_a_job_keeps_the_upload_out_of_retention():
    _job_on_volume("feedbackjob1", "done", days_old=181)
    assert app._deletion_date(app._read_meta("feedbackjob1")) is not None
    response = client.post("/api/feedback", json={"message": "Raum fehlt", "job": "feedbackjob1"})
    assert response.status_code == 200
    assert app._deletion_date(app._read_meta("feedbackjob1")) is None


def test_feedback_with_empty_message_is_bad():
    response = client.post("/api/feedback", json={"message": "   "})
    assert response.status_code == 400
    assert response.get_json() == {"type": "bad/EmptyMessage"}


def test_upload_page_has_no_saved_uploads_link():
    page = client.get("/").get_data(as_text=True)
    assert "Grundriss hochladen" in page
    assert "Gespeicherte Uploads" not in page


def test_unknown_path_renders_not_found_page():
    response = client.get("/api/version")
    assert response.status_code == 404
    page = response.get_data(as_text=True)
    assert "<h1>Nicht gefunden</h1>" in page
    assert "Grundriss hochladen" not in page  # the branded 404 page, not the upload page


def test_uploads_archive_requires_the_url_token():
    assert client.get("/uploads").status_code == 404
    assert client.get("/uploads/wrong-token").status_code == 404
    response = client.get("/uploads/test-archive-token")
    assert response.status_code == 200
    assert "Gespeicherte Uploads" in response.get_data(as_text=True)


def test_upload_rejects_unsupported_extension():
    _reset()
    response = client.post("/upload", data={"file": (io.BytesIO(b"x"), "plan.txt")})
    assert response.status_code == 400
    assert "Nur .dwg oder .dxf" in response.get_data(as_text=True)


def main():
    tests = [v for k, v in sorted(globals().items()) if k.startswith("test_")]
    failed = 0
    for t in tests:
        try:
            t()
            print(f"PASS {t.__name__}")
        # report any failure cleanly; a leaked RenderInterrupted is a BaseException
        except (Exception, app.pipeline.RenderInterrupted) as exc:  # noqa: BLE001
            failed += 1
            print(f"FAIL {t.__name__}: {exc!r}")
    print(f"\n{len(tests) - failed}/{len(tests)} passed")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
