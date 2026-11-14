import { cn } from "cn";

interface TitleWithEntityProps extends React.ComponentProps<"div"> {
  /** The static title text (e.g. the action name); kept on one line. */
  label: React.ReactNode;
}

/**
 * Title row combining a static label with an entity display (name, link, …) on one line.
 *
 * Typical use: a dialog title naming the action and the affected object — "Archive" +
 * `<AuthorDisplayName />`. The label never wraps; the entity part may truncate/wrap as its
 * own component decides.
 */
export function TitleWithEntity({ label, className, children, ...rest }: TitleWithEntityProps): React.ReactElement {
  return (
    <div className={cn("flex items-center gap-1.5", className)} {...rest}>
      <span className="whitespace-nowrap">{label}</span>
      {children}
    </div>
  );
}
