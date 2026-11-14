import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";

/**
 * Why the edit action of an archived record is disabled; its `disabledReason`, beside the `ArchivedEntityBanner`.
 */
export function ArchivedEditLockReason(): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  return <>{t("archivedBanner.editLocked")}</>;
}
