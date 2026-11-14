/**
 * Imports
 */

import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { type ArchiveUsageCount, EntityArchiveUsage } from "./EntityArchiveUsage";

/**
 * Tests
 */

const translations = {
  en: {
    translation: {
      books: {
        archive: {
          loadingUsage: "Loading usage …",
          activeUsage_BookReview_one: "One active review",
          activeUsage_BookReview_other: "{{count}} active reviews",
          archivedUsage_BookReview_one: "One archived review",
          archivedUsage_BookReview_other: "{{count}} archived reviews",
          activeUsage_BookOrder_one: "One active order",
          activeUsage_BookOrder_other: "{{count}} active orders",
          archivedUsage_BookOrder_one: "One archived order",
          archivedUsage_BookOrder_other: "{{count}} archived orders",
        },
      },
    },
  },
};

function renderEntityArchiveUsage(counts: readonly ArchiveUsageCount[] | undefined, isLoading: boolean): HTMLElement {
  const { container } = render(
    <I18nProvider languageResources={translations}>
      <EntityArchiveUsage counts={counts} isLoading={isLoading} keyPrefix="books.archive" />
    </I18nProvider>,
  );
  return container;
}

describe("EntityArchiveUsage", () => {
  it("says the usage is loading while the counts are fetched", () => {
    renderEntityArchiveUsage(undefined, true);
    expect(screen.getByText("Loading usage …")).toBeVisible();
  });

  it("drops the loading line once the counts exist", () => {
    renderEntityArchiveUsage([{ type: "BookReview", activeCount: 3, archivedCount: 2 }], true);
    expect(screen.queryByText("Loading usage …")).toBeNull();
    expect(screen.getByText("3 active reviews")).toBeVisible();
    expect(screen.getByText("2 archived reviews")).toBeVisible();
  });

  it("renders the singular for a count of one", () => {
    renderEntityArchiveUsage([{ type: "BookOrder", activeCount: 1, archivedCount: 1 }], false);
    expect(screen.getByText("One active order")).toBeVisible();
    expect(screen.getByText("One archived order")).toBeVisible();
  });

  it("renders no line for a count of 0 or null", () => {
    const container = renderEntityArchiveUsage(
      [
        { type: "BookReview", activeCount: 0, archivedCount: null },
        { type: "BookOrder", activeCount: 0, archivedCount: 0 },
      ],
      false,
    );
    expect(container).toBeEmptyDOMElement();
  });

  it("renders the lines of two types in the order of the counts", () => {
    const container = renderEntityArchiveUsage(
      [
        { type: "BookReview", activeCount: 2, archivedCount: 5 },
        { type: "BookOrder", activeCount: 4, archivedCount: 6 },
      ],
      false,
    );
    expect(Array.from(container.querySelectorAll("div")).map((line) => line.textContent)).toEqual([
      "2 active reviews",
      "5 archived reviews",
      "4 active orders",
      "6 archived orders",
    ]);
  });
});
