import {
  AdjustableValueAdjustedStringWithTypeType,
  AdjustableValueAutoStringWithTypeType,
  AdjustableValueManualStringWithTypeType,
} from "@neckar-hub/typescript-utils/openapi-types";
import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { AdjustableValueDisplay } from "./AdjustableValueDisplay";

const { Adjusted } = AdjustableValueAdjustedStringWithTypeType;
const { Auto } = AdjustableValueAutoStringWithTypeType;
const { Manual } = AdjustableValueManualStringWithTypeType;

describe("AdjustableValueDisplay", () => {
  it("renders a calculated value as plain text", () => {
    render(<AdjustableValueDisplay value={{ type: Auto, value: "John Ronald Reuel Tolkien" }} />);

    expect(screen.getByText("John Ronald Reuel Tolkien")).toBeInTheDocument();
  });

  it("renders a manual value as plain text", () => {
    render(<AdjustableValueDisplay value={{ type: Manual, value: "Anonymus" }} />);

    expect(screen.getByText("Anonymus")).toBeInTheDocument();
  });

  it("shows the override, not the calculated value", () => {
    render(<AdjustableValueDisplay value={{ type: Adjusted, calculated: "George Orwell", value: "Orwell, George" }} />);

    expect(screen.getByText("Orwell, George")).toBeInTheDocument();
    expect(screen.queryByText("George Orwell")).not.toBeInTheDocument();
  });

  it("discloses the calculated value behind an override", async () => {
    render(<AdjustableValueDisplay value={{ type: Adjusted, calculated: "George Orwell", value: "Orwell, George" }} />);

    await userEvent.click(screen.getByRole("button"));

    expect(screen.getByText("George Orwell")).toBeInTheDocument();
  });

  it("offers nothing to disclose for a calculated value", () => {
    render(<AdjustableValueDisplay value={{ type: Auto, value: "Jane Austen" }} />);

    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });

  it("offers nothing to disclose for a manual value", () => {
    // A manual value has no calculation behind it — there is nothing a popover could show.
    render(<AdjustableValueDisplay value={{ type: Manual, value: "Anonymus" }} />);

    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });

  it("renders the fallback when there is no value", () => {
    render(<AdjustableValueDisplay value={null} fallback="Kein Name" />);

    expect(screen.getByText("Kein Name")).toBeInTheDocument();
  });

  it("renders a non-string value through renderValue", () => {
    render(<AdjustableValueDisplay value={{ type: Adjusted, calculated: 1200, value: 999 }} renderValue={(amount) => `${amount} €`} />);

    expect(screen.getByText("999 €")).toBeInTheDocument();
  });
});
