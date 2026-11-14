import type { CustomIconProps } from "./CustomIconProps";

/**
 * Paper-plane send icon in a circle.
 */
export function CircleSendIcon(props: React.ComponentProps<"svg"> & CustomIconProps): React.ReactElement {
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
        d="M12.427 18.223a.277.277 0 0 0 .52-.013L16.55 7.674a.274.274 0 0 0-.352-.352L5.663 10.926a.277.277 0 0 0-.013.52l4.397 1.763a1.1 1.1 0 0 1 .617.616zM16.485 7.389l-6.066 6.066"
      />
    </svg>
  );
}
