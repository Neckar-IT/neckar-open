import type { UploadProgress } from "@neckar-hub/typescript-react-query";
import { cn } from "cn";
import { RefreshCwIcon, Trash2Icon } from "lucide-react";
import { useState } from "react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { ActionsDropdown, ActionsDropdownItem } from "../../actions/ActionDropdown";
import { toast } from "../../feedback";
import { Dialog, DialogContent, DialogTitle } from "../../primitives/dialog";
import { FileUploadSlot } from "../file-upload-slot";
import { Image } from "../image";

export interface ImageSlotProps {
  /** The URL of the stored image in full size, which the overlay shows; `null` or `undefined` while none was uploaded. */
  imageUrl: string | null | undefined;
  /** The URL of the smaller image the slot shows; without it the slot shows [imageUrl]. */
  thumbnailUrl?: string | null | undefined;
  /** The description of the image for assistive technology. */
  alt: string;
  /** The MIME types the slot takes, e.g. `["image/png", "image/webp"]`; a file of another type is refused before `onUpload` is called. */
  accept: readonly string[];
  /** Called with the picked or dropped file. Uploading replaces the stored image. */
  onUpload: (file: File) => void | Promise<void>;
  /** Called when the user removes the image. Only reachable while [imageUrl] is set; the caller confirms and reports the deletion. */
  onRemove: () => void | Promise<void>;
  /** The largest file the server accepts, in bytes; shown as a hint, and a larger file is refused before `onUpload` is called. */
  maximumSize?: number | undefined;
  /** While `true` the slot shows the upload progress instead of its content and is locked. */
  isUploading?: boolean | undefined;
  /** The phase of the running upload, as `useFileUploadMutation` reports it; without it the progress card shows an indeterminate upload. */
  uploadProgress?: UploadProgress | undefined;
  /** Aborts the running upload; without it the upload progress card shows no cancel button. */
  onCancelUpload?: (() => void) | undefined;
  /** While `true` picking, replacing and deleting are locked. */
  isRemoving?: boolean | undefined;
  /** Locks picking, dropping and replacing; deleting and the overlay stay available; [removeDisabled] locks deleting. */
  disabled?: boolean | undefined;
  /** Locks the delete item; picking, dropping and replacing stay available. */
  removeDisabled?: boolean | undefined;
  /** The slot has no size of its own: it fills what this class names, e.g. `size-64`. */
  className?: string | undefined;
}

/**
 * One image that belongs to an entity but is not one of its form fields: it is written through endpoints of its own
 * (upload, delete). It knows neither the entity nor the endpoint; the caller passes the URL and both callbacks.
 *
 * The slot fills the size the caller gives it through [ImageSlotProps.className]. With an image it shows the whole image in
 * the slot and offers replace and delete in a dropdown at the top right; a click on it opens the full-size image in a fullscreen
 * overlay. Without an image it shows a drop zone that takes a dragged image or opens the file picker. A file of another type
 * is refused with a toast; the caller reports the outcome of the upload.
 */
export function ImageSlot({
  imageUrl,
  thumbnailUrl,
  alt,
  accept,
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
}: ImageSlotProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const [overlayOpen, setOverlayOpen] = useState(false);

  const typeNames = accept.map(typeName).join(", ");

  const handleUpload = (file: File) => {
    // A dropped file bypasses the type filter of the picker.
    if (!accept.includes(file.type)) {
      toast.error(t({ key: "imageSlot.wrongType", options: { fileName: file.name, types: typeNames } }));
      return;
    }
    return onUpload(file);
  };

  return (
    <div>
      <div className={cn("relative", className)}>
        <FileUploadSlot
          accept={accept.join(",")}
          kindLabel="IMG"
          emptyTitle={t("imageSlot.empty")}
          acceptHint={typeNames}
          onUpload={handleUpload}
          maximumSize={maximumSize}
          isUploading={isUploading}
          uploadProgress={uploadProgress}
          onCancelUpload={onCancelUpload}
          isRemoving={isRemoving}
          disabled={disabled}
          className="h-full"
        >
          {imageUrl == null
            ? null
            : ({ openPicker, locked }) => (
                <div className="relative h-full w-full">
                  <button
                    type="button"
                    aria-label={t({ key: "imageSlot.open", options: { alt } })}
                    onClick={() => setOverlayOpen(true)}
                    className="block h-full w-full cursor-pointer rounded-lg"
                  >
                    <Image src={thumbnailUrl ?? imageUrl} alt={alt} className="bg-muted/40 rounded-lg border" />
                  </button>
                  <ActionsDropdown triggerLabel={t("imageSlot.actions")} triggerVariant="outline" className="absolute top-2 right-2">
                    <ActionsDropdownItem
                      action={{ icon: <RefreshCwIcon />, label: t("imageSlot.replace"), disabled: locked, onTrigger: openPicker }}
                    />
                    <ActionsDropdownItem
                      action={{
                        icon: <Trash2Icon />,
                        label: t("imageSlot.delete"),
                        variant: "destructive",
                        disabled: removeDisabled,
                        loading: isRemoving,
                        onTrigger: () => void onRemove(),
                      }}
                    />
                  </ActionsDropdown>
                </div>
              )}
        </FileUploadSlot>
      </div>

      {imageUrl != null && (
        <Dialog open={overlayOpen} onOpenChange={setOverlayOpen}>
          <DialogContent size="full" aria-describedby={undefined} className="gap-0 bg-black/90 p-0">
            <DialogTitle className="sr-only">{alt}</DialogTitle>
            <button type="button" onClick={() => setOverlayOpen(false)} className="h-full w-full cursor-zoom-out p-4">
              <img src={imageUrl} alt={alt} className="h-full w-full object-contain" />
            </button>
          </DialogContent>
        </Dialog>
      )}
    </div>
  );
}

/**
 * The short name of a MIME type for the hint, e.g. `image/svg+xml` becomes `SVG`.
 */
function typeName(mimeType: string): string {
  const subtype = mimeType.split("/")[1] ?? mimeType;
  return (subtype.split("+")[0] ?? subtype).toUpperCase();
}
