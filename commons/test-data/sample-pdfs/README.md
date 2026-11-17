# Sample PDFs

Gültige einseitige A4-PDFs mit exakter Größe: 50 KiB, 250 KiB, 1 MiB, 5 MiB, 20 MiB, 50 MiB, 99 MiB und 110 MiB (über dem Publisher-PDF-Limit von 100 MiB). Sie dienen den Beispieldaten von `internal/patterns` und manuellen Uploads. Git LFS speichert sie (`*.pdf` in `.gitattributes`).

Die Liste steht in `SamplePdfs` (`src/jvmMain/kotlin/it/neckar/test/data/SamplePdfs.kt`). `SamplePdfsTest` prüft, dass jede Datei die Größe ihres Namens hat und `TestFiles.pdf` sie byteweise gleich erzeugt.

Neu schreiben: `./gradlew :internal:open:commons:test-data:writeSamplePdfs`
