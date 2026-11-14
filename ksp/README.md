## KSP (Kotlin Symbol Processing) Plugins

Enthält KSP Compiler Plugins.

> [!NOTE]
> KSP plugins laufen zur Compile Time.
Deshalb sind nicht alle Dependencies gestattet!

Dies wird zum Compile-Zeitpunkt überprüft!

## Best Practices

### Log output

Standardmäßig ist der Log-Output nicht zu sehen.
Man kann das Log so sehen:

`gradle clean build --info --no-build-cache | grep "\[ksp\]"`

### Warning-Level unsichtbar!
Aus unerfindlichen Gründen scheint das Warning-Level nicht auf der Console zu landen.
Müsste mal™ näher untersucht werden.

## Projekt-Struktur

- `ksp-commons`: Hilfsklassen für KSP
- `ksp-test-utils`: Hilfsklassen um KSP-Plugins mit Unit-Tests zu testen

