/**
 * Imports
 */

import { cn } from "cn";

/**
 * Curve
 */

interface CurveProps {
  className?: string | undefined;
}

export function Curve(props: CurveProps): React.ReactElement {
  return (
    <svg
      className={cn("text-border", props.className)}
      fill="none"
      height="8"
      viewBox="0 0 8 8"
      width="8"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M7.5 8C7.5 3.48131 4.51869 0.5 0 0.5" stroke="currentColor" />
    </svg>
  );
}

/**
 * Line after a leaf element
 */

interface TimelineNavLineAfterProps {
  thisLeaf: "regular" | "parallel-left" | "parallel-right";
  nextLeaf: "none" | "regular" | "parallel-left" | "parallel-right";
}

export function TimelineNavLineAfter(props: TimelineNavLineAfterProps): React.ReactElement | null {
  // just a straight line from one leaf to the next
  if (props.thisLeaf === props.nextLeaf) {
    return <div className="bg-border ml-[15px] h-2 w-px" />;
  }

  // line leaving a leaf and curving to the left
  if (
    (props.thisLeaf === "regular" && props.nextLeaf === "none") ||
    (props.thisLeaf === "parallel-right" && props.nextLeaf === "none") ||
    (props.thisLeaf === "parallel-right" && props.nextLeaf === "regular")
  ) {
    return (
      <div className="flex h-4">
        <div className="bg-border mt-[7px] h-px w-2" />
        <Curve className="rotate-90" />
      </div>
    );
  }

  // line leaving a leaf and joining a line coming from the right
  if (props.thisLeaf === "parallel-left" && props.nextLeaf === "none") {
    return (
      <div className="relative w-full pt-[7px]">
        <div className="bg-border h-px w-full" />
        <Curve className="absolute top-0 left-2 rotate-90" />
      </div>
    );
  }

  // joining the two lines coming from this leaf and the leaf to the right and leading into the next leaf
  if (props.thisLeaf === "parallel-left" && props.nextLeaf === "regular") {
    return (
      <div className="relative ml-[15px] flex">
        <div className="bg-border h-4 w-px" />
        <Curve className="absolute top-[7px] left-0 -rotate-90" />
        <div className="bg-border mt-[7px] ml-[7px] h-px flex-1" />
      </div>
    );
  }

  return null;
}

/**
 * Line before a leaf element
 */

interface TimelineNavLineBeforeProps {
  previousLeaf: "none" | "regular" | "parallel";
  thisLeaf: "regular" | "parallel-left" | "parallel-right";
}

export function TimelineNavLineBefore(props: TimelineNavLineBeforeProps): React.ReactElement | null {
  // line coming from the left and curving into the leaf
  if (
    (props.previousLeaf === "none" && props.thisLeaf === "regular") ||
    (props.previousLeaf === "none" && props.thisLeaf === "parallel-right")
  ) {
    return (
      <div className="relative -mt-px flex h-4 w-[10px]">
        <div className="bg-border mt-2 h-px flex-1" />
        <Curve className="absolute top-2 left-2" />
      </div>
    );
  }

  // same as above but with different margins
  if (props.previousLeaf === "regular" && props.thisLeaf === "parallel-right") {
    return (
      <div className="relative mt-[7px] flex h-[9px] w-[10px]">
        <div className="bg-border h-px flex-1" />
        <Curve className="absolute left-2" />
      </div>
    );
  }

  // line coming from the left and splitting into two lines, one curving into the leaf and another going straight to the right
  if (props.previousLeaf === "none" && props.thisLeaf === "parallel-left") {
    return (
      <div className="relative -mt-px flex h-4 w-full">
        <div className="bg-border mt-2 h-px flex-1" />
        <Curve className="absolute top-2 left-2" />
      </div>
    );
  }

  // line coming from above and going into the leaf as well as splitting up and going to the right
  if (props.previousLeaf === "regular" && props.thisLeaf === "parallel-left") {
    return (
      <div className="relative ml-[15px] flex">
        <div className="bg-border h-4 w-px" />
        <Curve className="absolute top-0 left-0 rotate-180" />
        <div className="bg-border mt-[7px] ml-[7px] h-px w-full" />
      </div>
    );
  }

  return null;
}
