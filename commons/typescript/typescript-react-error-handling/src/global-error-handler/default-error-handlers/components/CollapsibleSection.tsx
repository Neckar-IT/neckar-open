/**
 * Imports
 */

import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@neckar-hub/typescript-react-ui/layout";
import { cn } from "cn";
import { ChevronRightIcon } from "lucide-react";
import { useState } from "react";

/**
 * Prop Types
 */

interface CollapsibleSectionProps {
  heading: string;
  children: React.ReactNode;
  initiallyOpen?: boolean | undefined;
  className?: string | undefined;
}

/**
 * Collapsible Section Component
 *
 * One expandable block of an error page: chevron, heading, content. Spans the full width of
 * [ErrorPageLayout], which is what keeps all sections on the same left and right edge.
 */

export function CollapsibleSection(props: CollapsibleSectionProps) {
  const [isOpen, setIsOpen] = useState(props.initiallyOpen ?? false);

  return (
    <Collapsible className={cn("w-full", props.className)} open={isOpen} onOpenChange={setIsOpen}>
      <CollapsibleTrigger className="text-muted-foreground flex items-center gap-1.5">
        <ChevronRightIcon className={cn("h-4 w-4 transition-transform duration-200", { "rotate-90": isOpen })} />
        <span className="text-sm font-semibold">{props.heading}</span>
      </CollapsibleTrigger>
      <CollapsibleContent>{props.children}</CollapsibleContent>
    </Collapsible>
  );
}
