import { cn } from "cn";

interface FormSectionHeaderProps extends React.PropsWithChildren {
  className?: string;
  /**
   * The element the heading renders as: `legend` inside a [FormFieldset], `div` everywhere else.
   */
  as?: "div" | "legend";
}

/**
 * Heading that introduces a group of form fields.
 *
 * Renders a plain bold label one step larger than the input labels (`text-base`
 * vs. `text-sm`) — used to break a long form into named sections without adding
 * separator lines.
 */
export function FormSectionHeader({ children, className, as: Component = "div" }: FormSectionHeaderProps): React.ReactElement {
  return <Component className={cn("text-base font-bold", className)}>{children}</Component>;
}
