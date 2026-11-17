import type { FieldValues, SubmitErrorHandler } from "react-hook-form";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { toast } from "../../feedback";

/**
 * Shared `onInvalid` handler for `form.handleSubmit(onValid, onInvalid)`.
 *
 * Without it, a failed validation is a **silent no-op**: `handleSubmit` only sets field errors, and
 * when the failing field has no visible input (e.g. an entry of an inline embedded list), the save
 * button appears to do nothing. This handler always shows an error toast (embedded translations)
 * so the user gets feedback, whatever field failed.
 *
 * ```ts
 * const onInvalidSubmit = useInvalidFormSubmitHandler();
 * // ...
 * void form.handleSubmit(onValid, onInvalidSubmit)();
 * ```
 */
export function useInvalidFormSubmitHandler<TFieldValues extends FieldValues = FieldValues>(): SubmitErrorHandler<TFieldValues> {
  const { t } = useEmbeddedTranslation();
  return () => {
    toast.error(t("formFeedback.invalidForm"));
  };
}
