import { Alert, AlertDescription, AlertTitle } from "@neckar-hub/typescript-react-ui/feedback";
import { InfoIcon, ServerOffIcon, WifiOffIcon } from "lucide-react";
import { useEmbeddedTranslation } from "../localization/useEmbeddedTranslation";
import { useDeviceIsOnline, useServiceAvailability } from "./appConnectivityState";
import { connectivityNotice } from "./serviceAvailabilityText";

/**
 * Banner for an offline device or a service that is not `ready`; `degraded` shows a hint that locks no action.
 */
export function AppConnectivityMessage(): React.ReactElement | null {
  const { t } = useEmbeddedTranslation();
  const notice = connectivityNotice(useDeviceIsOnline(), useServiceAvailability());

  if (notice === undefined) {
    return null;
  }

  return (
    <div className="animate-in fade-in slide-in-from-top-2 fixed top-4 right-4 z-100">
      <Alert variant="default" className="bg-background border-amber-500 shadow-md [&>svg]:text-amber-600">
        {notice.kind === "deviceOffline" && <WifiOffIcon className="size-4" />}
        {notice.kind === "serviceUnavailable" && <ServerOffIcon className="size-4" />}
        {notice.kind === "hint" && <InfoIcon className="size-4" />}
        <AlertTitle>{t(notice.textKey)}</AlertTitle>
        {notice.kind === "deviceOffline" && (
          <AlertDescription className="text-muted-foreground text-nowrap">{t("appConnectivity.description")}</AlertDescription>
        )}
      </Alert>
    </div>
  );
}
