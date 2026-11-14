import type { ReactElement } from "react";

/**
 * Where a filter control stands: `Inline` is the button the toolbar shows, `Panel` the form inside the popover of the
 * secondary filters.
 */
export type FilterForm = "Inline" | "Panel";

/**
 * One filter of a table: a function of the `DataSliceController` creates it, `Filters` places it.
 */
export interface TableFilter {
  /** Identifies the filter among those of its table. */
  readonly key: string;
  /** The i18n key of the label; `Filters` translates it. */
  readonly label: string;
  /** Whether the value differs from the default; the button of the secondary filters counts these. */
  readonly active: boolean;
  /** Shown only while set: a value a link sets and the page offers nothing to enter, such as an email address. */
  readonly onlyWhileSet?: boolean;
  /** Sets the filter back to its default; `Filters` shows it as the button "Reset" in the label row of the form `Panel` while the filter is `active`. */
  readonly reset?: () => void;
  /** The control, with the translated label. */
  render(args: { label: string; form: FilterForm }): ReactElement;
}
