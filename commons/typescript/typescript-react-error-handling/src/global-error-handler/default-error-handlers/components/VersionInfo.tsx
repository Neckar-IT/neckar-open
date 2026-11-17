/**
 * Imports
 */

import {
  BuildInfoValues,
  DeploymentInfoBox,
  displayedBuildInfo,
  type InjectedBuildInfo,
} from "@neckar-hub/typescript-react-ui/data-display";
import { UnknownImageDigest } from "@neckar-hub/typescript-utils/deploy-metadata";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { useVersionInfo, type VersionInfoData } from "../../VersionInfoContext";
import { CollapsibleSection } from "./CollapsibleSection";

/**
 * Prop Types
 */

interface InfoProps {
  errorCaughtBy: string;
  initiallyOpen?: boolean | undefined;
}

/**
 * Info Component
 */

export function VersionInfo(props: InfoProps) {
  const versionInfo = useVersionInfo();

  const { t } = useEmbeddedTranslation();

  return (
    <CollapsibleSection heading={t("versionInfo.heading")} initiallyOpen={props.initiallyOpen ?? true}>
      <DeploymentInfoBox>
        <dt>errorCaughtBy:</dt>
        <dd className="break-all">{props.errorCaughtBy}</dd>
        <BuildInfoValues buildInfo={displayedBuildInfo(injectedBuildInfo(versionInfo))} />
      </DeploymentInfoBox>
    </CollapsibleSection>
  );
}

/**
 * Undoes the `UnknownImageDigest` fallback of `appImageInfo()`, so a value nobody injected is
 * displayed as absent rather than as a reported `unknown`.
 */
function injectedBuildInfo(versionInfo: VersionInfoData): InjectedBuildInfo {
  return { imageDigest: versionInfo.imageDigest === UnknownImageDigest ? undefined : versionInfo.imageDigest };
}
