import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import {
  accessoryColumn,
  articleConditionsColumns,
  articleConditionsWithAccessoryColumns,
  articleConditionsWithInstallationTimeColumns,
  customerPriceColumn,
  fullArticleConditionsColumns,
  labourColumn,
  partColumn,
} from "./articleConditionsTimelineEntryTableColumns";
import { fullConditionsTableEntry, materialOnlyTableEntry } from "./conditionsFixtures";

describe("the column collectors", () => {
  it("follow the optional parts of the shape, with the customer price only where there is one", () => {
    expect(articleConditionsColumns().map((column) => column.id)).toEqual(["part"]);
    expect(articleConditionsWithAccessoryColumns().map((column) => column.id)).toEqual(["part", "accessory", "customerPrice"]);
    expect(articleConditionsWithInstallationTimeColumns().map((column) => column.id)).toEqual(["part", "labour", "customerPrice"]);
    expect(fullArticleConditionsColumns().map((column) => column.id)).toEqual(["part", "accessory", "labour", "customerPrice"]);
  });
});

describe("partColumn", () => {
  it("renders purchase price and surcharge rate, then the selling price per unit", () => {
    const { container } = render(<>{partColumn().cell(fullConditionsTableEntry)}</>);

    expect(container).toHaveTextContent(/^3[.,]?130[.,]45\s*€ \+40\s*% → 4[.,]?382[.,]63\s*€\s*\/ Meter$/);
    expect(container.querySelector(".font-semibold")).toHaveTextContent(/4[.,]?382[.,]63\s*€/);
    expect(container.querySelector(".text-xs")).toHaveTextContent(/^3[.,]?130[.,]45\s*€ \+40\s*%$/);
  });

  it("leaves out the rate where a purchase price of 0 € has none", () => {
    const part = { inputPrice: 0, sellingPrice: 16800, surcharge: 16800, surchargeRate: null };
    const entry = { ...materialOnlyTableEntry, priceSummary: { ...materialOnlyTableEntry.priceSummary, part } };

    const { container } = render(<>{partColumn().cell(entry)}</>);

    expect(container).toHaveTextContent(/^0[.,]00\s*€ → 168[.,]00\s*€$/);
  });

  it("rounds the rate to a whole percent", () => {
    const part = { inputPrice: 13000, sellingPrice: 17500, surcharge: 4500, surchargeRate: 0.346 };
    const entry = { ...materialOnlyTableEntry, priceSummary: { ...materialOnlyTableEntry.priceSummary, part } };

    const { container } = render(<>{partColumn().cell(entry)}</>);

    expect(container).toHaveTextContent(/\+35\s*%/);
  });

  it("has the same fixed width as the accessory column", () => {
    expect(partColumn().fixedWidth).toBeDefined();
    expect(partColumn().fixedWidth).toBe(accessoryColumn().fixedWidth);
  });

  it("names no unit for the per-piece default", () => {
    const { container } = render(<>{partColumn().cell(materialOnlyTableEntry)}</>);

    expect(container).not.toHaveTextContent("/");
  });
});

describe("accessoryColumn", () => {
  it("renders the accessory purchase price and surcharge rate, then its selling price", () => {
    const { container } = render(<>{accessoryColumn().cell(fullConditionsTableEntry)}</>);

    expect(container).toHaveTextContent(/^150[.,]00\s*€ \+40\s*% → 210[.,]00\s*€\s*\/ Meter$/);
  });

  it("renders the missing-value dash when no accessory is set", () => {
    render(<>{accessoryColumn().cell(materialOnlyTableEntry)}</>);

    expect(screen.getAllByText("-")).toHaveLength(1);
  });
});

describe("labourColumn", () => {
  it("renders installation time and hourly rate, then the labour price", () => {
    const { container } = render(<>{labourColumn().cell(fullConditionsTableEntry)}</>);

    expect(container).toHaveTextContent(/^8[.,]5\s*h × 75[.,]00\s*€\/h → 637[.,]50\s*€$/);
    expect(container.querySelector(".text-xs")).toHaveTextContent(/^8[.,]5\s*h × 75[.,]00\s*€\/h$/);
    expect(container.querySelector(".font-semibold")).toHaveTextContent(/^637[.,]50\s*€$/);
    expect(container.querySelector(".text-muted-foreground")).toHaveTextContent(/^75[.,]00\s*€\/h$/);
    expect(labourColumn().fixedWidth).toBeDefined();
  });

  it("says in its header hint that labour is priced at the current hourly rate", async () => {
    const user = userEvent.setup();
    render(<>{labourColumn().header}</>);

    await user.click(screen.getByRole("button", { name: "Show hint" }));

    expect(screen.getByText(/current one, for past and future entries alike/)).toBeInTheDocument();
  });

  it("keeps the whole line in one inline child of the marker, which a truncation zone turns into a flex container", () => {
    const { container } = render(<>{labourColumn().cell(fullConditionsTableEntry)}</>);

    expect(container.querySelector("[data-text-content]")?.children).toHaveLength(1);
  });

  it("renders a single missing-value dash for an article that is not mounted", () => {
    render(<>{labourColumn().cell(materialOnlyTableEntry)}</>);

    expect(screen.getAllByText("-")).toHaveLength(1);
  });
});

describe("customerPriceColumn", () => {
  it("has a header without a hint", () => {
    render(<>{customerPriceColumn().header}</>);

    expect(screen.getByText("Customer price")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Show hint" })).not.toBeInTheDocument();
  });

  it("renders the customer price the server calculated", () => {
    const { container } = render(<>{customerPriceColumn().cell(fullConditionsTableEntry)}</>);

    expect(container).toHaveTextContent(/5[.,]?230[.,]13\s*€/);
  });

  it("renders the missing-value dash where the server has no customer price", () => {
    const entry = { ...fullConditionsTableEntry, priceSummary: { ...fullConditionsTableEntry.priceSummary, customerPrice: null } };

    render(<>{customerPriceColumn().cell(entry)}</>);

    expect(screen.getByText("-")).toBeInTheDocument();
  });
});
