import { useFileSizeFormatter } from "@neckar-hub/typescript-react-i18n";
import type { UploadProgress } from "@neckar-hub/typescript-react-query";
import { cn } from "cn";
import { FileUpIcon, UploadIcon, XIcon } from "lucide-react";
import { useEffect, useRef, useState, type ChangeEventHandler, type DragEventHandler, type ReactNode, type RefObject } from "react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { toast } from "../../feedback";
import { Button } from "../../primitives/button";
import { Progress } from "../../primitives/progress";

/**
 * What [FileUploadSlot] hands to the render function of its [FileUploadSlotProps.children].
 */
export interface FileUploadSlotContext {
  /** Opens the file picker; the picked file is reported through `onUpload`. */
  openPicker: () => void;
  /** `true` while `disabled` or a removal locks picking and dropping; the caller decides which actions over the stored file follow it. */
  locked: boolean;
}

export interface FileUploadSlotProps {
  /** The file type filter of the picker, as MIME type or extension, e.g. `image/png`. */
  accept: string;
  /** The abbreviation on the file tile, e.g. `PNG`. */
  kindLabel: string;
  /** Headline of the empty slot, e.g. "Noch keine AGB hochgeladen". */
  emptyTitle: string;
  /** The hint under the pick button, e.g. "Nur PNG"; the maximum size is appended when [maximumSize] is set. */
  acceptHint: string;
  /**
   * The content while a file is stored; `null` or `undefined` shows the drop zone. A function receives the picker
   * and the lock state, for a stored state with a replace action.
   */
  children?: ReactNode | ((slot: FileUploadSlotContext) => ReactNode);
  /** Called with the picked or dropped file. */
  onUpload: (file: File) => void | Promise<void>;
  /** The largest file the server accepts, in bytes; shown as a hint, and a larger file is refused before `onUpload` is called. */
  maximumSize?: number | undefined;
  /** While `true` the slot shows the upload progress instead of its content and is locked. */
  isUploading?: boolean | undefined;
  /** The phase of the running upload, as `useFileUploadMutation` reports it; without it the progress card shows an indeterminate upload. */
  uploadProgress?: UploadProgress | undefined;
  /** Aborts the running upload; without it the progress card shows no cancel button. It offers none in `processing` either: the body is sent and the server may keep the file. */
  onCancelUpload?: (() => void) | undefined;
  /** While `true` the slot is locked: the caller removes the stored file. */
  isRemoving?: boolean | undefined;
  /** Locks picking and dropping. */
  disabled?: boolean | undefined;
  className?: string | undefined;
}

/**
 * The upload of one file of any type into a slot: a drop zone while the slot is empty, a progress card while the file
 * is transferred, and the caller's [FileUploadSlotProps.children] while a file is stored. It knows nothing about the
 * file type; [FileUploadSlotProps.accept], [FileUploadSlotProps.kindLabel] and [FileUploadSlotProps.acceptHint] name it.
 */
export function FileUploadSlot({
  accept,
  kindLabel,
  emptyTitle,
  acceptHint,
  children,
  onUpload,
  maximumSize,
  isUploading,
  uploadProgress,
  onCancelUpload,
  isRemoving,
  disabled,
  className,
}: FileUploadSlotProps): React.ReactElement {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [dragging, setDragging] = useState(false);
  const [uploadingFile, setUploadingFile] = useState<File | null>(null);
  const { t } = useEmbeddedTranslation();
  const formatFileSize = useFileSizeFormatter();

  const locked = disabled === true || isUploading === true || isRemoving === true;

  // The name and size on the progress card belong to the upload this slot started; an upload that ends forgets them.
  useEffect(() => {
    if (isUploading !== true) setUploadingFile(null);
  }, [isUploading]);

  const startUpload = (file: File) => {
    // A body over the limit is answered by the server before it is sent completely, and a browser may never show that answer.
    if (maximumSize != null && file.size > maximumSize) {
      toast.error(
        t({
          key: "fileUploadSlot.tooLarge",
          options: { fileName: file.name, size: formatFileSize(file.size), maximum: formatFileSize(maximumSize) },
        }),
      );
      return;
    }
    setUploadingFile(file);
    void onUpload(file);
  };

  const handleChange: ChangeEventHandler<HTMLInputElement> = (event) => {
    const selectedFile = event.target.files?.[0];
    // Resetting the input is what makes picking the same file twice a second upload: without it the
    // change event never fires again, and a re-upload after a failed one would do nothing.
    event.target.value = "";
    if (selectedFile == null) return;
    startUpload(selectedFile);
  };

  const handleDragOver: DragEventHandler<HTMLDivElement> = (event) => {
    // Without a cancelled dragover the browser navigates to a file dropped on a locked slot.
    event.preventDefault();
    if (locked) return;
    setDragging(true);
  };

  const handleDragLeave: DragEventHandler<HTMLDivElement> = () => setDragging(false);

  const handleDrop: DragEventHandler<HTMLDivElement> = (event) => {
    event.preventDefault();
    setDragging(false);
    if (locked) return;
    const droppedFile = event.dataTransfer.files[0];
    if (droppedFile == null) return;
    startUpload(droppedFile);
  };

  const openPicker = () => fileInputRef.current?.click();

  if (isUploading === true) {
    return (
      <UploadProgressCard
        file={uploadingFile}
        kindLabel={kindLabel}
        progress={uploadProgress}
        onCancel={onCancelUpload}
        onDragOver={handleDragOver}
        onDrop={handleDrop}
        className={className}
      />
    );
  }

  const stored = typeof children === "function" ? children({ openPicker, locked }) : children;

  if (stored == null) {
    return (
      <EmptyDropZone
        title={emptyTitle}
        acceptHint={acceptHint}
        accept={accept}
        maximumSize={maximumSize}
        dragging={dragging}
        locked={locked}
        inputRef={fileInputRef}
        onFileChange={handleChange}
        onPick={openPicker}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        className={className}
      />
    );
  }

  return (
    <div
      className={cn("rounded-lg", dragging && "outline-primary outline-2 outline-dashed", className)}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
    >
      <FileInput accept={accept} inputRef={fileInputRef} onChange={handleChange} />
      {stored}
    </div>
  );
}

interface FileInputProps {
  accept: string;
  inputRef: RefObject<HTMLInputElement | null>;
  onChange: ChangeEventHandler<HTMLInputElement>;
}

/**
 * The hidden file input behind every pick button of the slot.
 */
function FileInput({ accept, inputRef, onChange }: FileInputProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  return <input ref={inputRef} type="file" accept={accept} hidden={true} onChange={onChange} aria-label={t("fileUploadSlot.pick")} />;
}

interface EmptyDropZoneProps {
  title: string;
  acceptHint: string;
  accept: string;
  maximumSize: number | undefined;
  dragging: boolean;
  locked: boolean;
  inputRef: RefObject<HTMLInputElement | null>;
  onFileChange: ChangeEventHandler<HTMLInputElement>;
  onPick: () => void;
  onDragOver: DragEventHandler<HTMLDivElement>;
  onDragLeave: DragEventHandler<HTMLDivElement>;
  onDrop: DragEventHandler<HTMLDivElement>;
  className: string | undefined;
}

/**
 * The slot without a file: a drop zone that takes a dragged file or opens the file picker.
 */
function EmptyDropZone({
  title,
  acceptHint,
  accept,
  maximumSize,
  dragging,
  locked,
  inputRef,
  onFileChange,
  onPick,
  onDragOver,
  onDragLeave,
  onDrop,
  className,
}: EmptyDropZoneProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const formatFileSize = useFileSizeFormatter();

  return (
    <div
      className={cn(
        "bg-muted/30 flex flex-col items-center justify-center gap-2 rounded-lg border border-dashed px-6 py-10 text-center",
        dragging && "border-primary bg-primary/5",
        className,
      )}
      onDragOver={onDragOver}
      onDragLeave={onDragLeave}
      onDrop={onDrop}
    >
      <div className="bg-primary/10 text-primary mb-1 flex size-12 items-center justify-center rounded-full">
        <FileUpIcon className="size-6" />
      </div>
      <p className="font-semibold">{title}</p>
      {!locked && <p className="text-muted-foreground text-sm">{t("fileUploadSlot.dropHint")}</p>}
      <FileInput accept={accept} inputRef={inputRef} onChange={onFileChange} />
      <Button size="sm" disabled={locked} onClick={onPick} className="mt-1">
        <UploadIcon className="size-4" />
        {t("fileUploadSlot.pick")}
      </Button>
      <p className="text-muted-foreground mt-2 font-mono text-xs">
        {maximumSize == null
          ? acceptHint
          : t({ key: "fileUploadSlot.hintWithMaximum", options: { hint: acceptHint, maximum: formatFileSize(maximumSize) } })}
      </p>
    </div>
  );
}

interface UploadProgressCardProps {
  /** The file being uploaded; `null` when the upload was started elsewhere, then neither name nor size are shown. */
  file: File | null;
  kindLabel: string;
  progress: UploadProgress | undefined;
  onCancel: (() => void) | undefined;
  onDragOver: DragEventHandler<HTMLDivElement>;
  onDrop: DragEventHandler<HTMLDivElement>;
  className: string | undefined;
}

/**
 * The slot while a file is uploading. `uploading` shows a determinate bar with the transferred bytes and the percentage;
 * `processing` (the body is sent, the response is pending) and every phase without a fraction show an indeterminate bar.
 */
function UploadProgressCard({
  file,
  kindLabel,
  progress,
  onCancel,
  onDragOver,
  onDrop,
  className,
}: UploadProgressCardProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const formatFileSize = useFileSizeFormatter();

  const processing = progress?.kind === "processing";
  const fraction = progress?.kind === "uploading" ? progress.fraction : null;
  const percent = fraction == null ? null : Math.round(fraction * 100);

  return (
    <div
      className={cn("bg-muted/30 flex flex-col justify-center gap-3 rounded-lg border p-4", className)}
      onDragOver={onDragOver}
      onDrop={onDrop}
    >
      <div className="flex items-start gap-3">
        <FileKindTile kindLabel={kindLabel} />
        <div className="min-w-0 flex-1">
          {file != null && (
            <>
              <p className="truncate font-semibold">{file.name}</p>
              {fraction != null && (
                <p className="text-muted-foreground font-mono text-sm">
                  {t({
                    key: "fileUploadSlot.bytesOfTotal",
                    options: { loaded: formatFileSize(file.size * fraction), total: formatFileSize(file.size) },
                  })}
                </p>
              )}
            </>
          )}
        </div>
        {onCancel != null && !processing && (
          <Button variant="ghost" size="sm" onClick={() => onCancel()} aria-label={t("fileUploadSlot.cancelUploadLabel")}>
            <XIcon className="size-4" />
            {t("fileUploadSlot.cancelUpload")}
          </Button>
        )}
      </div>

      <Progress value={percent ?? "indeterminate"} label={processing ? t("fileUploadSlot.processing") : t("fileUploadSlot.uploading")} />
    </div>
  );
}

/**
 * The tile with the abbreviation of the file type, shown on the upload progress card and in a stored state.
 */
export function FileKindTile({ kindLabel }: { kindLabel: string }): React.ReactElement {
  return (
    <div className="bg-primary/10 text-primary flex size-11 shrink-0 items-center justify-center rounded-md text-xs font-bold">
      {kindLabel}
    </div>
  );
}
