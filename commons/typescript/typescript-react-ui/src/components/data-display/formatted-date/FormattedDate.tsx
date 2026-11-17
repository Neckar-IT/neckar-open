import { useDateTimeFormatter } from "@neckar-hub/typescript-react-i18n";
import { parseLocalDate } from "@neckar-hub/typescript-utils/date";

interface FormattedDateProps {
  date: Date | string | null | undefined;
}

/**
 * Renders a date in the user's locale format, as a `<span data-slot="date">`; a truncation zone never shrinks it, its column makes room.
 */
export function FormattedDate({ date }: FormattedDateProps): React.ReactNode {
  const formatDate = useDateTimeFormatter("date");
  if (date === null || date === undefined) return null;
  return <span data-slot="date">{formatDate(typeof date === "string" ? parseLocalDate(date) : date)}</span>;
}
