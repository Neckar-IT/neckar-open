/**
 * Check if the browser has a network connection (window.navigator.onLine) and show a fallback in case there is none.
 *
 * Be aware: window.navigator.online only checks for a network connection, not if this network is connected to the
 * internet.
 */

/**
 * Imports
 */

import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import { WifiOffIcon } from "lucide-react";
import type { PropsWithChildren, ReactElement } from "react";
import { useEffect, useState } from "react";
import { useEmbeddedTranslation } from "../../localization/useEmbeddedTranslation";

/**
 * Type Declarations
 */

interface OfflineFallbackProviderTextKeys {
  offline: TextKey;
  checkConnection: TextKey;
}

export type OfflineFallbackProviderProps = PropsWithChildren<{
  mockOnline?: boolean | undefined;
  textKeys?: Partial<OfflineFallbackProviderTextKeys>;
}>;

/**
 * Offline Fallback Provider Component
 */

export function OfflineFallbackProvider(props: OfflineFallbackProviderProps): ReactElement {
  const [isOnline, setIsOnline] = useState<boolean>(props.mockOnline ?? window.navigator.onLine);

  const { t } = useEmbeddedTranslation();
  const textKeysWithDefaults: OfflineFallbackProviderTextKeys = {
    offline: props.textKeys?.offline ?? "offlineFallbackProvider.offline",
    checkConnection: props.textKeys?.checkConnection ?? "offlineFallbackProvider.checkConnection",
  };

  // check for network connection
  useEffect(() => {
    if (props.mockOnline === undefined) {
      const updateOnlineStatus = () => {
        setIsOnline(window.navigator.onLine);
      };

      // listen for changes
      window.addEventListener("online", updateOnlineStatus);
      window.addEventListener("offline", updateOnlineStatus);

      // remove event listeners when component is unmounted
      return () => {
        window.removeEventListener("online", updateOnlineStatus);
        window.removeEventListener("offline", updateOnlineStatus);
      };
    } else {
      setIsOnline(props.mockOnline);
      return undefined;
    }
  }, [props.mockOnline]);

  if (!isOnline) {
    return (
      <div className="flex min-h-screen w-full items-center justify-center">
        <div className="flex flex-col items-center">
          <WifiOffIcon className="text-muted-foreground h-20 w-20" />
          <div className="mt-6 space-y-2 text-center">
            <p className="text-lg font-semibold">{t(textKeysWithDefaults.offline)}</p>
            <p className="text-muted-foreground max-w-60 text-sm">{t(textKeysWithDefaults.checkConnection)}</p>
          </div>
        </div>
      </div>
    );
  }

  return <>{props.children}</>;
}
