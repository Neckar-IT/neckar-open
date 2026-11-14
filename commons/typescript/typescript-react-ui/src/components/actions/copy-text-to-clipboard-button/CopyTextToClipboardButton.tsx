/**
 * Imports
 */

import { CheckIcon, CopyIcon, XIcon } from "lucide-react";
import { useState } from "react";
import { Button, type ButtonProps } from "../../primitives/button";

/**
 * Button that copies some text to the clipboard
 */

export type CopyTextToClipboardButtonProps = Omit<ButtonProps, "children" | "onClick"> & {
  text: string;
};

/**
 * Button that copies the given text to the clipboard.
 */
export function CopyTextToClipboardButton(props: CopyTextToClipboardButtonProps): React.ReactElement {
  const { text, ...rest } = props;

  const [isCopied, setIsCopied] = useState(false);
  const [isError, setIsError] = useState(false);

  const handleCopyTextToClipboard = () => {
    window.navigator.clipboard
      .writeText(text)
      .then(() => {
        setIsCopied(true);
        window.setTimeout(() => {
          setIsCopied(false);
        }, 2000);
        return undefined;
      })
      .catch(() => {
        setIsError(true);
        window.setTimeout(() => {
          setIsError(false);
        }, 2000);
      });
  };

  return (
    <Button {...rest} onClick={handleCopyTextToClipboard}>
      {isCopied ? <CheckIcon className="h-4 w-4" /> : isError ? <XIcon className="h-4 w-4" /> : <CopyIcon className="h-4 w-4" />}
    </Button>
  );
}
