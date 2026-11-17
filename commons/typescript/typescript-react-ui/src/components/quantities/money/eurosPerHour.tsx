import { NumberOutput, type NumberOutputProps } from "../../data-display/number-output";
import { conversionFactors } from "../conversionFactors";
import { type StripUnitProps } from "../StripUnitProps";

const unit = "€/h";
const conversionFactor = conversionFactors.money.toEuro;

/**
 * Displays an hourly rate in Euros
 *
 * @prop value - the rate per hour in cents
 */
export function EuroPerHourOutput({ fractionDigits = 2, ...rest }: StripUnitProps<NumberOutputProps>): React.ReactElement {
  return <NumberOutput {...rest} unit={unit} conversionFactor={conversionFactor} fractionDigits={fractionDigits} />;
}
