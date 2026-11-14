/**
 * Imports
 */

import { useNumberFormatter } from "@neckar-hub/typescript-react-i18n";
import { isNotDefined } from "@neckar-hub/typescript-utils/lang";
import { cn } from "cn";
import { NoValue } from "../../text/no-value";

/**
 * Prop Types
 */

export interface NumberOutputProps extends Omit<React.ComponentProps<"span">, "children"> {
  value: number | null | undefined;
  fractionDigits?: number;
  unit?: string;
  unitPosition?: "prefix" | "suffix";
  /**
   * A factor to convert the actual value to the displayed value
   */
  conversionFactor?: number;
}

/**
 * Component to display a formatted number
 *
 * @prop value - The number to display
 * @prop fractionDigits - Number of fraction digits to display
 * @prop unit - Optional unit to display alongside the number
 * @prop unitPosition - Position of the unit, either "prefix" or "suffix"
 */
export function NumberOutput({
  value,
  fractionDigits,
  unit,
  unitPosition = "suffix",
  conversionFactor = 1,
  className,
  ...rest
}: NumberOutputProps): React.ReactElement {
  const formatNumber = useNumberFormatter(fractionDigits);

  if (isNotDefined(value)) {
    return (
      <span {...rest} className={cn("text-nowrap whitespace-nowrap", className)}>
        <NoValue />
      </span>
    );
  }

  const displayedValue = value * conversionFactor;

  let valueString: string;
  switch (unitPosition) {
    case "prefix":
      valueString = unit ? `${unit} ${formatNumber(displayedValue)}` : formatNumber(displayedValue);
      break;
    case "suffix":
      valueString = unit ? `${formatNumber(displayedValue)} ${unit}` : formatNumber(displayedValue);
      break;
    default:
      throw new Error(`Unexpected value in Switch Statement: unitPosition`);
  }

  return (
    <span {...rest} className={cn("text-nowrap whitespace-nowrap", className)}>
      {valueString}
    </span>
  );
}
