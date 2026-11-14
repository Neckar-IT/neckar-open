import { useEmbeddedTranslation } from "../../localization/useEmbeddedTranslation";
import { Button, type ButtonProps } from "../primitives/button";

interface ConfirmButtonProps extends Omit<ButtonProps, "variant"> {}

export function ConfirmButton({ children, ...rest }: ConfirmButtonProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();

  return (
    <Button variant="confirm" {...rest}>
      {children ?? t("confirmation.buttonLabel")}
    </Button>
  );
}
