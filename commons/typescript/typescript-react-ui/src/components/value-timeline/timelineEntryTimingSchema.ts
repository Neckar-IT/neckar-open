import { z } from "zod";

/**
 * The timing half of every "add timeline entry" form — shared by all value timelines.
 *
 * The UI offers a radio button for the timing, matching the sealed REST discriminated union
 * (`Now` | `ScheduledFor`) of the upsert requests:
 *
 * - `type: "now"` → the entry takes effect at the server's `now()`. No date input. The server is
 *   the single source of truth for "now".
 * - `type: "scheduled"` → the user picks a date + time via the `DateTimePickerFormField`, which
 *   offers quick-select shortcuts (morgen / nächste Woche / nächster Monat) next to the calendar.
 *
 * Flat `ZodObject` (not `z.discriminatedUnion`) because the commons `FormField*` components
 * require a plain ZodObject. `validFrom` presence for `"scheduled"` is checked in the action
 * hook, which also rebuilds the payload as the sealed REST shape.
 *
 * `validFrom` is `nullish`: `DateTimePickerFormField` reports a cleared date as `null` and a form
 * that never touched the field holds `undefined`. Accepting only one of them would reject the
 * form's own empty state at the object parse and block the submit.
 *
 * Each instantiation extends this with its value fields, e.g.
 * `timelineEntryTimingSchema.extend({ price: z.number().int().positive() })`, and builds its
 * `defaultValues` on {@link timelineEntryTimingDefaultValues}.
 */
export const timelineEntryTimingSchema = z.object({
  type: z.enum(["now", "scheduled"]),
  validFrom: z.date().nullish(),
});

export type TimelineEntryTimingSchema = z.infer<typeof timelineEntryTimingSchema>;

/**
 * Form defaults of the timing half: the entry takes effect at the server's `now()`, no date picked.
 *
 * Every timeline form spreads this instead of writing the timing defaults itself, so a new page
 * cannot leave a field out of `defaultValues` — react-hook-form treats an absent key as an
 * uncontrolled field.
 *
 * `Required` rather than the schema type itself: a field added to the schema has to be seeded here
 * or this stops compiling. The schema type would accept the omission, since its optional fields are
 * optional properties.
 */
export const timelineEntryTimingDefaultValues: Required<TimelineEntryTimingSchema> = {
  type: "now",
  validFrom: null,
};
