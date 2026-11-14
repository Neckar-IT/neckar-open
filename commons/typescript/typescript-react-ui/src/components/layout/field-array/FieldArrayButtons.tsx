import { cn } from "cn";
import { PlusIcon, TrashIcon } from "lucide-react";
import { Button } from "../../primitives/button";

interface FieldArrayAddButtonProps {
  /** Appends a new row to the backing `useFieldArray`. */
  onClick: () => void;
  /** Button caption, e.g. "Add contact person". */
  label: string;
  /** Disables the button, e.g. once the domain's row maximum is reached. */
  disabled?: boolean;
  className?: string;
}

/**
 * Full-width add button below a [FieldArray] — appends one new row.
 */
export function FieldArrayAddButton({ onClick, label, disabled = false, className }: FieldArrayAddButtonProps): React.ReactElement {
  return (
    <Button variant="secondary" onClick={onClick} disabled={disabled} className={cn("w-full", className)}>
      <PlusIcon />
      <span>{label}</span>
    </Button>
  );
}

interface FieldArrayRemoveButtonProps {
  /** Removes this row from the backing `useFieldArray`. */
  onClick: () => void;
  /** Accessible label of the icon-only button, e.g. "Remove contact person". */
  label: string;
  /** Disables removal, e.g. when the domain requires at least one row. */
  disabled?: boolean;
  className?: string;
}

/**
 * Icon-only remove button for a [FieldArrayRowHeader] — removes its row.
 */
export function FieldArrayRemoveButton({ onClick, label, disabled = false, className }: FieldArrayRemoveButtonProps): React.ReactElement {
  return (
    <Button variant="secondary" size="icon" onClick={onClick} disabled={disabled} aria-label={label} className={className}>
      <TrashIcon />
    </Button>
  );
}
