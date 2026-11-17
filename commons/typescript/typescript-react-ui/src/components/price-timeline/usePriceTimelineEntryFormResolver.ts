import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { timelineEntryTimingDefaultValues, timelineEntryTimingSchema } from "../value-timeline";

/**
 * Form schema for a new price timeline entry: the shared timing fields
 * ({@link timelineEntryTimingSchema}, the `Now` | `ScheduledFor` radio plus the optional date)
 * extended by the price.
 *
 * `price` is in cents, matching the REST schema directly; `EuroInputFormField`
 * handles cents ↔ euros on the input.
 */
export const priceTimelineEntryUpsertRequestSchema = timelineEntryTimingSchema.extend({
  price: z.number().int().positive(),
});

export type PriceTimelineEntryUpsertRequestSchema = z.infer<typeof priceTimelineEntryUpsertRequestSchema>;

/**
 * Form defaults of an "add price entry" page: the timing fields as their explicit empty value.
 *
 * The price carries no default — an untouched form is invalid, which is what makes the empty form
 * reject the submit. `Required` over the remaining fields, so a field added to the schema has to be
 * seeded here or this stops compiling.
 */
export const priceTimelineEntryUpsertDefaultValues: Required<Omit<PriceTimelineEntryUpsertRequestSchema, "price">> = {
  ...timelineEntryTimingDefaultValues,
};

/** Every field of the form, the price only where a template supplies it. */
type PriceTimelineEntryUpsertDefaults = Required<Omit<PriceTimelineEntryUpsertRequestSchema, "price">> &
  Partial<Pick<PriceTimelineEntryUpsertRequestSchema, "price">>;

/**
 * Form defaults of an "add price entry" page that starts from the template of
 * `POST …/price-timeline/new`: the price in effect, or no price when none is in effect yet.
 */
export function priceTimelineEntryUpsertDefaultValuesFrom(template: { price: number | null }): PriceTimelineEntryUpsertDefaults {
  if (template.price === null) {
    return priceTimelineEntryUpsertDefaultValues;
  }
  return { ...priceTimelineEntryUpsertDefaultValues, price: template.price };
}

export function usePriceTimelineEntryFormResolver() {
  return zodResolver(priceTimelineEntryUpsertRequestSchema);
}
