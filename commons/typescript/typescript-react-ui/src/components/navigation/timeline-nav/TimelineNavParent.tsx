/**
 * Imports
 */

import { cn } from "cn";
import { ChevronRightIcon } from "lucide-react";
import { useState } from "react";
import { useLocation } from "react-router";
import { Button } from "../../primitives/button";
import { TimelineNavLeaf } from "./TimelineNavLeaf";
import { Curve } from "./TimelineNavLines";
import { TimelineNavParallelLeafs } from "./TimelineNavParallelLeafs";
import { TimelineNavStatusIcon } from "./TimelineNavStatusIcon";
import type { TimelineNavParentType, TimelineNavStatus } from "./TimelineNavTypes";

/**
 * Prop Types
 */

export interface TimelineNavParentProps extends TimelineNavParentType {
  className?: string | undefined;
  firstElement?: boolean | undefined;
  lastElement?: boolean | undefined;
}

/**
 * Component
 */

export function TimelineNavParent(props: TimelineNavParentProps): React.ReactElement {
  const { pathname } = useLocation();
  const [collapsed, setCollapsed] = useState(() => {
    // expand initially if any leaf inside is active
    return props.leafs.every((leaf) => {
      switch (leaf.type) {
        case "leaf":
          return leaf.href !== pathname;

        case "parallel-leafs": {
          const leftNotActive = leaf.leafsOnTheLeft.every((leaf) => leaf.href !== pathname);
          const rightNotActive = leaf.leafsOnTheRight.every((leaf) => leaf.href !== pathname);
          return leftNotActive && rightNotActive;
        }
        default:
          throw new Error(`Unexpected value in Switch Statement: leaf.type`);
      }
    });
  });

  return (
    <div>
      <SectionToggleButton
        className={props.className}
        collapsed={collapsed}
        isFirstElement={props.firstElement ?? false}
        isLastElement={props.lastElement ?? false}
        status={props.status}
        title={props.title}
        onClick={() => setCollapsed((prev) => !prev)}
      />

      {!collapsed ? (
        <div className="flex items-stretch pl-[23px]">
          <SecionMainLine />
          <div className="overflow-x-hidden">
            {props.leafs.map((leaf, index) => {
              const previousLeaf = props.leafs[index - 1]?.type === "leaf" ? "regular" : "parallel";

              if (leaf.type === "leaf") {
                const nextLeaf =
                  props.leafs.length === index + 1 ? "none" : props.leafs[index + 1]?.type === "leaf" ? "regular" : "parallel-left";

                return (
                  <TimelineNavLeaf
                    key={index}
                    href={leaf.href}
                    nextLeaf={nextLeaf}
                    previousLeaf={index === 0 ? "none" : previousLeaf}
                    status={leaf.status}
                    thisLeaf="regular"
                    title={leaf.title}
                  />
                );
              } else {
                const nextLeaf = props.leafs.length === index + 1 ? "none" : "regular";

                return (
                  <TimelineNavParallelLeafs
                    key={index}
                    leafsOnTheLeft={leaf.leafsOnTheLeft}
                    leafsOnTheRight={leaf.leafsOnTheRight}
                    nextLeaf={nextLeaf}
                    previousLeaf={index === 0 ? "none" : previousLeaf}
                  />
                );
              }
            })}
          </div>
        </div>
      ) : null}
    </div>
  );
}

/**
 * Section Toggle Button
 */

interface SectionToggleButtonProps {
  onClick: () => void;
  className?: string | undefined;
  isFirstElement: boolean;
  isLastElement: boolean;
  status: TimelineNavStatus;
  title: string;
  collapsed: boolean;
}

function SectionToggleButton(props: SectionToggleButtonProps): React.ReactElement {
  return (
    <Button
      className={cn("relative gap-1.5 rounded-full pr-6 pl-3", props.className)}
      customIconSize={true}
      variant="ghost"
      onClick={props.onClick}
    >
      <div>
        {!props.isFirstElement ? <div className="bg-border absolute top-0 left-[23px] h-2 w-0.5" /> : null}
        <TimelineNavStatusIcon status={props.status} />
        {!props.isLastElement || !props.collapsed ? <div className="bg-border absolute bottom-0 left-[23px] h-2 w-0.5" /> : null}
      </div>
      <ChevronRightIcon className={cn("size-5 transition-transform duration-300", { "rotate-90": !props.collapsed })} />
      <span className="text-base font-semibold">{props.title}</span>
    </Button>
  );
}

/**
 * main line and lines to splice into leafs and splice back in to the main line
 */

function SecionMainLine() {
  return (
    <>
      <div className="bg-border w-0.5 shrink-0" />
      <div className="relative w-3 shrink-0">
        <div className="absolute top-0 -left-px flex w-[13px]">
          <Curve className="rotate-180" />
          <div className="bg-border mt-[7px] h-px flex-1" />
        </div>
        <div className="absolute bottom-px -left-px -mt-px flex w-[13px]">
          <Curve className="-rotate-90" />
          <div className="bg-border h-px flex-1" />
        </div>
      </div>
    </>
  );
}
