import { cn } from "cn";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { Badge } from "../../primitives/badge";
import { Tooltip, TooltipContent, TooltipTrigger } from "../../primitives/tooltip";

interface SingleLineListProps {
  /**
   * The entries; the first one shows in the line, every one in the tooltip.
   */
  items: React.ReactNode[] | null | undefined;
  /**
   * Additional class name for the entries in the line and in the tooltip, such as a font.
   */
  className?: string | undefined;
}

/**
 * A list on one line: its first entry, truncated in a truncation zone, and a "+N" badge counting the entries the line leaves out.
 *
 * Hovering or focusing the line opens one tooltip listing every entry in full, one per line. A table cell holding the list keeps the
 * height of one line whatever the number of entries. No entry renders nothing.
 */
export function SingleLineList({ items, className }: SingleLineListProps): React.ReactElement | null {
  const { t } = useEmbeddedTranslation();
  if (items == null || items.length === 0) {
    return null;
  }

  const more = items.length - 1;
  return (
    <Tooltip>
      <TooltipTrigger asChild tabIndex={0}>
        <span className={cn("flex max-w-full min-w-0 items-center gap-1", className)}>
          <span data-text-content>
            <span>{items[0]}</span>
          </span>
          {more === 0 ? null : (
            <Badge
              variant="secondary"
              className="h-4 shrink-0 px-1 py-0 text-[10px] leading-none"
              aria-label={t({ key: "singleLineList.more", options: { count: more } })}
            >
              +{more}
            </Badge>
          )}
        </span>
      </TooltipTrigger>
      <TooltipContent className="max-w-[calc(100vw-2rem)]">
        <ul className={cn("flex min-w-0 flex-col", className)}>
          {items.map((item, index) => (
            // Keyed by position: two entries may render the same, so nothing else identifies one.
            <li key={index} className="truncate whitespace-nowrap">
              {item}
            </li>
          ))}
        </ul>
      </TooltipContent>
    </Tooltip>
  );
}
