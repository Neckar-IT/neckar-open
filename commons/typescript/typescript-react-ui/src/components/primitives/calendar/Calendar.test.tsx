import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { render, screen } from "@test/test-utils";
import { enUS } from "date-fns/locale";
import { describe, expect, it } from "vitest";
import { Calendar } from "./Calendar";

const june2024 = new Date(2024, 5, 15);
const germanOptions = { lng: "de-DE" };

describe("Calendar", () => {
  it("names the month in the language of the app", () => {
    render(
      <I18nProvider options={germanOptions}>
        <Calendar defaultMonth={june2024} />
      </I18nProvider>,
    );

    expect(screen.getByRole("grid", { name: "Juni 2024" })).toBeInTheDocument();
  });

  it("uses the locale the caller passes", () => {
    render(<Calendar defaultMonth={june2024} locale={enUS} />);

    expect(screen.getByRole("grid", { name: "June 2024" })).toBeInTheDocument();
  });
});
