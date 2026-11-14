import { useFormState } from "react-hook-form";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { Button, type ButtonProps } from "../../primitives/button";

interface FormSubmitButtonProps extends Omit<ButtonProps, "type" | "loading"> {}

export function FormSubmitButton({ children, ...rest }: FormSubmitButtonProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const { isValid, isSubmitting } = useFormState();

  return (
    <Button type="submit" loading={isSubmitting} aria-invalid={!isValid} {...rest}>
      {children ?? t("submitButton.label")}
    </Button>
  );
}
