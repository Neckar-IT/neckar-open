/**
 * Imports
 */

import { cn } from "cn";

/**
 * Props
 */

export interface Props {
  className?: string | undefined;
  value: number;
}

/**
 * SVG ring that fills proportionally to a 0-100 percentage value.
 *
 * @prop value - The value of the progress (0 - 100)
 */

export function CircularProgress({ className, value }: Props) {
  return (
    <svg className={cn("h-full w-full", className)} viewBox="0 0 24 24">
      <circle
        className="stroke-current"
        cx="12"
        cy="12"
        fill="transparent"
        r="9"
        strokeDashoffset={`calc(56.548667765 - (56.548667765 * ${value.toString()}) / 100)`}
        strokeLinecap="round"
        strokeWidth="2"
        style={{
          strokeDasharray: "56.548667765, 56.548667765",
          transition: "stroke-dashoffset 0.35s",
          transform: "rotate(-90deg)",
          transformOrigin: "50% 50%",
        }}
      />
      <circle
        className="stroke-current"
        cx="12"
        cy="12"
        fill="transparent"
        r="9"
        strokeLinecap="round"
        strokeWidth="2"
        style={{ opacity: 0.1 }}
      />
    </svg>
  );
}
