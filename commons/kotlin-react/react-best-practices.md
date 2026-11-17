## Best Practices

### Navigieren nach Aktion

#### Grundgedanke
- Aktion wird ausgeführt
- Fehlermeldung soll auf der Seite der Aktion angezeigt werden
- Im Erfolgsfall soll navigiert werden

#### Pattern
- Call `rest{action}` bzw. besser `restBusy`
- Action ausführen.
- Im Erfolgsfall dann navigieren.


