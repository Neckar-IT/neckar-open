/**
 * Imports
 */

import type { CustomIconProps } from "./CustomIconProps";

/**
 * Solar Panel Icon
 */

export function SolarPanelIcon(props: React.ComponentProps<"svg"> & CustomIconProps): React.ReactElement {
  return (
    <svg width={props.size ?? 24} height={props.size ?? 24} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" {...props}>
      <path
        d="M20.0784 17.7392L12.6066 22.4255C12.1127 22.7353 11.4596 22.5585 11.1893 22.042L3.59505 7.53032C3.3348 7.03303 3.53472 6.41898 4.03787 6.17023L11.6159 2.42368C12.0953 2.18667 12.6763 2.36878 12.9347 2.83703L20.4226 16.409C20.6789 16.8735 20.5278 17.4573 20.0784 17.7392Z"
        stroke={props.color ?? "currentColor"}
        strokeWidth="2"
      />
      <path d="M7.58655 14.82L16.6235 9.72351M8.32213 4.25926L16.6235 19.8113" stroke={props.color ?? "currentColor"} strokeWidth={1} />
    </svg>
  );
}
