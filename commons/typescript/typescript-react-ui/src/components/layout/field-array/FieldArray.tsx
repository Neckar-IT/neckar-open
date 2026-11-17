import { cn } from "cn";
import { FormFieldGrid } from "../../forms/form";

interface FieldArrayProps extends React.PropsWithChildren {
  className?: string;
}

/**
 * Vertical stack of the rows of a `useFieldArray` form section, plus its add button.
 *
 * This layout family (`FieldArray` > `FieldArrayRow` > `FieldArrayRowHeader` / `FieldArrayRowFields`,
 * with [FieldArrayAddButton] / [FieldArrayRemoveButton]) is the **card** layout for a `useFieldArray`
 * whose rows carry **several fields** — each row is a bordered card with a title and a field grid
 * (e.g. a measurement or a geometry area). For a **compact** list of narrow value objects (up to ~3
 * fields on one line), use `FormArrayRow` (`molecules/form`) instead — see the
 * `inline-embedded-value-object-list` pattern.
 */
export function FieldArray({ children, className }: FieldArrayProps): React.ReactElement {
  return <div className={cn("flex flex-col gap-3", className)}>{children}</div>;
}

/**
 * One row of a [FieldArray] — a bordered card holding the fields of a single entry.
 */
export function FieldArrayRow({ children, className }: FieldArrayProps): React.ReactElement {
  return <div className={cn("rounded-md border p-3", className)}>{children}</div>;
}

/**
 * Header of a [FieldArrayRow]: the entry's title on the left, its remove button on the right.
 */
export function FieldArrayRowHeader({ children, className }: FieldArrayProps): React.ReactElement {
  return <div className={cn("mb-2 flex items-center justify-between", className)}>{children}</div>;
}

/**
 * Title of a [FieldArrayRow], e.g. "MPPT-Eingang 1".
 */
export function FieldArrayRowTitle({ children, className }: FieldArrayProps): React.ReactElement {
  return <span className={cn("text-sm font-medium", className)}>{children}</span>;
}

/**
 * Grid holding the fields of one [FieldArrayRow]. Two columns, widening to three where the row has
 * more than four fields and the screen is wide enough.
 *
 * Composes [FormFieldGrid], which owns the grid layout — a field-array row and a standalone group of
 * related fields lay their fields out the same way.
 */
export function FieldArrayRowFields({ children, className }: FieldArrayProps): React.ReactElement {
  return <FormFieldGrid className={className}>{children}</FormFieldGrid>;
}
