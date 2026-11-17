/**
 * An action represents an operation that can be triggered by the user from multiple places in the UI (e.g. with a button or a dropdown item).
 * It encapsulates all information related to the action, such as its label, icon, trigger handler, and state (disabled, loading, hidden).
 *
 * The action itself determines its state (disabled, loading, hidden), if this information is derived from the action's data, but it can be
 * overwritten at the usage site (e.g. to disable an action in a specific context). This allows for a flexible and consistent way to
 * represent actions across the UI.
 */
export interface Action {
  icon: React.ReactNode;
  label: React.ReactNode;
  variant?: "default" | "destructive" | "confirm" | "secondary" | "outline";
  disabled?: boolean;
  /** Why the action is disabled: a tooltip on a button, a second line in a dropdown item. */
  disabledReason?: React.ReactNode;
  loading?: boolean;
  hidden?: boolean;
  hoverContent?: React.ReactNode;
  onTrigger: () => void;
}
