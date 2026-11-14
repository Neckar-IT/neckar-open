/**
 * Imports
 */

import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { Button } from "../../primitives/button";
import {
  Dialog,
  DialogBody,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "../../primitives/dialog";
import { readDeploymentInfo, type DeploymentInfo } from "./deploymentInfo";
import { DeploymentInfoContent } from "./DeploymentInfoContent";

/**
 * Prop Types
 */

export interface DeploymentInfoDialogProps {
  open?: boolean;
  onOpenChange: (open: boolean) => void;
  /**
   * What to display. Defaults to what the served HTML says about itself — pass it only to show a
   * deployment other than the running one, as the stories and the screenshot tests do.
   */
  deploymentInfo?: DeploymentInfo;
}

/**
 * Shows which commit runs on which deployment, so a screenshot answers a support question that
 * otherwise needs the browser's developer tools. What the screen has no room for reaches the
 * support request through the copy button, which carries every value the frontend knows about
 * itself and about the browser it runs in.
 *
 * Gets no visibility condition: the values stand in the served HTML anyway, and the support case is
 * exactly the one where no developer sits in front of the screen.
 *
 * @prop open - Whether the dialog is open (true by default, so it can be controlled or just rendered)
 */
export function DeploymentInfoDialog(props: DeploymentInfoDialogProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();

  return (
    <Dialog open={props.open ?? true} onOpenChange={props.onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t("deploymentInfo.title")}</DialogTitle>
          <DialogDescription>{t("deploymentInfo.description")}</DialogDescription>
        </DialogHeader>

        <DialogBody>
          <DeploymentInfoContent deploymentInfo={props.deploymentInfo ?? readDeploymentInfo()} />
        </DialogBody>

        <DialogFooter>
          <DialogClose asChild>
            <Button variant="outline">{t("deploymentInfo.close")}</Button>
          </DialogClose>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
