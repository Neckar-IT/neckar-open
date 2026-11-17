/**
 * Imports
 */

import type { CustomIconProps } from "./CustomIconProps";

/**
 * File-with-euro-sign icon
 */

export function FileEuroIcon(props: React.ComponentProps<"svg"> & CustomIconProps) {
  return (
    <svg fill="none" height={props.size ?? 24} viewBox="0 0 24 24" width={props.size ?? 24} xmlns="http://www.w3.org/2000/svg" {...props}>
      <path
        d="M4 22h14a2 2 0 0 0 2-2V7l-5-5H6a2 2 0 0 0-2 2v4"
        stroke={props.color ?? "currentColor"}
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
      />
      <path
        d="M14 2v4a2 2 0 0 0 2 2h4"
        stroke={props.color ?? "currentColor"}
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
      />
      <path
        d="M1 14h6M1 16h4.5M8.5 12a3.85 3.85 0 0 0-2.6-1A3.95 3.95 0 0 0 2 15c0 2.2 1.75 4 3.9 4 1 0 1.9-.4 2.6-1"
        stroke={props.color ?? "currentColor"}
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.5"
      />
    </svg>
  );
}
