import { type TextKey } from "@neckar-hub/typescript-react-i18n";
import { Button } from "@neckar-hub/typescript-react-ui/actions";
import { FullScreenCenter } from "@neckar-hub/typescript-react-ui/layout";
import { RefreshCwIcon } from "lucide-react";
import type { ReactElement } from "react";
import { useEmbeddedTranslation } from "../localization/useEmbeddedTranslation";

interface FullScreenMessageProps {
  icon: ReactElement;
  title: string;
  description: string;
  /** Secondary line below the description, e.g. a contact address. */
  hint?: ReactElement;
  action?: ReactElement;
}

/**
 * Full-screen message layout used by every auth loading and error screen.
 */
export function FullScreenMessage({ icon, title, description, hint, action }: FullScreenMessageProps): ReactElement {
  return (
    <FullScreenCenter>
      <div className="text-muted-foreground flex h-full w-full flex-col items-center justify-center gap-4">
        {icon}
        <div className="flex flex-col items-center gap-1">
          <span className="text-foreground text-base font-medium">{title}</span>
          <span className="max-w-72 text-center text-sm">{description}</span>
          {hint}
        </div>
        {action}
      </div>
    </FullScreenCenter>
  );
}

/** Icon and label belong together: a screen either takes both or neither. */
type ReloadPageButtonLabel = { icon: ReactElement; textKey: TextKey } | { icon?: never; textKey?: never };

/** Reloads the page. What that achieves differs per screen, and the user reads the label. */
export function ReloadPageButton(props: ReloadPageButtonLabel): ReactElement {
  const { t } = useEmbeddedTranslation();
  const { icon = <RefreshCwIcon className="mr-2 h-4 w-4" />, textKey = "authProvider.reloadPage" } = props;

  return (
    <Button variant="outline" size="sm" onClick={() => window.location.reload()}>
      {icon}
      {t(textKey)}
    </Button>
  );
}
