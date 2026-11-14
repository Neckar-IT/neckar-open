import { useHoursAndMinutesFormatter, useMaxFractionDigitsFormatter } from "@neckar-hub/typescript-react-i18n";
import { type Amount, AmountUnit } from "@neckar-hub/typescript-utils/openapi-types";
import { useEmbeddedTranslation } from "../../localization/useEmbeddedTranslation";

interface AmountOutputProps {
  amount: Amount;
}

const NonBreakingSpace = " ";

/**
 * The amount of a quote item with its unit, rounded to at most two fraction digits.
 * A missing unit is shown like `AmountUnit.None`: as pieces.
 */
export function AmountOutput({ amount }: AmountOutputProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const formatHours = useHoursAndMinutesFormatter();
  const formatNumber = useMaxFractionDigitsFormatter(2);

  switch (amount.unit ?? AmountUnit.None) {
    case AmountUnit.None:
      return <>{withUnit(formatNumber(amount.amount), t("quote.units.none"))}</>;
    case AmountUnit.Hours:
      return <>{withUnit(formatHours(amount.amount), t("quote.units.hours"))}</>;
    case AmountUnit.KiloMeters:
      return <>{withUnit(formatNumber(amount.amount), t("quote.units.kiloMeters"))}</>;
    case AmountUnit.Meters:
      return <>{withUnit(formatNumber(amount.amount), t("quote.units.meters"))}</>;
    case AmountUnit.SquareMeters:
      return <>{withUnit(formatNumber(amount.amount), t("quote.units.squareMeters"))}</>;
    default:
      throw new Error(`Unexpected amount unit: ${String(amount.unit)}`);
  }
}

function withUnit(value: string, unit: string): string {
  return `${value}${NonBreakingSpace}${unit}`;
}
