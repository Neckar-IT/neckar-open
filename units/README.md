# Annotations for Units

Dieses Projekt enthält Annotations für Units. Damit können z.B. Pixel und andere Einheiten im Code markiert werden.

Aktuell dienen diese Annotations ausschließlich der Dokumentation. Theoretisch wäre es denkbar, dass man z.B. per KSP-Plugin die Plausibilität überprüft.

## Einheiten auf der REST-API

Die Wire-Einheit je Größe legt ADL 0136 fest.
SI-Annotations (`@W`, `@kW`, `@V`, `@A`, `@kWh`, `@Ohms`, `@m`, `@mm`) liegen in `unit/src/commonMain/kotlin/it/neckar/open/unit/si/`,
weitere (`@Wp`, `@kWp`, `@pct`) in `.../unit/other/`. Die Value Classes `RatedPower`, `PeakPower`, `Voltage`, `Current`,
`AmountOfEnergy` und `Resistance` liegen im Energy-Modul der Produkte; `Money` (Cent) liegt in
`internal/open/commons/money/`.
