import { cn } from "cn";

export interface FormArrayRowProps {
  /**
   * The form fields of one row, laid out side by side.
   */
  children: React.ReactNode;
  /**
   * The row action, usually a remove button.
   */
  action: React.ReactNode;
  className?: string;
}

/**
 * One row of an array field (`useFieldArray`): the row's form fields side by side, followed by the
 * row action.
 *
 * The action is pushed down by the height of the field labels above the inputs (`text-sm` label
 * plus the `FormItem` gap), so it sits on the input line instead of the label line.
 */
export function FormArrayRow({ children, action, className }: FormArrayRowProps): React.ReactElement {
  return (
    <div className={cn("flex items-start gap-2", className)}>
      {children}
      <div className="mt-6">{action}</div>
    </div>
  );
}
