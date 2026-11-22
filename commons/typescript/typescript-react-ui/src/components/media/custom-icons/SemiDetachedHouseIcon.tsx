/**
 * Imports
 */

import type { CustomIconProps } from "./CustomIconProps";

/**
 * Semi-detached house: two houses sharing one wall.
 */

export function SemiDetachedHouseIcon(props: React.ComponentProps<"svg"> & CustomIconProps): React.ReactElement {
  return (
    <svg fill="none" height={props.size ?? 24} viewBox="0 0 24 24" width={props.size ?? 24} xmlns="http://www.w3.org/2000/svg" {...props}>
      <path
        stroke={props.color ?? "currentColor"}
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
        d="M7 21v-5.779a.677.677 0 0 0-.244-.51.903.903 0 0 0-.59-.212H2.834a.903.903 0 0 0-.589.211.677.677 0 0 0-.244.511M22 15.222a.677.677 0 0 0-.244-.51.903.903 0 0 0-.59-.212h-3.333a.903.903 0 0 0-.589.211.677.677 0 0 0-.244.511V21"
      />
      <path
        stroke={props.color ?? "currentColor"}
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
        d="M2 10c0-.29.035-.578.103-.842a2.3 2.3 0 0 1 .29-.686l3.89-6C6.483 2.167 6.737 2 7 2c.263 0 .517.167.717.472l3.89 6c.123.188.222.422.29.686.068.264.103.551.103.842v11H3.111c-.295 0-.577-.21-.786-.586C2.117 20.04 2 19.53 2 19v-9ZM12 10c0-.29.035-.578.103-.842.068-.264.167-.498.29-.686l3.89-6c.2-.305.454-.472.717-.472.263 0 .517.167.717.472l3.89 6c.123.188.222.422.29.686.068.264.103.551.103.842v9c0 .53-.117 1.04-.325 1.414-.209.375-.491.586-.786.586H12L12 19v-9Z"
      />
      <rect width="4" height="4" x="5" y="8" fill={props.color ?? "currentColor"} rx="1" />
      <rect width="4" height="4" x="15" y="8" fill={props.color ?? "currentColor"} rx="1" />
    </svg>
  );
}
