import { Button } from "@neckar-hub/typescript-react-ui/actions";
import type { UserDetails } from "@neckar-hub/typescript-utils/openapi-types";
import type { ReactElement } from "react";
import { stopImpersonation } from "../impersonation/impersonationStorage";
import { useEmbeddedTranslation } from "../localization/useEmbeddedTranslation";
import { formatUserName } from "./formatUserName";

/**
 * Names the user this tab acts as and leads back to the own account. Floats above the page, so no
 * product layout has to make room for it.
 */
export function ImpersonationBanner({ user }: { user: UserDetails }): ReactElement {
  const { t } = useEmbeddedTranslation();
  const name = formatUserName(user) || (user.email ?? user.loginName);

  return (
    <div
      className="bg-primary text-primary-foreground fixed bottom-4 left-1/2 z-50 flex -translate-x-1/2 items-center gap-3 rounded-full px-4 py-2 text-sm shadow-lg"
      role="status"
    >
      <span>{t({ key: "impersonation.signedInAs", options: { name } })}</span>
      <Button size="sm" variant="secondary" onClick={stopImpersonation}>
        {t("impersonation.returnToOwnAccount")}
      </Button>
    </div>
  );
}
