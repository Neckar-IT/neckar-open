import { ConfirmationStatus } from "@neckar-hub/typescript-utils/openapi-types";
import { BadgeCheckIcon } from "lucide-react";
import { useEmbeddedTranslation } from "../../localization/useEmbeddedTranslation";
import { FilterByItems } from "../filters/filter-by-items";
import { ConfirmationStatusBadge } from "./ConfirmationStatusBadge";

interface FilterByConfirmationStatusProps {
  value: ConfirmationStatus | undefined;
  onChange: (value: ConfirmationStatus | undefined) => void;
  /** Defaults to `Inline`. */
  form?: "Inline" | "Panel";
  /** Translated; defaults to "Bestätigung". */
  label?: string | undefined;
  className?: string | undefined;
}

/**
 * Filter control for an entity's confirmation status (draft/confirmed).
 */
export function FilterByConfirmationStatus({
  className,
  value,
  onChange,
  form,
  label,
}: FilterByConfirmationStatusProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const statuses: readonly ConfirmationStatus[] = Object.values(ConfirmationStatus);

  const confirmationStatusFilterOptions = statuses.map((status) => ({
    value: status,
    label: t(`confirmationStatus.${status}`),
    popoverLabel: <ConfirmationStatusBadge status={status} variant="with-text" />,
  }));

  return (
    <FilterByItems
      mode="single"
      form={form}
      icon={BadgeCheckIcon}
      options={confirmationStatusFilterOptions}
      placeholder={label ?? t("filterBar.confirmationStatus")}
      value={value}
      onChange={(selected) => onChange(statuses.find((status) => status === selected))}
      className={className}
    />
  );
}
