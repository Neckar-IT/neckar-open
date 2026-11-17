import { EuroInputFormField } from "@neckar-hub/typescript-react-ui/quantities";
import type { UseFormReturn } from "react-hook-form";
import { useEmbeddedTranslation } from "../../localization/useEmbeddedTranslation";
import { TimelineEntryTimingFormFields } from "../value-timeline";
import { priceTimelineEntryUpsertRequestSchema, type PriceTimelineEntryUpsertRequestSchema } from "./usePriceTimelineEntryFormResolver";

interface PriceTimelineEntryFormFieldsProps {
  form: UseFormReturn<PriceTimelineEntryUpsertRequestSchema>;
}

/**
 * Field group for a price timeline entry: the shared timing block ({@link TimelineEntryTimingFormFields},
 * following the sealed-union REST contract `Now` | `ScheduledFor`) plus the euro price input.
 *
 * A building block, not a page — it carries no `Page`, `FormCard`, `ButtonBar` or navigation. Each call
 * site wraps it in its own page shell and wires its own save/cancel actions. The field labels stay with
 * this block via the shared library's embedded translation.
 */
export function PriceTimelineEntryFormFields({ form }: PriceTimelineEntryFormFieldsProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const type = form.watch("type");

  return (
    <>
      <TimelineEntryTimingFormFields timingLabel={t("priceTimeline.fields.timing")} type={type} />
      <EuroInputFormField formSchema={priceTimelineEntryUpsertRequestSchema} fieldName="price" label={t("priceTimeline.fields.price")} />
    </>
  );
}
