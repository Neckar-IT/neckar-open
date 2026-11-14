/**
 * Imports
 */

import { TimelineNavLineAfter } from "./TimelineNavLines";

/**
 * Prop Types
 */

interface TimelineNavEmptyParallelLeafProps {
  thisLeaf: "parallel-left" | "parallel-right";
  nextLeaf: "none" | "regular" | "parallel-left" | "parallel-right";
}

/**
 * Component that only displays a line for when one side of the parallel leafs is shorter than the other
 */

export function TimelineNavEmptyParallelLeaf(props: TimelineNavEmptyParallelLeafProps): React.ReactElement {
  return (
    <div className="shrink-0">
      <div className="bg-border ml-[15px] h-[30px] w-px" />
      <TimelineNavLineAfter nextLeaf={props.nextLeaf} thisLeaf={props.thisLeaf} />
    </div>
  );
}
