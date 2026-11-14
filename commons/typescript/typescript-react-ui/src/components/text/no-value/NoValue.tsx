import { cn } from "cn";

interface NoValueProps {
  className?: string;
  /**
   * Optional label to display instead of default "-"
   */
  children?: React.ReactNode;
}

/**
 * Shows a label that represents an undefined value.
 *
 * Sets a native `title` attribute with the displayed text for tooltip on hover.
 */
export function NoValue(props: NoValueProps): React.ReactElement {
  const displayText = props.children ?? "-";
  const titleText = typeof displayText === "string" ? displayText : undefined;

  return (
    <span className={cn("text-muted-foreground italic", props.className)} title={titleText}>
      {displayText}
    </span>
  );
}
