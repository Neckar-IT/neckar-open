import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import { cn } from "cn";
import { useRef, type ChangeEventHandler, type MouseEventHandler } from "react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { Button } from "../../primitives/button";
import { InputWrapper, type InputWrapperProps } from "../input-wrapper";

/**
 * Text Keys
 */
interface FileInputTextKeys {
  placeholder: TextKey;
}

/**
 * Prop Types for the File Input component
 */
type FileInputProps = Omit<
  React.InputHTMLAttributes<HTMLInputElement>,
  "type" | "value" | "onChange" | "defaultValue" | "multiple" | "hidden"
> &
  Omit<InputWrapperProps, "onClear"> & {
    value: File | null | undefined;
    onChange: (value: File | null) => void;
    textKeys?: Partial<FileInputTextKeys>;
  };

/**
 * Select a file from the user's device and pass it to the parent component via the onChange callback.
 */
export function FileInput({
  value,
  onChange,
  isInvalid,
  className,
  disabled,
  customClearIcon,
  addOnLeft,
  addOnRight,
  textKeys,
  ...rest
}: FileInputProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const textKeysWithDefaults: FileInputTextKeys = {
    placeholder: textKeys?.placeholder ?? "fileInput.placeholder",
  };

  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleButtonClick: MouseEventHandler<HTMLButtonElement> = (event) => {
    event.preventDefault();
    fileInputRef.current?.click();
  };

  const handleChange: ChangeEventHandler<HTMLInputElement> = (event) => {
    const selectedFile = event.target.files?.[0] ?? null;
    onChange(selectedFile);
  };

  const handleClear = () => {
    onChange(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  };

  return (
    <>
      <InputWrapper
        disabled={disabled}
        className={className}
        onClear={handleClear}
        hideClearButton={!value}
        isInvalid={isInvalid}
        customClearIcon={customClearIcon}
        addOnLeft={addOnLeft}
        addOnRight={addOnRight}
      >
        <Button
          className={cn("w-full justify-start font-normal", { "text-muted-foreground": !value })}
          disabled={disabled}
          variant="ghost"
          onClick={handleButtonClick}
        >
          {value ? value.name : t(textKeysWithDefaults.placeholder)}
        </Button>
      </InputWrapper>

      <input {...rest} hidden={true} onChange={handleChange} ref={fileInputRef} type="file" />
    </>
  );
}
