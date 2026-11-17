/**
 * Imports
 */

import type { CustomIconProps } from "./CustomIconProps";

/**
 * Room Icon
 */

export function RoomIcon(props: React.ComponentProps<"svg"> & CustomIconProps) {
  return (
    <svg fill="none" height={props.size ?? 24} viewBox="0 0 24 24" width={props.size ?? 24} xmlns="http://www.w3.org/2000/svg" {...props}>
      <path
        d="M7 19a1 1 0 0 0 1 1h4a1 1 0 0 0 1-1v-3c0-.805.121-1.66.73-2.27.61-.609 1.465-.73 2.27-.73h3c.695 0 .84-.129.855-.145.016-.016.145-.16.145-.855V8a1 1 0 0 0-1-1H8a1 1 0 0 0-1 1v11ZM5 8a3 3 0 0 1 3-3h11a3 3 0 0 1 3 3v4c0 .805-.121 1.66-.73 2.27-.61.609-1.465.73-2.27.73h-3c-.695 0-.84.129-.855.145-.016.016-.145.16-.145.855v3a3 3 0 0 1-3 3H8a3 3 0 0 1-3-3V8ZM5 3V2h16v1H5ZM21 1h1v3h-1V1Z"
        fill="currentColor"
      />
      <path d="M5 1h1v3H5V1ZM3 22H2V5h1v17Z" fill="currentColor" />
      <path d="M1 22v-1h3v1H1ZM1 6V5h3v1H1Z" fill="currentColor" />
    </svg>
  );
}
