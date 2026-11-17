import type { UploadProgress } from "@neckar-hub/typescript-react-query";
import { DownloadIcon, FileTextIcon, RefreshCwIcon, Trash2Icon } from "lucide-react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { DescriptionList, DescriptionListLabel, DescriptionListRow, DescriptionListValue } from "../../data-display/description-list";
import { FileSizeOutput } from "../../data-display/file-size-output";
import { FormattedDateTime } from "../../data-display/formatted-date-time";
import { Button, buttonVariants } from "../../primitives/button";
import { FileKindTile, FileUploadSlot } from "../file-upload-slot";
import { Image } from "../image";

/**
 * The stored PDF a [PdfSlot] shows. Every URL is presigned and resolved per request.
 */
export interface PdfSlotDocument {
  /** The name the download is offered under. */
  fileName: string;
  /** The size of the file in bytes. */
  size: number;
  /** When the file was uploaded, as ISO-8601 instant. */
  uploadedAt: string;
  /** Serves the PDF for display in the browser; the preview opens it in a new tab. */
  url: string;
  /** Downloads the PDF as [fileName]. */
  downloadUrl: string;
  /** The image of the first page; `null` for a document stored without one. */
  thumbnailUrl?: string | null | undefined;
  /** The number of pages; without it neither the page badge nor the page count is shown. */
  pageCount?: number | null | undefined;
}

export interface PdfSlotProps {
  /** The stored PDF, `null` or `undefined` while none was uploaded. */
  document: PdfSlotDocument | null | undefined;
  /** Headline of the empty slot, e.g. "Noch keine AGB hochgeladen". */
  emptyTitle: string;
  /**
   * Called with the picked or dropped file. Uploading replaces whatever is in the slot — there is exactly one
   * file per slot.
   */
  onUpload: (file: File) => void | Promise<void>;
  /** Called when the user removes the file. Only reachable while [document] is set; the caller confirms and reports the deletion. */
  onRemove: () => void | Promise<void>;
  /** The largest file the server accepts, in bytes; shown as a hint, and a larger file is refused before `onUpload` is called. */
  maximumSize?: number | undefined;
  /** While `true` the slot shows the upload progress instead of its content and is locked. */
  isUploading?: boolean | undefined;
  /** The phase of the running upload, as `useFileUploadMutation` reports it; without it the progress card shows an indeterminate upload. */
  uploadProgress?: UploadProgress | undefined;
  /** Aborts the running upload; without it the upload progress card shows no cancel button. It offers none in `processing` either: the body is sent and the server may keep the file. */
  onCancelUpload?: (() => void) | undefined;
  /** While `true` the remove button shows a spinner; picking, replacing and removing are locked. */
  isRemoving?: boolean | undefined;
  /** Locks picking, dropping and replacing; removing, download and preview stay available; [removeDisabled] locks removing. */
  disabled?: boolean | undefined;
  /** Locks the remove button; picking, dropping and replacing stay available. */
  removeDisabled?: boolean | undefined;
  className?: string | undefined;
}

/**
 * One uploaded PDF that belongs to an entity but is not one of its form fields: it is written through endpoints
 * of its own (upload, remove) rather than through the entity's upsert, so it never appears in a form schema.
 *
 * With a file the slot shows the first page, the file details and the actions download, replace and remove.
 * Without one it shows a drop zone that takes a dragged PDF or opens the file picker.
 */
export function PdfSlot({
  document,
  emptyTitle,
  onUpload,
  onRemove,
  maximumSize,
  isUploading,
  uploadProgress,
  onCancelUpload,
  isRemoving,
  disabled,
  removeDisabled,
  className,
}: PdfSlotProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();

  return (
    <FileUploadSlot
      accept="application/pdf"
      kindLabel="PDF"
      emptyTitle={emptyTitle}
      acceptHint={t("pdfSlot.onlyPdf")}
      onUpload={onUpload}
      maximumSize={maximumSize}
      isUploading={isUploading}
      uploadProgress={uploadProgress}
      onCancelUpload={onCancelUpload}
      isRemoving={isRemoving}
      disabled={disabled}
      className={className}
    >
      {document == null
        ? null
        : ({ openPicker, locked }) => (
            <StoredPdf
              document={document}
              locked={locked}
              isRemoving={isRemoving}
              removeDisabled={removeDisabled}
              onPick={openPicker}
              onRemove={onRemove}
            />
          )}
    </FileUploadSlot>
  );
}

interface StoredPdfProps {
  document: PdfSlotDocument;
  locked: boolean;
  isRemoving: boolean | undefined;
  removeDisabled: boolean | undefined;
  onPick: () => void;
  onRemove: () => void | Promise<void>;
}

function StoredPdf({ document, locked, isRemoving, removeDisabled, onPick, onRemove }: StoredPdfProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();

  const pageCount = document.pageCount ?? null;

  return (
    <div className="grid gap-6 sm:grid-cols-[12rem_1fr]">
      <a
        href={document.url}
        target="_blank"
        rel="noopener noreferrer"
        aria-label={t({ key: "pdfSlot.open", options: { fileName: document.fileName } })}
        className="bg-muted/40 relative flex w-full max-w-48 items-start justify-center self-start overflow-hidden rounded-lg border p-3"
      >
        {document.thumbnailUrl != null ? (
          <Image src={document.thumbnailUrl} alt="" className="min-h-24 rounded-sm border bg-white shadow-sm" />
        ) : (
          <FileTextIcon className="text-muted-foreground my-10 size-12" />
        )}
        {pageCount != null && (
          <span className="bg-foreground/80 text-background absolute right-4 bottom-4 rounded px-1.5 py-0.5 text-xs font-medium">
            {t({ key: "pdfSlot.pageOfPages", options: { page: 1, pages: pageCount } })}
          </span>
        )}
      </a>

      <div className="flex min-w-0 flex-col gap-5">
        <div className="flex items-center gap-3">
          <FileKindTile kindLabel="PDF" />
          <div className="min-w-0">
            <p className="truncate font-semibold">{document.fileName}</p>
            <p className="text-muted-foreground text-sm">
              {pageCount == null ? t("pdfSlot.kind") : t({ key: "pdfSlot.kindWithPages", options: { count: pageCount } })}
            </p>
          </div>
        </div>

        <DescriptionList>
          <DescriptionListRow>
            <DescriptionListLabel className="text-sm normal-case">{t("pdfSlot.fileName")}</DescriptionListLabel>
            <DescriptionListValue className="font-mono font-normal break-all">{document.fileName}</DescriptionListValue>
          </DescriptionListRow>
          <DescriptionListRow>
            <DescriptionListLabel className="text-sm normal-case">{t("pdfSlot.size")}</DescriptionListLabel>
            <DescriptionListValue className="font-mono font-normal">
              <FileSizeOutput bytes={document.size} />
            </DescriptionListValue>
          </DescriptionListRow>
          <DescriptionListRow>
            <DescriptionListLabel className="text-sm normal-case">{t("pdfSlot.uploadedAt")}</DescriptionListLabel>
            <DescriptionListValue className="font-mono font-normal">
              <FormattedDateTime date={document.uploadedAt} />
            </DescriptionListValue>
          </DescriptionListRow>
        </DescriptionList>

        <div className="flex flex-wrap items-center gap-3">
          <a href={document.downloadUrl} download={document.fileName} className={buttonVariants({ variant: "default" })}>
            <DownloadIcon className="size-4" />
            {t("pdfSlot.download")}
          </a>
          <Button variant="outline" disabled={locked} onClick={onPick}>
            <RefreshCwIcon className="size-4" />
            {t("pdfSlot.replace")}
          </Button>
          <Button
            variant="outline"
            loading={isRemoving}
            disabled={removeDisabled}
            onClick={() => void onRemove()}
            className="text-destructive hover:text-destructive"
          >
            <Trash2Icon className="size-4" />
            {t("pdfSlot.remove")}
          </Button>
        </div>
      </div>
    </div>
  );
}
