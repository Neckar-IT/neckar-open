import { Badge } from "../../primitives/badge";
import { Tooltip, TooltipContent, TooltipTrigger } from "../../primitives/tooltip";

interface BadgeWithHintProps extends React.PropsWithChildren {
  variant?: React.ComponentProps<typeof Badge>["variant"];
  /** What the badge means and the facts behind it; shown on hover and keyboard focus. */
  hint: React.ReactNode;
  /** Opens this address in a new tab; without it the badge links nothing and stays usable inside a link. */
  href?: string | null | undefined;
  className?: string | undefined;
}

/**
 * A badge that explains itself: hovering or focusing it opens a tooltip with its meaning and facts. With `href` the badge is a link
 * that is also the tooltip's trigger.
 */
export function BadgeWithHint({ variant, hint, href, className, children }: BadgeWithHintProps): React.ReactElement {
  const badge = (
    <Badge variant={variant} className={className}>
      {children}
    </Badge>
  );

  return (
    <Tooltip>
      {href == null ? (
        // The trigger is a span of its own: as the trigger, the badge would lose its data-slot, which the truncation zone reads.
        <TooltipTrigger asChild tabIndex={0}>
          <span className="inline-flex shrink-0">{badge}</span>
        </TooltipTrigger>
      ) : (
        <TooltipTrigger asChild tabIndex={0}>
          <a href={href} target="_blank" rel="noreferrer" className="inline-flex shrink-0">
            {badge}
          </a>
        </TooltipTrigger>
      )}
      <TooltipContent className="max-w-[calc(100vw-2rem)]">{hint}</TooltipContent>
    </Tooltip>
  );
}
