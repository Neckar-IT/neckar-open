/**
 * Imports
 */

import { fireEvent, render, screen, waitFor, within } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { toast } from "../../feedback/toast";
import { ImageSlot, type ImageSlotProps } from "./ImageSlot";

/**
 * Tests
 *
 * ImageSlot shows one image that is written through endpoints of its own: the actions replace and delete
 * exist exactly while an image is there, a refused file never reaches `onUpload`, and deleting hands over to the caller.
 */

const toastErrorMock = vi.fn();
beforeEach(() => {
  toastErrorMock.mockReset();
  vi.spyOn(toast, "error").mockImplementation(toastErrorMock);
});

const baseProps: ImageSlotProps = {
  imageUrl: undefined,
  alt: "Portrait",
  maximumSize: 1024 * 1024,
  accept: ["image/jpeg", "image/png"],
  onUpload: vi.fn(() => Promise.resolve()),
  onRemove: vi.fn(),
};

/** Opens the dropdown with replace and delete at the top right of the image. */
async function openActions(): Promise<void> {
  await userEvent.setup().click(screen.getByRole("button", { name: "Image actions" }));
}

const png = new File(["png-bytes"], "picked.png", { type: "image/png" });

describe("ImageSlot without an image", () => {
  it("shows the placeholder with the upload action, the types and the maximum size, and no image actions", () => {
    render(<ImageSlot {...baseProps} />);

    expect(screen.getByText("No image yet")).toBeInTheDocument();
    expect(screen.getByText(/JPEG, PNG · max\. 1 MB/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Choose file/ })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Image actions" })).not.toBeInTheDocument();
  });
});

describe("ImageSlot with an image", () => {
  it("shows the whole image in the slot and offers replace and delete in the dropdown", async () => {
    render(<ImageSlot {...baseProps} imageUrl="https://store.example/portrait" />);

    expect(screen.getByAltText("Portrait")).toHaveAttribute("src", "https://store.example/portrait");
    expect(screen.getByAltText("Portrait")).toHaveClass("object-contain");
    expect(screen.queryByRole("menuitem")).not.toBeInTheDocument();

    await openActions();

    expect(screen.getByRole("menuitem", { name: "Replace" })).not.toHaveAttribute("aria-disabled", "true");
    expect(screen.getByRole("menuitem", { name: "Delete" })).not.toHaveAttribute("aria-disabled", "true");
  });

  it("shows the preview and opens the full-size image in an overlay on a click", async () => {
    render(<ImageSlot {...baseProps} imageUrl="https://store.example/full" thumbnailUrl="https://store.example/preview" />);

    expect(screen.getByAltText("Portrait")).toHaveAttribute("src", "https://store.example/preview");
    expect(screen.getByRole("button", { name: "Open Portrait in full size" })).toHaveClass("cursor-pointer");
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();

    await userEvent.setup().click(screen.getByRole("button", { name: "Open Portrait in full size" }));

    const overlay = screen.getByRole("dialog");
    expect(within(overlay).getByAltText("Portrait")).toHaveAttribute("src", "https://store.example/full");
  });

  it("opens the overlay while disabled", async () => {
    render(<ImageSlot {...baseProps} imageUrl="https://store.example/full" disabled={true} />);

    await userEvent.setup().click(screen.getByRole("button", { name: "Open Portrait in full size" }));

    expect(screen.getByRole("dialog")).toBeInTheDocument();
  });

  it("closes the overlay on a click on the image", async () => {
    render(<ImageSlot {...baseProps} imageUrl="https://store.example/full" thumbnailUrl="https://store.example/preview" />);

    await userEvent.setup().click(screen.getByRole("button", { name: "Open Portrait in full size" }));
    await userEvent.setup().click(within(screen.getByRole("dialog")).getByAltText("Portrait"));

    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
  });

  it("shows the full-size image in the slot without a preview", () => {
    render(<ImageSlot {...baseProps} imageUrl="https://store.example/full" />);

    expect(screen.getByAltText("Portrait")).toHaveAttribute("src", "https://store.example/full");
  });

  it("fills the size the caller gives it", () => {
    const { container } = render(<ImageSlot {...baseProps} imageUrl="https://store.example/portrait" className="size-64" />);

    expect(container.querySelector(".size-64")).toContainElement(screen.getByAltText("Portrait"));
  });

  it("opens the picker from the replace item", async () => {
    const click = vi.spyOn(HTMLInputElement.prototype, "click").mockImplementation(() => undefined);
    render(<ImageSlot {...baseProps} imageUrl="https://store.example/portrait" />);

    await openActions();
    await userEvent.setup().click(screen.getByRole("menuitem", { name: "Replace" }));

    expect(click).toHaveBeenCalledOnce();
    click.mockRestore();
  });

  it("locks replace and keeps delete while disabled", async () => {
    const onRemove = vi.fn();
    render(<ImageSlot {...baseProps} imageUrl="https://store.example/portrait" onRemove={onRemove} disabled={true} />);

    await openActions();

    expect(screen.getByRole("menuitem", { name: "Replace" })).toHaveAttribute("aria-disabled", "true");
    expect(screen.getByRole("menuitem", { name: "Delete" })).not.toHaveAttribute("aria-disabled", "true");
    await userEvent.setup().click(screen.getByRole("menuitem", { name: "Delete" }));
    expect(onRemove).toHaveBeenCalledOnce();
  });

  it("locks delete and keeps replace with removeDisabled", async () => {
    const onRemove = vi.fn();
    render(<ImageSlot {...baseProps} imageUrl="https://store.example/portrait" onRemove={onRemove} removeDisabled={true} />);

    await openActions();

    expect(screen.getByRole("menuitem", { name: "Replace" })).not.toHaveAttribute("aria-disabled", "true");
    expect(screen.getByRole("menuitem", { name: "Delete" })).toHaveAttribute("aria-disabled", "true");
    await userEvent.setup().click(screen.getByRole("menuitem", { name: "Delete" }));
    expect(onRemove).not.toHaveBeenCalled();
  });
});

describe("ImageSlot upload", () => {
  it("reports an accepted file", async () => {
    const onUpload = vi.fn(() => Promise.resolve());
    render(<ImageSlot {...baseProps} onUpload={onUpload} />);

    fireEvent.change(screen.getByLabelText("Choose file"), { target: { files: [png] } });

    await waitFor(() => expect(onUpload).toHaveBeenCalledWith(png));
    expect(toastErrorMock).not.toHaveBeenCalled();
  });

  it("shows the progress and locks the slot while the caller uploads", () => {
    render(
      <ImageSlot
        {...baseProps}
        imageUrl="https://store.example/portrait"
        isUploading={true}
        uploadProgress={{ kind: "uploading", fraction: 0.5 }}
      />,
    );

    expect(screen.getByText("50 %")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Image actions" })).not.toBeInTheDocument();
  });

  it("cancels the upload from the progress card", async () => {
    const onCancelUpload = vi.fn();
    render(
      <ImageSlot {...baseProps} isUploading={true} uploadProgress={{ kind: "uploading", fraction: 0.5 }} onCancelUpload={onCancelUpload} />,
    );

    await userEvent.setup().click(screen.getByRole("button", { name: "Cancel upload" }));

    expect(onCancelUpload).toHaveBeenCalledOnce();
  });

  it("refuses a file of another type with an error toast and without calling onUpload", () => {
    const onUpload = vi.fn(() => Promise.resolve());
    render(<ImageSlot {...baseProps} onUpload={onUpload} />);

    fireEvent.change(screen.getByLabelText("Choose file"), {
      target: { files: [new File(["x"], "doc.pdf", { type: "application/pdf" })] },
    });

    expect(toastErrorMock).toHaveBeenCalledExactlyOnceWith("doc.pdf is not an accepted type; allowed are JPEG, PNG.");
    expect(onUpload).not.toHaveBeenCalled();
  });

  it("refuses a file over the maximum size with an error toast and without calling onUpload", () => {
    const onUpload = vi.fn(() => Promise.resolve());
    render(<ImageSlot {...baseProps} onUpload={onUpload} />);
    const large = new File([new Uint8Array(1024 * 1024 + 1)], "large.png", { type: "image/png" });

    fireEvent.change(screen.getByLabelText("Choose file"), { target: { files: [large] } });

    expect(toastErrorMock).toHaveBeenCalledOnce();
    expect(toastErrorMock.mock.calls[0]?.[0]).toContain("large.png");
    expect(onUpload).not.toHaveBeenCalled();
  });
});

describe("ImageSlot delete", () => {
  it("calls onRemove once from the delete item and opens no dialog of its own", async () => {
    const onRemove = vi.fn();
    render(<ImageSlot {...baseProps} imageUrl="https://store.example/portrait" onRemove={onRemove} />);

    await openActions();
    await userEvent.setup().click(screen.getByRole("menuitem", { name: "Delete" }));

    expect(onRemove).toHaveBeenCalledOnce();
    expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument();
  });

  it("locks replace and delete while the removal runs", async () => {
    const onRemove = vi.fn();
    render(<ImageSlot {...baseProps} imageUrl="https://store.example/portrait" onRemove={onRemove} isRemoving={true} />);

    await openActions();

    expect(screen.getByRole("menuitem", { name: "Replace" })).toHaveAttribute("aria-disabled", "true");
    expect(screen.getByRole("menuitem", { name: "Delete" })).toHaveAttribute("aria-disabled", "true");
    await userEvent.setup().click(screen.getByRole("menuitem", { name: "Delete" }));
    expect(onRemove).not.toHaveBeenCalled();
  });
});
