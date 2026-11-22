/**
 * Imports
 */

import type { CustomIconProps } from "./CustomIconProps";

/**
 * Beaker filled to half.
 */

export function BeakerHalfFullIcon(props: React.ComponentProps<"svg"> & CustomIconProps) {
  return (
    <svg fill="none" height={props.size ?? 24} viewBox="0 0 24 24" width={props.size ?? 24} xmlns="http://www.w3.org/2000/svg" {...props}>
      <path
        d="M4.5 3h15M6 3v16a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V3"
        stroke={props.color ?? "currentColor"}
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
      />
      <path d="M8 11h8v7.5a.5.5 0 0 1-.5.5h-7a.5.5 0 0 1-.5-.5V11Z" fill={props.color ?? "currentColor"} />
    </svg>
  );
}
