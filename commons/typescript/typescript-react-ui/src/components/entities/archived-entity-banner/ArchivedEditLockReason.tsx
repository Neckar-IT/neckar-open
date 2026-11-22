import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";

/**
 * Why the edit and the save action of an archived record are disabled; its `disabledReason`, beside the `ArchivedEntityBanner`.
 */
export function ArchivedEditLockReason(): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  return <>{t("archivedBanner.editLocked")}</>;
}
