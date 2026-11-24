import { zodResolver } from "@hookform/resolvers/zod";
import { useMemo } from "react";
import type { z } from "zod";
import { useEmbeddedTranslation } from "../../localization/useEmbeddedTranslation";
import type { TimelineEntryTimingSchema } from "./timelineEntryTimingSchema";

/**
 * Adds to `schema` the rule that a scheduled entry needs `validFrom`, reported at `validFrom`.
 * zod runs the rule only once the object parse passes, so an invalid value field hides the message.
 */
export function useTimelineEntryFormResolver<Schema extends z.ZodType<TimelineEntryTimingSchema, Record<string, unknown>>>(schema: Schema) {
  const { t } = useEmbeddedTranslation();
  return useMemo(
    () =>
      zodResolver(
        schema.superRefine((values, ctx) => {
          if (values.type === "scheduled" && values.validFrom == null) {
            ctx.addIssue({ code: "custom", path: ["validFrom"], message: t("timeline.validationErrors.validFromRequired") });
          }
        }),
      ),
    [schema, t],
  );
}
