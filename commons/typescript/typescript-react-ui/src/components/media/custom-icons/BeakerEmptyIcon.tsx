/**
 * Imports
 */

import { type CustomIconProps } from "./CustomIconProps";

/**
 * Beaker Empty Icon
 */

export function BeakerEmptyIcon(props: React.ComponentProps<"svg"> & CustomIconProps) {
  return (
    <svg fill="none" height={props.size ?? 24} viewBox="0 0 24 24" width={props.size ?? 24} xmlns="http://www.w3.org/2000/svg" {...props}>
      <path
        d="M4.5 3h15M6 3v16a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V3"
        stroke={props.color ?? "currentColor"}
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
      />
    </svg>
  );
}
