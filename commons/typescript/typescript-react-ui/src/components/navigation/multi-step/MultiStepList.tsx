/**
 * Imports
 */

import { useTranslation } from "@neckar-hub/typescript-react-i18n";
import { cn } from "cn";
import { Fragment } from "react";
import type { Step } from "./MultiStepControl";

/**
 * Step List
 */

interface MultiStepListProps {
  steps: Step[];
  currentStepIndex: number;
  className?: string | undefined;
}

export function MultiStepList({ steps, currentStepIndex, className }: MultiStepListProps): React.ReactElement {
  return (
    <div className={cn("flex items-center gap-4", className)}>
      {steps.map((step, index) => (
        <Fragment key={index}>
          {index > 0 && <MultiStepSeparator index={index} currentStepIndex={currentStepIndex} />}
          <MultiStepItem index={index} currentStepIndex={currentStepIndex} step={step} />
        </Fragment>
      ))}
    </div>
  );
}

/**
 * Multi Step Item
 */

interface MultiStepItemProps {
  index: number;
  currentStepIndex: number;
  step: Step;
}

function MultiStepItem({ index, currentStepIndex, step }: MultiStepItemProps): React.ReactElement {
  const { t } = useTranslation();

  const isDisabled = step.disabled === true;
  const isActive = index === currentStepIndex;

  return (
    <div className="flex items-center gap-2">
      <div
        className={cn("flex h-9 w-9 items-center justify-center rounded-full border", {
          "border-primary text-primary font-semibold": isActive,
          "text-muted-foreground": isDisabled,
        })}
      >
        {index + 1}
      </div>
      <div className={cn("text-sm", { "text-primary font-semibold": isActive, "text-muted-foreground": isDisabled })}>{t(step.title)}</div>
    </div>
  );
}

/**
 * Multi Step Separator
 */

interface MultiStepSeparatorProps {
  index: number;
  currentStepIndex: number;
}

function MultiStepSeparator({ index, currentStepIndex }: MultiStepSeparatorProps): React.ReactElement {
  const isCompleted = index <= currentStepIndex;

  return <div className={cn("bg-border h-1 flex-1 rounded-full", { "bg-primary opacity-50": isCompleted })} />;
}
