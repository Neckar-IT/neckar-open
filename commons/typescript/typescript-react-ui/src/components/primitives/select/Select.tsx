/**
 * Dropdown select widget; compose with SelectTrigger, SelectContent, and SelectItem.
 */

import { SelectContent as ShadcnSelectContent } from "@neckar-hub/typescript-react-ui-primitives/ui/select";

export {
  Select,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectScrollDownButton,
  SelectScrollUpButton,
  SelectSeparator,
  SelectTrigger,
  SelectValue,
} from "@neckar-hub/typescript-react-ui-primitives/ui/select";

/**
 * Content opening below the trigger (`position="popper"`): the primitive's `item-aligned` default lays the list over the trigger,
 * which hides the field and the label next to it.
 */
export function SelectContent({ position = "popper", ...props }: React.ComponentProps<typeof ShadcnSelectContent>): React.ReactElement {
  return <ShadcnSelectContent position={position} {...props} />;
}
