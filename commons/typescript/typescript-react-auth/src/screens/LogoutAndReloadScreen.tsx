/**
 * Imports
 */

import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import { LoadingIndicator } from "@neckar-hub/typescript-react-ui/feedback";
import { FullScreenCenter } from "@neckar-hub/typescript-react-ui/layout";
import { type ReactElement, useEffect } from "react";
import { useLogoutMutation } from "../hooks/useLogoutMutation";
import { useEmbeddedTranslation } from "../localization/useEmbeddedTranslation";

/**
 * Properties
 */

interface LogoutAndReloadScreenProps {
  // The text shown next to the loading indicator. Defaults to the package's own "authProvider.loggingOut"
  textKey?: TextKey;
}

/**
 * Automatically logs out the current user - and redirects to the "/" (window.location.replace - which reloads the application)
 */
export function LogoutAndReloadScreen(props: LogoutAndReloadScreenProps): ReactElement {
  const { t } = useEmbeddedTranslation();
  const { mutateAsync } = useLogoutMutation();

  useEffect(() => {
    void (async () => {
      try {
        await mutateAsync(undefined);
        window.location.replace("/");
      } catch {
        // No redirect: a logout the server refused must not look like it worked. The query client
        // surfaces the error (`throwOnError`); caught here so the rejection is handled.
      }
    })();

    // It is not possible to clean up the effect
    // The component will be unmounted during the logout - because the login screen will be displayed, instead.
  }, [mutateAsync]);

  // Visible for as long as the logout mutation runs - until the redirect above replaces the page
  return (
    <FullScreenCenter>
      <LoadingIndicator textKey={t(props.textKey ?? "authProvider.loggingOut")} />
    </FullScreenCenter>
  );
}
