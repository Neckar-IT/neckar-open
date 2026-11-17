/**
 * Imports
 */

import { fireEvent, render, screen } from "@test/test-utils";
import { describe, expect, it, vi } from "vitest";
import { FileSlot } from "./FileSlot";

/**
 * Tests
 *
 * FileSlot shows one uploaded file that is written through endpoints of its own rather than through
 * a form: the download link exists exactly while a URL is there, and picking a file calls back
 * immediately instead of holding the selection.
 */

const baseProps = {
  fileName: "data-sheet.pdf",
  accept: "application/pdf",
  onUpload: vi.fn(),
  onRemove: vi.fn(),
};

const pdf = new File(["pdf-bytes"], "picked.pdf", { type: "application/pdf" });

describe("FileSlot without a file", () => {
  it("offers uploading and neither a download nor a remove control", () => {
    render(<FileSlot {...baseProps} url={null} uploadedAt={null} />);

    expect(screen.queryByRole("link")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Upload/ })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Remove/ })).not.toBeInTheDocument();
  });
});

describe("FileSlot with a file", () => {
  it("links to the file under the name the property gives it", () => {
    render(<FileSlot {...baseProps} url="https://store.example/presigned" uploadedAt="2026-03-01T10:00:00Z" />);

    const link = screen.getByRole("link", { name: /data-sheet\.pdf/ });
    expect(link).toHaveAttribute("href", "https://store.example/presigned");
    expect(link).toHaveAttribute("download", "data-sheet.pdf");
  });

  it("offers replacing instead of uploading, plus removing", () => {
    render(<FileSlot {...baseProps} url="https://store.example/presigned" uploadedAt={null} />);

    expect(screen.getByRole("button", { name: /Replace/ })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Remove/ })).toBeInTheDocument();
  });

  it("reports the removal", () => {
    const onRemove = vi.fn();
    render(<FileSlot {...baseProps} onRemove={onRemove} url="https://store.example/presigned" uploadedAt={null} />);

    fireEvent.click(screen.getByRole("button", { name: /Remove/ }));

    expect(onRemove).toHaveBeenCalledOnce();
  });
});

describe("FileSlot file picker", () => {
  it("reports the picked file and clears the input, so the same file can be picked again", () => {
    const onUpload = vi.fn();
    render(<FileSlot {...baseProps} onUpload={onUpload} url={null} uploadedAt={null} />);

    const input = screen.getByLabelText("Choose file");
    fireEvent.change(input, { target: { files: [pdf] } });

    expect(onUpload).toHaveBeenCalledWith(pdf);
    expect((input as HTMLInputElement).value).toBe("");
  });

  it("stays silent when the picker is dismissed without a file", () => {
    const onUpload = vi.fn();
    render(<FileSlot {...baseProps} onUpload={onUpload} url={null} uploadedAt={null} />);

    fireEvent.change(screen.getByLabelText("Choose file"), { target: { files: [] } });

    expect(onUpload).not.toHaveBeenCalled();
  });
});
