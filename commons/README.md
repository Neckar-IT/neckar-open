it.neckar.open
==================

Classes commonly used.

Neues Modul hinzufügen:
* Verzeichnis mit `build.gradle.kts` anlegen
* Pfad in `/settings.gradle.kts` includen (alphabetisch einsortiert)
* Property in `OpenProjects.Commons` (`internal/open/project-registry/open-projects/src/main/kotlin/it/neckar/projects/OpenProjects.kt`) einfügen, benannt nach dem Verzeichnis in camelCase
* `./gradlew :verifyProjects` aufrufen

`internal/build.gradle.kts` konfiguriert jedes registrierte Modul. Vollständige Checkliste: [docs/gradle/create-project.md](../../../docs/gradle/create-project.md).
