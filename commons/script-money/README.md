# script-money

```ts
const totalBookNetPrice = sumCents(positions.map((position) => centsTimes(position.sellingPrice, position.amount)));
return `Versand (versandkostenfrei ab ${formatEuro(freeShippingFrom)})`;
```

Cent-Arithmetik für Pricing-Scripts mit der Semantik von `Money`, `VatRate` und `RoundingStrategy.Default` aus
[`internal/open/commons/money`](../money/). Ein Betrag ist eine ganze Zahl von Cents als `number`. Jede Funktion wirft
bei einem Bruchteil eines Cents und bei einem Wert, den `number` nicht mehr exakt hält:

- **Betrag oder Ergebnis:** ab ±2⁵³.
- **`centsTimesAndRound`:** Betrag oder Produkt jenseits von 2⁵², wie `Money.timesAndRound`.
- **`vatOf`, `netToGross`:** Produkt aus Cents und Basispunkten des Satzes ab ±2⁵³, bei 19 % ab etwa
  4,7 · 10¹² Cents.

| Funktion                            | Kotlin-Vorbild                                                           |
| ----------------------------------- | ------------------------------------------------------------------------ |
| `assertSafeCents(cents)`            | Wertebereich von `Long`, soweit `number` ihn exakt hält                  |
| `centsTimes(cents, count)`          | `Money.times(Int)`                                                       |
| `centsTimesAndRound(cents, factor)` | `Money.timesAndRound(factor)`, halber Cent weg von null                  |
| `sumCents(values)`                  | `Money.fastSumBy`                                                        |
| `vatOf(netCents, vatRate)`          | `Money.Net.vatAmount`                                                    |
| `netToGross(netCents, vatRate)`     | `Money.WithVat.gross`                                                    |
| `formatEuro(cents)`                 | `MoneyAmount.format(I18nConfiguration.Germany, WhitespaceConfig.Spaces)` |

## Ein Script-Package nutzt die Funktionen

1. `package.json`: `"@neckar-hub/script-money": "workspace:*"` unter `devDependencies`.
2. `tsconfig.json`: `"types": ["@neckar-hub/script-money/globals"]` unter `compilerOptions`.
3. `vitest.config.ts`: `setupFiles: ["@neckar-hub/script-money/vitest-setup"]`; `src/vitestSetup.ts` legt die Funktionen
   auf `globalThis`.

## Zur Laufzeit

- `TypeScriptCode.fromClasspath` des Moduls `script-engine` liest `/scripts/scriptMoney.ts` vom Classpath und hängt die
  Datei an jedes Script an. Eine Zeilennummer in einem Übersetzungsfehler bleibt die Zeile des Scripts.
- `src/scriptMoney.ts` deklariert auf oberster Ebene nur Funktionen. Sie werden gehoisted; ein Script ruft sie schon beim
  Laden seines Moduls auf.
- **Script deklariert einen Top-Level-Namen von `src/scriptMoney.ts`:** Der Typecheck meldet nichts, auch bei einem nicht
  exportierten Namen wie `assertExact`. `TypeScriptCompiler.compile` weist das Script beim Start des Service ab und nennt
  den doppelten Namen.

## Generiert

`pnpm bundle` übersetzt `src/scriptMoney.ts` und `src/vitestSetup.ts` nach `dist/src/`. `generate-globals.mjs` schreibt
`dist/src/script-money-globals.d.ts`: dieselben Deklarationen ohne `export`, also global.

## Korpus

`src/scriptMoney.cases.json` hält die Fälle, gegen die `scriptMoney.test.ts` die Funktionen und `ScriptMoneyCorpusTest`
in `internal/open/commons/money` den Kotlin-Code prüfen.
