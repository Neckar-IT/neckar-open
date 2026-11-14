/**
 * Imports
 */

import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import type { PropsWithChildren, ReactElement, ReactNode } from "react";
import { createContext, useContext, useEffect, useMemo, useRef, useState } from "react";

/**
 * Type Declarations
 */

type MaybePromise<T> = T | Promise<T>;

export interface Step {
  /**
   * Step Title to be displayed in the header (will attempt to translate)
   */
  title: TextKey;
  component: ReactNode;
  disabled?: boolean;
}

interface MultiStepControlContextType {
  control: MultiStepControl;
  currentStepIndex: number;
  steps: Step[];
  onNext: (newStepIndex: number) => void;
  onPrevious: (newStepIndex: number) => void;
  isSubmitting: boolean;
  setIsSubmitting: (isSubmitting: boolean) => void;
}

type MultiStepControlProviderProps = PropsWithChildren<
  Omit<MultiStepControlContextType, "control" | "isSubmitting" | "setIsSubmitting"> & {
    isSubmitting?: boolean;
  }
>;

/**
 * Multi Step Control holds the callbacks that are called when the user is navigating between the steps
 */

export class MultiStepControl {
  private onNextCallbacks = new Set<() => MaybePromise<boolean>>();
  private onPreviousCallbacks = new Set<() => MaybePromise<boolean>>();

  public addOnNextCallback(callback: () => MaybePromise<boolean>) {
    this.onNextCallbacks.add(callback);
  }

  public addOnPreviousCallback(callback: () => MaybePromise<boolean>) {
    this.onPreviousCallbacks.add(callback);
  }

  public deleteOnNextCallback(callback: () => MaybePromise<boolean>) {
    this.onNextCallbacks.delete(callback);
  }

  public deleteOnPreviousCallback(callback: () => MaybePromise<boolean>) {
    this.onPreviousCallbacks.delete(callback);
  }

  private async executeCallbacks(callbacks: Set<() => MaybePromise<boolean>>) {
    // Iterate over a snapshot: a step component may re-register its callback (delete + add) while
    // `await cb()` is suspended. Mutating the live Set mid-iteration makes the for-of visit the newly
    // added entry forever, which never terminates and freezes the browser in an infinite render loop.
    for (const cb of Array.from(callbacks)) {
      // eslint-disable-next-line no-await-in-loop -- callbacks must run sequentially and the loop short-circuits on the first falsy result; Promise.all would run all of them and lose the early exit.
      const result = await cb();
      if (!result) return false;
    }
    return true;
  }

  public handleNext() {
    return this.executeCallbacks(this.onNextCallbacks);
  }

  public handlePrevious() {
    return this.executeCallbacks(this.onPreviousCallbacks);
  }
}

/**
 * Context
 */

const MultiStepControlContext = createContext<MultiStepControlContextType | null>(null);

export function MultiStepControlProvider({
  children,
  isSubmitting,
  currentStepIndex,
  steps,
  onNext,
  onPrevious,
}: MultiStepControlProviderProps): ReactElement {
  // The control instance holds the onNext/onPrevious subscribers. It must survive every re-render,
  // otherwise subscribers registered via useOnNext/useOnPrevious would be silently dropped.
  // useRef (not useMemo) guarantees the instance is created exactly once and is never discarded —
  // useMemo is only a performance hint and React may recompute it.
  const controlRef = useRef<MultiStepControl | null>(null);
  controlRef.current ??= new MultiStepControl();
  const control = controlRef.current;

  const [localIsSubmitting, setLocalIsSubmitting] = useState(false);

  const contextValue = useMemo<MultiStepControlContextType>(
    () => ({
      control,
      currentStepIndex,
      steps,
      onNext,
      onPrevious,
      isSubmitting: isSubmitting || localIsSubmitting,
      setIsSubmitting: setLocalIsSubmitting,
    }),
    [control, currentStepIndex, steps, onNext, onPrevious, isSubmitting, localIsSubmitting],
  );

  return <MultiStepControlContext value={contextValue}>{children}</MultiStepControlContext>;
}

type NavigationDirection = "next" | "previous";

/**
 * Registers a navigation callback on the MultiStepControl for the lifetime of the calling component.
 *
 * The latest callback is held in a ref and invoked through a stable wrapper, so the registration effect
 * runs only on mount/unmount. Registering the raw callback directly would re-run the effect on every
 * render (an inline callback has a new identity each time) and mutate the subscriber Set while
 * executeCallbacks iterates it, producing an infinite render loop.
 */
function useNavigationCallback(direction: NavigationDirection, callback: () => MaybePromise<boolean>, hookName: string): void {
  const context = useContext(MultiStepControlContext);

  if (context === null) {
    throw new Error(`${hookName} must be used within a MultiStep Component!`);
  }

  const callbackRef = useRef(callback);
  callbackRef.current = callback;

  const control = context.control;

  useEffect(() => {
    const stableCallback = () => callbackRef.current();

    switch (direction) {
      case "next":
        control.addOnNextCallback(stableCallback);
        return () => control.deleteOnNextCallback(stableCallback);
      case "previous":
        control.addOnPreviousCallback(stableCallback);
        return () => control.deleteOnPreviousCallback(stableCallback);
      default:
        throw new Error(`Unknown navigation direction: ${String(direction)}`);
    }
  }, [control, direction]);
}

/**
 * Hook to register a onNext callback
 */

export function useOnNext(callback: () => MaybePromise<boolean>): void {
  useNavigationCallback("next", callback, "useOnNext");
}

/**
 * Hook to register a onPrevious callback
 */

export function useOnPrevious(callback: () => MaybePromise<boolean>): void {
  useNavigationCallback("previous", callback, "useOnPrevious");
}

/**
 * Hook to trigger the callbacks
 */

export function useMultiStepControl() {
  const context = useContext(MultiStepControlContext);

  if (context === null) {
    throw new Error("useMultiStepControl must be used within a MultiStep Component!");
  }

  const handleNext = async () => {
    context.setIsSubmitting(true);
    const result = await context.control.handleNext();
    context.setIsSubmitting(false);

    if (result) {
      let incrementBy = 1;

      // skip disabled steps
      while (context.steps[context.currentStepIndex + incrementBy]?.disabled && context.currentStepIndex < context.steps.length - 1) {
        incrementBy++;
      }

      context.onNext(Math.min(context.currentStepIndex + incrementBy, context.steps.length - 1));
    }
  };

  const handlePrevious = async () => {
    context.setIsSubmitting(true);
    const result = await context.control.handlePrevious();
    context.setIsSubmitting(false);

    if (result) {
      let decrementBy = 1;

      // skip disabled steps
      while (context.steps[context.currentStepIndex - decrementBy]?.disabled && context.currentStepIndex > 0) {
        decrementBy++;
      }

      context.onPrevious(Math.max(context.currentStepIndex - decrementBy, 0));
    }
  };

  return { handleNext, handlePrevious, isSubmitting: context.isSubmitting, setIsSubmitting: context.setIsSubmitting };
}
