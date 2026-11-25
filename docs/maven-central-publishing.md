# Publish nach Maven Central

Open-Source-Module unter `internal/open/` werden auf Maven Central veröffentlicht.
Die Version ist global in `monorepo.version` definiert.

**Plugin:** [com.vanniktech.maven.publish](https://vanniktech.github.io/gradle-maven-publish-plugin/)
**Konfiguration:** [`internal/open/build.gradle.kts`](../build.gradle.kts)


## Snapshot publishen

```bash
# Einzelnes Modul
./gradlew :internal:open:commons:kotlin-lang:publishToMavenCentral

# Alle open-source Module
./gradlew publishToMavenCentral
```

Snapshots (Version endet auf `-SNAPSHOT`) werden direkt ins Sonatype Snapshots Repository veröffentlicht, ohne Staging.

Prüfen:

```bash
curl -s https://central.sonatype.com/repository/maven-snapshots/it/neckar/open/kotlin-lang-jvm/9.0.0-SNAPSHOT/maven-metadata.xml
```


## Release

```bash
tools/bin/nit-release open            # Version aus den technischen Notes des Release
tools/bin/nit-release open 10.2.0     # Version ausdrücklich
```

Phasen, Versionsregeln und Exit-Codes: `tools/bin/nit-release --help`. Der Release-Commit des Release-MR `open | Release open-<X.Y.Z>` setzt `monorepo.version` auf `<X.Y.Z>` und enthält das Ergebnis von `:kotlinUpgradePackageLock`.

Auf dem Tag `open-<X.Y.Z>` veröffentlicht der Job `📦 Publish open to Maven Central` in [`gitlab-ci.d/tag.yml`](../../../gitlab-ci.d/tag.yml) die Artefakte; Ablauf und Exit-Codes: [`publish-to-maven-central.sh`](../../../gitlab-ci.d/scripts/publish-to-maven-central.sh). Eine veröffentlichte Version lässt sich auf Maven Central nie wieder ersetzen; der Review-Punkt ist deshalb der Release-MR.

Ein Retry von `📦 Publish open to Maven Central` lädt ein zweites Deployment hoch. Ein hochgeladenes Deployment, das `PUBLISHED` nicht erreicht hat, gibt der manuelle Job `📦 Finish open Maven Central deployment` mit `MAVEN_CENTRAL_DEPLOYMENT_ID=<deployment-id>` frei; die Fehlermeldung des Publish-Jobs nennt die ID.

### Publishing prüfen

```bash
./gradlew :internal:open:verifyMavenCentralPublishing            # open
./gradlew -p internal/buildplatform verifyMavenCentralPublishing # buildplatform
```

`verifyMavenCentralPublishing` lädt nichts hoch:

1. Der Task `deleteMavenCentralVerificationRepository` löscht `build/maven-central-verification/repository` unter `internal/open` oder `internal/buildplatform`.
2. `publishAllPublicationsToMavenCentralVerificationRepository` jedes Projekts mit Maven-Central-Publication schreibt die signierten Artefakte in dieses Verzeichnis. Ohne die Gradle-Property `signingInMemoryKey` signiert der lokale GPG-Agent mit `signing.gnupg.keyName`.
3. `verifyMavenCentralPublishing` prüft jedes POM des Verzeichnisses und nennt jeden Verstoß mit dem Pfad des POM.

Regeln, nach denen Maven Central ein Deployment ablehnt ([`MavenCentralPublicationRules.kt`](../../buildplatform/conventions/src/main/kotlin/it/neckar/gradle/publishing/MavenCentralPublicationRules.kt)):

- Das POM hat eine `version`.
- `name`, `description` und `url` sind gesetzt.
- `licenses` enthält eine Lizenz mit `name`.
- `developers` enthält einen Developer mit `name`.
- `scm` hat `url` und `connection`.
- Jede Dependency in `dependencies` und `dependencyManagement` hat eine `version`.
- Neben einem `.jar` liegen `-sources.jar` und `-javadoc.jar`.
- Jede Artefakt-Datei hat ihre Signatur `<datei>.asc`.
- Das Verzeichnis enthält mindestens ein POM.

Das Plugin `it.neckar.maven-central-publishing` ([`MavenCentralPublishingPlugin.kt`](../../buildplatform/conventions/src/main/kotlin/it/neckar/gradle/publishing/MavenCentralPublishingPlugin.kt)) registriert die Tasks:

- `internal/open/build.gradle.kts` ruft `apply(plugin = BuildplatformPlugins.MavenCentralPublishing)` auf.
- `internal/buildplatform/build.gradle.kts` ruft `apply<MavenCentralPublishingPlugin>()` auf. [`internal/buildplatform/buildSrc`](../../buildplatform/buildSrc/build.gradle.kts) kompiliert die Klasse aus den Sourcen von `conventions`.

Der Job `⏲ verify main nightly` ([`gitlab-ci.d/schedule/verify-main-nightly.yml`](../../../gitlab-ci.d/schedule/verify-main-nightly.yml)) startet `verifyMavenCentralPublishing` von `internal/open` und `internal/buildplatform` jede Nacht auf `main` über `./gradlew $GRADLE_CLI_OPTS --continue -Pcoverage=true verifyNightly --profile --info`. Gradle signiert mit einem Wegwerf-Key. Die Jobs `📦 Verify open Maven Central publishing` und `📦 Verify buildplatform Maven Central publishing` des Release-MR prüfen, was `verifyNightly` vom Publish-Job unterscheidet:

- `verifyNightly` baut `main` mit der `-SNAPSHOT`-Versionsdatei, der Release-MR den Commit mit dem Trailer `Release-Tag:`.
- `verifyNightly` läuft mit dem `GRADLE_LOG_LEVEL` des Jobs `⏲ verify main nightly`, der Publish-Job mit seinem eigenen `GRADLE_LOG_LEVEL` und mit `clean`.
- `verifyNightly` baut `internal/buildplatform` als Included Build, der Publish-Job mit `-p internal/buildplatform` als eigenen Build samt Tests.

[`verify-maven-central-publishing.sh [--snapshot] <product>`](../../../gitlab-ci.d/scripts/verify-maven-central-publishing.sh) startet den Task mit den Argumenten des Publish-Jobs; CI ruft das Script im Release-MR auf:

| Schritt | Release-MR: `verify-maven-central-publishing.sh <product>` | `HEAD`: `verify-maven-central-publishing.sh --snapshot <product>` |
|---|---|---|
| Jobs | - `📦 Verify open Maven Central publishing`<br>- `📦 Verify buildplatform Maven Central publishing`<br>- Datei: [`gitlab-ci.d/mr.yml`](../../../gitlab-ci.d/mr.yml) | kein CI-Job; Aufruf auf der Workstation |
| Commit | der Commit mit dem Trailer `Release-Tag:`, den die Tag-Pipeline baut, detached ausgecheckt | `HEAD` |
| Versionsdatei | gleich der Version des Tags im MR-Titel | ungeprüft, `-SNAPSHOT` |
| Signatur | Wegwerf-Key: der Release-Signing-Key liegt in `secret/ci/protected` und ist nur auf geschützten Refs lesbar | Wegwerf-Key, derselbe Ablauf wie im Job des Release-MR |
| Gradle-Aufruf | Argumente des Jobs `📦 Publish <product> to Maven Central` aus `gitlab-ci.d/tag.yml`, mit `verifyMavenCentralPublishing` statt `publishToMavenCentral` | Argumente des Jobs `📦 Publish <product> to Maven Central` aus `gitlab-ci.d/tag.yml`, mit `verifyMavenCentralPublishing` statt `publishToMavenCentral` |
| Dry Run von [`create-gitlab-release.sh`](../../../gitlab-ci.d/scripts/create-gitlab-release.sh) | Payload des Tags im MR-Titel nach `build-reports/maven-central-verification/gitlab-release-payload.json` | Payload des höchsten Release-Verzeichnisses `releases/<tag_prefix>-X.Y.Z` des Products nach `build-reports/maven-central-verification/gitlab-release-payload.json` |

## `tools/bin/nit-maven-central poll`

[`tools/bin/nit-maven-central poll`](../../../tools/bin/nit-maven-central) pollt den Deployment-Status über die Central-Portal-API und schickt auf Wunsch den Publish-Request. Ohne Flag fragt es auf einem Terminal nach; ohne Terminal bricht es mit Exit-Code 2 ab, statt stillschweigend nichts zu veröffentlichen.

| Flag | Wirkung |
|---|---|
| `--publish` | Gibt das Deployment frei, sobald es `VALIDATED` ist, und wartet bis `PUBLISHED` |
| `--await-validation` | Stoppt bei `VALIDATED`, veröffentlicht nichts |
| `--timeout <sekunden>` | Abbruch nach dieser Zeit. Default: 1800. `0` = ein Poll ohne Warten |
| `--poll-interval <sekunden>` | Abstand zwischen zwei Polls. Default: 10 |
| `--help` | Hilfe |

| Exit-Code | Bedeutung |
|---|---|
| 0 | Zielzustand erreicht (`PUBLISHED` mit `--publish`, `VALIDATED` mit `--await-validation`) |
| 1 | Deployment `FAILED`, API-Fehler oder fehlende Credentials |
| 2 | Aufruffehler (unbekanntes Flag, fehlende Deployment-ID, kein Entscheidungs-Flag ohne Terminal) |
| 3 | Timeout |

Tests: [`tools/maven-central/tests/test-poll-deployment.sh`](../../../tools/maven-central/tests/test-poll-deployment.sh), im Build über `:tools:script-tests:test`.

Credentials kommen aus `ORG_GRADLE_PROJECT_mavenCentralUsername`/`…Password`; in CI lädt `gitlab-ci.d/scripts/load-ci-secrets.sh` beide aus OpenBao. Ohne diese Variablen fällt das Skript auf `~/.gradle/gradle.properties` zurück.

---


# Setup

Einmalige Einrichtung für Maven Central Publishing.


## Namespace

Der verifizierte Namespace `it.neckar` des Sonatype-Accounts erlaubt den Upload jeder Gruppe `it.neckar.*`, Snapshots eingeschlossen.

## Sonatype User Token generieren

1. Auf https://central.sonatype.com einloggen (Neckar IT Account)
2. Oben rechts auf den Benutzernamen klicken > **View Account**
3. Abschnitt **Generate User Token** > Button **Generate User Token**
4. Username und Password des Tokens kopieren — **werden nur einmal angezeigt!**

Direktlink: https://central.sonatype.com/account


## GPG-Key einrichten

Falls noch kein GPG-Key vorhanden:

```bash
gpg --full-generate-key
# RSA, 4096 Bit, kein Ablaufdatum, Name + E-Mail eintragen
```

Key ID ermitteln (letzte 8 Zeichen des Fingerprints):

```bash
gpg --list-keys --keyid-format short
# pub   rsa4096/7B42FE9D 2024-01-15 [SC]
#                ^^^^^^^^ das ist die Key ID
```

Public Key auf einen Keyserver hochladen (damit Maven Central die Signatur verifizieren kann):

```bash
gpg --keyserver keyserver.ubuntu.com --send-keys <key-id>
```


## Credentials in `~/.gradle/gradle.properties` eintragen

Die Datei `~/.gradle/gradle.properties` enthält benutzerspezifische Gradle-Einstellungen.
Falls die Datei noch nicht existiert: anlegen. Falls sie existiert: die folgenden Zeilen ergänzen.

Siehe auch: [`docs/gradle/sample-gradle.properties`](../../../docs/gradle/sample-gradle.properties)

```properties
# Maven Central Publishing — Sonatype User Token
# Token von: https://central.sonatype.com/account
mavenCentralUsername=<user-token-username>
mavenCentralPassword=<user-token-password>

# GPG Signing — Key ID (letzte 8 Zeichen des Fingerprints)
signing.gnupg.keyName=<key-id>
```


## CI/CD-Konfiguration

Die Jobs `📦 Publish open to Maven Central`, `📦 Publish buildplatform to Maven Central` und der Snapshot-Job [`gitlab-ci.d/schedule/publish-sonatype-snapshots.yml`](../../../gitlab-ci.d/schedule/publish-sonatype-snapshots.yml) führen `publishToMavenCentral` in CI aus. `gitlab-ci.d/scripts/load-ci-secrets.sh` lädt die Variablen vorher aus dem OpenBao-Pfad `secret/ci/protected`; OpenBao liefert ihn nur geschützten Refs, den Release-Jobs also auf den Protected Tags `open-*` und `buildplatform-*`.

In CI wird In-Memory-Signing verwendet (kein GPG-Agent verfügbar). Das Vanniktech-Plugin erkennt die `signingInMemory*`-Variablen automatisch.

Ein OpenBao-Admin schreibt jeden Key; der Befehl liest den Wert von stdin: `tools/bin/nit-bao --admin kv patch -method=rw secret/ci/protected <key>=-`.

| Variable | Key in `secret/ci/protected` | Beschreibung | Wert ermitteln |
|----------|------------------------------|-------------|----------------|
| `ORG_GRADLE_PROJECT_mavenCentralUsername` | `maven-central-username` | User Token Username | Sonatype Token |
| `ORG_GRADLE_PROJECT_mavenCentralPassword` | `maven-central-password` | User Token Password | Sonatype Token |
| `ORG_GRADLE_PROJECT_signingInMemoryKey` | `signing-in-memory-key` | Private Key des Release-Signing-Keys (ASCII-armored, ohne Passphrase) | `gpg --armor --export-secret-keys <fingerprint>` |
| `ORG_GRADLE_PROJECT_signingInMemoryKeyId` | `signing-in-memory-key-id` | Key ID des Release-Signing-Keys (letzte 8 Zeichen) | `gpg --list-keys --keyid-format short` |
| — | `signing-revocation-certificate` | Widerrufszertifikat des Release-Signing-Keys | `openpgp-revocs.d/<fingerprint>.rev` im `GNUPGHOME` der Erzeugung |

### Release-Signing-Key

CI signiert mit einem eigenen Key `Neckar IT Release Signing <release@neckar.it>` (RSA 4096, Ablauf nach 2 Jahren). Der Private Key hat keine Passphrase; `secret/ci/protected` schützt ihn, und eine Passphrase im selben Pfad schützte nichts zusätzlich. Der Private Key liegt nur in OpenBao.

Vor dem Ablauf einen neuen Key erzeugen, den Public Key veröffentlichen und beide Keys in `secret/ci/protected` ersetzen:

```bash
export GNUPGHOME="$(mktemp -d)"
gpg --batch --passphrase '' --quick-gen-key 'Neckar IT Release Signing <release@neckar.it>' rsa4096 sign 2y
gpg --armor --export <fingerprint> | curl -s --data-urlencode keytext@- https://keyserver.ubuntu.com/pks/add
gpg --armor --export <fingerprint> | jq -Rs '{keytext: .}' | curl -s -H 'Content-Type: application/json' -d @- https://keys.openpgp.org/vks/v1/upload \
  | jq '{token, addresses: ["release@neckar.it"]}' | curl -s -H 'Content-Type: application/json' -d @- https://keys.openpgp.org/vks/v1/request-verify
# keys.openpgp.org publishes the user ID after the link in the mail to release@neckar.it is confirmed
gpg --batch --armor --export-secret-keys <fingerprint> | tools/bin/nit-bao --admin kv patch -method=rw secret/ci/protected signing-in-memory-key=-
printf %s <key-id> | tools/bin/nit-bao --admin kv patch -method=rw secret/ci/protected signing-in-memory-key-id=-
tools/bin/nit-bao --admin kv patch -method=rw secret/ci/protected signing-revocation-certificate=- < "$GNUPGHOME/openpgp-revocs.d/<fingerprint>.rev"
rm -rf "$GNUPGHOME"
```


## Referenzen

- Plugin-Dokumentation: https://vanniktech.github.io/gradle-maven-publish-plugin/
- Sonatype Central Portal: https://central.sonatype.com
- GPG-Grundlagen: https://central.sonatype.org/publish/requirements/gpg/
