/**
 * Imports
 */

import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import { useTranslation } from "@neckar-hub/typescript-react-i18n";
import { isNotDefined } from "@neckar-hub/typescript-utils/lang";
import { cn } from "cn";
import { FormLabel, useFormField } from "./Form";

/**
 * Prop Types
 */

export interface FormFieldLabelProps {
  label?: TextKey;
  labelComponent?: React.ReactNode;
  required?: boolean;
  /**
   * If true, a blue asterisk will be displayed next to the label.
   * Indicates that this field is required for confirmation (but optional for saving as draft).
   */
  softRequired?: boolean;
  disabled?: boolean;
  className?: string;
}

/**
 * Label Component for Form Fields
 *
 * @prop label - A TextKey. The component will try to translate this key and display the key if no translation is found.
 * @prop labelComponent - A ReactNode that will be displayed instead of the label if provided (label takes precedence).
 * @prop required - If true, a red asterisk will be displayed next to the string label.Has no Effect on the labelComponent.
 */

export function FormFieldLabel(props: FormFieldLabelProps): React.ReactNode {
  const { formLabelId } = useFormField();

  if (!hasFormFieldLabel(props)) {
    return null;
  }

  return (
    <FormLabel id={formLabelId} className={cn({ "opacity-50": props.disabled }, props.className)}>
      {props.label ? (
        <FormFieldStringLabel label={props.label} required={props.required} softRequired={props.softRequired} />
      ) : (
        props.labelComponent
      )}
    </FormLabel>
  );
}

/**
 * Whether {@link FormFieldLabel} renders a label for these props
 */

export function hasFormFieldLabel(props: Pick<FormFieldLabelProps, "label" | "labelComponent">): boolean {
  return !(isNotDefined(props.label) && isNotDefined(props.labelComponent));
}

/**
 * Display the String Label with translated text
 */

function FormFieldStringLabel(props: { label: string; required?: boolean; softRequired?: boolean }): React.ReactElement {
  const { t } = useTranslation();

  return (
    <div className="flex items-center">
      <div>{t(props.label)}</div>
      {props.required ? (
        <span className="text-primary ml-1">*</span>
      ) : props.softRequired ? (
        <span className="text-primary/60 ml-1">*</span>
      ) : null}
    </div>
  );
}
