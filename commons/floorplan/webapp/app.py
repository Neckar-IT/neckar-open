#!/usr/bin/env python3
"""Floor-plan Viewer — upload a DWG/DXF, get an interactive per-floor plan viewer in the browser.

Thin Flask product around ../tools/floorplan_pipeline.py. Each upload becomes a job that runs the full
pipeline (DWG->DXF, room extraction, per-floor layer rendering in render workers) from the one pipeline
thread, so the browser never blocks: the upload returns immediately, a progress page polls the job status,
and once done the same URL serves the viewer. The job state lives in jobs/<job>/meta.json, so a restart
resumes open jobs. The viewer reads the pipeline's manifest.json and composes the transparent per-layer PNGs
live (pan/zoom, per-layer colour/visibility, room overlay + list) — no per-floor HTML.

Run:  ../tools/.venv/bin/python app.py         (venv: ezdxf + matplotlib + pillow + flask)
Prereq: the room-extraction jar (floorplan_pipeline builds room detection on it) and `java` on PATH;
        DWG input additionally needs ODAFileConverter (+ xvfb). See README.md.
"""
import datetime
import hashlib
import hmac
import json
import logging
import os
import queue
import re
import shutil
import sys
import threading
import uuid
import zoneinfo

from flask import Flask, abort, jsonify, redirect, request, send_file, send_from_directory, url_for

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.abspath(os.path.join(HERE, "..", "tools")))
import floorplan_import  # noqa: E402
import floorplan_pipeline as pipeline  # noqa: E402

# Uploaded files, render output and feedback are the actual value — kept under a configurable data
# directory so a container can mount a persistent volume there ($FLOORPLAN_DATA_DIR=/data).
DATA_DIR = os.environ.get("FLOORPLAN_DATA_DIR", HERE)
JOBS_DIR = os.path.join(DATA_DIR, "jobs")
os.makedirs(JOBS_DIR, exist_ok=True)
FEEDBACK_LOG = os.path.join(DATA_DIR, "feedback.jsonl")  # global append-only feedback log
ALLOWED_EXT = {".dwg", ".dxf"}

def pinned_image_digest(injected):
    """The injected digest; empty when absent or `unpinned` (a deploy without a pinned digest).

    Any other value that is not sha256 plus 64 hex characters is a broken deploy and fails the start, like a Kotlin service."""
    if injected in ("", "unpinned"):
        return ""
    if re.fullmatch(r"sha256:[a-f0-9]{64}", injected) is None:
        raise ValueError(f"APP_IMAGE_DIGEST must be sha256:<64 hex characters> or unpinned but was <{injected}>")
    return injected


# The pinned image digest from the compose file, for deploy-check (X-App-Image-Digest, /api/floorplan/meta-tags);
# empty outside a pinned deploy, e.g. `python app.py` locally.
APP_IMAGE_DIGEST = pinned_image_digest(os.environ.get("APP_IMAGE_DIGEST", ""))

# The uploads archive is deliberately unlinked (deep link only, listed in mea.neckar.it): the
# service is public, uploads are customer floor plans, and per-job URLs are unguessable — the
# archive is the only enumeration point, so it sits behind this URL token. Fail-closed: without
# the env var (e.g. local `python app.py`) the archive is unreachable; set any value locally.
ARCHIVE_TOKEN = os.environ.get("FLOORPLAN_ARCHIVE_TOKEN", "")

MIN_FREE_BYTES = 2 * 1024 ** 3  # below this free space uploads are refused and global-health is Degraded
RETENTION_DAYS = 180  # an upload without feedback is deleted this long after uploaded_at
RETENTION_HOUR = 3  # Europe/Berlin; the first request from this hour on queues the retention run of the day
# gunicorn respawns a worker the job killed (out of memory); without the limit every respawn re-queues it.
MAX_ATTEMPTS = 3
RETENTION_STAMP = os.path.join(DATA_DIR, "retention.json")  # {"last_run": "2026-10-06"}, a Berlin date
_BERLIN = zoneinfo.ZoneInfo("Europe/Berlin")

_MESSAGE_QUEUED = "In Warteschlange …"
_MESSAGE_RUNNING = "Verarbeitung startet …"

_META_LOCK = threading.Lock()  # one read-modify-write of a meta.json at a time
# Job ids (str) and Berlin dates (datetime.date, one retention run each), consumed by the one pipeline thread.
_QUEUE = queue.Queue()
_retention_queued_for = None  # Berlin date whose retention run is already queued or done

app = Flask(__name__)
app.config["MAX_CONTENT_LENGTH"] = 300 * 1024 * 1024  # 300 MB upload cap
app.logger.setLevel(logging.INFO)  # the default level WARNING drops the retention log lines


# ----------------------------------------------------------------------------- persistence

def _now_iso():
    return datetime.datetime.now().isoformat(timespec="seconds")


def _meta_path(job):
    return os.path.join(JOBS_DIR, job, "meta.json")


def _read_meta(job):
    """The per-upload record and the only source of the job state; None for a job without meta.json."""
    try:
        with open(_meta_path(job), encoding="utf-8") as handle:
            return json.load(handle)
    except (OSError, ValueError):
        return None


def _write_json(path, data):
    """The replace is atomic, so a reader never sees a half-written file."""
    temporary_path = path + ".tmp"
    with open(temporary_path, "w", encoding="utf-8") as handle:
        json.dump(data, handle, ensure_ascii=False, indent=2)
    os.replace(temporary_path, path)


def _write_meta(job, **fields):
    with _META_LOCK:
        meta = _read_meta(job) or {}
        meta["job"] = job
        meta.update(fields)
        _write_json(_meta_path(job), meta)
    return meta


def _free_bytes():
    return shutil.disk_usage(DATA_DIR).free


def _sha256(path):
    digest = hashlib.sha256()
    with open(path, "rb") as handle:
        for chunk in iter(lambda: handle.read(1 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _manifest_counts(job):
    """(floor_count, room_count) from a finished job's manifest, or (0, 0)."""
    try:
        with open(os.path.join(JOBS_DIR, job, "out", "manifest.json"), encoding="utf-8") as handle:
            manifest = json.load(handle)
        return len(manifest["floors"]), sum(floor.get("room_count", 0) for floor in manifest["floors"])
    except (OSError, ValueError, KeyError):
        return 0, 0


# ----------------------------------------------------------------------------- job execution

def _run_job(job):
    meta = _read_meta(job)
    if meta is None:
        return
    job_dir = os.path.join(JOBS_DIR, job)
    input_path = os.path.join(job_dir, "input" + meta.get("ext", ""))
    out_dir = os.path.join(job_dir, "out")
    try:
        if not os.path.exists(input_path):
            raise FileNotFoundError("Die hochgeladene Datei fehlt.")
        shutil.rmtree(out_dir, ignore_errors=True)  # leftover of a run the last restart interrupted
        _write_meta(job, state="running", message=_MESSAGE_RUNNING, started_at=_now_iso(),
                    attempts=meta.get("attempts", 0) + 1)
        pipeline.process(input_path, out_dir, px_per_m=200, margin_m=1.0, want_preview=True,
                         progress=lambda message: _write_meta(job, message=message))
        floors, rooms = _manifest_counts(job)
        _write_meta(job, state="done", message="Fertig", floors=floors, rooms=rooms, finished_at=_now_iso())
    except pipeline.RenderInterrupted:
        raise  # the job stays running, and the next start resumes it
    except BaseException as error:  # SystemExit (pipeline aborts) included
        message = str(error) or error.__class__.__name__
        # Don't leak internal absolute paths to the user; show the original filename instead.
        filename = meta.get("filename", "die Datei")
        message = message.replace(input_path, filename).replace(job_dir, filename)
        shutil.rmtree(out_dir, ignore_errors=True)  # a partial render serves no viewer
        _write_meta(job, state="error", message=message[:500], finished_at=_now_iso())


def _remove_incomplete_uploads():
    """Removes every job directory without meta.json: an upload the last process did not finish saving."""
    for job in os.listdir(JOBS_DIR):
        if not os.path.exists(_meta_path(job)):
            shutil.rmtree(os.path.join(JOBS_DIR, job), ignore_errors=True)


def _resume_interrupted_jobs():
    """Re-queues every job a restart left in state queued or running, oldest upload first.

    A job that already took MAX_ATTEMPTS runs ends with state error."""
    metas = [meta for meta in (_read_meta(job) for job in os.listdir(JOBS_DIR))
             if meta and meta.get("state") in ("queued", "running")]
    for meta in sorted(metas, key=lambda meta: meta.get("uploaded_at", "")):
        job = meta["job"]
        try:
            if meta.get("attempts", 0) >= MAX_ATTEMPTS:
                shutil.rmtree(os.path.join(JOBS_DIR, job, "out"), ignore_errors=True)
                _write_meta(job, state="error", finished_at=_now_iso(),
                            message=f"Verarbeitung nach {MAX_ATTEMPTS} Versuchen abgebrochen.")
                continue
            _write_meta(job, state="queued", message=_MESSAGE_QUEUED)
        except OSError:  # a full volume must not keep the process from starting; the next start retries
            app.logger.exception("resume: could not record the state of job %s", job)
            continue
        _QUEUE.put(job)


def _pipeline_worker():
    while True:
        item = _QUEUE.get()
        try:
            if isinstance(item, datetime.date):
                _run_retention(item)
            else:
                _run_job(item)
        except pipeline.RenderInterrupted:
            return  # the interpreter exits; the queued jobs stay queued for the next start
        except Exception:  # the one pipeline thread outlives any failing item
            app.logger.exception("pipeline worker: item failed")
        finally:
            _QUEUE.task_done()


# ----------------------------------------------------------------------------- retention

def _has_feedback(job):
    return os.path.exists(os.path.join(JOBS_DIR, job, "feedback.jsonl"))


def _deletion_date(meta):
    """The date whose retention run deletes the upload; None for an upload that is kept: one with feedback or
    without a readable uploaded_at."""
    if _has_feedback(meta.get("job", "")):
        return None
    try:
        uploaded_at = datetime.datetime.fromisoformat(meta.get("uploaded_at", ""))
    except (TypeError, ValueError):
        return None
    return uploaded_at.date() + datetime.timedelta(days=RETENTION_DAYS)


def _delete_expired_jobs(today):
    for job in os.listdir(JOBS_DIR):
        meta = _read_meta(job)
        if not meta or meta.get("state") not in ("done", "error"):
            continue
        deletion_date = _deletion_date(meta)
        if deletion_date is None or deletion_date > today:
            continue
        try:
            shutil.rmtree(os.path.join(JOBS_DIR, job))
        except OSError:  # the next run retries this job; the other jobs are still deleted
            app.logger.exception("retention: could not delete job %s", job)
            continue
        app.logger.info("retention: deleted job %s (uploaded %s, no feedback)", job, meta["uploaded_at"][:10])


def _berlin_now():
    return datetime.datetime.now(_BERLIN)


def _last_retention_run():
    try:
        with open(RETENTION_STAMP, encoding="utf-8") as handle:
            return json.load(handle).get("last_run", "")
    except (OSError, ValueError, AttributeError):
        return ""


def _retention_recorded(berlin_date):
    return _last_retention_run() >= berlin_date.isoformat()


def _run_retention(berlin_date):
    """Deletes the expired jobs once per Berlin date; a second queue entry of that date finds it recorded."""
    if _retention_recorded(berlin_date):
        return
    _delete_expired_jobs(berlin_date)
    _write_json(RETENTION_STAMP, {"last_run": berlin_date.isoformat()})


def _queue_retention_if_due(berlin_now=None):
    """Queues one retention run per Berlin date; the pipeline thread does the deleting."""
    global _retention_queued_for
    berlin_now = berlin_now or _berlin_now()
    today = berlin_now.date()
    if _retention_queued_for == today or berlin_now.hour < RETENTION_HOUR:
        return
    _retention_queued_for = today
    if not _retention_recorded(today):
        _QUEUE.put(today)


# ----------------------------------------------------------------------------- templates

_HEAD = """<!doctype html><html lang="de"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>__TITLE__ · Floor-plan Viewer</title>
<style>
 :root{
   --ink:#1f4e5f; --ink-2:#2c6f85; --accent:#bf5a1e;
   --bg:#eae7df; --surface:#fff; --panel:#f7f5f0; --line:#e2ddd1;
   --text:#26251f; --muted:#8b867a;
   --shadow:0 1px 3px #0000001a,0 8px 24px #00000012;
   --radius:14px;
 }
 *{box-sizing:border-box}
 html,body{height:100%}
 body{margin:0;font:15px/1.55 ui-sans-serif,system-ui,-apple-system,"Segoe UI",Roboto,sans-serif;
   color:var(--text);background:var(--bg);-webkit-font-smoothing:antialiased}
 a{color:var(--ink)}
 .brand{display:flex;align-items:center;gap:10px;font-weight:650;color:var(--ink);letter-spacing:.2px}
 .brand .mark{width:26px;height:26px;border-radius:7px;background:linear-gradient(135deg,var(--ink),var(--ink-2));
   display:grid;place-items:center;color:#fff;font-size:15px}
 .btn{display:inline-flex;align-items:center;gap:7px;padding:10px 18px;border-radius:10px;border:0;cursor:pointer;
   background:var(--ink);color:#fff;font:inherit;font-weight:600;box-shadow:var(--shadow);transition:filter .15s,transform .05s}
 .btn:hover{filter:brightness(1.07)} .btn:active{transform:translateY(1px)}
 .btn.ghost{background:var(--surface);color:var(--ink);border:1px solid var(--line);box-shadow:none;font-weight:550}
 .btn.ghost:hover{background:var(--panel)}
 .btn.warn{background:var(--accent);color:#fff;box-shadow:0 1px 3px #bf5a1e40}
 .btn.warn:hover{filter:brightness(1.06)}
 .btn.sm{padding:8px 13px;font-size:13px;border-radius:9px;box-shadow:none}

 /* ---- centered pages (upload / progress / error) ---- */
 .center{min-height:100%;display:flex;flex-direction:column}
 .topnav{display:flex;align-items:center;justify-content:space-between;padding:18px 26px}
 .stagearea{flex:1;display:flex;align-items:center;justify-content:center;padding:24px}
 .card{width:min(560px,94vw);background:var(--surface);border:1px solid var(--line);border-radius:var(--radius);
   box-shadow:var(--shadow);padding:34px}
 .card h1{font-size:23px;margin:0 0 6px;color:var(--ink)}
 .card .sub{color:var(--muted);margin:0 0 22px}
 .drop{border:2px dashed #cdc6b8;border-radius:12px;background:var(--panel);padding:40px 20px;text-align:center;
   cursor:pointer;transition:border-color .15s,background .15s;display:block}
 .drop.over{border-color:var(--ink);background:#fbfaf6}
 .drop .big{font-size:17px;font-weight:600;color:var(--text)}
 .drop input{display:none} .drop .hint{color:var(--muted);font-size:13px;margin-top:6px}
 .drop .fname{margin-top:12px;font-size:13px;color:var(--ink);font-weight:600;min-height:18px}
 .actions{margin-top:20px;display:flex;gap:10px;align-items:center}
 .foot{color:var(--muted);font-size:12.5px;text-align:center;padding:14px}
 .err{background:#fff4f2;border:1px solid #e7bfb2;border-radius:12px;padding:16px 18px;color:#8f3115}

 /* progress spinner */
 .spin{width:44px;height:44px;border-radius:50%;border:4px solid #e7e2d6;border-top-color:var(--ink);
   animation:sp 0.9s linear infinite;margin:6px auto 18px}
 @keyframes sp{to{transform:rotate(360deg)}}
 .prog{text-align:center}
 .prog .msg{font-weight:600;font-size:16px} .prog .elapsed{color:var(--muted);font-size:13px;margin-top:4px}
 .bar{height:6px;border-radius:6px;background:#e7e2d6;overflow:hidden;margin-top:18px}
 .bar > i{display:block;height:100%;width:35%;border-radius:6px;background:linear-gradient(90deg,var(--ink),var(--ink-2));
   animation:slide 1.4s ease-in-out infinite}
 @keyframes slide{0%{margin-left:-35%}100%{margin-left:100%}}

 /* form fields (feedback form + modal) */
 textarea,input.fld{width:100%;padding:10px 12px;border:1px solid var(--line);border-radius:9px;font:inherit;
   background:var(--surface);color:var(--text);margin-top:10px}
 textarea{min-height:110px;resize:vertical}
 textarea:focus,input.fld:focus{outline:2px solid #1f4e5f33;border-color:var(--ink)}
 .ok{background:#eef6f0;border:1px solid #bcd9c6;border-radius:12px;padding:16px 18px;color:#1f5133}
</style></head><body>"""

_UPLOAD = """
<div class="center">
 <div class="topnav"><span class="brand"><span class="mark">▤</span> Floor-plan Viewer</span></div>
 <div class="stagearea"><div class="card">
  <h1>Grundriss hochladen</h1>
  <p class="sub">Lade ein DWG oder DXF hoch und erhalte pro Geschoss einen interaktiven Grundriss mit
   erkannten Räumen und einzeln umschaltbaren Layern.</p>
  <form id="f" method="post" action="/upload" enctype="multipart/form-data">
   <label class="drop" id="drop">
    <input type="file" name="file" id="file" accept=".dwg,.dxf" required>
    <div class="big">Datei hierher ziehen oder klicken</div>
    <div class="hint">.dwg oder .dxf · bis 300 MB</div>
    <div class="fname" id="fname"></div>
   </label>
   <div class="actions"><button class="btn" type="submit" id="go">Verarbeiten</button>
    <span class="hint" style="color:var(--muted)">Große Pläne brauchen ~1 Minute.</span></div>
  </form>
 </div></div>
 <div class="foot">DWG/DXF → Raumerkennung → Grundriss-Rendering pro Geschoss</div>
</div>
<script>
 const d=drop,i=file,n=fname,f=document.getElementById('f');
 i.onchange=()=>{n.textContent=i.files[0]?'✓ '+i.files[0].name:''};
 ['dragover','dragenter'].forEach(e=>d.addEventListener(e,ev=>{ev.preventDefault();d.classList.add('over')}));
 ['dragleave','drop'].forEach(e=>d.addEventListener(e,ev=>{ev.preventDefault();d.classList.remove('over')}));
 d.addEventListener('drop',ev=>{i.files=ev.dataTransfer.files;n.textContent=i.files[0]?'✓ '+i.files[0].name:''});
 f.addEventListener('submit',()=>{go.textContent='Lade hoch …';go.disabled=true});
</script>"""

_PROGRESS = """
<div class="center">
 <div class="topnav"><span class="brand"><span class="mark">▤</span> Floor-plan Viewer</span></div>
 <div class="stagearea"><div class="card prog">
  <div class="spin"></div>
  <div class="msg" id="msg">Verarbeite …</div>
  <div class="elapsed" id="elapsed"></div>
  <div class="bar"><i></i></div>
  <p class="sub" style="margin-top:20px">Die Datei <b>__INPUT__</b> wird verarbeitet: Raumerkennung und
   Rendering aller Geschosse. Diese Seite aktualisiert sich selbst.</p>
 </div></div>
</div>
<script>
 const t0=Date.now();
 setInterval(()=>{elapsed.textContent=Math.round((Date.now()-t0)/1000)+' s';},1000);
 async function poll(){
  try{
   const r=await fetch('/api/jobs/__JOB__/status'); const s=(await r.json()).data;
   if(s.state==='done'){location.reload();return;}
   if(s.state==='error'){document.querySelector('.prog').innerHTML=
     '<div class="err"><b>Verarbeitung fehlgeschlagen</b><br>'+ (s.message||'') +
     '</div><div class="actions" style="justify-content:center"><a class="btn ghost" href="/">← neue Datei</a></div>';return;}
   if(s.message) msg.textContent=s.message;
  }catch(e){}
  setTimeout(poll,1500);
 }
 poll();
</script>"""

_ERROR = """
<div class="center">
 <div class="topnav"><span class="brand"><span class="mark">▤</span> Floor-plan Viewer</span></div>
 <div class="stagearea"><div class="card">
  <h1>__H1__</h1>
  <div class="err">__MSG__</div>
  <p class="sub" style="margin-top:16px">__NOTE__</p>
  <div class="actions"><a class="btn" href="/">← neue Datei</a>
   <a class="btn ghost" href="/feedback-form?job=__JOB__">⚑ Problem melden</a></div>
 </div></div>
</div>"""

_ARCHIVE_HEAD = """
<style>
 .tbl{width:100%;border-collapse:collapse;background:var(--surface);border:1px solid var(--line);
   border-radius:12px;overflow:hidden;box-shadow:var(--shadow)}
 .tbl th{text-align:left;font-size:12px;text-transform:uppercase;letter-spacing:.5px;color:var(--muted);
   padding:11px 14px;border-bottom:1px solid var(--line);background:var(--panel)}
 .tbl td{padding:11px 14px;border-bottom:1px solid var(--line);font-size:14px;vertical-align:middle}
 .tbl tr:last-child td{border-bottom:0}
 .tbl td.fn{font-weight:600;color:var(--text)} .tbl td.num{font-variant-numeric:tabular-nums;color:var(--muted)}
 .tbl td.muted{color:var(--muted);font-size:13px} .tbl td.act{white-space:nowrap}
 .badge{display:inline-block;margin-left:6px;padding:1px 7px;border-radius:20px;background:#fbe9de;
   color:var(--accent);font-size:12px;font-weight:600}
</style>
<div class="center">
 <div class="topnav">
  <a class="brand" href="/" style="text-decoration:none"><span class="mark">▤</span> Floor-plan Viewer</a>
  <a class="btn" href="/">Neue Datei</a>
 </div>
 <div class="stagearea" style="align-items:flex-start;padding-top:10px">
  <div style="width:min(940px,94vw)">
   <h1 style="color:var(--ink);font-size:22px;margin:8px 0 4px">Gespeicherte Uploads</h1>
   <p class="muted" style="margin:0 0 18px">Ein Upload ohne Hinweis wird __RETENTION_DAYS__ Tage nach dem
    Hochladen gelöscht, ein Upload mit Hinweis (⚑) bleibt.</p>
"""

_FEEDBACK_FORM = """
<div class="center">
 <div class="topnav"><a class="brand" href="/" style="text-decoration:none"><span class="mark">▤</span> Floor-plan Viewer</a></div>
 <div class="stagearea"><div class="card">
  <h1>Problem oder Hinweis melden</h1>
  <p class="sub">__CTX__</p>
  <div id="form">
   <textarea id="msg" placeholder="Was ist falsch, fehlt oder fällt auf?"></textarea>
   <input class="fld" id="contact" placeholder="E-Mail für Rückfragen (optional)">
   <div class="actions"><button class="btn" id="send">Senden</button><a class="btn ghost" href="/">Zurück</a></div>
  </div>
  <div id="done" class="ok" style="display:none">Danke! Deine Meldung ist gespeichert.</div>
 </div></div>
</div>
<script>
 const JOB="__JOB__";
 send.onclick=async()=>{ const m=msg.value.trim(); if(!m){msg.focus();return;} send.disabled=true;
  try{ await fetch('/api/feedback',{method:'POST',headers:{'Content-Type':'application/json'},
    body:JSON.stringify({job:JOB||null,message:m,contact:contact.value})}); }catch(e){}
  document.getElementById('form').style.display='none'; document.getElementById('done').style.display='block';
 };
</script>"""


def _shell(title, body):
    return _HEAD.replace("__TITLE__", title) + body + "</body></html>"


# ----------------------------------------------------------------------------- viewer (SPA)

_VIEWER = """
<div id="app" class="app">
 <header class="vtop">
  <span class="brand"><span class="mark">▤</span> Floor-plan Viewer</span>
  <div class="fileinfo" id="fileinfo"></div>
  <label class="floorpick"><span class="fplabel">Geschoss</span><select id="floorsel"></select></label>
  <div class="vactions">
   <a class="btn ghost sm" id="dlOriginal" title="Original-Datei herunterladen">↓ Original</a>
   <button class="btn warn sm" id="fbBtn">⚑ Problem melden</button>
   <a class="btn sm" href="/">＋ Neue Datei</a>
  </div>
 </header>
 <aside class="vpanel" id="vpanel"></aside>
 <div class="modal" id="fbModal" hidden>
  <div class="sheet">
   <h3 style="margin:0 0 4px;color:var(--ink)">Problem oder Hinweis melden</h3>
   <p class="muted" id="fbCtx" style="margin:0 0 6px;font-size:13px"></p>
   <div id="fbForm">
    <textarea id="fbMsg" placeholder="Was ist falsch, fehlt oder fällt auf? (z. B. Raum nicht erkannt, Geschoss falsch geteilt)"></textarea>
    <input class="fld" id="fbContact" placeholder="E-Mail für Rückfragen (optional)">
    <div class="mrow"><button class="btn ghost" id="fbCancel">Abbrechen</button><button class="btn" id="fbSend">Senden</button></div>
   </div>
   <div id="fbDone" class="ok" hidden>Danke! Deine Meldung ist gespeichert.</div>
  </div>
 </div>
 <main class="vstage" id="vstage">
  <div class="canvaswrap" id="canvas"></div>
  <div class="zoom">
   <button id="zin" title="Vergrößern">+</button>
   <button id="zout" title="Verkleinern">−</button>
   <button id="zfit" title="Einpassen">⤢</button>
  </div>
 </main>
</div>
<style>
 body{overflow:hidden}
 .app{height:100vh;display:grid;grid-template-columns:320px 1fr;grid-template-rows:58px 1fr;
   grid-template-areas:"top top" "panel stage"}
 .vtop{grid-area:top;display:flex;align-items:center;gap:14px;padding:0 16px;background:var(--surface);
   border-bottom:1px solid var(--line);z-index:5;min-width:0}
 .vtop .brand{flex:none}
 .vtop .fileinfo{flex:none;max-width:190px;color:var(--muted);font-size:13px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
 .floorpick{flex:1 1 auto;min-width:0;display:flex;align-items:center;gap:9px}
 .fplabel{flex:none;font-size:12px;text-transform:uppercase;letter-spacing:.5px;color:var(--muted)}
 #floorsel{max-width:280px;min-width:0;padding:8px 12px;border-radius:9px;border:1px solid var(--line);
   background:var(--surface);color:var(--text);font:inherit;font-weight:600;cursor:pointer}
 #floorsel:hover{border-color:var(--ink)}
 .vactions{flex:none;display:flex;gap:8px;align-items:center}
 .vpanel{grid-area:panel;background:var(--panel);border-right:1px solid var(--line);overflow:auto;padding:16px}
 .sec{margin-bottom:20px}
 .sec h3{font-size:12px;text-transform:uppercase;letter-spacing:.6px;color:var(--muted);margin:0 0 10px}
 .phint{font-size:11.5px;color:var(--muted);margin:-4px 0 8px;line-height:1.4}
 .quick{display:grid;grid-template-columns:1fr 1fr;gap:6px}
 .quick button{padding:8px 10px;border-radius:8px;border:1px solid var(--line);background:var(--surface);
   cursor:pointer;font:inherit;font-size:13px;text-align:center}
 .quick button:hover{border-color:var(--ink);color:var(--ink)}
 .toggle{display:flex;align-items:center;gap:8px;font-size:14px;cursor:pointer;user-select:none}
 .toggle input{accent-color:var(--ink);width:16px;height:16px}
 .layers{display:flex;flex-direction:column;gap:1px}
 .lfilter{width:100%;box-sizing:border-box;margin:0 0 8px;padding:7px 10px;border-radius:8px;border:1px solid var(--line);
   background:var(--surface);color:var(--text);font:inherit;font-size:13px}
 .lrow[hidden]{display:none}
 .lrow{display:grid;grid-template-columns:16px minmax(0,1fr) auto 58px;align-items:center;gap:9px;
   padding:6px 6px;border-radius:8px}
 .lrow:hover{background:#fff}
 .lvis{width:16px;height:16px;accent-color:var(--ink);cursor:pointer}
 .lname{font-size:13px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
 .lrow.off .lname{color:#bdb8ab}
 .sw{display:flex;gap:5px}
 .lend{display:flex;align-items:center;justify-content:flex-end;gap:6px}
 .chip{width:17px;height:17px;padding:0;border-radius:50%;border:1px solid #00000022;cursor:pointer;transition:transform .1s}
 .chip:hover{transform:scale(1.15)}
 .chip-original{background:conic-gradient(from 210deg,#e34a4a,#e6a52e,#3ba776,#3f7fd0,#8a55c8,#e34a4a)}
 .chip-teal{background:#1f4e5f} .chip-gray{background:#c4c8ca} .chip-black{background:#1a1a1a}
 .chip.sel{box-shadow:0 0 0 2px var(--surface),0 0 0 3px var(--ink)}
 .lrow.off .chip{opacity:.35}
 .aa{flex:none;font-size:12px;font-weight:700;font-style:italic;color:var(--muted);width:15px;text-align:center}
 .swch{position:relative;display:inline-block;width:36px;height:21px;flex:none}
 .swch input{position:absolute;opacity:0;width:0;height:0;margin:0}
 .swch .tk{position:absolute;inset:0;background:#d4cebf;border-radius:20px;cursor:pointer;transition:background .15s}
 .swch .tk::before{content:"";position:absolute;width:17px;height:17px;left:2px;top:2px;background:#fff;
   border-radius:50%;box-shadow:0 1px 2px #0004;transition:transform .15s}
 .swch input:checked + .tk{background:var(--ink)}
 .swch input:checked + .tk::before{transform:translateX(15px)}
 .swch input:disabled + .tk{opacity:.4;cursor:default}
 .swrow{display:flex;align-items:center;gap:10px;font-size:14px;cursor:pointer;user-select:none;padding:3px 0}
 .swrow.dim{opacity:.55}
 .rooms{display:flex;flex-direction:column;gap:2px}
 .rroom{display:flex;justify-content:space-between;gap:8px;padding:6px 8px;border-radius:8px;cursor:pointer;font-size:13px}
 .rroom:hover{background:#fff} .rroom .ra{color:var(--muted);font-variant-numeric:tabular-nums}
 .vstage{grid-area:stage;position:relative;overflow:hidden;background:
   linear-gradient(#0000 0 0) ,#f1efe9;touch-action:none;cursor:grab}
 .vstage.drag{cursor:grabbing}
 .vstage::before{content:"";position:absolute;inset:0;background-image:
   radial-gradient(#d9d5cb 1px,transparent 1px);background-size:22px 22px;opacity:.5}
 .canvaswrap{position:absolute;top:0;left:0;transform-origin:0 0;will-change:transform;
   box-shadow:0 6px 30px #0002;background:#fff}
 .canvaswrap img{position:absolute;inset:0;width:100%;height:100%;pointer-events:none}
 .zoom{position:absolute;right:16px;bottom:16px;display:flex;flex-direction:column;gap:6px;z-index:4}
 .zoom button{width:38px;height:38px;border-radius:10px;border:1px solid var(--line);background:var(--surface);
   box-shadow:var(--shadow);cursor:pointer;font-size:18px;color:var(--ink)}
 .zoom button:hover{background:var(--panel)}
 .empty{position:absolute;inset:0;display:grid;place-items:center;color:var(--muted)}
 .modal{position:fixed;inset:0;background:#0007;display:flex;align-items:center;justify-content:center;z-index:50}
 .modal[hidden]{display:none}
 .sheet{background:var(--surface);border-radius:16px;box-shadow:var(--shadow);padding:24px;width:min(460px,92vw)}
 .mrow{display:flex;gap:10px;justify-content:flex-end;margin-top:14px}
</style>
<script>
const JOB="__JOB__";
const COLORS=[["original"],["teal"],["gray"],["black"]];
const COLOR_LABEL={original:"Originalfarben",teal:"Türkis",gray:"Grau",black:"Schwarz"};
let M=null, floor=null, imgs={}, roomsImg=null, coords=null;
let scale=1, tx=0, ty=0;
const stage=document.getElementById('vstage'), canvas=document.getElementById('canvas');

function showEmpty(msg){ const s=document.getElementById('vstage');
  s.innerHTML='<div class="empty">'+msg+'</div>'; }
fetch(`/j/${JOB}/manifest.json`).then(r=>{ if(!r.ok) throw 0; return r.json(); }).then(m=>{
  M=m;
  if(!m || !Array.isArray(m.floors) || !m.floors.length){ showEmpty('Keine darstellbaren Geschosse in dieser Datei.'); return; }
  fileinfo.textContent = `${m.input||''} · ${m.floors.length} Geschoss${m.floors.length>1?'e':''} · ${m.source_unit||''}`;
  const sel=document.getElementById('floorsel');
  m.floors.forEach((fl,i)=>{ const o=document.createElement('option'); o.value=i;
    o.textContent=fl.label||('Geschoss '+(i+1)); sel.appendChild(o); });
  sel.onchange=()=>selectFloor(+sel.value);
  selectFloor(0);
}).catch(()=>showEmpty('Das Ergebnis konnte nicht geladen werden.'));

function selectFloor(i){
  floor=M.floors[i];
  const sel=document.getElementById('floorsel'); if(sel) sel.value=i;
  buildCanvas(); buildPanel();
  coords=null;
  fetch(`/j/${JOB}/${floor.dir}/coordinates.json`).then(r=>r.ok?r.json():null).then(c=>{coords=c;buildRooms();}).catch(()=>{});
}

// A row that is switched on again shows the colour it had before it was switched off.
function setVisible(L,show){
  if(show){ L._color=L._color||L._lastColor; } else if(L._color){ L._lastColor=L._color; L._color=null; }
}
function layerBase(L){ return `/j/${JOB}/${floor.dir}/${L.dir}`; }
function paint(L){
  const img=imgs[L.dir]; if(!img) return;
  if(!L._color){ img.style.display='none'; return; }
  let v=L._color;
  if(L._text){ const nt=L._color+'-notext';
    if(L.variants.includes(nt)) v=nt; else if(L.only_text){ img.style.display='none'; return; } }
  img.src=layerBase(L)+'/'+v+'.png'; img.style.display='block';
}

function buildCanvas(){
  canvas.innerHTML=''; imgs={}; roomsImg=null;
  const sz=Array.isArray(floor.image_size_px)?floor.image_size_px:[1000,1000];
  const w=sz[0]||1000, h=sz[1]||1000;
  canvas.style.width=w+'px'; canvas.style.height=h+'px';
  (floor.layers||[]).forEach(L=>{
    L._color=L.initially_visible===false?null:'original'; L._text=false;
    L._lastColor='original';
    const img=new Image(); img.onerror=()=>{img.style.display='none';};
    img.style.display='none'; canvas.appendChild(img); imgs[L.dir]=img; paint(L);
  });
  if(floor.rooms_overlay){
    roomsImg=new Image(); roomsImg.src=`/j/${JOB}/${floor.dir}/${floor.rooms_overlay}`;
    roomsImg.style.zIndex=999; canvas.appendChild(roomsImg);
  }
  fit();
}

function buildPanel(){
  const p=document.getElementById('vpanel');
  p.innerHTML=`
   <div class="sec"><h3>Alle Layer einfärben</h3>
     <div class="quick">
       <button data-all="original">Originalfarben</button>
       <button data-all="teal">Türkis</button>
       <button data-all="gray">Grau</button>
       <button data-all="black">Schwarz</button>
     </div>
     <div class="quick" style="margin-top:6px">
       <button data-vis="1">Alle einblenden</button>
       <button data-vis="0">Alle ausblenden</button>
     </div>
   </div>
   <div class="sec"><h3>Anzeige</h3>
     <label class="swrow ${floor.rooms_overlay?'':'dim'}"><span class="swch"><input type="checkbox" id="roomsCb" ${floor.rooms_overlay?'checked':''} ${floor.rooms_overlay?'':'disabled'}><span class="tk"></span></span> Räume anzeigen</label>
     <label class="swrow"><span class="swch"><input type="checkbox" id="alltext"><span class="tk"></span></span> Beschriftung überall aus</label>
   </div>
   <div class="sec"><h3>Layer (${floor.layers.length})</h3>
     <p class="phint">Häkchen blendet den Layer ein/aus · Farbe wählen · Schalter „Aa" zeigt/verbirgt die Beschriftung.</p>
     <input type="search" class="lfilter" id="layerFilter" placeholder="Layer filtern">
     <div class="layers" id="layers"></div></div>
   <div class="sec" id="roomsSec"></div>`;
  const box=document.getElementById('layers');
  floor.layers.forEach(L=>box.appendChild(layerRow(L)));
  document.getElementById('layerFilter').oninput=e=>{ const q=e.target.value.toLowerCase();
    floor.layers.forEach(L=>{ L._row.hidden=!L.name.toLowerCase().includes(q); }); };
  document.querySelectorAll('.quick button[data-all]').forEach(b=>b.onclick=()=>{
    const c=b.dataset.all;
    floor.layers.forEach(L=>{ L._color=c; paint(L); refreshRow(L._row,L); });
  });
  document.querySelectorAll('.quick button[data-vis]').forEach(b=>b.onclick=()=>{
    const show=b.dataset.vis==='1';
    floor.layers.forEach(L=>{ setVisible(L,show); paint(L); refreshRow(L._row,L); });
  });
  roomsCb.onchange=e=>{ if(roomsImg) roomsImg.style.display=e.target.checked?'block':'none'; };
  document.getElementById('alltext').onchange=e=>{
    floor.layers.forEach(L=>{ L._text=e.target.checked; paint(L); refreshRow(L._row,L); }); };
  buildRooms();
}

function layerRow(L){
  const row=document.createElement('div'); row.className='lrow';
  const vis=document.createElement('input'); vis.type='checkbox'; vis.className='lvis'; vis.title='Ein-/Ausblenden';
  vis.onchange=()=>{ setVisible(L,vis.checked); paint(L); refreshRow(row,L); };
  const name=document.createElement('div'); name.className='lname'; name.textContent=L.name; name.title=L.name;
  const sw=document.createElement('div'); sw.className='sw';
  COLORS.forEach(([c])=>{ const b=document.createElement('button'); b.className='chip chip-'+c; b.dataset.c=c;
    b.title=COLOR_LABEL[c];
    b.onclick=()=>{ L._color=c; paint(L); refreshRow(row,L); }; sw.appendChild(b); });
  row.append(vis,name,sw);
  const hasText=(L.variants||[]).some(v=>v.endsWith('-notext')) || L.only_text;
  let txIn=null;
  if(hasText){
    const end=document.createElement('div'); end.className='lend';
    const aa=document.createElement('span'); aa.className='aa'; aa.textContent='Aa'; aa.title='Beschriftung';
    const lab=document.createElement('label'); lab.className='swch'; lab.title='Beschriftung dieses Layers anzeigen';
    txIn=document.createElement('input'); txIn.type='checkbox';
    const tk=document.createElement('span'); tk.className='tk';
    lab.append(txIn,tk); end.append(aa,lab);
    txIn.onchange=()=>{ L._text=!txIn.checked; paint(L); };   // switch on = Beschriftung sichtbar
    row.append(end);
  }
  L._row=row; row._refs={vis,sw,txIn};
  refreshRow(row,L);
  return row;
}
function refreshRow(row,L){
  if(!row) return;
  row.classList.toggle('off',!L._color);
  row._refs.vis.checked=!!L._color;
  [...row._refs.sw.children].forEach(b=>b.classList.toggle('sel',L._color===b.dataset.c));
  if(row._refs.txIn) row._refs.txIn.checked = !L._text;   // checked = text visible
}
function syncRooms(){ const cb=document.getElementById('roomsCb'); if(cb&&roomsImg) roomsImg.style.display=cb.checked?'block':'none'; }

function buildRooms(){
  const sec=document.getElementById('roomsSec'); if(!sec) return;
  if(!coords||!coords.rooms||!coords.rooms.length){ sec.innerHTML=''; return; }
  sec.innerHTML=`<h3>Räume (${coords.rooms.length})</h3><div class="rooms" id="roomlist"></div>`;
  const list=document.getElementById('roomlist');
  coords.rooms.forEach(r=>{
    const el=document.createElement('div'); el.className='rroom';
    el.innerHTML=`<span>${r.name||'—'}</span><span class="ra">${r.area_m2?r.area_m2.toFixed(1)+' m²':''}</span>`;
    el.onclick=()=>zoomToRoom(r); list.appendChild(el);
  });
}

/* ---- pan / zoom ---- */
function apply(){ canvas.style.transform=`translate(${tx}px,${ty}px) scale(${scale})`; }
function fit(){
  const sz=Array.isArray(floor.image_size_px)?floor.image_size_px:[1000,1000];
  const w=sz[0]||1000, h=sz[1]||1000, sw=stage.clientWidth||1, sh=stage.clientHeight||1;
  scale=Math.min(sw/w,sh/h)*0.92 || 1; tx=(sw-w*scale)/2; ty=(sh-h*scale)/2; apply();
}
function zoomAt(mx,my,f){ const ns=Math.max(0.1,Math.min(12,scale*f));
  tx=mx-(mx-tx)*(ns/scale); ty=my-(my-ty)*(ns/scale); scale=ns; apply(); }
function zoomToRoom(r){
  if(!r.polygon_px||!r.polygon_px.length) return;
  const xs=r.polygon_px.map(p=>p[0]), ys=r.polygon_px.map(p=>p[1]);
  const x0=Math.min(...xs),x1=Math.max(...xs),y0=Math.min(...ys),y1=Math.max(...ys);
  const rw=x1-x0||1, rh=y1-y0||1, sw=stage.clientWidth, sh=stage.clientHeight;
  scale=Math.min(sw/rw,sh/rh)*0.55;
  tx=sw/2-(x0+x1)/2*scale; ty=sh/2-(y0+y1)/2*scale; apply();
}
stage.addEventListener('wheel',e=>{ e.preventDefault(); const r=stage.getBoundingClientRect();
  zoomAt(e.clientX-r.left,e.clientY-r.top, e.deltaY<0?1.12:1/1.12); },{passive:false});
let drag=null;
stage.addEventListener('pointerdown',e=>{
  if(e.target.closest('.zoom')) return;   // don't start a pan (which captures the pointer) on the zoom controls
  drag={x:e.clientX,y:e.clientY,tx,ty}; stage.classList.add('drag');
  stage.setPointerCapture(e.pointerId); });
stage.addEventListener('pointermove',e=>{ if(!drag)return; tx=drag.tx+(e.clientX-drag.x); ty=drag.ty+(e.clientY-drag.y); apply(); });
stage.addEventListener('pointerup',()=>{ drag=null; stage.classList.remove('drag'); });
stage.addEventListener('pointercancel',()=>{ drag=null; stage.classList.remove('drag'); });
zin.onclick=()=>{ const r=stage.getBoundingClientRect(); zoomAt(r.width/2,r.height/2,1.2); };
zout.onclick=()=>{ const r=stage.getBoundingClientRect(); zoomAt(r.width/2,r.height/2,1/1.2); };
zfit.onclick=fit;
window.addEventListener('resize',()=>{ if(floor) fit(); });

/* ---- original download + feedback ---- */
dlOriginal.href=`/j/${JOB}/original`;
function openFb(){ fbCtx.textContent=`Datei: ${M?M.input:''}${floor?' · '+floor.label:''}`;
  fbForm.style.display=''; fbDone.hidden=true; fbMsg.value=''; fbModal.hidden=false; fbMsg.focus(); }
function closeFb(){ fbModal.hidden=true; }
fbBtn.onclick=openFb; fbCancel.onclick=closeFb;
fbModal.addEventListener('click',e=>{ if(e.target===fbModal) closeFb(); });
fbSend.onclick=async()=>{ const m=fbMsg.value.trim(); if(!m){fbMsg.focus();return;} fbSend.disabled=true;
  try{ await fetch('/api/feedback',{method:'POST',headers:{'Content-Type':'application/json'},
    body:JSON.stringify({job:JOB,message:m,contact:fbContact.value,floor:floor?floor.label:null})}); }catch(e){}
  fbSend.disabled=false; fbForm.style.display='none'; fbDone.hidden=false;
  setTimeout(closeFb,1400); };
</script>"""


def _viewer(job):
    return _HEAD.replace("__TITLE__", "Viewer") + _VIEWER.replace("__JOB__", job) + "</body></html>"


_NOTE_REPORT = "Wenn das nicht sein sollte, melde es uns."
_NOTE_FILE_KEPT = ("Wenn das nicht sein sollte, melde es uns — die hochgeladene Datei bleibt gespeichert, "
                   "damit wir den Fehler nachvollziehen können.")


def _error_page(message, code, job="", title="Verarbeitung fehlgeschlagen", note=_NOTE_REPORT):
    """A clean, branded error page (never a raw stack trace) with a feedback link."""
    body = (_ERROR.replace("__H1__", title).replace("__MSG__", str(message))
            .replace("__NOTE__", note).replace("__JOB__", job or ""))
    return _shell("Fehler", body), code


# ----------------------------------------------------------------------------- api
# JSON endpoints follow the monorepo REST conventions (template: internal/patterns): kebab-case
# paths under /api, service meta endpoints under /api/floorplan (the api_url in
# products-catalog.yaml), and the RestResponse envelope {"type": "good", "data": ...} /
# {"type": "bad/<Variant>"} on the wire.

def _good(data):
    return jsonify(type="good", data=data)


@app.before_request
def _retention_hook():
    _queue_retention_if_due()


@app.after_request
def _app_version_header(response):
    if APP_IMAGE_DIGEST:
        response.headers["X-App-Image-Digest"] = APP_IMAGE_DIGEST
    return response


@app.get("/api/floorplan/global-health")
def global_health():
    """Anonymous liveness probe (Docker healthcheck, monitoring) — mirrors the Ktor MetaRouting.

    Degraded answers HTTP 200, so the Docker healthcheck keeps the viewer of finished jobs routed."""
    free = _free_bytes()
    if free < MIN_FREE_BYTES:
        return _good({"type": "Degraded", "freeBytes": free})
    return _good({"type": "Healthy"})


@app.get("/api/floorplan/meta-tags")
def meta_tags():
    """Deploy metadata (image digest) for deployment verification — mirrors the Ktor MetaRouting."""
    return _good({
        "gitHash": None,
        "gitHashShort": None,
        "gitCommitDateTime": None,
        "imageDigest": APP_IMAGE_DIGEST or None,
    })


# ----------------------------------------------------------------------------- routes

@app.get("/")
def index():
    return _shell("Upload", _UPLOAD)


@app.post("/upload")
def upload():
    file = request.files.get("file")
    if not file or not file.filename:
        return _error_page("Keine Datei ausgewählt.", 400)
    ext = os.path.splitext(file.filename)[1].lower()
    if ext not in ALLOWED_EXT:
        return _error_page("Nur .dwg oder .dxf werden unterstützt.", 400)
    file.stream.seek(0, os.SEEK_END)
    size = file.stream.tell()
    file.stream.seek(0)
    free = _free_bytes()
    if free < MIN_FREE_BYTES or free < 3 * size:
        return _error_page("Der Speicher des Dienstes ist voll; die Datei wurde nicht gespeichert.", 507,
                           title="Speicher voll")
    job = uuid.uuid4().hex[:12]
    job_dir = os.path.join(JOBS_DIR, job)
    os.makedirs(job_dir)
    try:
        input_path = os.path.join(job_dir, "input" + ext)
        file.save(input_path)
        _write_meta(job, filename=file.filename, ext=ext, size=os.path.getsize(input_path),
                    sha256=_sha256(input_path), uploaded_at=_now_iso(), state="queued",
                    message=_MESSAGE_QUEUED, attempts=0)
        _QUEUE.put(job)
        return redirect(url_for("result", job=job))
    except Exception:  # never surface a stack trace; the app stays up
        shutil.rmtree(job_dir, ignore_errors=True)  # a half-saved file would occupy the volume until the next start
        app.logger.exception("upload: could not save job %s", job)  # the error text carries the internal path
        return _error_page("Upload konnte nicht gestartet werden.", 500)


@app.get("/j/<job>")
def result(job):
    meta = _read_meta(job)
    if meta is None:
        abort(404)
    state = meta.get("state")
    if state == "done":
        return _viewer(job)
    if state == "error":
        return _error_page(meta.get("message") or "unbekannter Fehler", 500, job, note=_NOTE_FILE_KEPT)
    return _shell("Verarbeitung",
                  _PROGRESS.replace("__JOB__", job).replace("__INPUT__", meta.get("filename", "Datei")))


@app.get("/api/jobs/<job>/status")
def status(job):
    meta = _read_meta(job) or {}
    return _good({"state": meta.get("state", "unknown"), "message": meta.get("message", "")})


@app.get("/j/<job>/original")
def original(job):
    """Download the exact file the user uploaded, under its original name."""
    meta = _read_meta(job)
    if not meta:
        abort(404)
    path = os.path.join(JOBS_DIR, job, "input" + meta.get("ext", ""))
    if not os.path.exists(path):
        abort(404)
    return send_file(path, as_attachment=True, download_name=meta.get("filename") or os.path.basename(path))


@app.get("/j/<job>/import.json")
def job_import(job):
    """Per floor with rooms: the rooms in the pixels of the floor's background image; 404 until the job is done."""
    meta = _read_meta(job)
    if meta is None or meta.get("state") != "done":
        abort(404)
    try:
        return jsonify(floorplan_import.import_result(os.path.join(JOBS_DIR, job, "out")))
    except floorplan_import.ImportResultFromEarlierVersion:
        return jsonify(error="Dieser Job stammt aus einer früheren Version ohne Import-Ergebnis; bitte die Datei neu hochladen."), 409
    except floorplan_import.RoomExtractionFailed:
        return jsonify(error="Die Raumerkennung ist für diesen Job fehlgeschlagen; das Import-Ergebnis fehlt."), 409
    except floorplan_import.ImportResultIncomplete:
        return jsonify(error="Ein Geschoss mit Räumen ließ sich nicht zeichnen; das Import-Ergebnis ist unvollständig."), 409


@app.get("/j/<job>/<path:subpath>")
def job_static(job, subpath):
    return send_from_directory(os.path.join(JOBS_DIR, job, "out"), subpath)


_STATE_LABEL = {"queued": "Warteschlange", "running": "läuft", "done": "fertig", "error": "Fehler"}


def _feedback_count(job):
    path = os.path.join(JOBS_DIR, job, "feedback.jsonl")
    try:
        with open(path, encoding="utf-8") as handle:
            return sum(1 for line in handle if line.strip())
    except OSError:
        return 0


@app.get("/feedback-form")
def feedback_form():
    """Standalone feedback page (used where there is no viewer, e.g. the error page)."""
    job = (request.args.get("job") or "").strip()
    meta = _read_meta(job) if job else None
    ctx = (f'Bezieht sich auf: {meta["filename"]}' if meta and meta.get("filename")
           else "Allgemeiner Hinweis")
    return _shell("Feedback", _FEEDBACK_FORM.replace("__CTX__", ctx).replace("__JOB__", job))


@app.get("/uploads/<token>")
def uploads(token):
    """Archive of every stored upload: original name, size, state, counts, deletion date, links.

    Deep link only — guarded by the URL token (see ARCHIVE_TOKEN above), 404 on mismatch."""
    if not ARCHIVE_TOKEN or not hmac.compare_digest(token, ARCHIVE_TOKEN):
        abort(404)
    metas = [m for m in (_read_meta(j) for j in os.listdir(JOBS_DIR)) if m]
    metas.sort(key=lambda m: m.get("uploaded_at", ""), reverse=True)
    rows = []
    for meta in metas:
        job = meta.get("job", "")
        state = meta.get("state", "?")
        size_mb = (meta.get("size", 0) or 0) / (1024 * 1024)
        counts = (f'{meta.get("floors", 0)} Gesch. · {meta.get("rooms", 0)} Räume'
                  if state == "done" else _STATE_LABEL.get(state, state))
        fb = _feedback_count(job)
        fb_badge = f'<span class="badge">⚑ {fb}</span>' if fb else ""
        view = f'<a href="/j/{job}">öffnen</a>' if state == "done" else (
            f'<a href="/j/{job}">Status</a>' if state in ("queued", "running") else "—")
        deletion_date = _deletion_date(meta)
        deletion = deletion_date.isoformat() if deletion_date else (
            "bleibt (Feedback)" if _has_feedback(job) else "—")
        rows.append(
            f'<tr><td class="fn">{meta.get("filename", "?")} {fb_badge}</td>'
            f'<td class="muted">{meta.get("uploaded_at", "")}</td>'
            f'<td class="num">{size_mb:.1f} MB</td>'
            f'<td>{counts}</td>'
            f'<td class="muted">{deletion}</td>'
            f'<td class="act">{view} · <a href="/j/{job}/original">Download</a></td></tr>')
    body = (_ARCHIVE_HEAD.replace("__RETENTION_DAYS__", str(RETENTION_DAYS))
            + (('<table class="tbl"><thead><tr><th>Datei</th><th>Hochgeladen</th><th>Größe</th>'
                '<th>Ergebnis</th><th>Löschung</th><th></th></tr></thead><tbody>' + "".join(rows) + '</tbody></table>')
               if rows else '<p class="muted">Noch keine Uploads gespeichert.</p>')
            + '</div></div></div>')
    return _shell("Archiv", body)


@app.post("/api/feedback")
def feedback():
    """Store a user hint / bug report. Appended to a global log and (if a job is named) the job's own log,
    so a report about a specific plan stays next to that plan's kept original file."""
    data = request.get_json(silent=True) or request.form
    message = (data.get("message") or "").strip()
    if not message:
        return jsonify(type="bad/EmptyMessage"), 400
    job = (data.get("job") or "").strip() or None
    entry = {"at": _now_iso(), "job": job, "message": message[:4000],
             "contact": (data.get("contact") or "").strip()[:200],
             "floor": (data.get("floor") or "").strip()[:120] or None,
             "user_agent": request.headers.get("User-Agent", "")[:300]}
    line = json.dumps(entry, ensure_ascii=False) + "\n"
    with open(FEEDBACK_LOG, "a", encoding="utf-8") as handle:
        handle.write(line)
    if job and os.path.isdir(os.path.join(JOBS_DIR, job)):
        with open(os.path.join(JOBS_DIR, job, "feedback.jsonl"), "a", encoding="utf-8") as handle:
            handle.write(line)
    return _good(None)


@app.errorhandler(404)
def _err_404(error):
    return _error_page("Seite oder Datei nicht gefunden.", 404, title="Nicht gefunden")


@app.errorhandler(413)
def _err_413(error):
    return _error_page("Datei zu groß (maximal 300 MB).", 413)


@app.errorhandler(500)
def _err_500(error):
    return _error_page("Unerwarteter Serverfehler. Bitte melde es uns über die Feedback-Funktion.", 500)


# gunicorn runs one worker without --preload, so the import and this thread share the one process. A render
# worker imports a directly started app.py as __mp_main__ and must not remove uploads or resume the running job.
if __name__ != "__mp_main__":
    _remove_incomplete_uploads()
    _resume_interrupted_jobs()
    threading.Thread(target=_pipeline_worker, daemon=True, name="pipeline").start()


if __name__ == "__main__":
    port = int(os.environ.get("PORT", "8090"))
    app.run(host="127.0.0.1", port=port, threaded=True)
