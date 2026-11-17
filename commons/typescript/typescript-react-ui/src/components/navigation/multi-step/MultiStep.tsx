/**
 * Imports
 */

import { cn } from "cn";
import { MultiStepButtons, type MultiStepButtonsProps } from "./MultiStepButtons";
import { MultiStepControlProvider, type Step } from "./MultiStepControl";
import { MultiStepList } from "./MultiStepList";

/**
 * Prop Types
 */

interface MultiStepProps {
  steps: Step[];
  currentStepIndex: number;
  className?: string;
  textKeys?: MultiStepButtonsProps["textKeys"];
  onNext: (newStepIndex: number) => void;
  onPrevious: (newStepIndex: number) => void;
  isSubmitting?: boolean;
}

/**
 * Stepped wizard layout with a step list, current step content, and previous/next navigation buttons.
 */

export function MultiStep({
  steps,
  currentStepIndex,
  className,
  textKeys,
  onNext,
  onPrevious,
  isSubmitting,
}: MultiStepProps): React.ReactElement {
  return (
    <MultiStepControlProvider
      steps={steps}
      currentStepIndex={currentStepIndex}
      onNext={onNext}
      onPrevious={onPrevious}
      isSubmitting={isSubmitting}
    >
      <div className={cn("flex flex-col gap-4", className)}>
        <MultiStepList steps={steps} currentStepIndex={currentStepIndex} />

        {steps[currentStepIndex]?.component}

        <MultiStepButtons currentStepIndex={currentStepIndex} stepsCount={steps.length} textKeys={textKeys} />
      </div>
    </MultiStepControlProvider>
  );
}
