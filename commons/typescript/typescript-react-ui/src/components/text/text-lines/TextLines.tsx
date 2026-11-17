import { cn } from "cn";
import { HighlightText } from "../highlight-text";

interface TextLinesProps {
  /**
   * The texts, one per line.
   */
  texts: string[] | null | undefined;
  /**
   * Additional class name for the wrapper.
   */
  className?: string | undefined;
}

/**
 * A list of free texts, one line per entry, each highlighting the current search terms.
 *
 * A list with no entry renders nothing: a list nothing was put into says as little as an absent one,
 * and a placeholder would need a text the value itself does not carry.
 */
export function TextLines({ texts, className }: TextLinesProps): React.ReactElement | null {
  if (texts == null || texts.length === 0) {
    return null;
  }

  return (
    <div className={cn("flex flex-col gap-0.5", className)}>
      {texts.map((text, index) => (
        // Keyed by position: two entries may hold the same text, so nothing else identifies one.
        <HighlightText key={index} text={text} />
      ))}
    </div>
  );
}
