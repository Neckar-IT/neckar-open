import { render, screen } from "@testing-library/react";
import { FormattedDateTime } from "./formatted-date-time";

/** The text of the value `EntityDetailsList` renders behind the label; `null` for a label without a value element. */
export function detailsValueOf(label: string): string | null {
  return screen.getByText(label).nextElementSibling?.textContent ?? null;
}

/** The text `FormattedDateTime` renders for the instant, in the language and time zone the test runs in. */
export function formattedDateTimeOf(instant: string): string {
  const { container, unmount } = render(<FormattedDateTime date={instant} />);
  const text = container.textContent ?? "";
  unmount();
  return text;
}
