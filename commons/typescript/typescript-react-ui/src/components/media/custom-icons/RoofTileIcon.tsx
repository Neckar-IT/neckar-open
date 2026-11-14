/**
 * Imports
 */

import type { CustomIconProps } from "./CustomIconProps";

/**
 * Roof Tile Icon
 */

export function RoofTileIcon(props: React.ComponentProps<"svg"> & CustomIconProps): React.ReactElement {
  return (
    <svg fill="none" height={props.size ?? 24} viewBox="0 0 24 24" width={props.size ?? 24} xmlns="http://www.w3.org/2000/svg" {...props}>
      <path stroke={props.color ?? "currentColor"} strokeWidth="2" d="M18.5 20.645c0-2.485-1.79-4.5-4-4.5-2.209 0-4 2.015-4 4.5" />
      <path
        fill={props.color ?? "currentColor"}
        d="M1.123 15.273c.143 0 .06-.014.202 0 2.653.24 4.052 2.604 4.163 4.972.008.134.012.266.012.4h-2c0-.09-.003-.18-.008-.27-.074-1.608-1.291-3.113-2.75-3.22-.08-.007-.16-.01-.242-.01l.623-1.872Z"
      />
      <path
        stroke={props.color ?? "currentColor"}
        strokeWidth="2"
        d="M11.5 19.644H4.942M22.5 8.645c0-2.486-1.79-4.5-4-4.5-2.209 0-4 2.014-4 4.5"
      />
      <path
        fill={props.color ?? "currentColor"}
        d="M5.072 3.355c2.87 0 4.428 2.366 4.428 5.29h-2c0-2.047-1.45-3.5-3-3.5l.572-1.79ZM17.607 20.01l3.996-12.015 1.898.631-3.996 12.015zM10.48 17.395 13.6 8.006l1.898.632-3.122 9.388z"
      />
      <path
        fill={props.color ?? "currentColor"}
        d="M3.84 19.653 7.881 7.502l1.898.631-4.04 12.15zM.5 17.148 5.083 3.364l1.898.63L2.45 17.149H.5Z"
      />
      <path stroke={props.color ?? "currentColor"} strokeWidth="2" d="M15.5 7.645h-7" />
    </svg>
  );
}
