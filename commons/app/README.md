# commons/app

Anwendungs- und Kommandozeilen-Basis: Clikt-Kommandos, Anwendungs-Metadaten, Passwort-Utilities, Desktop-Anbindung.

Kotlin Multiplatform mit den Targets **jvm** und **linuxX64**, registriert als `Projects.open.commons.app = multiplatform("app", ProjectRole.Library, Jvm, LinuxX64)` in `internal/open/project-registry/open-projects/src/main/kotlin/it/neckar/projects/OpenProjects.kt`. Das Native-Target gibt es, damit Kotlin/Native-Binaries dieselben Flags, Env-Variablen und Value Classes nutzen wie die JVM-Services.

## Source-Sets

| Source-Set | Inhalt |
|---|---|
| `commonMain` | `clikt/CredentialOptions.kt` (GitLab- und Keycloak-Optionen, `--http-port`), die Value Classes `GitlabAccessToken`, `ProjectPath`, `GitLabProjectId`, `env/EnvironmentAccess.kt` (`.env`-Parsing, `expect` für Umgebungsvariable und Datei-Lesen) |
| `linuxX64Main` | die `actual`-Implementierungen des Umgebungszugriffs über `getenv` und `fopen` |
| `jvmMain` | alles Übrige: `BaseCommand`, Anwendungs- und Umgebungs-Metadaten, GitLab-Package-Registry, Passwort-Utilities sowie `ui/swing/`, `desktop/`, `xdg/`, `logging/` — gebunden an Swing, `java.awt.Desktop`, `java.io.File` bzw. Logback |
| `commonTest` | Value-Class-Invarianten und `.env`-Parsing, laufen auf beiden Targets |
| `jvmTest` | die JVM-Tests |

## `BaseCommand` bleibt vorerst in `jvmMain`

Sie hängt an dotenv, der Logback-Initialisierung, dem JVM-Trust-Store für eigene CA-Zertifikate und dem Runtime-Kontext. Ihre Optionen ohne diese Bindung stehen bereits in `commonMain` auf Clikts `BaseCliktCommand`: ein Native-Kommando erhält `--gitlab-access-token`, `--gitlab-project`, `--keycloak-realm-url`, `--keycloak-client-id` und `--http-port` mit identischer Semantik.

Für den Umzug nach `commonMain` brauchen zwei Module ein `linuxX64`-Target:

| Modul | Typen, die `BaseCommand` daraus liest |
|---|---|
| `internal/open/commons/kotlin-lang/` | `it.neckar.open.http.Url`, `it.neckar.runtime.context.RuntimeContext` |
| `internal/open/commons/descriptors/` | `DeploymentStage`, `ExecutionEnvironment`, `RuntimeContextEnv`, `ServiceHost` |

Beide sind als `multiplatform(path, Jvm, Js, WasmJs)` registriert; `LinuxX64` müsste dort ergänzt werden.
