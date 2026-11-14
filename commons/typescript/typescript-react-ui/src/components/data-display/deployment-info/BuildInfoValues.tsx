/**
 * Imports
 */

import type { InjectedBuildInfo } from "./deploymentInfo";
import { InjectedImageDigest, InjectedValue } from "./DeploymentInfoBox";

/**
 * The build value of the running frontend, as rows for a `DeploymentInfoBox`.
 */
export function BuildInfoValues({ buildInfo }: { buildInfo: InjectedBuildInfo }): React.ReactElement {
  return (
    <>
      <InjectedValue name="imageDigest" value={buildInfo.imageDigest} />
      <InjectedImageDigest imageDigest={buildInfo.imageDigest} />
    </>
  );
}
