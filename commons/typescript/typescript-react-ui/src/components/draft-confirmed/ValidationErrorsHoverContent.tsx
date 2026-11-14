import { useEmbeddedTranslation } from "../../localization/useEmbeddedTranslation";
import type { FieldValidationError } from "./validation";
import { ValidationErrorsList } from "./ValidationErrorsList";

interface ValidationErrorsHoverContentProps {
  errors: FieldValidationError[];
}

/**
 * Content for a HoverCard that explains why an entity cannot be confirmed.
 * Shows a heading and a list of validation errors.
 */
export function ValidationErrorsHoverContent({ errors }: ValidationErrorsHoverContentProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();

  return (
    <>
      <p className="font-semibold">{t("validation.preventConfirmation")}</p>
      <ValidationErrorsList errors={errors} />
    </>
  );
}
