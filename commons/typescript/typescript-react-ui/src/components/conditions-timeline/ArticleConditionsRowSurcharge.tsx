import type { ArticleConditionsPriceRow } from "@neckar-hub/typescript-utils/openapi-types";
import { NoValue } from "../text/no-value";
import { SurchargeOutput } from "./PriceSurcharge";

/** The server-calculated surcharge of a part or accessory row. */
export function ArticleConditionsRowSurcharge({ row }: { row: ArticleConditionsPriceRow }): React.ReactElement {
  return row.surcharge === null ? <NoValue /> : <SurchargeOutput surcharge={row.surcharge} surchargeRate={row.surchargeRate} />;
}
