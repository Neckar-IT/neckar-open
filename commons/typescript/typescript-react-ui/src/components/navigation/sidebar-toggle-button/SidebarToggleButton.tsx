import { PanelLeftCloseIcon, PanelLeftOpenIcon } from "lucide-react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { Button } from "../../primitives/button";
import { useSidebar } from "../../primitives/sidebar";

/**
 * Icon Button to toggle the sidebar between expanded and collapsed states. The button displays an appropriate icon and label based on the current state of the sidebar.
 */
export function SidebarToggleButton(): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const { state, toggleSidebar } = useSidebar();

  return (
    <Button className="h-7 w-7" size="icon" variant="ghost" onClick={toggleSidebar}>
      {state === "expanded" ? (
        <>
          <PanelLeftCloseIcon className="h-4 w-4" />
          <span className="sr-only">{t("sidebarToggleButton.collapse")}</span>
        </>
      ) : (
        <>
          <PanelLeftOpenIcon className="h-4 w-4" />
          <span className="sr-only">{t("sidebarToggleButton.expand")}</span>
        </>
      )}
    </Button>
  );
}
