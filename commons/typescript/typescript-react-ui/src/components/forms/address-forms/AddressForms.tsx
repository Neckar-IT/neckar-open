/**
 * Imports
 */

import { zodResolver } from "@hookform/resolvers/zod";
import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import type { Address, AddressConfirmedWithType, AddressDraftWithType } from "@neckar-hub/typescript-utils/openapi-types";
import { cn } from "cn";
import { type FieldPath, useForm } from "react-hook-form";
import z, { type ZodObject } from "zod";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { ButtonBar } from "../../layout/button-bar";
import { Form, FormSubmitButton, FormCancelButton } from "../form";
import { TextInputFormField } from "../text-input-form-field";

/**
 * Type Declarations
 */

export interface AddressFormFieldsTextKeys {
  street: TextKey;
  houseNumber: TextKey;
  zipCode: TextKey;
  city: TextKey;
}

interface AddressFormFieldsProps<T extends ZodObject> {
  /**
   * The zod Schema that contains the address fields (either as top-level or nested fields). If the address is nested, you have to provide
   * the schemaKey as well.
   */
  formSchema: T;
  /**
   * Optional prefix for the address fields in the schema
   */
  schemaKey?: FieldPath<z.infer<T>>;
  className?: string;
  disabled?: boolean;
  /**
   * When true, every address field renders a blue soft-required asterisk — the fields are optional
   * for saving a Draft but required for the eventual Confirm. Use with the nullable
   * {@link addressDraftSchema} so a partial address can still be saved.
   */
  softRequired?: boolean;
  textKeys?: Partial<AddressFormFieldsTextKeys>;
}

/**
 * Form Schema Definitions
 */

export const addressConfirmedSchema = z.object({
  street: z.string().nonempty(),
  houseNumber: z.string().nonempty(),
  city: z.string().nonempty(),
  zipCode: z.string().nonempty(),
});

export const addressDraftSchema = z.object({
  street: z.string().nullable(),
  houseNumber: z.string().nullable(),
  city: z.string().nullable(),
  zipCode: z.string().nullable(),
});

export type AddressConfirmedFormValues = z.infer<typeof addressConfirmedSchema>;
export type AddressDraftFormValues = z.infer<typeof addressDraftSchema>;

/**
 * Normalizes an address into draft-form default values. Address fields are required on the wire
 * (`street: string | null`) since `Address.Draft` lost its Kotlin property defaults, so `undefined`
 * can no longer occur by type. The normalization stays as the runtime guard for the rollout
 * window: a backend still running the previous model omits fields holding their default, and an
 * `undefined` default value would fail `z.string().nullable()` (`received_undefined` → a
 * "must not be empty" error on an untouched empty field).
 */
export function toAddressDraftFormValues(address: Address): AddressDraftFormValues {
  return {
    street: address.street ?? null,
    houseNumber: address.houseNumber ?? null,
    zipCode: address.zipCode ?? null,
    city: address.city ?? null,
  };
}

/**
 * Form Fields to create or update an Address
 */

export function AddressFormFields<T extends ZodObject>({
  formSchema,
  schemaKey,
  className,
  disabled,
  softRequired,
  textKeys,
}: AddressFormFieldsProps<T>): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const textKeysWithDefaults: AddressFormFieldsTextKeys = {
    street: textKeys?.street ?? "addressForms.street",
    houseNumber: textKeys?.houseNumber ?? "addressForms.houseNumber",
    zipCode: textKeys?.zipCode ?? "addressForms.zipCode",
    city: textKeys?.city ?? "addressForms.city",
  };

  const fieldNamePrefix = schemaKey ? `${schemaKey}.` : "";
  const fieldName = (name: string) => `${fieldNamePrefix}${name}` as FieldPath<z.infer<T>>;

  return (
    <div className={cn("flex flex-col gap-4", className)}>
      <div className="flex w-full gap-2">
        <TextInputFormField
          className="w-5/6"
          formSchema={formSchema}
          label={t(textKeysWithDefaults.street)}
          fieldName={fieldName("street")}
          disabled={disabled}
          softRequired={softRequired}
        />
        <TextInputFormField
          className="w-1/6 min-w-32"
          formSchema={formSchema}
          label={t(textKeysWithDefaults.houseNumber)}
          fieldName={fieldName("houseNumber")}
          disabled={disabled}
          softRequired={softRequired}
        />
      </div>

      <div className="flex w-full gap-2">
        <TextInputFormField
          className="w-1/6 min-w-32"
          formSchema={formSchema}
          label={t(textKeysWithDefaults.zipCode)}
          fieldName={fieldName("zipCode")}
          disabled={disabled}
          softRequired={softRequired}
        />
        <TextInputFormField
          className="w-5/6"
          formSchema={formSchema}
          label={t(textKeysWithDefaults.city)}
          fieldName={fieldName("city")}
          disabled={disabled}
          softRequired={softRequired}
        />
      </div>
    </div>
  );
}

interface AddressDraftFormProps {
  className?: string;
  address: Address;
  onSubmit: (address: AddressDraftWithType) => Promise<void>;
  onCancel: () => void;
}

/**
 * Form to edit an address, returns a draft address
 */

export function AddressDraftForm({ className, address, onSubmit, onCancel }: AddressDraftFormProps): React.ReactElement {
  const form = useForm<AddressDraftFormValues>({
    resolver: zodResolver(addressDraftSchema),
    defaultValues: toAddressDraftFormValues(address),
  });

  const handleSubmit = form.handleSubmit(async (formValues) => {
    await onSubmit({
      ...address,
      ...formValues,
      type: "Draft",
    });
  });

  return (
    <Form {...form} className={className} onSubmit={handleSubmit}>
      <AddressFormFields formSchema={addressDraftSchema} />
      <ButtonBar className="mt-4">
        <FormCancelButton onClick={onCancel} />
        <FormSubmitButton />
      </ButtonBar>
    </Form>
  );
}

interface AddressConfirmedFormTextKeys extends AddressFormFieldsTextKeys {
  confirm: TextKey;
}

interface AddressConfirmedFormProps {
  className?: string;
  address: Address;
  onSubmit: (address: AddressConfirmedWithType) => Promise<void>;
  onCancel: () => void;
  textKeys?: Partial<AddressConfirmedFormTextKeys>;
}

/**
 * Form to edit and confirm an address, returns a confirmed address
 */

export function AddressConfirmedForm({ className, address, onSubmit, onCancel, textKeys }: AddressConfirmedFormProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();

  // address may be Draft (nullable fields); confirmed schema requires non-empty strings,
  // so map null/missing fields to "" so the form starts with empty inputs the user can fill in.
  const form = useForm<AddressConfirmedFormValues>({
    resolver: zodResolver(addressConfirmedSchema),
    defaultValues: {
      street: address.street ?? "",
      houseNumber: address.houseNumber ?? "",
      city: address.city ?? "",
      zipCode: address.zipCode ?? "",
    },
  });

  const handleSubmit = form.handleSubmit(async (formValues) => {
    await onSubmit({
      ...address,
      ...formValues,
      // Confirmed requires a country; the form does not edit it, so fall back to the domain default.
      country: address.country ?? "Germany",
      type: "Confirmed",
    });
  });

  return (
    <Form {...form} className={className} onSubmit={handleSubmit}>
      <AddressFormFields formSchema={addressConfirmedSchema} textKeys={textKeys} />
      <ButtonBar className="mt-4">
        <FormCancelButton onClick={onCancel} />
        <FormSubmitButton>{t(textKeys?.confirm ?? "addressForms.confirm")}</FormSubmitButton>
      </ButtonBar>
    </Form>
  );
}
