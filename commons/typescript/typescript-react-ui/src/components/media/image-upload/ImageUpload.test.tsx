/**
 * Imports
 */

import { elementAt, fireEvent, render, screen } from "@test/test-utils";
import { afterEach, beforeEach, describe, expect, it, type Mock, vi } from "vitest";
import { ImageUpload, ImageUploadInput, ImageUploadPreview } from "./ImageUpload";

/**
 * Tests
 *
 * ImageUpload is a compound component: a context provider plus an input and a preview. The preview
 * turns a selected File into an object URL and revokes it on cleanup.
 */

const file = new File(["image-bytes"], "photo.png", { type: "image/png" });

let createObjectURL: Mock<(obj: Blob | MediaSource) => string>;
let revokeObjectURL: Mock<(url: string) => void>;

beforeEach(() => {
  // jsdom does not implement the object-URL API; stub it so the preview can render and be asserted.
  createObjectURL = vi.fn<(obj: Blob | MediaSource) => string>(() => "blob:mock-url");
  revokeObjectURL = vi.fn<(url: string) => void>();
  URL.createObjectURL = createObjectURL;
  URL.revokeObjectURL = revokeObjectURL;
});

afterEach(() => {
  vi.restoreAllMocks();
});

const previewImage = (container: HTMLElement) => container.querySelector("img");

describe("ImageUpload context guard", () => {
  it("throws when ImageUploadInput is rendered outside the provider", () => {
    // The thrown error is expected; silence the React error log it produces.
    const errorSpy = vi.spyOn(console, "error").mockImplementation(() => {});

    expect(() => render(<ImageUploadInput />)).toThrow("useImageUploadContext must be used within an ImageUpload Component");

    errorSpy.mockRestore();
  });
});

describe("ImageUploadInput", () => {
  it("shows the label when no file is selected", () => {
    render(
      <ImageUpload value={undefined} onChange={vi.fn()}>
        <ImageUploadInput />
      </ImageUpload>,
    );

    expect(screen.getByText("Select File")).toBeInTheDocument();
  });

  it("shows the file name when a file is selected", () => {
    render(
      <ImageUpload value={file} onChange={vi.fn()}>
        <ImageUploadInput />
      </ImageUpload>,
    );

    expect(screen.getByText("photo.png")).toBeInTheDocument();
  });

  it("reports the chosen file through onChange", () => {
    const onChange = vi.fn();
    const { container } = render(
      <ImageUpload value={undefined} onChange={onChange}>
        <ImageUploadInput />
      </ImageUpload>,
    );

    const fileInput = container.querySelector('input[type="file"]');
    if (fileInput === null) throw new Error("file input not found");
    fireEvent.change(fileInput, { target: { files: [file] } });

    expect(onChange).toHaveBeenCalledExactlyOnceWith(file);
  });

  it("maps a cleared file input to undefined", () => {
    const onChange = vi.fn();
    render(
      <ImageUpload value={file} onChange={onChange}>
        <ImageUploadInput />
      </ImageUpload>,
    );

    // With a file present the buttons are [file button, clear]; the clear button is last.
    const buttons = screen.getAllByRole("button");
    fireEvent.click(elementAt(buttons, -1));

    expect(onChange).toHaveBeenCalledExactlyOnceWith(undefined);
  });
});

describe("ImageUploadPreview", () => {
  it("falls back to the preview URL when no file is selected", () => {
    const { container } = render(
      <ImageUpload value={undefined} onChange={vi.fn()}>
        <ImageUploadPreview preview="https://example.com/p.png" />
      </ImageUpload>,
    );

    expect(previewImage(container)).toHaveAttribute("src", "https://example.com/p.png");
  });

  it("renders the selected file as an object URL", () => {
    const { container } = render(
      <ImageUpload value={file} onChange={vi.fn()}>
        <ImageUploadPreview preview="https://example.com/p.png" />
      </ImageUpload>,
    );

    expect(createObjectURL).toHaveBeenCalledWith(file);
    // The object URL takes precedence over the preview fallback.
    expect(previewImage(container)).toHaveAttribute("src", "blob:mock-url");
  });

  it("revokes the object URL when unmounted", () => {
    const { unmount } = render(
      <ImageUpload value={file} onChange={vi.fn()}>
        <ImageUploadPreview />
      </ImageUpload>,
    );

    unmount();

    expect(revokeObjectURL).toHaveBeenCalledWith("blob:mock-url");
  });

  it("dims the preview when disabled", () => {
    const { container } = render(
      <ImageUpload value={undefined} onChange={vi.fn()} disabled>
        <ImageUploadPreview />
      </ImageUpload>,
    );

    expect(container.querySelector(".opacity-50")).toBeInTheDocument();
  });
});
