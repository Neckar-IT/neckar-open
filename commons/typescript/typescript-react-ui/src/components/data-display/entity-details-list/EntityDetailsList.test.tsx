/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { DescriptionListLabel, DescriptionListRow, DescriptionListValue } from "../description-list";
import { EntityDetailsList } from "./EntityDetailsList";

/**
 * Tests
 *
 * EntityDetailsList renders one label and value per row.
 */

describe("EntityDetailsList", () => {
  it("renders one row per entry, the label as term and the value as description", () => {
    render(
      <EntityDetailsList
        rows={[
          { label: "Title", value: "Pride and Prejudice" },
          { label: "Author", value: <a href="/authors/1">Jane Austen</a> },
        ]}
      />,
    );

    expect(screen.getAllByRole("term").map((term) => term.textContent)).toEqual(["Title", "Author"]);
    expect(screen.getAllByRole("definition").map((definition) => definition.textContent)).toEqual(["Pride and Prejudice", "Jane Austen"]);
    expect(screen.getByRole("link", { name: "Jane Austen" })).toHaveAttribute("href", "/authors/1");
  });

  it("renders the rows passed as children after the rows", () => {
    render(
      <EntityDetailsList rows={[{ label: "Title", value: "Emma" }]}>
        <DescriptionListRow>
          <DescriptionListLabel>Gift wrap</DescriptionListLabel>
          <DescriptionListValue>yes</DescriptionListValue>
        </DescriptionListRow>
      </EntityDetailsList>,
    );

    expect(screen.getAllByRole("term").map((term) => term.textContent)).toEqual(["Title", "Gift wrap"]);
  });
});
