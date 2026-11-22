import { useEmbeddedTranslation } from "../../localization/useEmbeddedTranslation";
import { LockableButton } from "../actions/LockableButton";
import type { ButtonProps } from "../primitives/button";

interface ConfirmButtonProps extends Omit<ButtonProps, "variant"> {}

export function ConfirmButton({ children, ...rest }: ConfirmButtonProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();

  return (
    <LockableButton variant="confirm" {...rest}>
      {children ?? t("confirmation.buttonLabel")}
    </LockableButton>
  );
}
