/**
 * Imports
 */

import { reactionToFailure } from "@neckar-hub/typescript-react-query";
import { Button } from "@neckar-hub/typescript-react-ui/actions";
import { CircularProgress } from "@neckar-hub/typescript-react-ui/feedback";
import { useCountdown } from "@neckar-hub/typescript-react-utils/utility-hooks";
import { PartyPopperIcon } from "lucide-react";
import { useEffect } from "react";
import { useEmbeddedTranslation } from "../../localization/useEmbeddedTranslation";
import { ErrorPageLayout } from "./components/ErrorPageLayout";

/**
 * Error Handler
 */

/**
 * The version mismatch page for a protocol version mismatch or a lazy chunk that does not load after a deploy.
 */
export function versionMismatchErrorHandler(error: unknown): React.ReactNode | null {
  return reactionToFailure(error, "query").type === "versionMismatch" ? <VersionMismatchPage /> : null;
}

/**
 * Error Page
 */

const RELOAD_INTERVAL = 60;

export function VersionMismatchPage() {
  const { t } = useEmbeddedTranslation();

  const [remainingSeconds] = useCountdown({
    countStart: RELOAD_INTERVAL,
    intervalMs: 1000,
    initiallyRunning: true,
    countStop: 0,
  });

  const handleReload = (reason: "auto-countdown" | "user-click") => {
    console.warn(`[versionMismatchErrorHandler] reloading page (${reason})`);
    window.location.reload();
  };

  useEffect(() => {
    if (remainingSeconds === 0) {
      handleReload("auto-countdown");
    }
  }, [remainingSeconds]);

  return (
    <ErrorPageLayout>
      <PartyPopperIcon className="text-primary h-24 w-24" />
      <div className="flex max-w-96 flex-col items-center gap-2 text-center">
        <p className="text-lg font-semibold">{t("versionMismatchErrorHandler.newVersionAvailable")}</p>
        <p className="text-sm">{t("versionMismatchErrorHandler.reloadApplication")}</p>
      </div>
      <Button customIconSize={true} onClick={() => handleReload("user-click")}>
        <CircularProgress className="size-5" value={(remainingSeconds / RELOAD_INTERVAL) * 100} />
        <span>{t("versionMismatchErrorHandler.reloadNow")}</span>
      </Button>
      <div className="text-muted-foreground space-y-2 text-center text-sm">
        <p>{t("versionMismatchErrorHandler.keybindings")}</p>
        <p>
          {t({
            key: "versionMismatchErrorHandler.autoRefresh",
            options: { remainingSeconds },
          })}
        </p>
      </div>
    </ErrorPageLayout>
  );
}
