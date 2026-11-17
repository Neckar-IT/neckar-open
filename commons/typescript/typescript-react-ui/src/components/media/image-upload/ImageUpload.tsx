/**
 * Imports
 */

import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import { cn } from "cn";
import { type LucideIcon } from "lucide-react";
import type { PropsWithChildren, ReactElement } from "react";
import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { FileInput } from "../../inputs/file-input";
import { Image } from "../image";

/**
 * Context
 */

interface ImageUploadContextType {
  value: File | undefined;
  onChange: (file?: File) => void;
  disabled?: boolean | undefined;
  accept?: string | undefined;
}

const ImageUploadContext = createContext<ImageUploadContextType | null>(null);

/**
 * Image Upload Provider
 */

export type ImageUploadProps = PropsWithChildren<{
  value: File | undefined;
  onChange: (file?: File) => void;
  className?: string | undefined;
  disabled?: boolean | undefined;
  accept?: string;
}>;

/**
 * Image upload field with drag-and-drop and preview.
 */
export function ImageUpload(props: ImageUploadProps): ReactElement {
  const contextValue = useMemo(
    () => ({ value: props.value, onChange: props.onChange, disabled: props.disabled, accept: props.accept }),
    [props.value, props.onChange, props.disabled, props.accept],
  );
  return (
    <ImageUploadContext.Provider value={contextValue}>
      <div className={props.className}>{props.children}</div>
    </ImageUploadContext.Provider>
  );
}

/**
 * Hook to use the context
 */

function useImageUploadContext(): ImageUploadContextType {
  const context = useContext(ImageUploadContext);

  if (!context) {
    throw new Error("useImageUploadContext must be used within an ImageUpload Component");
  }

  return context;
}

/**
 * Image Upload Input
 */

interface ImageUploadInputTextKeys {
  label: TextKey;
}

interface ImageUploadInputProps extends Omit<React.InputHTMLAttributes<HTMLInputElement>, "onChange" | "type" | "disabled" | "accept"> {
  icon?: LucideIcon | undefined;
  textKeys?: Partial<ImageUploadInputTextKeys>;
}

export const ImageUploadInput = ({ textKeys, ...props }: ImageUploadInputProps) => {
  const { value, onChange, disabled, accept } = useImageUploadContext();
  const textKeysWithDefaults: ImageUploadInputTextKeys = {
    label: textKeys?.label ?? "imageUpload.input",
  };

  const handleChange = (value: File | null) => {
    onChange(value ?? undefined);
  };

  return (
    <FileInput
      {...props}
      accept={accept}
      value={value}
      onChange={handleChange}
      disabled={disabled}
      textKeys={{ placeholder: textKeysWithDefaults.label }}
    />
  );
};
ImageUploadInput.displayName = "ImageUploadInput";

/**
 * Image Upload Preview
 *
 * @prop {string | null | undefined} preview - The image URL to display if no upload is selected.
 */

interface ImageUploadPreviewProps extends Omit<React.ImgHTMLAttributes<HTMLImageElement>, "src"> {
  preview?: string | null | undefined;
}

export function ImageUploadPreview({ className, ...props }: ImageUploadPreviewProps) {
  const { value, disabled } = useImageUploadContext();

  // Object URLs created via URL.createObjectURL must be revoked when no longer
  // needed, otherwise the browser keeps the underlying blob alive until page unload.
  // useEffect with cleanup handles both the value-change and unmount cases.
  const [objectUrl, setObjectUrl] = useState<string | undefined>(undefined);
  useEffect(() => {
    if (!value) {
      setObjectUrl(undefined);
      return;
    }
    const url = URL.createObjectURL(value);
    setObjectUrl(url);
    return () => {
      URL.revokeObjectURL(url);
    };
  }, [value]);

  const imageUrl = objectUrl ?? props.preview ?? undefined;

  return <Image className={cn({ "opacity-50": disabled }, className)} src={imageUrl} {...props} />;
}
