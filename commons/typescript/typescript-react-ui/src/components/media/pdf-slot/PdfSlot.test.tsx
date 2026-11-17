/**
 * Imports
 */

import { fireEvent, render, screen } from "@test/test-utils";
import { describe, expect, it, vi } from "vitest";
import { PdfSlot, type PdfSlotDocument } from "./PdfSlot";

/**
 * Tests
 *
 * PdfSlot shows one uploaded PDF that is written through endpoints of its own: the details and the
 * download, replace and remove actions exist exactly while a document is there, and picking or dropping
 * a file calls back immediately instead of holding the selection.
 */

const document: PdfSlotDocument = {
  fileName: "data-sheet.pdf",
  size: 2.5 * 1024 * 1024,
  uploadedAt: "2026-03-01T10:00:00Z",
  url: "https://store.example/display",
  downloadUrl: "https://store.example/download",
  thumbnailUrl: "https://store.example/thumbnail",
  pageCount: 2,
};

const baseProps = {
  emptyTitle: "No data sheet yet",
  onUpload: vi.fn(),
  onRemove: vi.fn(),
};

const pdf = new File(["pdf-bytes"], "picked.pdf", { type: "application/pdf" });

describe("PdfSlot without a document", () => {
  it("shows the empty title, the maximum size and the picker, and no document actions", () => {
    render(<PdfSlot {...baseProps} document={null} maximumSize={10 * 1024 * 1024} />);

    expect(screen.getByText("No data sheet yet")).toBeInTheDocument();
    expect(screen.getByText(/max\. 10 MB/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Choose file/ })).toBeInTheDocument();
    expect(screen.queryByRole("link")).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Delete/ })).not.toBeInTheDocument();
  });
});

describe("PdfSlot with a document", () => {
  it("opens the PDF in a new tab from the first page image", () => {
    render(<PdfSlot {...baseProps} document={document} />);

    const link = screen.getByRole("link", { name: "Open data-sheet.pdf" });
    expect(link).toHaveAttribute("href", "https://store.example/display");
    expect(link).toHaveAttribute("target", "_blank");
    expect(link.querySelector("img")).toHaveAttribute("src", "https://store.example/thumbnail");
    expect(screen.getByText("Page 1 / 2")).toBeInTheDocument();
  });

  it("downloads the file under its name", () => {
    render(<PdfSlot {...baseProps} document={document} />);

    const link = screen.getByRole("link", { name: /Download/ });
    expect(link).toHaveAttribute("href", "https://store.example/download");
    expect(link).toHaveAttribute("download", "data-sheet.pdf");
  });

  it("lists file size and page count", () => {
    render(<PdfSlot {...baseProps} document={document} />);

    expect(screen.getByText("File size").nextElementSibling).toHaveTextContent("2.5 MB");
    expect(screen.getByText("PDF document · 2 pages")).toBeInTheDocument();
  });

  it("shows neither page badge nor page count without a page count, and a placeholder without a thumbnail", () => {
    render(<PdfSlot {...baseProps} document={{ ...document, pageCount: null, thumbnailUrl: null }} />);

    expect(screen.queryByText(/Page 1/)).not.toBeInTheDocument();
    expect(screen.getByText("PDF document")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Open data-sheet.pdf" }).querySelector("img")).toBeNull();
  });

  it("shows the fallback icon when the first page image fails to load", () => {
    render(<PdfSlot {...baseProps} document={document} />);

    fireEvent.error(screen.getByRole("link", { name: "Open data-sheet.pdf" }).querySelector("img")!);

    expect(screen.getByTestId("image-fallback-icon")).toBeInTheDocument();
  });

  it("reports the removal", () => {
    const onRemove = vi.fn();
    render(<PdfSlot {...baseProps} onRemove={onRemove} document={document} />);

    fireEvent.click(screen.getByRole("button", { name: /Delete/ }));

    expect(onRemove).toHaveBeenCalledOnce();
  });
});

describe("PdfSlot while disabled", () => {
  it("locks replace but keeps remove and download", () => {
    const onRemove = vi.fn();
    render(<PdfSlot {...baseProps} onRemove={onRemove} document={document} disabled={true} />);

    expect(screen.getByRole("button", { name: "Replace" })).toBeDisabled();
    expect(screen.getByRole("link", { name: "Download" })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Delete" }));
    expect(onRemove).toHaveBeenCalledOnce();
  });
});

describe("PdfSlot with removeDisabled", () => {
  it("locks remove and keeps replace", () => {
    render(<PdfSlot {...baseProps} document={document} removeDisabled={true} />);

    expect(screen.getByRole("button", { name: "Replace" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "Delete" })).toBeDisabled();
  });
});

describe("PdfSlot while removing", () => {
  it("locks replace and remove", () => {
    render(<PdfSlot {...baseProps} document={document} isRemoving={true} />);

    expect(screen.getByRole("button", { name: "Replace" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Delete" })).toBeDisabled();
  });
});

describe("PdfSlot file picker", () => {
  it("accepts PDF files only and reports the picked file", () => {
    const onUpload = vi.fn();
    render(<PdfSlot {...baseProps} onUpload={onUpload} document={document} />);

    const input = screen.getByLabelText("Choose file");
    fireEvent.change(input, { target: { files: [pdf] } });

    expect(input).toHaveAttribute("accept", "application/pdf");
    expect(onUpload).toHaveBeenCalledWith(pdf);
  });

  it("opens the picker from the replace button", () => {
    const click = vi.spyOn(HTMLInputElement.prototype, "click").mockImplementation(() => undefined);
    render(<PdfSlot {...baseProps} document={document} />);

    fireEvent.click(screen.getByRole("button", { name: "Replace" }));

    expect(click).toHaveBeenCalledOnce();
    click.mockRestore();
  });
});

describe("PdfSlot while uploading", () => {
  it("offers no picker, replace or delete over a stored document", () => {
    render(<PdfSlot {...baseProps} document={document} isUploading={true} uploadProgress={{ kind: "uploading", fraction: 0.5 }} />);

    expect(screen.queryByRole("button", { name: /Choose file|Replace|Delete/ })).not.toBeInTheDocument();
    expect(screen.queryByLabelText("Choose file")).not.toBeInTheDocument();
    expect(screen.getByText("PDF")).toBeInTheDocument();
  });
});
