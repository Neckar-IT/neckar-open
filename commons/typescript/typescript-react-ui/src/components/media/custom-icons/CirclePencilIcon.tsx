import type { CustomIconProps } from "./CustomIconProps";

/**
 * Pencil-in-a-circle edit icon.
 */
export function CirclePencilIcon(props: React.ComponentProps<"svg"> & CustomIconProps): React.ReactElement {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" width={props.size ?? 24} height={props.size ?? 24} fill="none" viewBox="0 0 24 24" {...props}>
      <path
        stroke={props.color ?? "currentColor"}
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
        d="M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10"
      />
      <path
        stroke={props.color ?? "currentColor"}
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.5"
        d="M16.706 9.34a1.446 1.446 0 1 0-2.045-2.046l-6.845 6.847c-.12.119-.208.265-.257.426l-.677 2.232a.257.257 0 0 0 .32.32l2.232-.678c.16-.049.307-.136.426-.255zM13.539 8.41l2.052 2.052"
      />
    </svg>
  );
}
