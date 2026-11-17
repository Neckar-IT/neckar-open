import { DialogBody } from "../../primitives/dialog";
import { FormBody } from "./FormBody";

/**
 * Dialog body for form fields — the dialog counterpart to [FormCardContent]: the plain
 * [DialogBody] container chrome (padding, scrolling) around a [FormBody] field stack. Without it,
 * fields rendered into the bare `DialogBody` stick together (no gap) and dialog forms look
 * different from page forms.
 */
export function FormDialogBody({ className, ...props }: React.ComponentProps<"div">): React.ReactElement {
  return (
    <DialogBody>
      <FormBody className={className} {...props} />
    </DialogBody>
  );
}
