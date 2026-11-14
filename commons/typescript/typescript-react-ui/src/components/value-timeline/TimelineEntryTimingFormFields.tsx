import { startOfDay, startOfNextDay, startOfNextMonth, startOfNextWeek } from "@neckar-hub/typescript-utils/date";
import { useMemo } from "react";
import { useEmbeddedTranslation } from "../../localization/useEmbeddedTranslation";
import { RadioGroupFormField, RadioGroupFormFieldItem } from "../forms/radio-group-form-field";
import { DateTimePickerFormField } from "../inputs/date-time";
import { timelineEntryTimingSchema } from "./timelineEntryTimingSchema";

interface TimelineEntryTimingFormFieldsProps {
  /**
   * Label of the radio group — value-specific wording ("Preis ab", "Konditionen ab").
   */
  timingLabel: string;
  /**
   * The currently selected timing, watched by the caller (`form.watch("type")`). The date picker
   * renders only for `"scheduled"`.
   */
  type: string;
}

/**
 * The timing block of an "add timeline entry" form: the immediate/scheduled radio group and — for
 * the scheduled variant — the date-time picker with quick-select shortcuts.
 *
 * Identical for every value timeline; the fields the entry's value needs are rendered by the
 * instantiation's form page next to this block.
 */
export function TimelineEntryTimingFormFields({ timingLabel, type }: TimelineEntryTimingFormFieldsProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();

  // Quick-selects and `min` are anchored to render time. For a form that lives
  // long enough to cross midnight this would drift — acceptable here (new
  // entries are a short-lived dialog), but memoize to avoid unnecessary
  // re-renders inside the picker.
  const { today, quickSelects } = useMemo(() => {
    const now = new Date();
    return {
      today: startOfDay(now),
      quickSelects: [
        { title: t("timeline.timing.tomorrow"), date: startOfNextDay(now) },
        { title: t("timeline.timing.nextWeek"), date: startOfNextWeek(now) },
        { title: t("timeline.timing.nextMonth"), date: startOfNextMonth(now) },
      ],
    };
  }, [t]);

  return (
    <>
      <RadioGroupFormField formSchema={timelineEntryTimingSchema} fieldName="type" label={timingLabel}>
        <RadioGroupFormFieldItem value="now">{t("timeline.timing.now")}</RadioGroupFormFieldItem>
        <RadioGroupFormFieldItem value="scheduled">{t("timeline.timing.scheduled")}</RadioGroupFormFieldItem>
      </RadioGroupFormField>

      {type === "scheduled" && (
        <DateTimePickerFormField
          formSchema={timelineEntryTimingSchema}
          fieldName="validFrom"
          label={t("timeline.fields.validFrom")}
          min={today}
          quickSelects={quickSelects}
        />
      )}
    </>
  );
}
