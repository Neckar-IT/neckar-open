import { useDateTimeFormatter } from "@neckar-hub/typescript-react-i18n";

interface ValidFromDateProps {
  isoInstant: string;
  className?: string;
}

/**
 * Renders a timeline `validFrom` instant as a date without time (e.g. `01.07.2026`).
 *
 * A timeline entry is effective from a calendar day; the time component is noise for scanning.
 * When the instant is not local midnight, the full date-and-time is preserved in a `title`
 * tooltip so the stored value is never silently hidden.
 */
export function ValidFromDate({ isoInstant, className }: ValidFromDateProps): React.ReactElement {
  const formatDate = useDateTimeFormatter("date");
  const formatDateAndTime = useDateTimeFormatter("date-and-time");

  const date = new Date(isoInstant);
  const isLocalMidnight = date.getHours() === 0 && date.getMinutes() === 0 && date.getSeconds() === 0 && date.getMilliseconds() === 0;

  return (
    <span data-text-content className={className} title={isLocalMidnight ? undefined : formatDateAndTime(date)}>
      {formatDate(date)}
    </span>
  );
}
