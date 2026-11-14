/**
 * Imports
 */

import { reactionToFailure } from "@neckar-hub/typescript-react-query";
import { Button } from "@neckar-hub/typescript-react-ui/actions";
import { CloudOffIcon } from "lucide-react";
import { useEffect, useRef } from "react";
import {
  outageToServiceAvailability,
  type ServiceAvailability,
  useRecoveryRound,
  useServiceAvailability,
} from "../../app-connectivity/appConnectivityState";
import { serviceAvailabilityText } from "../../app-connectivity/serviceAvailabilityText";
import { useEmbeddedTranslation } from "../../localization/useEmbeddedTranslation";
import { ErrorPageLayout } from "./components/ErrorPageLayout";

/**
 * Error Handler
 */

/**
 * `ServiceUnavailablePage` for 502, 503, 504 and network errors. [recover] fetches again; without it the retry button reloads the page.
 */
export function serviceUnavailableErrorHandler(error: unknown, recover?: () => void): React.ReactNode | null {
  const reaction = reactionToFailure(error, "query");
  if (reaction.type !== "serviceUnavailablePage") {
    return null;
  }
  return <ServiceUnavailablePage fallbackAvailability={outageToServiceAvailability(reaction.failure)} onRecover={recover} />;
}

/**
 * Service Unavailable Page
 */

export interface ServiceUnavailablePageProps {
  /** The availability the failed request itself stands for; shown while the health poll reports no unavailable state. */
  fallbackAvailability: ServiceAvailability;
  /** Fetches again; called in every recovery round of the health poll, and by the retry button. */
  onRecover?: () => void;
  /** The caller fetches again on an interval of its own, as `useMeSession` does, so the page continues without a health poll. */
  fetchesOnItsOwn?: boolean;
}

export function ServiceUnavailablePage(props: ServiceUnavailablePageProps): React.ReactNode {
  const { t } = useEmbeddedTranslation();
  const polledAvailability = useServiceAvailability();
  const { onRecover } = props;

  const recoveryRound = useRecoveryRound();
  const handledRound = useRef(recoveryRound);
  const isAvailable = polledAvailability?.type === "available";
  // AppConnectivityProvider starts a round once the poll answers `available`, at most one per half poll interval; a page that
  // stays mounted, as in the session gate, fetches again in every round.
  useEffect(() => {
    if (recoveryRound !== handledRound.current) {
      handledRound.current = recoveryRound;
      onRecover?.();
    }
  }, [recoveryRound, onRecover]);

  const availability: ServiceAvailability =
    polledAvailability !== undefined && !isAvailable ? polledAvailability : props.fallbackAvailability;
  const text = serviceAvailabilityText(availability);

  // Without recovery from the caller, as under GlobalErrorHandler, only a document load leaves the page.
  const canRecover = onRecover !== undefined;
  // Only a health poll or the caller's own interval calls onRecover without a click.
  const continuesByItself = canRecover && (polledAvailability !== undefined || props.fetchesOnItsOwn === true);
  const reload = () => window.location.reload();
  const handleGoBack = () => {
    if (window.history.length > 1) {
      if (!canRecover) {
        window.addEventListener("popstate", reload, { once: true });
      }
      window.history.back();
    } else {
      // A deep link has no entry to step back to: start over at the application's entry point.
      window.location.href = import.meta.env.BASE_URL;
    }
  };

  return (
    <ErrorPageLayout>
      <CloudOffIcon className="text-muted-foreground h-24 w-24" />
      <div className="flex flex-col items-center gap-2 text-center">
        {text !== undefined && <p className="text-lg font-semibold">{t(text.textKey)}</p>}
        {continuesByItself && text?.additionKey !== undefined && <p className="text-sm">{t(text.additionKey)}</p>}
      </div>

      <div className="flex gap-2">
        <Button variant="outline" onClick={handleGoBack}>
          {t("serviceUnavailablePage.goBack")}
        </Button>
        <Button onClick={onRecover ?? reload}>{t("serviceUnavailablePage.retry")}</Button>
      </div>
    </ErrorPageLayout>
  );
}
