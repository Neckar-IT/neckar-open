import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { Button, type ButtonProps } from "../../primitives/button";

export interface CancelButtonProps extends Omit<ButtonProps, "type"> {}

/**
 * Basic Cancel Button
 *
 * This button can be used to closed dialogs or cancel other actions.
 * To cancel a form submission, please use the FormCancelButton component instead.
 *
 * The button **never steals focus** from the active element (`preventDefault` on mousedown):
 * cancelling must not depend on the form's state. If the button took focus, the blurred field
 * would fire its `onTouched` validation, the appearing error message would shift the layout, and
 * the browser would swallow the click (`mousedown` / `mouseup` landing on different elements).
 * Click semantics stay intact — moving the pointer away before release still aborts, and keyboard
 * activation is unaffected. See issue #2431.
 */
export function CancelButton({ children, onMouseDown, ...rest }: CancelButtonProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();

  return (
    <Button
      variant="outline"
      onMouseDown={(event) => {
        event.preventDefault();
        onMouseDown?.(event);
      }}
      {...rest}
    >
      {children ?? t("cancelButton.label")}
    </Button>
  );
}
