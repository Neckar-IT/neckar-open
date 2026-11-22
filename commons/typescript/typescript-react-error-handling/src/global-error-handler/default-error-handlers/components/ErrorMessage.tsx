/**
 * Imports
 */

import { cn } from "cn";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";

/**
 * Prop Types
 */

interface ErrorMessageProps {
  errorCode?: string | null;
  /** Class of the server exception an `UnexpectedApplicationError` names. */
  exceptionType?: string | null;
  errorMessage?: string | null;
  className?: string | null;
}

export function ErrorMessage(props: ErrorMessageProps): React.ReactNode {
  const { t } = useEmbeddedTranslation();
  const prefix = props.errorCode || props.exceptionType;
  // The exception type stands alone for a body without a message, which every 5xx body is.
  const text = props.errorMessage || (props.exceptionType ? null : t("errorMessage.noMessage"));

  return (
    <div className={cn("w-full", props.className)}>
      <p className="text-muted-foreground text-sm font-semibold">{t("errorMessage.error")}</p>
      <pre className="bg-muted max-h-48 overflow-auto rounded-md border px-4 py-2 break-all whitespace-pre-wrap">
        {prefix ? <strong>{text ? `${prefix}: ` : prefix}</strong> : null}
        {text ? <span>{text}</span> : null}
      </pre>
    </div>
  );
}
