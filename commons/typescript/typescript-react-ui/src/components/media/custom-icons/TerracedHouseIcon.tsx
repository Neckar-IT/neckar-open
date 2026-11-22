/**
 * Imports
 */

import type { CustomIconProps } from "./CustomIconProps";

/**
 * Terraced house: a row of houses sharing their side walls.
 */

export function TerracedHouseIcon(props: React.ComponentProps<"svg"> & CustomIconProps): React.ReactElement {
  return (
    <svg fill="none" height={props.size ?? 24} viewBox="0 0 24 24" width={props.size ?? 24} xmlns="http://www.w3.org/2000/svg" {...props}>
      <path
        stroke={props.color ?? "currentColor"}
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
        d="M18.83 21v-5.779c0-.191.064-.375.179-.51a.569.569 0 0 1 .432-.212h2.447c.162 0 .318.076.433.211.114.136.179.32.179.511"
      />
      <path
        stroke={props.color ?? "currentColor"}
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
        d="M22.5 10c0-.29-.025-.578-.072-.842a2.012 2.012 0 0 0-.354-.825l-5.422-5.932c-.238-.264-.612-.522-.796-.522-.183 0-.356.315-.356.608V21H21.722c.206 0 .404-.21.55-.586.146-.375.228-.884.228-1.414v-9ZM4.83 21v-5.779c0-.191.064-.375.179-.51a.569.569 0 0 1 .432-.212h2.447c.162 0 .318.076.433.211.115.136.179.32.179.511"
      />
      <path
        stroke={props.color ?? "currentColor"}
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
        d="M8.5 10c0-.29-.025-.578-.072-.842a2.013 2.013 0 0 0-.354-.825L2.652 2.401c-.238-.264-.612-.522-.795-.522-.184 0-.357.315-.357.608V21h7V10ZM11.83 21v-5.779c0-.191.064-.375.179-.51a.569.569 0 0 1 .432-.212h2.447c.162 0 .318.076.433.211.114.136.179.32.179.511"
      />
      <path
        stroke={props.color ?? "currentColor"}
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
        d="M15.5 10c0-.29-.025-.578-.072-.842a2.012 2.012 0 0 0-.354-.825L9.652 2.401c-.238-.264-.612-.522-.795-.522-.184 0-.357.315-.357.608V21h7V10Z"
      />
      <rect width="3" height="3" x="3.665" y="9" fill={props.color ?? "currentColor"} rx="1" />
      <rect width="3" height="3" x="10.665" y="9" fill={props.color ?? "currentColor"} rx="1" />
      <rect width="3" height="3" x="17.665" y="9" fill={props.color ?? "currentColor"} rx="1" />
    </svg>
  );
}
