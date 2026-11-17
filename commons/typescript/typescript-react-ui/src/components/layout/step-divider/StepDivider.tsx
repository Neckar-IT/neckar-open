/**
 * Step Divider (inspired by TailwindUi: https://tailwindui.com/components/application-ui/navigation/steps#component-ef491b1515ff05e8cc7429f37bc0fae5)
 */

import { cn } from "cn";

/**
 * Prop Types
 */

interface Props {
  className?: string | undefined;
}

/**
 * Diagonal SVG chevron used as a visual separator between steps in a step bar.
 */

export function StepDivider({ className }: Props) {
  return (
    <svg className={cn("text-border h-12 w-4", className)} fill="none" viewBox="0 0 16 52" xmlns="http://www.w3.org/2000/svg">
      <g clipPath="url(#step-divider-clip-path)" stroke="currentColor">
        <path d="m.563 52.757 15-27M15.563 26.243l-15-27" />
      </g>
      <defs>
        <clipPath id="step-divider-clip-path">
          <path d="M0 0h16v52H0z" fill="#fff" />
        </clipPath>
      </defs>
    </svg>
  );
}
