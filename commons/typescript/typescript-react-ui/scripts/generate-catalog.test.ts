/**
 * Tests for the low-value-description guard and the column-helper rule of the component-catalog generator.
 */

import { describe, expect, it } from "vitest";
import { commentToDescription, isColumnHelper, isLowValueDescription } from "./generate-catalog";

describe("isLowValueDescription", () => {
  it("flags a bare Radix `Root`", () => {
    expect(isLowValueDescription("Root", "Dialog")).toBe(true);
    expect(isLowValueDescription("Root", "Popover")).toBe(true);
  });

  it("flags `<Name> Root` re-export boilerplate", () => {
    expect(isLowValueDescription("Accordion Root", "Accordion")).toBe(true);
    expect(isLowValueDescription("Table Root", "Table")).toBe(true);
    expect(isLowValueDescription("HoverCard Root", "HoverCard")).toBe(true);
  });

  it("flags `<Name> Component` echoes", () => {
    expect(isLowValueDescription("Badge Component", "Badge")).toBe(true);
    expect(isLowValueDescription("Number Input Component", "NumberInput")).toBe(true);
    expect(isLowValueDescription("Select Form Field Component", "SelectFormField")).toBe(true);
  });

  it("flags any noun phrase ending in `Component`/`Root`, even when it does not echo the name", () => {
    expect(isLowValueDescription("Responsive Data Table Component", "ResponsiveTable")).toBe(true);
    expect(isLowValueDescription("Time Picker Component", "TimeInput")).toBe(true);
    expect(isLowValueDescription("SelectFromListFormField Component", "SelectIdFromListFormField")).toBe(true);
  });

  it("flags pure name echoes ignoring case and separators", () => {
    expect(isLowValueDescription("Sidebar", "Sidebar")).toBe(true);
    expect(isLowValueDescription("Toolbar", "Toolbar")).toBe(true);
    expect(isLowValueDescription("Data Container", "DataContainer")).toBe(true);
    expect(isLowValueDescription("Text Input Form Field", "TextInputFormField")).toBe(true);
  });

  it("does not flag icon descriptions that spell out the icon name", () => {
    expect(isLowValueDescription("Apartment icon", "ApartmentIcon")).toBe(false);
    expect(isLowValueDescription("Solar Panel Icon", "SolarPanelIcon")).toBe(false);
  });

  it("does not flag descriptions that add information beyond the name", () => {
    expect(isLowValueDescription("Inline alert/callout box with severity variants.", "Alert")).toBe(false);
    expect(isLowValueDescription("Image Component that renders an image or a fallback if the image could not be loaded", "Image")).toBe(
      false,
    );
    expect(isLowValueDescription("Timeline Navigation", "TimelineNav")).toBe(false);
    expect(isLowValueDescription("Duration Input Form Field Component (value is in milliseconds!)", "DurationInputFormField")).toBe(false);
  });
});

describe("isColumnHelper", () => {
  it("recognizes the column factories of a table directory", () => {
    expect(isColumnHelper("dateColumn")).toBe(true);
    expect(isColumnHelper("dateTimeColumn")).toBe(true);
    expect(isColumnHelper("selectEntityColumn")).toBe(true);
  });

  it("does not recognize components or multi-column factories", () => {
    expect(isColumnHelper("ResponsiveTable")).toBe(false);
    expect(isColumnHelper("TwoColumnFullWidthStory")).toBe(false);
    expect(isColumnHelper("articleConditionsColumns")).toBe(false);
  });
});

describe("commentToDescription", () => {
  it("renders a {@link} as code", () => {
    expect(commentToDescription("/**\n * Renders every dialog registered through {@link registerEntityDialog}.\n */")).toBe(
      "Renders every dialog registered through `registerEntityDialog`.",
    );
  });
});
