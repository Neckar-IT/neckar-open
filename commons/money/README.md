# money

Geldbeträge in Euro-Cent. Der Steuerbezug eines Betrags ist sein Typ (ADL 0213); `MoneyAmount` ist die gemeinsame Basis mit `cents` und der Formatierung:

| Typ | Bedeutung |
|---|---|
| `Money.Net` | Nettobetrag: Einzelpreis, Zeilen- und Dokumentnetto, Rabatt, Katalog- und Kostenpreis, Stundensatz |
| `Money.Gross` | Bruttobetrag: Dokumentbrutto, Zahlbetrag, Investitionssumme |
| `Money.Vat` | Steuerbetrag |
| `Money` | Betrag ohne Steuerbezug: Kreditsumme, Förderung, Anzahlung |
| `Money.WithVat` | Netto mit `VatRate`; `vatAmount` und `gross` sind abgeleitet und werden mitserialisiert |

`Money.Net * VatRate` liefert `Money.Vat`, `Money.Net + Money.Vat` liefert `Money.Gross`; jede andere Addition bleibt in ihrem Typ, und von `Money.Gross` führt keine Rechnung zu `Money.Net`.

Jede Rundung auf ganze Cent folgt einer `RoundingStrategy`; `RoundingStrategy.Default` ist die kaufmännische Rundung (halber Cent weg von null), die vier Betragstypen nehmen in `timesAndRound`, `divAndRound`, `euros` und `Money.Net.vatAmount` eine andere entgegen; die Steuer rechnet `Money.Net.vatAmount` in ganzen Zahlen über `VatRate.basisPoints`, damit ein binärer Satz keinen halben Cent verschiebt.

Jeder der vier Beträge serialisiert als Cent-`Long` (ADL 0136): JSON `number`, `BsonInt64`, im OpenAPI-Schema `integer`/`int64`. Sub-Cent-Genauigkeit trägt `MoneyFraction`.

Preisstrukturen desselben Moduls: `PriceWithProfit`, `PricePerHour`, `PricePerHourWithProfit`, `WorkingTimesWithPrice`, `PriceWithProfitAndWorkingTime`, `QuantityUnit`.
