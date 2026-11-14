/**
 * Imports
 */

import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { EntityOverviewPurePage } from "./EntityOverviewPurePage";

/**
 * Tests
 */

const translations = {
  en: {
    translation: {
      authors: { title: "Authors" },
    },
  },
};

function renderEntityOverviewPurePage(): void {
  render(
    <I18nProvider languageResources={translations}>
      <EntityOverviewPurePage title="authors.title">
        <table aria-label="authors table" />
      </EntityOverviewPurePage>
    </I18nProvider>,
  );
}

describe("EntityOverviewPurePage", () => {
  it("renders the translated title as primary heading", () => {
    renderEntityOverviewPurePage();

    expect(screen.getByRole("heading", { level: 1, name: "Authors" })).toBeInTheDocument();
  });

  it("renders the children below the heading", () => {
    renderEntityOverviewPurePage();

    const heading = screen.getByRole("heading", { level: 1 });
    const table = screen.getByRole("table", { name: "authors table" });
    expect(heading.compareDocumentPosition(table) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });
});
