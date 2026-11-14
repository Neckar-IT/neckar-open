/**
 * Imports
 */

import { cva } from "class-variance-authority";
import { cn } from "cn";
import { Link, useLocation } from "react-router";
import { Tooltip, TooltipContent, TooltipTrigger } from "../../primitives/tooltip";
import { TimelineNavLineAfter, TimelineNavLineBefore } from "./TimelineNavLines";
import { TimelineNavStatusIcon } from "./TimelineNavStatusIcon";
import type { TimelineNavLeafType } from "./TimelineNavTypes";

/**
 * Prop Types
 */

export interface TimelineNavLeafProps extends Omit<TimelineNavLeafType, "type"> {
  className?: string | undefined;
  collapsed?: boolean | undefined;
  title: string;
  previousLeaf: "none" | "regular" | "parallel";
  thisLeaf: "regular" | "parallel-left" | "parallel-right";
  nextLeaf: "none" | "regular" | "parallel-left" | "parallel-right";
}

/**
 * Variants
 */

const timelineNavLeafVariants = cva(
  "hover:border-muted-foreground hover:bg-muted hover:text-foreground flex items-center gap-2 overflow-x-hidden rounded-full border p-1 pr-4 hover:cursor-pointer [&_svg]:opacity-60 [&_svg]:hover:opacity-100",
  {
    variants: {
      active: {
        true: "border-muted-foreground bg-muted text-foreground [&_svg]:opacity-100",
        false: "border-border text-muted-foreground",
      },
      collapsed: {
        true: "pr-1",
        false: "pr-4",
      },
    },
    defaultVariants: {
      active: false,
      collapsed: false,
    },
  },
);

/**
 * Process Navigation Leaf Component
 */

export function TimelineNavLeaf(props: TimelineNavLeafProps): React.ReactElement {
  const { pathname } = useLocation();
  const active = props.href === pathname;

  if (props.collapsed) {
    return (
      <div className={cn("shrink-0", props.className)}>
        <TimelineNavLineBefore previousLeaf={props.previousLeaf} thisLeaf={props.thisLeaf} />
        <Tooltip delayDuration={100}>
          <TooltipTrigger className="block">
            <Link
              className={cn(
                timelineNavLeafVariants({
                  active,
                  collapsed: props.collapsed,
                }),
              )}
              to={props.href}
            >
              <TimelineNavStatusIcon className="h-5 w-5 shrink-0" status={props.status} />
            </Link>
          </TooltipTrigger>
          <TooltipContent>{props.title}</TooltipContent>
        </Tooltip>
        <TimelineNavLineAfter nextLeaf={props.nextLeaf} thisLeaf={props.thisLeaf} />
      </div>
    );
  }

  return (
    <div className={cn("shrink-0 overflow-x-hidden", props.className)}>
      <TimelineNavLineBefore previousLeaf={props.previousLeaf} thisLeaf={props.thisLeaf} />
      <div className="flex">
        <Link
          className={cn(
            timelineNavLeafVariants({
              active,
              collapsed: props.collapsed,
            }),
          )}
          to={props.href}
        >
          <TimelineNavStatusIcon className="h-5 w-5 shrink-0" status={props.status} />
          <span className="flex-1 truncate text-sm font-medium">{props.title}</span>
        </Link>
      </div>
      <TimelineNavLineAfter nextLeaf={props.nextLeaf} thisLeaf={props.thisLeaf} />
    </div>
  );
}
