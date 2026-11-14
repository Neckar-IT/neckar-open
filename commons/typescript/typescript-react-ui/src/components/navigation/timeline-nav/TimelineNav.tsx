/**
 * Imports
 */

import { TimelineNavParent } from "./TimelineNavParent";
import { type TimelineNavData } from "./TimelineNavTypes";

/**
 * Prop Types
 */

export interface TimelineNavProps {
  className: string | undefined;
  data: TimelineNavData;
}

/**
 * Timeline Navigation
 *
 * This component ca display a series of steps in a timeline and allows navigation between the steps.
 */

export function TimelineNav(props: TimelineNavProps): React.ReactElement {
  return (
    <div className={props.className}>
      {props.data.map((parent, index) => (
        <TimelineNavParent
          key={index}
          firstElement={index === 0}
          lastElement={index === props.data.length - 1}
          leafs={parent.leafs}
          status={parent.status}
          title={parent.title}
        />
      ))}
    </div>
  );
}
