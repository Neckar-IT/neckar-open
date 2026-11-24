import { parseIsoDurationHours } from "@neckar-hub/typescript-utils/duration";
import type { ArticleConditions, PriceWithProfit } from "@neckar-hub/typescript-utils/openapi-types";
import { z } from "zod";
import { timelineEntryTimingDefaultValues, timelineEntryTimingSchema, useTimelineEntryFormResolver } from "../value-timeline";

/**
 * Plain object schema of the "add conditions entry" form: the shared timing fields
 * ({@link timelineEntryTimingSchema}, the `Now` | `ScheduledFor` radio plus the optional date)
 * extended by the commercial state of the article. Used as the `formSchema` prop of the field
 * components, which require a `ZodObject` for field-name inference.
 *
 * The unit is optional (an empty string is allowed): an empty value is the per-piece default that
 * {@link toArticleConditions} normalizes to `null`, so there is no required-field error for it.
 *
 * Accessory and installation time are optional on the domain object, so the form gates them behind
 * a checkbox each: unchecked (the default) hides the fields and sends `null`; checked makes the
 * gated fields mandatory. The accessory carries no unit of its own — its price belongs to one unit
 * of the part. That requirement is a cross-field one and therefore lives in
 * [articleConditionsTimelineEntryUpsertResolverSchema], not in this object.
 *
 * Every optional field is `nullish`, not `optional`: an emptied `TextInput` or `NumberInput`
 * reports `null`, while a field left out of `defaultValues` is `undefined`. A field accepting only
 * one of them fails the object parse, which happens before the checkbox rules below run — so a
 * cleared price would block the submit even with its checkbox unchecked.
 *
 * Prices are in cents, matching the REST schema directly; `EuroInputFormField` handles
 * cents ↔ euros on the input. The installation time is entered in hours and converted to the
 * ISO-8601 duration of the wire format by {@link toArticleConditions}.
 */
export const articleConditionsTimelineEntryUpsertRequestSchema = timelineEntryTimingSchema.extend({
  partPurchasePrice: z.number().int().nonnegative(),
  partSellingPrice: z.number().int().nonnegative(),
  partUnit: z.string().nullish(),

  withAccessory: z.boolean(),
  accessoryPurchasePrice: z.number().int().nonnegative().nullish(),
  accessorySellingPrice: z.number().int().nonnegative().nullish(),

  withInstallationTime: z.boolean(),
  installationTimeHours: z.number().positive().nullish(),
});

export type ArticleConditionsTimelineEntryUpsertRequestSchema = z.infer<typeof articleConditionsTimelineEntryUpsertRequestSchema>;

type PartPriceField = "partPurchasePrice" | "partSellingPrice";

/**
 * Every field of the form, the two mandatory part prices only where a template supplies them — an
 * untouched empty form is invalid, and that is what makes it reject the submit.
 *
 * `Required` over the remaining fields, so a field added to the schema has to be seeded in
 * {@link articleConditionsTimelineEntryUpsertDefaultValues} or that stops compiling.
 */
type ArticleConditionsTimelineEntryUpsertDefaults = Required<Omit<ArticleConditionsTimelineEntryUpsertRequestSchema, PartPriceField>> &
  Partial<Pick<ArticleConditionsTimelineEntryUpsertRequestSchema, PartPriceField>>;

/**
 * The conditions a new entry starts from, whichever optional parts the article type declares — the
 * template of `POST …/conditions-timeline/new` of every article type is assignable to it.
 */
interface ArticleConditionsToStartFrom {
  conditions: (ArticleConditions & { accessory?: PriceWithProfit | null; installationTime?: string | null }) | null;
}

/**
 * Form defaults of an "add conditions entry" page. With conditions each optional-part checkbox mirrors
 * the entry in effect; without them `optionalParts` decides and every optional field starts empty.
 */
export function articleConditionsTimelineEntryUpsertDefaultValues(
  optionalParts: {
    withAccessory: boolean;
    withInstallationTime: boolean;
  },
  template: ArticleConditionsToStartFrom,
): ArticleConditionsTimelineEntryUpsertDefaults {
  const conditions = template.conditions;
  if (conditions === null) {
    return {
      ...timelineEntryTimingDefaultValues,
      partUnit: null,
      withAccessory: optionalParts.withAccessory,
      accessoryPurchasePrice: null,
      accessorySellingPrice: null,
      withInstallationTime: optionalParts.withInstallationTime,
      installationTimeHours: null,
    };
  }

  const accessory = conditions.accessory ?? null;
  const installationTime = conditions.installationTime ?? null;
  return {
    ...timelineEntryTimingDefaultValues,
    partPurchasePrice: conditions.part.inputPrice,
    partSellingPrice: conditions.part.sellingPrice,
    partUnit: conditions.unit ?? null,
    withAccessory: accessory !== null,
    accessoryPurchasePrice: accessory?.inputPrice ?? null,
    accessorySellingPrice: accessory?.sellingPrice ?? null,
    withInstallationTime: installationTime !== null,
    installationTimeHours: installationTimeHoursOf(installationTime),
  };
}

/**
 * The installation time of the wire format in hours. Kotlin writes a `Duration` as `PT…H…M…S` only,
 * so a value the parser cannot read is a broken response, not an empty field.
 */
function installationTimeHoursOf(installationTime: string | null): number | null {
  if (installationTime === null) return null;

  const hours = parseIsoDurationHours(installationTime);
  if (hours === null) throw new Error(`Unreadable installation time: ${installationTime}`);
  return hours;
}

/**
 * Validation schema for the resolver: the plain object plus the checkbox-gated requirements.
 *
 * An accessory is all-or-nothing on the wire — a `PriceWithProfit` needs both prices — so checking
 * the accessory box makes both fields mandatory. The same holds for the installation time with its
 * single field. Unchecked boxes leave their fields unvalidated; the values are dropped on submit.
 */
const articleConditionsTimelineEntryUpsertResolverSchema = articleConditionsTimelineEntryUpsertRequestSchema.superRefine((values, ctx) => {
  if (values.withAccessory) {
    if (values.accessoryPurchasePrice == null) {
      ctx.addIssue({ code: "custom", path: ["accessoryPurchasePrice"], message: "Accessory purchase price is required" });
    }
    if (values.accessorySellingPrice == null) {
      ctx.addIssue({ code: "custom", path: ["accessorySellingPrice"], message: "Accessory selling price is required" });
    }
  }

  if (values.withInstallationTime && values.installationTimeHours == null) {
    ctx.addIssue({ code: "custom", path: ["installationTimeHours"], message: "Installation time is required" });
  }
});

export function useArticleConditionsTimelineEntryFormResolver() {
  return useTimelineEntryFormResolver(articleConditionsTimelineEntryUpsertResolverSchema);
}
