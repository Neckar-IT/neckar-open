/**
 * Imports
 */

import { act, fireEvent, render, screen } from "@test/test-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { FilterByTextInput } from "./FilterByTextInput";

/**
 * Tests
 */

describe("FilterByTextInput", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("renders the initial value in the input", () => {
    render(<FilterByTextInput value="hello" onChange={() => {}} />);

    expect(screen.getByRole("textbox")).toHaveValue("hello");
  });

  it("renders an empty input when value is undefined", () => {
    render(<FilterByTextInput value={undefined} onChange={() => {}} />);

    expect(screen.getByRole("textbox")).toHaveValue("");
  });

  it("reflects typed text in the input immediately, before the debounce fires", () => {
    const onChange = vi.fn();
    render(<FilterByTextInput value={undefined} onChange={onChange} />);

    fireEvent.change(screen.getByRole("textbox"), { target: { value: "abc" } });

    // The displayed value updates synchronously...
    expect(screen.getByRole("textbox")).toHaveValue("abc");
    // ...while onChange is deferred by the debounce.
    expect(onChange).not.toHaveBeenCalled();
  });

  it("debounces onChange with the default delay of 300ms", () => {
    const onChange = vi.fn();
    render(<FilterByTextInput value={undefined} onChange={onChange} />);

    fireEvent.change(screen.getByRole("textbox"), { target: { value: "abc" } });

    vi.advanceTimersByTime(299);
    expect(onChange).not.toHaveBeenCalled();

    vi.advanceTimersByTime(1);
    expect(onChange).toHaveBeenCalledTimes(1);
    expect(onChange).toHaveBeenCalledWith("abc");
  });

  it("honors a custom delay", () => {
    const onChange = vi.fn();
    render(<FilterByTextInput value={undefined} onChange={onChange} delay={1000} />);

    fireEvent.change(screen.getByRole("textbox"), { target: { value: "abc" } });

    vi.advanceTimersByTime(300);
    expect(onChange).not.toHaveBeenCalled();

    vi.advanceTimersByTime(700);
    expect(onChange).toHaveBeenCalledExactlyOnceWith("abc");
  });

  it("collapses rapid keystrokes into a single trailing onChange with the latest value", () => {
    const onChange = vi.fn();
    render(<FilterByTextInput value={undefined} onChange={onChange} />);

    const input = screen.getByRole("textbox");
    fireEvent.change(input, { target: { value: "a" } });
    fireEvent.change(input, { target: { value: "ab" } });
    fireEvent.change(input, { target: { value: "abc" } });

    vi.advanceTimersByTime(300);

    expect(onChange).toHaveBeenCalledExactlyOnceWith("abc");
  });

  it("collapses keystrokes into one onChange while the caller renders a new callback in between", () => {
    const onChange = vi.fn();
    const { rerender } = render(<FilterByTextInput form="Panel" value={undefined} onChange={(value) => onChange(value)} />);
    const input = screen.getByRole("textbox");

    fireEvent.change(input, { target: { value: "a" } });
    rerender(<FilterByTextInput form="Panel" value={undefined} onChange={(value) => onChange(value)} />);
    fireEvent.change(input, { target: { value: "ab" } });
    vi.advanceTimersByTime(300);

    expect(onChange).toHaveBeenCalledExactlyOnceWith("ab");
  });

  it("takes an outside value that differs from its last delivered change", () => {
    const onChange = vi.fn();
    const { rerender } = render(<FilterByTextInput value={undefined} onChange={onChange} />);

    fireEvent.change(screen.getByRole("textbox"), { target: { value: "Weber" } });
    vi.advanceTimersByTime(300);
    rerender(<FilterByTextInput value="Weber" onChange={onChange} />);
    rerender(<FilterByTextInput value="Krämer" onChange={onChange} />);

    expect(screen.getByRole("textbox")).toHaveValue("Krämer");
  });

  it("delivers the pending text of a panel field once when it unmounts before the debounce fires", () => {
    const onChange = vi.fn();
    const { unmount } = render(<FilterByTextInput form="Panel" value={undefined} onChange={onChange} />);

    fireEvent.change(screen.getByRole("textbox"), { target: { value: "Weber" } });
    unmount();
    expect(onChange).toHaveBeenCalledExactlyOnceWith("Weber");

    vi.advanceTimersByTime(300);
    expect(onChange).toHaveBeenCalledOnce();
  });

  it("keeps typing over the echo of its own earlier change", () => {
    const onChange = vi.fn();
    const { rerender } = render(<FilterByTextInput value={undefined} onChange={onChange} />);
    const input = screen.getByRole("textbox");

    fireEvent.change(input, { target: { value: "ab" } });
    vi.advanceTimersByTime(300);
    fireEvent.change(input, { target: { value: "abc" } });
    rerender(<FilterByTextInput value="ab" onChange={onChange} />);

    expect(input).toHaveValue("abc");
    vi.advanceTimersByTime(300);
    expect(onChange).toHaveBeenLastCalledWith("abc");
  });

  it("delivers the pending text of an inline field once when its popover closes", () => {
    const onChange = vi.fn();
    render(<FilterByTextInput form="Inline" label="Customer" value={undefined} onChange={onChange} />);

    fireEvent.click(screen.getByRole("button", { name: "Customer" }));
    fireEvent.change(screen.getByRole("textbox", { name: "Customer" }), { target: { value: "Weber" } });
    fireEvent.keyDown(screen.getByRole("textbox", { name: "Customer" }), { key: "Escape" });

    expect(screen.queryByRole("textbox")).not.toBeInTheDocument();
    expect(onChange).toHaveBeenCalledExactlyOnceWith("Weber");
    vi.advanceTimersByTime(300);
    expect(onChange).toHaveBeenCalledOnce();
  });

  it("drops the pending text of the search field when its page unmounts", () => {
    const onChange = vi.fn();
    const { unmount } = render(<FilterByTextInput value={undefined} onChange={onChange} />);

    fireEvent.change(screen.getByRole("textbox"), { target: { value: "Harp" } });
    unmount();
    vi.advanceTimersByTime(300);

    expect(onChange).not.toHaveBeenCalled();
  });

  it("takes a reset from outside over a change still waiting for the debounce", () => {
    const onChange = vi.fn();
    const { rerender } = render(<FilterByTextInput value="abc" onChange={onChange} />);

    fireEvent.change(screen.getByRole("textbox"), { target: { value: "abcd" } });
    rerender(<FilterByTextInput value={undefined} onChange={onChange} />);
    act(() => {
      vi.advanceTimersByTime(300);
    });

    expect(screen.getByRole("textbox")).toHaveValue("");
    expect(onChange).not.toHaveBeenCalled();
  });

  it("does not render the clear button while the value is empty", () => {
    render(<FilterByTextInput value={undefined} onChange={() => {}} />);

    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });

  it("renders the clear button once the input has a value", () => {
    render(<FilterByTextInput value="hello" onChange={() => {}} />);

    expect(screen.getByRole("button")).toBeInTheDocument();
  });

  it("clears the input and reports undefined when the clear button is clicked", () => {
    const onChange = vi.fn();
    render(<FilterByTextInput value="hello" onChange={onChange} />);

    fireEvent.click(screen.getByRole("button"));

    // The input empties immediately and the clear button disappears.
    expect(screen.getByRole("textbox")).toHaveValue("");
    expect(screen.queryByRole("button")).not.toBeInTheDocument();

    vi.advanceTimersByTime(300);
    expect(onChange).toHaveBeenCalledExactlyOnceWith(undefined);
  });

  it("forwards remaining input props such as placeholder and disabled", () => {
    render(<FilterByTextInput value={undefined} onChange={() => {}} placeholder="Search…" disabled />);

    const input = screen.getByPlaceholderText("Search…");
    expect(input).toBeDisabled();
  });

  it("applies the className to the wrapping container", () => {
    const { container } = render(<FilterByTextInput value={undefined} onChange={() => {}} className="my-filter" />);

    expect(container.querySelector(".my-filter")).toBeInTheDocument();
  });
});
