/**
 * Imports
 */

import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { CopyTextToClipboardButton } from "../../actions/copy-text-to-clipboard-button";
import type { DeploymentInfo } from "./deploymentInfo";
import { DeploymentInfoBox, InjectedImageDigest, InjectedValue } from "./DeploymentInfoBox";
import { deploymentInfoAsJson } from "./deploymentInfoJson";

/**
 * The two values a support answer is about, and the button that carries all of them — the ones on
 * screen and the ones the screen has no room for.
 *
 * Takes its values as a prop and reads nothing itself, so a story and a screenshot test can show a
 * partly injected deployment without a running container.
 */
export function DeploymentInfoContent({ deploymentInfo }: { deploymentInfo: DeploymentInfo }): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const { runtimeContext, build } = deploymentInfo;

  // what the application concluded where the injected stage does not decide it — an unexpectedly
  // locked or unlocked developer UI is otherwise unexplainable from this display
  const assumedStage =
    runtimeContext.deploymentStage === runtimeContext.effectiveDeploymentStage
      ? undefined
      : t({ key: "deploymentInfo.assumedStage", options: { stage: runtimeContext.effectiveDeploymentStage } });

  return (
    <section className="flex flex-col gap-1">
      {/* the button sits in the box's corner rather than inside it: the box is a `<dl>` and takes dt/dd pairs only */}
      <div className="relative">
        <DeploymentInfoBox>
          <InjectedValue name="deploymentStage" value={runtimeContext.deploymentStage} />
          <InjectedImageDigest imageDigest={build.imageDigest} />
        </DeploymentInfoBox>

        <CopyTextToClipboardButton
          aria-label={t("deploymentInfo.copy")}
          className="absolute top-1 right-1"
          size="sm"
          text={deploymentInfoAsJson(deploymentInfo)}
          title={t("deploymentInfo.copy")}
          variant="ghost"
        />
      </div>

      {assumedStage === undefined ? null : <p className="text-muted-foreground text-xs">{assumedStage}</p>}
    </section>
  );
}
