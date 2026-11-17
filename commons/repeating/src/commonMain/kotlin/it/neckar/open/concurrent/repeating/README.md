# `it.neckar.open.concurrent.repeating`

Wiederkehrende Hintergrund-Jobs auf Coroutinen: ein Block, ein Rhythmus, und die Eigenschaften, die
eine handgerollte `while (true) { … delay(…) }`-Schleife jedes Mal neu erfinden müsste.

Ein eigenes Gradle-Modul (`internal/open/commons/repeating/`). Es hängt an `kotlinx-coroutines`,
`logging`, `disposable`, `annotations`, `unit` und den Primitiven aus `kotlin-lang` (Platform-Locks,
das mockbare `now()`) — sonst nichts. Kein Bezug zu Ktor, REST, Health oder Metriken; die Anbindung
an Beobachter läuft über [`RepeatingCycleListener`](RepeatingCycleListener.kt).

## Der kürzeste Fall

```kotlin
Repeating.every(60.seconds) { pollTargets() }
  .start(scope)
```

Der erste Zyklus läuft sofort, danach jeweils 60 Sekunden nach dem Ende des vorherigen. Wirft ein
Zyklus, wird das geloggt und der nächste läuft trotzdem.

## Wann läuft ein Zyklus — `CycleSchedule`

| Schedule | Aufruf | Bedeutung |
|---|---|---|
| `IntervalSchedule` | `Repeating.every(30.seconds)` | erster Zyklus sofort, dann Pause nach jedem Zyklusende |
| `RandomIntervalSchedule` | `Repeating.following(RandomIntervalSchedule(30.seconds, 90.seconds))` | Pause pro Zyklus gezogen, auch vor dem ersten — gegen Gleichtakt in einer Flotte |
| `ImmediateSchedule` | `Repeating.immediately { … }` | keine Pause; die Arbeit selbst gibt den Takt (blockierendes Lesen, Warten auf einen Kindprozess) |
| `HourlySchedule` | `Repeating.hourlyAt(15, zone)` | „:15 nach jeder vollen Stunde" (JVM) |
| `DailySchedule` | `Repeating.dailyAt(LocalTime.of(3, 0), zone)` | „03:00 jede Nacht" (JVM) |
| `WeeklySchedule` | `Repeating.weeklyAt(MONDAY, LocalTime.of(4, 0), zone)` | „Montag 04:00" (JVM) |
| `WeekdaySchedule` | `Repeating.following(WeekdaySchedule(LocalTime.of(7, 30), zone))` | „Montag bis Freitag 07:30" (JVM) |

Die Wall-Clock-Schedules rechnen vor jedem Zyklus neu aus dem Kalender — über die
Sommerzeitumstellung bleibt der Job an seiner lokalen Uhrzeit statt eine Stunde zu verrutschen. Der
nächste Lauf folgt auf den Zeitpunkt, für den der vorige Zyklus stand: seinen geplanten Zeitpunkt oder
seinen Start, wenn ein Trigger ihn auslöste oder er nach seinem geplanten Zeitpunkt begann.
Verstreichen während eines Zyklus ein oder mehrere geplante Zeitpunkte, startet direkt nach dessen Ende
genau ein Zyklus. Hat der Job einen Error-Backoff, folgt der nächste Lauf nach einem gescheiterten Zyklus
auf das Ende des Backoffs oder auf den Start des Zyklus, den ein Trigger im Backoff auslöst; die geplanten
Zeitpunkte davor entfallen.
Ein Lauf, den der Prozess verpasst, weil er nicht lief, wird nicht nachgeholt.

Ein eigener Rhythmus implementiert `CycleSchedule`: `description`, `maxGap` und `awaitCycle`. `awaitCycle`
wartet auf den nächsten Zyklus und liefert den Zeitpunkt, für den dieser Zyklus steht.

## Was der Job aushält

```kotlin
Repeating.every(15.seconds) { reconcile() }
  .cycleTimeout(5.minutes)
  .errorBackoff(ErrorBackoff.exponential(initial = 30.seconds, maximum = 10.minutes))
  .onError { alerting.report(it) }
  .start(scope)
```

| Baustein | Wirkung | Default |
|---|---|---|
| Fehlertoleranz | ein Zyklus, der wirft, beendet die Schleife nicht — auch nicht über den Error-Handler oder den Listener | immer an |
| `cycleTimeout` | ein hängender Zyklus wird abgebrochen, der nächste läuft planmäßig | aus |
| `errorBackoff` | nach wiederholten Fehlern wird die Wiederholrate gedrosselt | aus |
| Supervisor | ein Worker, der unerwartet endet, wird neu gestartet — solange sein Scope lebt | immer an |

## Außer der Reihe auslösen

```kotlin
repeating.triggerNow()
```

Führt sofort einen Zyklus aus, statt auf den Rhythmus zu warten — für Arbeit, die normalerweise
periodisch ist, aber manchmal einen Anlass hat: ein Webhook trifft ein, jemand drückt einen Knopf,
eine Abhängigkeit ist zurück. Blockiert nie und führt den Block nie auf der Coroutine des Aufrufers
aus; der Zyklus läuft im Scope des Jobs, mit Timeout, Fehlerbehandlung und Meldung wie jeder andere.

Trigger werden zusammengefasst: Während ein Zyklus läuft, ergeben beliebig viele Trigger genau einen
Nachlauf — der startet danach und deckt damit ohnehin ab, worum es den späteren Triggern ging. Der
Rhythmus bleibt unberührt; nach dem getriggerten Zyklus geht es planmäßig weiter.

Ein laufender `errorBackoff` wird abgekürzt — „die Abhängigkeit ist zurück, lauf jetzt" ist der
Hauptgrund, einen scheiternden Job zu triggern. Auf einem gestoppten Job hat ein Trigger keine
Wirkung: `start()` verwirft die Trigger, die währenddessen eintrafen, weil ein Trigger einen Moment
meint und nicht eine Absicht für später.

## Was der Job über sich verrät

```kotlin
val repeating = Repeating.every(1.minutes) { sweep() }.start(scope)

repeating.state        // Idle | Running | StoppedItself
repeating.statistics   // gestartete, abgeschlossene, fehlgeschlagene Zyklen, letzter Fehler, letzter Erfolg
```

Wer bei jedem Zyklus benachrichtigt werden will statt nachzusehen, registriert einen
`RepeatingCycleListener`. Im Monorepo verbindet ihn `it.neckar.open.health` per `trackedAs(…)` mit
der `BackgroundJobRegistry`, sodass ein toter Job in `GET /health` und im Alert erscheint.

## Lebenszyklus

- `start(scope)` ist idempotent; ein zweiter Aufruf ist ein No-op.
- `cancel()` beendet Worker und Supervisor. Danach lässt sich derselbe Job wieder starten; die
  Statistik bleibt erhalten.
- `stopRepeating()` im Block beendet die Schleife von innen — für eine Endbedingung, die nur die
  Arbeit kennt (Stream zu Ende, Queue geschlossen). Der Supervisor belebt sie dann nicht wieder.
- Der Supervisor läuft standardmäßig im selben Scope wie der Worker: ein Supervisor, der seinen
  Worker-Scope überlebt, könnte ihn nur noch erfolglos neu zu starten versuchen.
- `RepeatingJobs` hält die Jobs einer Komponente und beendet sie gemeinsam.

## Grenzen

- **Kein Warten auf einen getriggerten Zyklus.** `triggerNow()` kehrt sofort zurück; wer wissen will,
  wann der ausgelöste Zyklus fertig ist, beobachtet ihn über einen `RepeatingCycleListener`.
- **Keine Serialisierung gegen fremde Aufrufer.** Wer denselben Block auch außerhalb des Jobs
  ausführt, braucht dafür ein eigenes Mutex (siehe `AutoMergeScheduler`).
- **Keine Persistenz.** Zähler und Zeitstempel gelten für den laufenden Prozess; nach einem Neustart
  beginnt die Zählung von vorn.
- **Kein verteiltes Locking.** Laufen zwei Instanzen desselben Service, läuft der Job zweimal.
