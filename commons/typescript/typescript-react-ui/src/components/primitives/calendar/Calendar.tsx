import { useLanguage } from "@neckar-hub/typescript-react-i18n";
import { Calendar as ShadcnCalendar } from "@neckar-hub/typescript-react-ui-primitives/ui/calendar";
import { setCalendarLocale } from "./setCalendarLocale";

/**
 * Date-picker calendar grid supporting single-date, range, and multi-select with locale-aware formatting.
 */
// Calendar in the language of the app: the primitive formats month and weekday names in English unless it receives a
// `locale`. A `locale` passed by the caller wins.
export function Calendar({ locale, ...props }: React.ComponentProps<typeof ShadcnCalendar>): React.ReactElement {
  const detectedLanguage = useLanguage();

  return <ShadcnCalendar locale={locale ?? setCalendarLocale(detectedLanguage)} {...props} />;
}
