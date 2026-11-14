/**
 * Imports
 */

import { imageDigestUrl, shortImageDigest } from "@neckar-hub/typescript-utils/deploy-metadata";
import type { PropsWithChildren } from "react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { NoValue } from "../../text/no-value";
import { KeyValueGrid } from "../key-value-grid";

/**
 * Monospace frame for deployment values.
 *
 * Children are `<dt>`/`<dd>` pairs, so a caller lists a value of its own alongside.
 */
export function DeploymentInfoBox({ children }: PropsWithChildren): React.ReactElement {
  return <KeyValueGrid className="bg-muted rounded-md border px-4 py-2 font-mono text-xs">{children}</KeyValueGrid>;
}

/**
 * One injected value: its name, and either what arrived or the statement that nothing did.
 */
export function InjectedValue({ name, value }: { name: string; value: string | undefined }): React.ReactElement {
  const { t } = useEmbeddedTranslation();

  return (
    <>
      <dt>{name}:</dt>
      <dd className="break-all">{value ?? <NoValue>{t("deploymentInfo.notInjected")}</NoValue>}</dd>
    </>
  );
}

/**
 * The short image digest, linked to the page on deployments.neckar.it that names its commit.
 */
export function InjectedImageDigest({ imageDigest }: { imageDigest: string | undefined }): React.ReactElement {
  const { t } = useEmbeddedTranslation();

  return (
    <>
      <dt>imageDigestShort:</dt>
      <dd className="break-all">
        {imageDigest === undefined ? (
          <NoValue>{t("deploymentInfo.notInjected")}</NoValue>
        ) : (
          <a className="underline" href={imageDigestUrl(imageDigest)} rel="noreferrer" target="_blank" title={imageDigest}>
            {shortImageDigest(imageDigest)}
          </a>
        )}
      </dd>
    </>
  );
}
