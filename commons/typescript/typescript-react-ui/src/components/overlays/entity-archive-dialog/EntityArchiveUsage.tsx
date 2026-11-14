import { useTranslation } from "@neckar-hub/typescript-react-i18n";
import type { ReactElement } from "react";

/**
 * The records of one type that depend on the entity to archive.
 */
export interface ArchiveUsageCount {
  readonly type: string;
  readonly activeCount: number;
  readonly archivedCount?: number | null;
}

export interface EntityArchiveUsageProps {
  /**
   * `undefined` while the details are not loaded.
   */
  counts: readonly ArchiveUsageCount[] | undefined;
  isLoading: boolean;
  /**
   * E.g. `books.archive`; holds `loadingUsage` and, per count type, `activeUsage_<type>_one/_other` and `archivedUsage_<type>_one/_other`.
   */
  keyPrefix: string;
}

/**
 * The `usage` of an `EntityArchiveDialog`: one line per type for the active and one for the archived records that
 * depend on the entity, in the order of `counts`. A count of 0 or `null` renders no line.
 */
export function EntityArchiveUsage({ counts, isLoading, keyPrefix }: EntityArchiveUsageProps): ReactElement {
  const { t } = useTranslation();

  return (
    <>
      {isLoading && counts === undefined ? <div className="mt-2 text-sm opacity-70">{t(`${keyPrefix}.loadingUsage`)}</div> : null}
      {counts?.map((count) => (
        <ArchiveUsageLines key={count.type} count={count} keyPrefix={keyPrefix} />
      ))}
    </>
  );
}

function ArchiveUsageLines({ count, keyPrefix }: { count: ArchiveUsageCount; keyPrefix: string }): ReactElement {
  const { t } = useTranslation();
  const archivedCount = count.archivedCount ?? 0;

  return (
    <>
      {count.activeCount > 0 ? (
        <div className="mt-2 font-medium">{t(`${keyPrefix}.activeUsage`, { context: count.type, count: count.activeCount })}</div>
      ) : null}
      {archivedCount > 0 ? (
        <div className="mt-1 text-sm opacity-80">{t(`${keyPrefix}.archivedUsage`, { context: count.type, count: archivedCount })}</div>
      ) : null}
    </>
  );
}
