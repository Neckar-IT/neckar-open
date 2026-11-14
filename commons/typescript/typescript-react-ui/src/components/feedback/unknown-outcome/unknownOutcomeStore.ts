import { useEffect, useSyncExternalStore } from "react";

/**
 * A mutation with an unknown outcome (HTTP 504 or a network error), waiting for the user's decision in the form shell
 * that was submitting. `action` names it once `withActionFailedToast` knows the label.
 */
export interface UnknownOutcome {
  shellId: string;
  action: string | undefined;
}

// Module level, the same lifetime as sonner's `toast`: the mutation cache handler and `withActionFailedToast` are plain functions.
const submittingShellIds: string[] = [];
let unknownOutcome: UnknownOutcome | undefined;
const listeners = new Set<() => void>();

function notify(): void {
  listeners.forEach((listener) => listener());
}

/**
 * Marks the shell [shellId] as submitting while [isSubmitting] holds; an unknown outcome in that time belongs to the shell that started
 * submitting last. React-hook-form keeps `isSubmitting` set across every await of the submit, confirmation dialogs included.
 */
export function useSubmittingShell(shellId: string, isSubmitting: boolean): void {
  useEffect(() => {
    if (!isSubmitting) {
      return undefined;
    }
    submittingShellIds.push(shellId);
    return () => {
      const index = submittingShellIds.lastIndexOf(shellId);
      if (index >= 0) {
        submittingShellIds.splice(index, 1);
      }
    };
  }, [shellId, isSubmitting]);
}

/**
 * Records an unknown outcome for the submitting shell; false when no shell is submitting, so the caller shows the toast.
 */
export function recordUnknownOutcome(action: string | undefined): boolean {
  const shellId = submittingShellIds.at(-1);
  if (shellId === undefined) {
    return false;
  }
  unknownOutcome = { shellId, action: action ?? (unknownOutcome?.shellId === shellId ? unknownOutcome.action : undefined) };
  notify();
  return true;
}

export function clearUnknownOutcome(): void {
  unknownOutcome = undefined;
  notify();
}

/**
 * Clears the unanswered mutation when the shell [shellId] was submitting it.
 */
export function clearUnknownOutcomeOf(shellId: string): void {
  if (unknownOutcome?.shellId === shellId) {
    clearUnknownOutcome();
  }
}

function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

/**
 * The unanswered mutation of the shell [shellId], or undefined when another shell or none was submitting it.
 */
export function useUnknownOutcome(shellId: string): UnknownOutcome | undefined {
  const mutation = useSyncExternalStore(subscribe, () => unknownOutcome);
  return mutation?.shellId === shellId ? mutation : undefined;
}
