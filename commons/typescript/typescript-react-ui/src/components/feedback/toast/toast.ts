// Sonner has no per-type default options, so we wrap `error` to inject longer duration + close button.

import { toast as sonnerToast } from "sonner";

const ERROR_DEFAULTS = {
  duration: 10000,
  closeButton: true,
};

const errorWithDefaults: typeof sonnerToast.error = (message, options) => sonnerToast.error(message, { ...ERROR_DEFAULTS, ...options });

/**
 * Toast notification helper built on sonner.
 */
const toast: typeof sonnerToast = Object.assign((...args: Parameters<typeof sonnerToast>) => sonnerToast(...args), sonnerToast, {
  error: errorWithDefaults,
});

/**
 * Id of the error toast for a failed mutation. `createMutationErrorToastHandler` shows its generic message under it first, and
 * `withActionFailedToast` replaces that message with the one naming the action.
 */
const mutationFailedToastId = "mutation-failed";

export { mutationFailedToastId, toast };
