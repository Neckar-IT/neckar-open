/**
 * Imports
 */

import { cn } from "cn";
import { useEffect, useState } from "react";
import { useLocation } from "react-router";
import { TimelineNavEmptyParallelLeaf } from "./TimelineNavEmptyParallelLeaf";
import { TimelineNavLeaf } from "./TimelineNavLeaf";
import { type TimelineNavLeafType, type TimelineNavParallelLeafsType } from "./TimelineNavTypes";

/**
 * Prop Types
 */

export interface TimelineNavParallelLeafsProps extends Omit<TimelineNavParallelLeafsType, "type"> {
  className?: string | undefined;
  previousLeaf: "none" | "regular" | "parallel";
  nextLeaf: "none" | "regular";
}

/**
 * Parallel Leafs Component
 */

export function TimelineNavParallelLeafs(props: TimelineNavParallelLeafsProps): React.ReactElement {
  const collapsedSide = useCollapsedSide(props.leafsOnTheLeft, props.leafsOnTheRight);
  const { leafsOnTheLeft, leafsOnTheRight } = padToEqualLength(props.leafsOnTheLeft, props.leafsOnTheRight);

  return (
    <div className="flex overflow-x-hidden">
      <div className={cn("overflow-x-hidden", { "shrink-0": collapsedSide === "left" })}>
        {leafsOnTheLeft.map((left, leftIndex) => {
          if (left.type === "leaf") {
            return (
              <TimelineNavLeaf
                key={leftIndex}
                collapsed={collapsedSide === "left"}
                href={left.href}
                nextLeaf={leafsOnTheLeft.length === leftIndex + 1 ? props.nextLeaf : "parallel-left"}
                previousLeaf={leftIndex === 0 ? props.previousLeaf : "parallel"}
                status={left.status}
                thisLeaf="parallel-left"
                title={left.title}
              />
            );
          } else {
            return (
              <TimelineNavEmptyParallelLeaf
                key={leftIndex}
                nextLeaf={leafsOnTheLeft.length === leftIndex + 1 ? props.nextLeaf : "parallel-left"}
                thisLeaf="parallel-left"
              />
            );
          }
        })}
      </div>
      <div className="relative min-w-3 flex-1 shrink-0">
        <div className="bg-border absolute top-[7px] right-0 left-0 h-px" />
        <div className="bg-border absolute right-0 bottom-[8px] left-0 h-px" />
      </div>
      <div className={cn("overflow-x-hidden", { "shrink-0": collapsedSide === "right" })}>
        {leafsOnTheRight.map((right, rightIndex) => {
          if (right.type === "leaf") {
            return (
              <TimelineNavLeaf
                key={rightIndex}
                collapsed={collapsedSide === "right"}
                href={right.href}
                nextLeaf={leafsOnTheRight.length === rightIndex + 1 ? props.nextLeaf : "parallel-right"}
                previousLeaf={rightIndex === 0 ? props.previousLeaf : "parallel"}
                status={right.status}
                thisLeaf="parallel-right"
                title={right.title}
              />
            );
          } else {
            return (
              <TimelineNavEmptyParallelLeaf
                key={rightIndex}
                nextLeaf={leafsOnTheRight.length === rightIndex + 1 ? props.nextLeaf : "parallel-right"}
                thisLeaf="parallel-right"
              />
            );
          }
        })}
      </div>
    </div>
  );
}

/**
 * Determine the collapsed side
 *
 * By default the right side is collapsed. On navigation the side with an active leaf will be expanded.
 * When navigationg away from the parallel leafs, the expanded side will stay as is.
 */

function useCollapsedSide(leafsOnTheLeft: TimelineNavLeafType[], leafsOnTheRight: TimelineNavLeafType[]): "left" | "right" {
  const { pathname } = useLocation();
  const [collapsedSide, setCollapsedSide] = useState<"left" | "right">("right");

  useEffect(() => {
    if (leafsOnTheLeft.some((leaf) => leaf.href === pathname)) setCollapsedSide("right");
    if (leafsOnTheRight.some((leaf) => leaf.href === pathname)) setCollapsedSide("left");
  }, [pathname, leafsOnTheLeft, leafsOnTheRight]);

  return collapsedSide;
}

/**
 * Function to add empty elements until both sides have the same length
 */

function padToEqualLength(leafsOnTheLeftFromProps: TimelineNavLeafType[], leafsOnTheRightFromProps: TimelineNavLeafType[]) {
  const maxLength = Math.max(leafsOnTheLeftFromProps.length, leafsOnTheRightFromProps.length);

  const leafsOnTheLeft: (TimelineNavLeafType | { type: "empty" })[] = [...leafsOnTheLeftFromProps];
  while (leafsOnTheLeft.length < maxLength) leafsOnTheLeft.push({ type: "empty" });

  const leafsOnTheRight: (TimelineNavLeafType | { type: "empty" })[] = [...leafsOnTheRightFromProps];
  while (leafsOnTheRight.length < maxLength) leafsOnTheRight.push({ type: "empty" });

  return { leafsOnTheLeft, leafsOnTheRight };
}
