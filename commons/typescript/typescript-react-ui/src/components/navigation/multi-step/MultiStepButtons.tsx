/**
 * Imports
 */

import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import { cn } from "cn";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { Button } from "../../primitives/button";
import { useMultiStepControl } from "./MultiStepControl";

/**
 * Prop Types
 */

export interface MultiStepButtonsProps {
  currentStepIndex: number;
  stepsCount: number;
  className?: string;
  textKeys?: Partial<MultiStepButtonNextTextKeys & MultiStepButtonPreviousTextKeys>;
}

interface MultiStepButtonNextTextKeys {
  next: TextKey;
  finish: TextKey;
}

interface MultiStepButtonNextProps {
  currentStepIndex: number;
  stepsCount: number;
  textKeys?: Partial<MultiStepButtonNextTextKeys>;
}

interface MultiStepButtonPreviousTextKeys {
  previous: TextKey;
  cancel: TextKey;
}

interface MultiStepButtonPreviousProps {
  currentStepIndex: number;
  textKeys?: Partial<MultiStepButtonPreviousTextKeys>;
}

/**
 * Buttons to control the Multi Step Component
 */

export function MultiStepButtons({ currentStepIndex, stepsCount, className, textKeys }: MultiStepButtonsProps): React.ReactElement {
  return (
    <div className={cn("flex justify-end gap-2", className)}>
      <MultiStepButtonPrevious currentStepIndex={currentStepIndex} textKeys={textKeys} />
      <MultiStepButtonNext currentStepIndex={currentStepIndex} stepsCount={stepsCount} textKeys={textKeys} />
    </div>
  );
}

/**
 * Button to go to the next step
 */

export function MultiStepButtonNext({ currentStepIndex, stepsCount, textKeys }: MultiStepButtonNextProps): React.ReactElement {
  const { handleNext, isSubmitting } = useMultiStepControl();

  const { t } = useEmbeddedTranslation();
  const defaultText =
    currentStepIndex === stepsCount - 1 ? t(textKeys?.finish ?? "multiStep.finish") : t(textKeys?.next ?? "multiStep.next");

  return (
    <Button onClick={handleNext} loading={isSubmitting}>
      {defaultText}
    </Button>
  );
}

/**
 * Button to go to the previous step
 */

export function MultiStepButtonPrevious({ currentStepIndex, textKeys }: MultiStepButtonPreviousProps): React.ReactElement {
  const { handlePrevious, isSubmitting } = useMultiStepControl();

  const { t } = useEmbeddedTranslation();
  const defaultText = currentStepIndex === 0 ? t(textKeys?.cancel ?? "multiStep.cancel") : t(textKeys?.previous ?? "multiStep.previous");

  return (
    <Button onClick={handlePrevious} disabled={isSubmitting} variant="outline">
      {defaultText}
    </Button>
  );
}
