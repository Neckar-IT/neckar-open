/**
 * Imports
 */

import { type CustomIconProps } from "./CustomIconProps";

/**
 * Connector: a line between two points.
 */

export function ConnectorIcon(props: React.ComponentProps<"svg"> & CustomIconProps) {
  return (
    <svg fill="none" height={props.size ?? 24} viewBox="0 0 24 24" width={props.size ?? 24} xmlns="http://www.w3.org/2000/svg" {...props}>
      <path
        clipRule="evenodd"
        d="M15.5 5.5h3v3h-3v-3ZM15 10h5V4h-6v4.879L8.879 14H4v6h6v-5l5-5Zm-9.5 5.5h3v3h-3v-3Z"
        fill="currentColor"
        fillRule="evenodd"
      />
    </svg>
  );
}
