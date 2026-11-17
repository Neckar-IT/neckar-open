/**
 * Imports
 */

import { Button } from "@neckar-hub/typescript-react-ui/actions";
import { AlertTriangleIcon } from "lucide-react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { ErrorMessage } from "./ErrorMessage";
import { ErrorPageLayout } from "./ErrorPageLayout";
import { ErrorSupportActions } from "./ErrorSupportActions";
import { LogoutButton } from "./LogoutButton";
import { RequestDetails } from "./RequestDetails";
import { StackTrace } from "./StackTrace";
import { TraceLink } from "./TraceLink";
import { VersionInfo } from "./VersionInfo";

/**
 * Prop Types
 */

export interface UnexpectedErrorPageProps {
  error: unknown;
  errorCaughtBy: string;
  /** `errorCode` of an `ExpectedApplicationError` payload. */
  errorCode?: string;
  errorMessage?: string;
  /** `exceptionType` of an `UnexpectedApplicationError` payload. */
  exceptionType?: string;
  stackTrace?: string;
}

/**
 * Every error that is neither a version mismatch, a missing capability nor an unavailable service.
 */

export function UnexpectedErrorPage(props: UnexpectedErrorPageProps) {
  const { t } = useEmbeddedTranslation();

  const handleReload = () => {
    console.warn("[UnexpectedErrorPage] reloading page (user-click)");
    window.location.reload();
  };

  return (
    <ErrorPageLayout>
      <AlertTriangleIcon className="text-muted-foreground h-24 w-24" />
      <p>
        <strong>{t("unexpectedErrorPage.oops")}</strong>
        <span className="ml-1.5">{t("unexpectedErrorPage.somethingWentWrong")}</span>
      </p>

      <div className="flex gap-2">
        <Button onClick={handleReload}>{t("unexpectedErrorPage.reloadApplication")}</Button>
        <LogoutButton />
      </div>

      <ErrorMessage errorCode={props.errorCode} errorMessage={props.errorMessage} exceptionType={props.exceptionType} />

      <TraceLink error={props.error} />

      <RequestDetails error={props.error} initiallyOpen={true} />

      <VersionInfo errorCaughtBy={props.errorCaughtBy} />

      {props.stackTrace !== undefined && <StackTrace initiallyOpen={true} stackTrace={props.stackTrace} />}

      <ErrorSupportActions
        error={props.error}
        errorCaughtBy={props.errorCaughtBy}
        errorCode={props.errorCode}
        errorMessage={props.errorMessage}
        exceptionType={props.exceptionType}
        hint={t("unexpectedErrorPage.sendScreenshot")}
        stackTrace={props.stackTrace}
      />
    </ErrorPageLayout>
  );
}
