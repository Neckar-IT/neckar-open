/**
 * Imports
 */

import type { CustomIconProps } from "./CustomIconProps";

/**
 * Inverter Icon
 *
 * inspired by Hadi from NounProject (https://thenounproject.com/icon/inverter-4109362/)
 */

export function InverterIcon(props: React.ComponentProps<"svg"> & CustomIconProps): React.ReactElement {
  return (
    <svg fill="none" height={props.size ?? 24} viewBox="0 0 24 24" width={props.size ?? 24} xmlns="http://www.w3.org/2000/svg" {...props}>
      <path
        fill={props.color ?? "currentColor"}
        d="M4.603 1.246h14.794a1.73 1.73 0 0 1 1.727 1.728v1.093h.898c1.304 0 1.304 1.983 0 1.983h-.898v11.9h.898c1.304 0 1.304 1.983 0 1.983h-.898v1.093a1.73 1.73 0 0 1-1.727 1.727H4.603a1.73 1.73 0 0 1-1.727-1.727v-1.093h-.898c-1.304 0-1.304-1.983 0-1.983h.898V6.05h-.898c-1.304 0-1.304-1.983 0-1.983h.898V2.974a1.73 1.73 0 0 1 1.727-1.728ZM19.142 3.23H4.858V20.77h14.284V3.23Z"
      />
      <path stroke={props.color ?? "currentColor"} strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="m13 7-3 5h4l-3 5" />
    </svg>
  );
}
