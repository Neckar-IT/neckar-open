/**
 * Imports
 */

import type { UploadProgress } from "@neckar-hub/typescript-react-query";
import { fireEvent, render, screen } from "@test/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { toast } from "../../feedback/toast";
import { FileUploadSlot, type FileUploadSlotProps } from "./FileUploadSlot";

/**
 * Tests
 *
 * FileUploadSlot takes any file type: the drop zone, the picker, the lock and the progress card exist for
 * the type its props name, and picking or dropping a file calls back immediately.
 */

const toastErrorMock = vi.fn();
beforeEach(() => {
  toastErrorMock.mockReset();
  vi.spyOn(toast, "error").mockImplementation(toastErrorMock);
});

const baseProps: FileUploadSlotProps = {
  accept: "image/png",
  kindLabel: "BILD",
  emptyTitle: "No picture yet",
  acceptHint: "PNG only",
  onUpload: vi.fn(),
};

const png = new File(["png-bytes"], "picked.png", { type: "image/png" });

describe("FileUploadSlot without content", () => {
  it("shows the empty title, the hint with the maximum size and the picker", () => {
    render(<FileUploadSlot {...baseProps} maximumSize={10 * 1024 * 1024} />);

    expect(screen.getByText("No picture yet")).toBeInTheDocument();
    expect(screen.getByText("PNG only · max. 10 MB")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Choose file/ })).toBeInTheDocument();
  });

  it("shows the hint alone without a maximum size", () => {
    render(<FileUploadSlot {...baseProps} />);

    expect(screen.getByText("PNG only")).toBeInTheDocument();
  });

  it("filters the picker by the accept prop", () => {
    render(<FileUploadSlot {...baseProps} />);

    expect(screen.getByLabelText("Choose file")).toHaveAttribute("accept", "image/png");
  });

  it("reports a dropped file", () => {
    const onUpload = vi.fn();
    const { container } = render(<FileUploadSlot {...baseProps} onUpload={onUpload} />);

    fireEvent.drop(container.firstElementChild as Element, { dataTransfer: { files: [png] } });

    expect(onUpload).toHaveBeenCalledWith(png);
  });

  it.each([
    ["disabled", { disabled: true }],
    ["removing", { isRemoving: true }],
  ])("cancels the dragover of a slot that is %s and ignores the drop", (_name, props) => {
    const onUpload = vi.fn();
    const { container } = render(<FileUploadSlot {...baseProps} {...props} onUpload={onUpload} />);

    const notCancelled = fireEvent.dragOver(container.firstElementChild as Element, { dataTransfer: { files: [png] } });
    fireEvent.drop(container.firstElementChild as Element, { dataTransfer: { files: [png] } });

    expect(notCancelled).toBe(false);
    expect(onUpload).not.toHaveBeenCalled();
    expect(screen.getByRole("button", { name: /Choose file/ })).toBeDisabled();
  });

  it("offers dragging a file only while the slot is unlocked", () => {
    const { rerender } = render(<FileUploadSlot {...baseProps} />);
    expect(screen.getByText(/Drag a file here/)).toBeInTheDocument();

    rerender(<FileUploadSlot {...baseProps} disabled={true} />);
    expect(screen.queryByText(/Drag a file here/)).not.toBeInTheDocument();
  });
});

describe("FileUploadSlot with a maximum size", () => {
  const tenBytes = new File(["0123456789"], "ten.png", { type: "image/png" });

  beforeEach(() => {
    toastErrorMock.mockClear();
  });

  it("refuses a dropped file over the maximum with an error toast and without calling onUpload", () => {
    const onUpload = vi.fn();
    const { container } = render(<FileUploadSlot {...baseProps} maximumSize={9} onUpload={onUpload} />);

    fireEvent.drop(container.firstElementChild as Element, { dataTransfer: { files: [tenBytes] } });

    expect(onUpload).not.toHaveBeenCalled();
    expect(toastErrorMock).toHaveBeenCalledExactlyOnceWith("ten.png is 10 B; the maximum is 9 B.");
  });

  it("refuses a picked file over the maximum", () => {
    const onUpload = vi.fn();
    render(<FileUploadSlot {...baseProps} maximumSize={9} onUpload={onUpload} />);

    fireEvent.change(screen.getByLabelText("Choose file"), { target: { files: [tenBytes] } });

    expect(onUpload).not.toHaveBeenCalled();
    expect(toastErrorMock).toHaveBeenCalledTimes(1);
  });

  it("uploads a file of exactly the maximum without a toast", () => {
    const onUpload = vi.fn();
    const { container } = render(<FileUploadSlot {...baseProps} maximumSize={10} onUpload={onUpload} />);

    fireEvent.drop(container.firstElementChild as Element, { dataTransfer: { files: [tenBytes] } });

    expect(onUpload).toHaveBeenCalledWith(tenBytes);
    expect(toastErrorMock).not.toHaveBeenCalled();
  });

  it("refuses a file over the maximum while a file is stored", () => {
    const onUpload = vi.fn();
    const { container } = render(
      <FileUploadSlot {...baseProps} maximumSize={9} onUpload={onUpload}>
        stored picture
      </FileUploadSlot>,
    );

    fireEvent.drop(container.firstElementChild as Element, { dataTransfer: { files: [tenBytes] } });

    expect(onUpload).not.toHaveBeenCalled();
    expect(toastErrorMock).toHaveBeenCalledTimes(1);
  });
});

describe("FileUploadSlot with content", () => {
  it("shows the children instead of the drop zone, with a picker that stays hidden", () => {
    render(<FileUploadSlot {...baseProps}>stored picture</FileUploadSlot>);

    expect(screen.getByText("stored picture")).toBeInTheDocument();
    expect(screen.queryByText("No picture yet")).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Choose file/ })).not.toBeInTheDocument();
  });

  it("hands the lock to a render function", () => {
    render(
      <FileUploadSlot {...baseProps} disabled={true}>
        {({ openPicker, locked }) => (
          <button type="button" disabled={locked} onClick={openPicker}>
            Replace picture
          </button>
        )}
      </FileUploadSlot>,
    );

    expect(screen.getByRole("button", { name: "Replace picture" })).toBeDisabled();
  });

  it("opens the picker from the render function", () => {
    const click = vi.spyOn(HTMLInputElement.prototype, "click").mockImplementation(() => undefined);
    render(<FileUploadSlot {...baseProps}>{({ openPicker }) => <button onClick={openPicker}>Replace picture</button>}</FileUploadSlot>);

    fireEvent.click(screen.getByRole("button", { name: "Replace picture" }));

    expect(click).toHaveBeenCalledOnce();
    click.mockRestore();
  });

  it("reports a file dropped on the stored state", () => {
    const onUpload = vi.fn();
    const { container } = render(
      <FileUploadSlot {...baseProps} onUpload={onUpload}>
        stored picture
      </FileUploadSlot>,
    );

    fireEvent.drop(container.firstElementChild as Element, { dataTransfer: { files: [png] } });

    expect(onUpload).toHaveBeenCalledWith(png);
  });
});

describe("FileUploadSlot file picker", () => {
  it("reports the picked file and clears the input, so the same file can be picked again", () => {
    const onUpload = vi.fn();
    render(<FileUploadSlot {...baseProps} onUpload={onUpload} />);

    const input = screen.getByLabelText("Choose file");
    fireEvent.change(input, { target: { files: [png] } });

    expect(onUpload).toHaveBeenCalledWith(png);
    expect((input as HTMLInputElement).value).toBe("");
  });

  it("stays silent when the picker is dismissed without a file", () => {
    const onUpload = vi.fn();
    render(<FileUploadSlot {...baseProps} onUpload={onUpload} />);

    fireEvent.change(screen.getByLabelText("Choose file"), { target: { files: [] } });

    expect(onUpload).not.toHaveBeenCalled();
  });
});

describe("FileUploadSlot while uploading", () => {
  const bigPng = new File([new Uint8Array(2 * 1024 * 1024)], "Foto.png", { type: "image/png" });

  /** The progress card shows the name of the file `onUpload` was called with, so the file is picked first. */
  function renderUploading(uploadProgress: UploadProgress, onCancelUpload?: () => void, content?: string) {
    const props = { ...baseProps, onCancelUpload };
    const view = render(<FileUploadSlot {...props}>{content}</FileUploadSlot>);
    fireEvent.change(screen.getByLabelText("Choose file"), { target: { files: [bigPng] } });
    view.rerender(
      <FileUploadSlot {...props} isUploading={true} uploadProgress={uploadProgress}>
        {content}
      </FileUploadSlot>,
    );
    return view;
  }

  it("shows name, kind tile, transferred bytes, percentage and the determinate bar while transferring", () => {
    renderUploading({ kind: "uploading", fraction: 0.5 });

    expect(screen.getByText("Foto.png")).toBeInTheDocument();
    expect(screen.getByText("BILD")).toBeInTheDocument();
    expect(screen.getByText("1 MB of 2 MB")).toBeInTheDocument();
    expect(screen.getByText("50 %")).toBeInTheDocument();
    expect(screen.getByRole("progressbar", { name: "Uploading file" })).toHaveAttribute("aria-valuenow", "50");
  });

  it("shows an indeterminate bar without bytes and percentage while processing", () => {
    renderUploading({ kind: "processing" });

    expect(screen.getByText("Processing file")).toBeInTheDocument();
    expect(screen.queryByText(/ of /)).not.toBeInTheDocument();
    expect(screen.queryByText(/%/)).not.toBeInTheDocument();
    expect(screen.getByRole("progressbar")).not.toHaveAttribute("aria-valuenow");
  });

  it("shows an indeterminate bar while the progress is unknown", () => {
    renderUploading({ kind: "indeterminate" });

    expect(screen.getByText("Uploading file")).toBeInTheDocument();
    expect(screen.getByRole("progressbar")).not.toHaveAttribute("aria-valuenow");
  });

  it("calls onCancelUpload from the cancel button", () => {
    const onCancelUpload = vi.fn();
    renderUploading({ kind: "uploading", fraction: 0.5 }, onCancelUpload);

    fireEvent.click(screen.getByRole("button", { name: "Cancel upload" }));

    expect(onCancelUpload).toHaveBeenCalledOnce();
  });

  it("shows no cancel button while processing, because the server may keep the file", () => {
    renderUploading({ kind: "processing" }, vi.fn());

    expect(screen.queryByRole("button", { name: "Cancel upload" })).not.toBeInTheDocument();
  });

  it("shows no cancel button without onCancelUpload", () => {
    renderUploading({ kind: "uploading", fraction: 0.5 });

    expect(screen.queryByRole("button", { name: "Cancel upload" })).not.toBeInTheDocument();
  });

  it.each<UploadProgress>([{ kind: "uploading", fraction: 0.5 }, { kind: "processing" }])(
    "offers neither picker nor stored content in phase %j",
    (progress) => {
      renderUploading(progress, undefined, "stored picture");

      expect(screen.queryByText("stored picture")).not.toBeInTheDocument();
      expect(screen.queryByLabelText("Choose file")).not.toBeInTheDocument();
    },
  );

  it("cancels the dragover of the progress card and ignores a drop", () => {
    const onUpload = vi.fn();
    const { container } = render(<FileUploadSlot {...baseProps} onUpload={onUpload} isUploading={true} />);

    const notCancelled = fireEvent.dragOver(container.firstElementChild as Element, { dataTransfer: { files: [png] } });
    fireEvent.drop(container.firstElementChild as Element, { dataTransfer: { files: [png] } });

    expect(notCancelled).toBe(false);
    expect(onUpload).not.toHaveBeenCalled();
  });

  it("forgets the file name after the upload ends", () => {
    const view = renderUploading({ kind: "uploading", fraction: 0.5 });
    view.rerender(<FileUploadSlot {...baseProps} />);
    view.rerender(<FileUploadSlot {...baseProps} isUploading={true} uploadProgress={{ kind: "uploading", fraction: 0.5 }} />);

    expect(screen.queryByText("Foto.png")).not.toBeInTheDocument();
  });
});
