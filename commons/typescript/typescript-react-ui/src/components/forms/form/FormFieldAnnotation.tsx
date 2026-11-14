/**
 * Imports
 */

import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import { useTranslation } from "@neckar-hub/typescript-react-i18n";
import { isDefined } from "@neckar-hub/typescript-utils/lang";
import { cn } from "cn";
import get from "lodash/get";
import { useFormContext } from "react-hook-form";
import { findFirstErrorMessage } from "./findFirstErrorMessage";
import { FormDescription, FormMessage, useFormFieldName } from "./Form";

/**
 * Prop Types
 */

export interface FormFieldAnnotationProps {
  /**
   * Text Description (will attempt to translate, takes precedence over descriptionComponent, will not be rendered if there is an error)
   */
  description?: TextKey;
  /**
   * Description Component (will be used if description is not defined, not translated, will not be rendered if there is an error)
   */
  descriptionComponent?: React.ReactNode;
  disabled: boolean | undefined;
  className?: string;
}

/**
 * Form Field Annotation
 *
 * Component that renders a form field validation error (if there is one) or the form field description (if there is one)
 */

export function FormFieldAnnotation(props: FormFieldAnnotationProps): React.ReactNode {
  const { t } = useTranslation();

  const form = useFormContext();
  const fieldName = useFormFieldName();
  const errorsForField = get(form.formState.errors, fieldName);
  const firstErrorMessage = findFirstErrorMessage(errorsForField);

  // FormMessage reads the error from the form field context.
  if (firstErrorMessage) {
    return <FormMessage className={props.className}>{firstErrorMessage}</FormMessage>;
  }

  // Render the description if there is no error and the description is defined
  if (isDefined(props.description) || isDefined(props.descriptionComponent)) {
    return (
      <FormDescription className={cn({ "opacity-50": props.disabled }, props.className)}>
        {props.description ? t(props.description) : props.descriptionComponent}
      </FormDescription>
    );
  }

  // don't render anything if there is no error and no description
  return null;
}
