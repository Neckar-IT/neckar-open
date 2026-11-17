/**
 * Status of a nav step step
 */

export type TimelineNavStatus = "not-started" | "in-progress" | "completed" | "warning" | "error";

/**
 * Leaf Type
 */

export interface TimelineNavLeafType {
  type: "leaf";
  title: string;
  status: TimelineNavStatus;
  href: string;
}

export interface TimelineNavParallelLeafsType {
  type: "parallel-leafs";
  leafsOnTheLeft: TimelineNavLeafType[];
  leafsOnTheRight: TimelineNavLeafType[];
}

/**
 * Parent Type
 */

export interface TimelineNavParentType {
  title: string;
  status: TimelineNavStatus;
  leafs: (TimelineNavLeafType | TimelineNavParallelLeafsType)[];
}

/**
 * Process Navigation Type
 */

export type TimelineNavData = TimelineNavParentType[];
