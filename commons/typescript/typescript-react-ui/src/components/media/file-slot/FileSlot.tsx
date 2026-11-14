import { cn } from "cn";
import { DownloadIcon, Trash2Icon, UploadIcon } from "lucide-react";
import { useRef, type ChangeEventHandler } from "react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { FormattedDateTime } from "../../data-display/formatted-date-time";
import { Button } from "../../primitives/button";
import { NoValue } from "../../text/no-value";

/**
 * Prop Types for the File Slot component
 */
export interface FileSlotProps {
  /**
   * Where the file can be downloaded — a presigned URL resolved per request. `null` exactly when no
   * file was ever uploaded.
   */
  url: string | null | undefined;
  /**
   * When the file was uploaded, `null` when none ever was. Rendered next to the download link so a
   * stale document is recognizable as one.
   */
  uploadedAt: string | null | undefined;
  /**
   * The name the download is offered under. Derived from the property, not from the file the user
   * picked: the store keeps the bytes under an address of its own and never the original name.
   */
  fileName: string;
  /**
   * What the file picker accepts, as a MIME type (`application/pdf`).
   */
  accept: string;
  /**
   * Called with the picked file. Uploading replaces whatever is in the slot — there is exactly one
   * file per slot.
   */
  onUpload: (file: File) => void | Promise<void>;
  /**
   * Called when the user removes the file. Only reachable while [url] is set.
   */
  onRemove: () => void | Promise<void>;
  isUploading?: boolean | undefined;
  isRemoving?: boolean | undefined;
  disabled?: boolean | undefined;
  className?: string | undefined;
}

/**
 * One uploaded file that belongs to an entity but is not one of its form fields: it is written
 * through endpoints of its own (upload, remove) rather than through the entity's upsert, so it
 * never appears in a form schema. Shows the download link when a file is there and the picker
 * either way.
 */
export function FileSlot({
  url,
  uploadedAt,
  fileName,
  accept,
  onUpload,
  onRemove,
  isUploading,
  isRemoving,
  disabled,
  className,
}: FileSlotProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const fileInputRef = useRef<HTMLInputElement>(null);

  // An empty string reaches this component the same way `null` does: a presigned URL that resolved
  // to nothing. Both mean the slot is empty.
  const hasFile = url != null && url !== "";
  const busy = isUploading === true || isRemoving === true;

  const handleChange: ChangeEventHandler<HTMLInputElement> = (event) => {
    const selectedFile = event.target.files?.[0];
    // Resetting the input is what makes picking the same file twice a second upload: without it the
    // change event never fires again, and a re-upload after a failed one would do nothing.
    event.target.value = "";
    if (selectedFile == null) return;
    void onUpload(selectedFile);
  };

  return (
    <div className={cn("flex flex-wrap items-center gap-3", className)}>
      {hasFile ? (
        <>
          <a
            href={url}
            download={fileName}
            target="_blank"
            rel="noreferrer"
            className="text-primary inline-flex items-center gap-1 hover:underline"
          >
            <DownloadIcon className="size-4" />
            {fileName}
          </a>
          {uploadedAt != null ? (
            <span className="text-muted-foreground text-sm">
              {t("fileSlot.uploadedAt")} <FormattedDateTime date={uploadedAt} />
            </span>
          ) : null}
        </>
      ) : (
        <NoValue>{t("fileSlot.empty")}</NoValue>
      )}

      <input ref={fileInputRef} type="file" accept={accept} hidden={true} onChange={handleChange} aria-label={t("fileSlot.pick")} />

      <Button
        variant="secondary"
        size="sm"
        loading={isUploading}
        disabled={disabled === true || busy}
        onClick={() => fileInputRef.current?.click()}
      >
        <UploadIcon className="size-4" />
        {hasFile ? t("fileSlot.replace") : t("fileSlot.upload")}
      </Button>

      {hasFile ? (
        <Button variant="ghost" size="sm" loading={isRemoving} disabled={disabled === true || busy} onClick={() => void onRemove()}>
          <Trash2Icon className="size-4" />
          {t("fileSlot.remove")}
        </Button>
      ) : null}
    </div>
  );
}
