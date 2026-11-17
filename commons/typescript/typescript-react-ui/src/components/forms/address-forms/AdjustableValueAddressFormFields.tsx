/**
 * Imports
 */

import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import {
  type AdjustableValueAddressDraft,
  AdjustableValueAdjustedAddressDraftWithTypeType,
  AdjustableValueAutoAddressDraftWithTypeType,
  AdjustableValueManualAddressDraftWithTypeType,
} from "@neckar-hub/typescript-utils/openapi-types";
import { cn } from "cn";
import { InfoIcon, PencilIcon, UndoIcon } from "lucide-react";
import { type FieldPath, type FieldValues, useFormContext, useWatch } from "react-hook-form";
import z, { type ZodObject } from "zod";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { AddressOutput } from "../../data-display/address-output";
import { Button } from "../../primitives/button";
import { Label } from "../../primitives/label";
import { Popover, PopoverContent, PopoverTrigger } from "../../primitives/popover";
import { AddressFormFields, type AddressFormFieldsTextKeys } from "./AddressForms";

/**
 * Destructure type variables to enhance readability
 */

const { Adjusted } = AdjustableValueAdjustedAddressDraftWithTypeType;
const { Auto } = AdjustableValueAutoAddressDraftWithTypeType;
const { Manual } = AdjustableValueManualAddressDraftWithTypeType;

/**
 * Builds the form schema for an adjustable address around the schema of the address itself.
 *
 * The address schema is a parameter because how strict an address has to be depends on the form, not on the
 * adjustable wrapper: a draft form accepts empty fields (`addressDraftSchema`), a form that confirms the address
 * does not (`addressConfirmedSchema`).
 */

export function adjustableValueAddressSchema<A extends ZodObject>(addressSchema: A) {
  return z.discriminatedUnion("type", [
    z.object({ type: z.literal(Adjusted), calculated: addressSchema, value: addressSchema }),
    z.object({ type: z.literal(Auto), value: addressSchema }),
    z.object({ type: z.literal(Manual), value: addressSchema }),
  ]);
}

/**
 * Turns a calculated address into an override the user may edit, keeping the calculated one to fall back to.
 *
 * The counterpart of {@link resetToCalculatedAddress}: it is what makes an `Auto` address editable at all.
 */

export function overrideCalculatedAddress(adjustableValue: AdjustableValueAddressDraft): AdjustableValueAddressDraft {
  switch (adjustableValue.type) {
    case Auto:
      // The calculated address becomes the starting point of the override and stays available for a reset
      return { type: Adjusted, calculated: adjustableValue.value, value: adjustableValue.value };
    case Adjusted:
    case Manual:
      return adjustableValue;
    default:
      throw new Error(`Unexpected value in Switch Statement: adjustableValue.type`);
  }
}

/**
 * Discards the user's override and returns to the calculated address.
 *
 * `Auto` and `Manual` are returned unchanged - neither carries a second address to fall back to, so this does not
 * always end up on `Auto`. Mirrors `AdjustableValue.resetToCalculated` in Kotlin.
 */

export function resetToCalculatedAddress(adjustableValue: AdjustableValueAddressDraft): AdjustableValueAddressDraft {
  switch (adjustableValue.type) {
    case Adjusted:
      return { type: Auto, value: adjustableValue.calculated };
    case Auto:
    case Manual:
      return adjustableValue;
    default:
      throw new Error(`Unexpected value in Switch Statement: adjustableValue.type`);
  }
}

/**
 * Prop Types
 */

interface AdjustableValueAddressFormFieldsTextKeys extends AddressFormFieldsTextKeys {
  heading: TextKey;
  calculatedValueLabel: TextKey;
  override: TextKey;
  reset: TextKey;
  showCalculated: TextKey;
}

export interface AdjustableValueAddressFormFieldsProps<T extends ZodObject> {
  /**
   * The zod schema of the whole form. Build the adjustable address inside it with {@link adjustableValueAddressSchema}.
   */
  formSchema: T;
  /**
   * Path to the adjustable address within the form values, e.g. `jobSiteLocation`. The address fields themselves live
   * below it, under `<schemaKey>.value`.
   */
  schemaKey: FieldPath<z.infer<T>>;
  className?: string;
  disabled?: boolean;
  /**
   * When true, every address field renders a blue soft-required asterisk - see {@link AddressFormFields}.
   */
  softRequired?: boolean;
  textKeys?: Partial<AdjustableValueAddressFormFieldsTextKeys>;
}

/**
 * Form fields to edit an address that carries where it came from - the address counterpart of
 * `AdjustableValueTextInput`.
 *
 * The three variants of an adjustable value each get their own state, so the origin of the address stays visible
 * instead of being silently overwritten:
 *
 * | Variant    | Address fields | Actions                                                     |
 * |------------|----------------|-------------------------------------------------------------|
 * | `Auto`     | disabled       | pencil: start overriding the calculated address              |
 * | `Adjusted` | editable       | info: reveal the calculated address; undo: return to it      |
 * | `Manual`   | editable       | none - nothing was calculated, so there is nothing to reset  |
 *
 * ## Requirements
 *  - Must be rendered inside a `<Form/>` (react-hook-form `FormProvider`).
 *  - The form values at `schemaKey` hold the whole adjustable value, not just the address.
 */

export function AdjustableValueAddressFormFields<T extends ZodObject>({
  formSchema,
  schemaKey,
  className,
  disabled,
  softRequired,
  textKeys,
}: AdjustableValueAddressFormFieldsProps<T>): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const { setValue, formState } = useFormContext();

  // FormFieldBase disables the address fields while submitting or read-only; the header actions are not form fields and
  // must follow along.
  const disabledOrSubmitting = disabled || formState.isSubmitting || formState.disabled;

  const textKeysWithDefaults: AdjustableValueAddressFormFieldsTextKeys = {
    street: textKeys?.street ?? "addressForms.street",
    houseNumber: textKeys?.houseNumber ?? "addressForms.houseNumber",
    zipCode: textKeys?.zipCode ?? "addressForms.zipCode",
    city: textKeys?.city ?? "addressForms.city",
    heading: textKeys?.heading ?? "adjustableValueAddress.heading",
    calculatedValueLabel: textKeys?.calculatedValueLabel ?? "adjustableValueFormField.calculatedValueLabel",
    override: textKeys?.override ?? "adjustableValueAddress.override",
    reset: textKeys?.reset ?? "adjustableValueAddress.reset",
    showCalculated: textKeys?.showCalculated ?? "adjustableValueAddress.showCalculated",
  };

  const adjustableValue = useWatch({ name: schemaKey }) as AdjustableValueAddressDraft | undefined;

  if (adjustableValue === undefined) {
    throw new Error(`No adjustable address found at "${schemaKey}" - the form needs a default value holding the whole adjustable value`);
  }

  // Replace the whole adjustable value at once so the address fields follow the new variant. The path cast is the one
  // AddressFormFields also needs: a generic schema's field paths are only known at the call site.
  const applyVariant = (updated: AdjustableValueAddressDraft) => {
    setValue(schemaKey as FieldPath<FieldValues>, updated, { shouldDirty: true });
  };

  return (
    <div className={cn("flex flex-col gap-4", className)}>
      <div className="flex min-h-9 items-center justify-between gap-2">
        <Label>{t(textKeysWithDefaults.heading)}</Label>

        {/* A Manual address gets no action at all: it was never calculated, so there is nothing to reveal, nothing
            to reset to, and its fields are editable to begin with */}
        <div className="flex items-center">
          {adjustableValue.type === Adjusted ? (
            <>
              <Popover>
                <PopoverTrigger asChild>
                  <Button variant="ghost" size="icon" aria-label={t(textKeysWithDefaults.showCalculated)}>
                    <InfoIcon className="text-muted-foreground" />
                  </Button>
                </PopoverTrigger>
                <PopoverContent className="text-sm">
                  <div className="text-muted-foreground">{t(textKeysWithDefaults.calculatedValueLabel)}</div>
                  <AddressOutput address={adjustableValue.calculated} />
                </PopoverContent>
              </Popover>

              <Button
                variant="ghost"
                size="icon"
                disabled={disabledOrSubmitting}
                aria-label={t(textKeysWithDefaults.reset)}
                onClick={() => applyVariant(resetToCalculatedAddress(adjustableValue))}
              >
                <UndoIcon />
              </Button>
            </>
          ) : null}

          {adjustableValue.type === Auto ? (
            <Button
              variant="ghost"
              size="icon"
              disabled={disabledOrSubmitting}
              aria-label={t(textKeysWithDefaults.override)}
              onClick={() => applyVariant(overrideCalculatedAddress(adjustableValue))}
            >
              <PencilIcon />
            </Button>
          ) : null}
        </div>
      </div>

      <AddressFormFields
        formSchema={formSchema}
        schemaKey={`${schemaKey}.value` as FieldPath<z.infer<T>>}
        // The calculated address of an Auto value is shown, not edited: overriding it is the pencil's job
        disabled={adjustableValue.type === Auto || disabledOrSubmitting}
        softRequired={softRequired}
        textKeys={textKeysWithDefaults}
      />
    </div>
  );
}
