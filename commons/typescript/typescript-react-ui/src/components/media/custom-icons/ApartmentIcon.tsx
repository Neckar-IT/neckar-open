/**
 * Imports
 */

import type { CustomIconProps } from "./CustomIconProps";

/**
 * Apartment icon
 */

export function ApartmentIcon(props: React.ComponentProps<"svg"> & CustomIconProps) {
  return (
    <svg fill="none" height={props.size ?? 24} viewBox="0 0 24 24" width={props.size ?? 24} xmlns="http://www.w3.org/2000/svg" {...props}>
      <path d="M10.66 18.508v-8h-4v8a1 1 0 1 1-2 0v-8a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v8a1 1 0 1 1-2 0Z" fill="currentColor" />
      <path
        clipRule="evenodd"
        d="M2 16.492v-9a3 3 0 0 1 3-3h14a3 3 0 0 1 3 3v9.016a3 3 0 0 1-3 3H5a3 3 0 0 1-3-3v-.015Zm18 0v-9a1 1 0 0 0-1-1H5a1 1 0 0 0-1 1v9.016a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-.015Z"
        fill="currentColor"
        fillRule="evenodd"
      />
      <path
        clipRule="evenodd"
        d="M15.02 8.508c-.602 0-1.05.356-1.305.736-.254.38-.365.836-.365 1.263V12.655c0 .427.111.884.365 1.263.255.38.703.737 1.305.737h2.676c.602 0 1.05-.357 1.305-.737.253-.38.365-.836.365-1.263v-2.146c0-.428-.111-.884-.365-1.264-.255-.38-.703-.736-1.305-.736H15.02Zm2.346 2.573v-.573H15.35v2.145h2.016v-1.572Z"
        fill="currentColor"
        fillRule="evenodd"
      />
    </svg>
  );
}
