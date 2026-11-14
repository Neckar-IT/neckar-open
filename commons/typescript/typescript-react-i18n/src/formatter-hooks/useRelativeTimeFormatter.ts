/**
 * Imports
 */

import dayjs from "dayjs";
import "dayjs/locale/de";
import "dayjs/locale/en";
import relativeTimePlugin from "dayjs/plugin/relativeTime";
import { useLanguage } from "../utility-hooks/useLanguage";

/**
 * Configure DayJS
 */

dayjs.extend(relativeTimePlugin);

/**
 * Options
 */

interface Options {
  baseDate?: Date | undefined;
  withoutAffix?: boolean | undefined;
}

/**
 * Hook to return a relative time formatter function
 *
 * It will automatically detect the language from i18n. The locales for english (en) and german (de) are preloaded.
 * If you need any other locales, you will have to import them yourself (e.g. `import "dayjs/locale/fr"`)
 */

export function useRelativeTimeFormatter(options: Options = { baseDate: new Date(), withoutAffix: false }) {
  const language = useLanguage();

  return (date: Date) => dayjs(date).locale(language).from(options.baseDate, options.withoutAffix);
}
