import { formatIsoDurationFromHours } from "@neckar-hub/typescript-utils/duration";
import type {
  ArticleConditions,
  ArticleConditionsWithAccessory,
  ArticleConditionsWithInstallationTime,
  FullArticleConditions,
  PriceWithProfit,
} from "@neckar-hub/typescript-utils/openapi-types";
import type { ArticleConditionsTimelineEntryUpsertRequestSchema } from "./useArticleConditionsTimelineEntryFormResolver";

/** The domain's canonical "per piece" unit — encoded as `null` on the wire, never as this string. */
const PIECE_UNIT = "Stück";

/**
 * Normalizes the free-text unit field onto the wire encoding: `null` means "per piece". A `null`
 * (an emptied `TextInput`), empty or whitespace-only value is the common per-piece case; an explicitly typed "Stück" (trimmed,
 * case-insensitive) is the same meaning and must not reach the wire as a literal, because the domain
 * rejects an explicit piece unit. Any other unit is trimmed and passed through.
 */
export function normalizePartUnit(partUnit: string | null | undefined): string | null {
  const trimmed = partUnit?.trim() ?? "";
  if (trimmed === "" || trimmed.toLowerCase() === PIECE_UNIT.toLowerCase()) return null;
  return trimmed;
}

/**
 * Projects the part fields of the form onto the {@link ArticleConditions} of the wire format — the
 * shape every article type has.
 *
 * The unit is normalized to `null` for the per-piece case (empty or a typed "Stück"), so the domain
 * invariant "no explicit piece unit" cannot be violated through the form.
 *
 * No discriminator is written: which implementation the article type uses is part of the endpoint's
 * request type, not of the value. A page whose article type carries the optional parts uses
 * {@link toFullArticleConditions} — the request of an article type without them rejects the keys.
 */
export function toArticleConditions(values: ArticleConditionsTimelineEntryUpsertRequestSchema): ArticleConditions {
  return {
    part: { inputPrice: values.partPurchasePrice, sellingPrice: values.partSellingPrice },
    unit: normalizePartUnit(values.partUnit),
  };
}

/**
 * The accessory the form holds, or `null` when its checkbox is unchecked — regardless of what the
 * hidden fields still hold from an earlier edit. A checked box has mandatory fields (the resolver
 * rejects the form before this is reached), so the remaining nullish check is a type guard.
 */
function accessoryOf(values: ArticleConditionsTimelineEntryUpsertRequestSchema): PriceWithProfit | null {
  if (!values.withAccessory) return null;
  if (values.accessoryPurchasePrice == null || values.accessorySellingPrice == null) return null;

  return { inputPrice: values.accessoryPurchasePrice, sellingPrice: values.accessorySellingPrice };
}

/**
 * The installation time the form holds as an ISO-8601 duration, or `null` when its checkbox is
 * unchecked. Entered in hours; see {@link accessoryOf} for the unchecked-box rule.
 */
function installationTimeOf(values: ArticleConditionsTimelineEntryUpsertRequestSchema): string | null {
  if (!values.withInstallationTime) return null;
  if (values.installationTimeHours == null) return null;

  return formatIsoDurationFromHours(values.installationTimeHours);
}

/**
 * Projects the form onto the shape of an article type that ships with accessory but is not mounted —
 * a part sold over the counter with a bracket. The installation-time key is absent from the result;
 * the request of such an article type rejects it.
 */
export function toArticleConditionsWithAccessory(
  values: ArticleConditionsTimelineEntryUpsertRequestSchema,
): ArticleConditionsWithAccessory {
  return {
    ...toArticleConditions(values),
    accessory: accessoryOf(values),
  };
}

/**
 * Projects the form onto the shape of an article type that is mounted but ships without accessory —
 * a PV module, an independence manager. The accessory keys are absent from the result; the request
 * of such an article type rejects them.
 */
export function toArticleConditionsWithInstallationTime(
  values: ArticleConditionsTimelineEntryUpsertRequestSchema,
): ArticleConditionsWithInstallationTime {
  return {
    ...toArticleConditions(values),
    installationTime: installationTimeOf(values),
  };
}

/** Projects the form onto the shape of an article type carrying every optional part. */
export function toFullArticleConditions(values: ArticleConditionsTimelineEntryUpsertRequestSchema): FullArticleConditions {
  return {
    ...toArticleConditions(values),
    accessory: accessoryOf(values),
    installationTime: installationTimeOf(values),
  };
}
