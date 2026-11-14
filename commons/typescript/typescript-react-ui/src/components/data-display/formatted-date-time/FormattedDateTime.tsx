import { useDateTimeFormatter } from "@neckar-hub/typescript-react-i18n";

interface FormattedDateTimeProps {
  date: Date | string | null | undefined;
}

/**
 * Renders a date and time in the user's locale format, as a `<span>` like `FormattedDate`; a truncation zone never shrinks it.
 */
export function FormattedDateTime({ date }: FormattedDateTimeProps): React.ReactNode {
  const formatDateTime = useDateTimeFormatter("date-and-time");
  if (date === null || date === undefined) return null;
  return <span data-slot="date">{formatDateTime(typeof date === "string" ? new Date(date) : date)}</span>;
}
