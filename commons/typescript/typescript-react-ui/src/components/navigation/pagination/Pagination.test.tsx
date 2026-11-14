/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { Pagination } from "./Pagination";

/**
 * Setup
 */

const props = {
  pageIndex: 1,
  pageSize: 20,
  pageCount: 5,
  onChange: vi.fn(),
};

/**
 * Tests
 */

describe("Pagination Component", () => {
  it("should render the current page and the total number of pages", () => {
    render(<Pagination {...props} />);
    expect(screen.getByText("Page 2 of 5")).toBeInTheDocument();
  });

  it("should have the first and previous page buttons disabled when the current page index is 0", () => {
    render(<Pagination {...props} pageIndex={0} />);
    expect(screen.getByTestId("first-page-button")).toBeDisabled();
    expect(screen.getByTestId("previous-page-button")).toBeDisabled();
  });

  it("should have the last and next page buttons disabled when the current page index is the last page", () => {
    render(<Pagination {...props} pageIndex={4} />);
    expect(screen.getByTestId("last-page-button")).toBeDisabled();
    expect(screen.getByTestId("next-page-button")).toBeDisabled();
  });

  it("should call onChange with pageIndex = 0 when the first page button is clicked", async () => {
    const user = userEvent.setup();
    render(<Pagination {...props} pageIndex={2} />);
    await user.click(screen.getByTestId("first-page-button"));
    expect(props.onChange).toHaveBeenCalledWith(0, props.pageSize);
  });

  it("should call onChange with pageIndex = pageIndex - 1 when the previous page button is clicked", async () => {
    const user = userEvent.setup();
    const pageIndex = 2;
    render(<Pagination {...props} pageIndex={pageIndex} />);
    await user.click(screen.getByTestId("previous-page-button"));
    expect(props.onChange).toHaveBeenCalledWith(pageIndex - 1, props.pageSize);
  });

  it("should call onChange with pageIndex = pageIndex + 1 when the next page button is clicked", async () => {
    const user = userEvent.setup();
    const pageIndex = 2;
    render(<Pagination {...props} pageIndex={pageIndex} />);
    await user.click(screen.getByTestId("next-page-button"));
    expect(props.onChange).toHaveBeenCalledWith(pageIndex + 1, props.pageSize);
  });

  it("should call onChange with pageIndex = pageCount - 1 when the last page button is clicked", async () => {
    const user = userEvent.setup();
    const pageCount = 5;
    render(<Pagination {...props} pageCount={pageCount} />);
    await user.click(screen.getByTestId("last-page-button"));
    expect(props.onChange).toHaveBeenCalledWith(pageCount - 1, props.pageSize);
  });

  it("should change the page size and keep the first item on the current page when increasing the page size", async () => {
    const user = userEvent.setup();
    render(<Pagination {...props} />);
    await user.click(screen.getByTestId("page-size-selector"));
    await user.click(screen.getByText("50"));
    expect(props.onChange).toHaveBeenCalledWith(0, 50);
  });

  it("should change the page size and keep the first item on the current page when decreasing the page size", async () => {
    const user = userEvent.setup();
    render(<Pagination {...props} />);
    await user.click(screen.getByTestId("page-size-selector"));
    await user.click(screen.getByText("10"));
    expect(props.onChange).toHaveBeenCalledWith(2, 10);
  });
});
