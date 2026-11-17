/**
 * Imports
 */

import type { CustomIconProps } from "./CustomIconProps";

/**
 * Lock-in-a-circle icon
 */

export function CircleLockIcon(props: React.ComponentProps<"svg"> & CustomIconProps) {
  return (
    <svg
      fill="none"
      height={props.size ?? 24}
      stroke={props.color ?? "currentColor"}
      strokeLinecap="round"
      strokeLinejoin="round"
      viewBox="0 0 24 24"
      width={props.size ?? 24}
      xmlns="http://www.w3.org/2000/svg"
      {...props}
    >
      <circle cx="12" cy="12" r="10" strokeWidth="2" />
      <path
        d="M9.29167 10.959V8.79229C9.29167 8.07399 9.57701 7.38512 10.0849 6.87721C10.5928 6.3693 11.2817 6.08395 12 6.08395C12.7183 6.08395 13.4072 6.3693 13.9151 6.87721C14.423 7.38512 14.7083 8.07399 14.7083 8.79229V10.959M8.20833 10.959H15.7917C16.39 10.959 16.875 11.444 16.875 12.0423V15.834C16.875 16.4323 16.39 16.9173 15.7917 16.9173H8.20833C7.61002 16.9173 7.125 16.4323 7.125 15.834V12.0423C7.125 11.444 7.61002 10.959 8.20833 10.959Z"
        strokeWidth="1.25"
      />
    </svg>
  );
}
