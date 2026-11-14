/**
 * Imports
 */

import { Button } from "@neckar-hub/typescript-react-ui/actions";
import { clearTokenPair, getAccessToken } from "@neckar-hub/typescript-react-utils/custom-axios-instance";
import { LogOutIcon } from "lucide-react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";

/**
 * Logout Button
 *
 * Renders a logout button on error pages. Clears tokens and reloads the app.
 * Only visible when a user is logged in (access token present).
 */

export function LogoutButton() {
  const { t } = useEmbeddedTranslation();

  if (getAccessToken() === null) {
    return null;
  }

  const handleLogout = () => {
    clearTokenPair();
    window.location.replace("/");
  };

  return (
    <Button variant="destructive" onClick={handleLogout}>
      <LogOutIcon />
      {t("errorPageLogout.label")}
    </Button>
  );
}
