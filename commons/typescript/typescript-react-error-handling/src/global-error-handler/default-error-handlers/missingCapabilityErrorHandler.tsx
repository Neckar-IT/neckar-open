/**
 * Imports
 */

import { capabilityTranslationKey, Trans } from "@neckar-hub/typescript-react-i18n";
import { reactionToFailure } from "@neckar-hub/typescript-react-query";
import { Button } from "@neckar-hub/typescript-react-ui/actions";
import { type ExpectedApplicationError } from "@neckar-hub/typescript-utils/openapi-types";
import { LockKeyholeIcon } from "lucide-react";
import { EmbeddedTranslationNamespace, useEmbeddedTranslation } from "../../localization/useEmbeddedTranslation";
import { ErrorPageViewport } from "./components/ErrorPageLayout";
import { buildMailtoUrl } from "./components/mailtoUrl";
import { TraceLink } from "./components/TraceLink";

/**
 * Error Handler
 *
 * `PermissionDeniedPage` for an HTTP 403 with `errorCode` `AUTHZ-1` and `attributes.missingCapability`.
 */
export function missingCapabilityErrorHandler(error: unknown, errorCaughtBy: string): React.ReactNode | null {
  const reaction = reactionToFailure(error, "query");
  if (reaction.type !== "permissionDenied") return null;

  return (
    <PermissionDeniedPage
      error={error}
      errorCaughtBy={errorCaughtBy}
      errorData={reaction.payload}
      missingCapability={reaction.missingCapability}
    />
  );
}

/**
 * Permission-Denied Page
 */

export interface PermissionDeniedPageProps {
  error: unknown;
  errorData: ExpectedApplicationError;
  errorCaughtBy: string;
  missingCapability: string;
}

export function PermissionDeniedPage(props: PermissionDeniedPageProps): React.ReactNode {
  const { t } = useEmbeddedTranslation();

  const handleGoBack = () => {
    // A full document load is the only way off this page. The error boundary above renders it
    // instead of the application, and nothing here can reset that boundary — a history-only
    // navigation would swap the address bar and leave the 403 page on screen.
    //
    // So: step back to the entry the user came from, and reload once the address bar carries its
    // URL. Reloading any earlier would re-request the forbidden route and land right back here.
    // When that entry belongs to another document the browser loads it directly and the listener
    // dies with this one.
    if (window.history.length > 1) {
      window.addEventListener("popstate", () => window.location.reload(), { once: true });
      window.history.back();
    } else {
      // Direct deep link, nothing to step back to: start over at the application's own entry
      // point. BASE_URL rather than "/" so an app served under a base path stays inside it.
      window.location.href = import.meta.env.BASE_URL;
    }
  };

  // The raw key serves as fallback so a missing translation still tells the user *which* capability was checked.
  const capabilityI18nKey = capabilityTranslationKey(props.missingCapability);
  const capabilityLabel = t({
    key: capabilityI18nKey,
    options: { defaultValue: props.missingCapability },
  });

  const mailSubject = t({
    key: "missingCapabilityErrorHandler.contactAdminMailSubject",
    options: { capabilityLabel },
  });
  const mailBody = t({
    key: "missingCapabilityErrorHandler.contactAdminMailBody",
    options: { capabilityLabel, capabilityKey: props.missingCapability },
  });
  const mailtoUrl = buildMailtoUrl(null, mailSubject, mailBody);

  return (
    <ErrorPageViewport>
      <div className="grid w-full max-w-4xl items-center gap-12 md:grid-cols-2">
        <IllustrationBlock />

        <div className="flex flex-col items-start gap-4 text-left">
          <p className="text-muted-foreground text-xs font-medium tracking-widest uppercase">HTTP 403</p>

          <h1 className="text-4xl font-bold tracking-tight">{t("missingCapabilityErrorHandler.heading")}</h1>

          <p>
            <Trans
              i18nKey="missingCapabilityErrorHandler.message"
              ns={EmbeddedTranslationNamespace}
              values={{ capabilityLabel }}
              components={{ strong: <strong /> }}
            />
          </p>

          <p className="text-muted-foreground text-sm">{t("missingCapabilityErrorHandler.contactAdminHint")}</p>

          <div className="mt-2 flex flex-wrap items-center gap-3">
            <Button onClick={handleGoBack}>{t("missingCapabilityErrorHandler.goBack")}</Button>
            <Button asChild={true} variant="outline">
              <a href={mailtoUrl}>{t("missingCapabilityErrorHandler.contactAdmin")}</a>
            </Button>
          </div>

          <div className="text-muted-foreground mt-4 flex w-full flex-col gap-1.5 font-mono text-xs">
            <p>
              {t("missingCapabilityErrorHandler.errorCodeLabel")} {props.errorData.errorCode} · {props.missingCapability}
            </p>
            <TraceLink error={props.error} />
          </div>
        </div>
      </div>
    </ErrorPageViewport>
  );
}

/**
 * Decorative illustration shown next to the message. The corner brackets give
 * the panel a "technical drawing" feel that fits the system-level "permission"
 * context better than a single floating icon.
 */
function IllustrationBlock(): React.ReactNode {
  return (
    <div className="bg-primary/5 relative flex aspect-square items-center justify-center rounded-3xl">
      <span className="border-primary/30 absolute top-4 left-4 h-6 w-6 rounded-tl-md border-t-2 border-l-2" />
      <span className="border-primary/30 absolute top-4 right-4 h-6 w-6 rounded-tr-md border-t-2 border-r-2" />
      <span className="border-primary/30 absolute bottom-4 left-4 h-6 w-6 rounded-bl-md border-b-2 border-l-2" />
      <span className="border-primary/30 absolute right-4 bottom-4 h-6 w-6 rounded-br-md border-r-2 border-b-2" />
      <LockKeyholeIcon className="text-primary/80 h-32 w-32" strokeWidth={1.25} />
    </div>
  );
}
