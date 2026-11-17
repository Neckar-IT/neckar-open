import { parseIsoDurationHours } from "@neckar-hub/typescript-utils/duration";
import type { ArticleConditionsLabourPriceRow } from "@neckar-hub/typescript-utils/openapi-types";
import { EuroPerHourOutput, HoursOutput } from "../quantities";
import { NoValue } from "../text/no-value";

/**
 * What a labour price comes from: `8,5 h × 75,00 €/h`.
 */
export function ArticleConditionsLabourBreakdown({
  labour,
  rateClassName,
}: {
  labour: ArticleConditionsLabourPriceRow;
  rateClassName?: string;
}): React.ReactElement {
  const hours = parseIsoDurationHours(labour.installationTime);

  return (
    <>
      {hours === null ? <NoValue /> : <HoursOutput value={hours} />}
      {" × "}
      <EuroPerHourOutput value={labour.hourlyRate} className={rateClassName} />
    </>
  );
}
