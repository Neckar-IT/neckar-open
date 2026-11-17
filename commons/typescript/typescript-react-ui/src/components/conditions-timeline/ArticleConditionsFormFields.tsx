import type { UseFormReturn } from "react-hook-form";
import { useEmbeddedTranslation } from "../../localization/useEmbeddedTranslation";
import { CheckboxDependentFields, CheckboxFormField } from "../forms/checkbox-form-field";
import { FormFieldDisplay } from "../forms/form";
import { TextInputFormField } from "../forms/text-input-form-field";
import { Label } from "../primitives/label";
import { EuroInputFormField, HoursInputFormField } from "../quantities";
import { NoValue } from "../text/no-value";
import { TimelineEntryTimingFormFields } from "../value-timeline";
import { PriceSurcharge } from "./PriceSurcharge";
import {
  articleConditionsTimelineEntryUpsertRequestSchema,
  type ArticleConditionsTimelineEntryUpsertRequestSchema,
} from "./useArticleConditionsTimelineEntryFormResolver";

interface ArticleConditionsFormFieldsProps {
  form: UseFormReturn<ArticleConditionsTimelineEntryUpsertRequestSchema>;
}

/**
 * Field group every article-conditions entry has: the shared timing block
 * ({@link TimelineEntryTimingFormFields}, following the sealed-union REST contract `Now` | `ScheduledFor`)
 * and the part's purchase and selling price.
 *
 * A building block, not a page — it carries no `Page`, `FormCard`, `ButtonBar` or navigation. Optional parts
 * (unit, accessory, installation time) are separate blocks — {@link ArticleConditionsUnitFormField},
 * {@link ArticleConditionsAccessoryFormFields}, {@link ArticleConditionsInstallationTimeFormFields} — that a
 * call site whose article has them adds after this one, followed by the surcharges and the customer price in
 * {@link ArticleConditionsPriceOverviewSection}. An article that is sold per piece with no accessory or
 * installation (a book) renders this block and the overview alone. The projection onto the wire shape is
 * `toArticleConditions`.
 */
export function ArticleConditionsFormFields({ form }: ArticleConditionsFormFieldsProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const type = form.watch("type");

  return (
    <>
      <TimelineEntryTimingFormFields timingLabel={t("conditionsTimeline.fields.timing")} type={type} />
      <EuroInputFormField
        formSchema={articleConditionsTimelineEntryUpsertRequestSchema}
        fieldName="partPurchasePrice"
        label={t("conditionsTimeline.fields.purchasePrice")}
      />
      <EuroInputFormField
        formSchema={articleConditionsTimelineEntryUpsertRequestSchema}
        fieldName="partSellingPrice"
        label={t("conditionsTimeline.fields.sellingPrice")}
      />
    </>
  );
}

interface ArticleConditionsOptionalFormFieldsProps {
  form: UseFormReturn<ArticleConditionsTimelineEntryUpsertRequestSchema>;
}

/**
 * The unit text field: the unit one purchase/selling price refers to (e.g. "Meter"). Cleared to `null` for a
 * per-piece article. Rendered only by call sites whose article can be priced per unit.
 */
export function ArticleConditionsUnitFormField(): React.ReactElement {
  const { t } = useEmbeddedTranslation();

  return (
    <TextInputFormField
      formSchema={articleConditionsTimelineEntryUpsertRequestSchema}
      fieldName="partUnit"
      label={t("conditionsTimeline.fields.unit")}
      placeholder={t("conditionsTimeline.fields.unitPlaceholder")}
    />
  );
}

/**
 * Accessory checkbox gating the accessory purchase/selling price pair. Unchecked (the default) sends `null`;
 * checked, both prices are mandatory. The accessory carries no unit of its own — its price belongs to one unit
 * of the part. Rendered only by call sites whose article can carry an accessory.
 */
export function ArticleConditionsAccessoryFormFields({ form }: ArticleConditionsOptionalFormFieldsProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const withAccessory = form.watch("withAccessory");

  return (
    <>
      <CheckboxFormField
        formSchema={articleConditionsTimelineEntryUpsertRequestSchema}
        fieldName="withAccessory"
        label={t("conditionsTimeline.fields.withAccessory")}
        description={t("conditionsTimeline.fields.withAccessoryDescription")}
      />
      {withAccessory && (
        <CheckboxDependentFields>
          <EuroInputFormField
            formSchema={articleConditionsTimelineEntryUpsertRequestSchema}
            fieldName="accessoryPurchasePrice"
            label={t("conditionsTimeline.fields.accessoryPurchasePrice")}
          />
          <EuroInputFormField
            formSchema={articleConditionsTimelineEntryUpsertRequestSchema}
            fieldName="accessorySellingPrice"
            label={t("conditionsTimeline.fields.accessorySellingPrice")}
          />
        </CheckboxDependentFields>
      )}
    </>
  );
}

/**
 * Installation-time checkbox gating the person-hours input. Unchecked (the default) sends `null`; checked, the
 * hours are mandatory. Rendered only by call sites whose article carries installation time.
 */
export function ArticleConditionsInstallationTimeFormFields({ form }: ArticleConditionsOptionalFormFieldsProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const withInstallationTime = form.watch("withInstallationTime");

  return (
    <>
      <CheckboxFormField
        formSchema={articleConditionsTimelineEntryUpsertRequestSchema}
        fieldName="withInstallationTime"
        label={t("conditionsTimeline.fields.withInstallationTime")}
        description={t("conditionsTimeline.fields.withInstallationTimeDescription")}
      />
      {withInstallationTime && (
        <CheckboxDependentFields>
          <HoursInputFormField
            formSchema={articleConditionsTimelineEntryUpsertRequestSchema}
            fieldName="installationTimeHours"
            label={t("conditionsTimeline.fields.installationTime")}
          />
        </CheckboxDependentFields>
      )}
    </>
  );
}

interface SurchargePreviewProps {
  label: string;
  /** Cents, as held by the form. Nullish while the field is empty. */
  purchasePrice: number | null | undefined;
  /** Cents, as held by the form. Nullish while the field is empty. */
  sellingPrice: number | null | undefined;
}

/**
 * Shows what the entered price pair earns, so the calculation can be checked before saving.
 *
 * A read-only row through {@link FormFieldDisplay}, so it carries the same label-and-value rhythm as
 * the price fields it follows instead of a second, hand-built one.
 *
 * Updates whenever a price field is left, not on every keystroke: `NumberInput` reports its value on
 * blur, which is also when the entered digits first form a number worth calculating with.
 *
 * Holds its row from the start and shows {@link NoValue} until both prices are entered — a surcharge
 * derived from one price alone would state a result the input does not yet support, and a row that only
 * appears with the second price would shift the fields and buttons below it while the form is filled in.
 *
 * The figure sits in a box as tall as an `Input` and is centred in it, so it lines up with the text
 * *inside* the price fields rather than with the top edge of their boxes. Bare text would align with
 * the labels and read as belonging to the row above whenever the preview stands beside its prices
 * instead of below them. It is set at the `Input`'s own `text-sm` for the same reason: inheriting
 * the form's base size would render the derived figure a step larger than the amounts it is derived
 * from, and the largest number in the row would be the one nobody typed.
 */
export function SurchargePreview({ label, purchasePrice, sellingPrice }: SurchargePreviewProps): React.ReactElement {
  const bothPricesEntered = purchasePrice != null && sellingPrice != null;

  return (
    <FormFieldDisplay labelComponent={<Label>{label}</Label>}>
      <div className="flex h-10 items-center text-sm">
        {bothPricesEntered ? <PriceSurcharge price={{ inputPrice: purchasePrice, sellingPrice }} /> : <NoValue />}
      </div>
    </FormFieldDisplay>
  );
}
