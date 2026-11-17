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


## Namespaces

Ein verifizierter Namespace erlaubt den Upload seiner eigenen Gruppe und jeder Untergruppe. Der Sonatype-Account hält unter https://central.sonatype.com/publishing/namespaces diese Namespaces:

| Namespace | Organisation | Status | Gruppen im Repository |
|---|---|---|---|
| `it.neckar` | Neckar IT | Verified, SNAPSHOTs enabled | - `it.neckar.open`<br>- `it.neckar.buildplatform` |
| `com.meistercharts` | Meistercharts | Verified | keine |
| `com.cedarsoft` | Cedarsoft | Verified | keine |
| `com.clashinspector` | Clashinspector | Verified | keine |

Maven Central nimmt Snapshots nur für einen Namespace mit „SNAPSHOTs enabled“ an.

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
