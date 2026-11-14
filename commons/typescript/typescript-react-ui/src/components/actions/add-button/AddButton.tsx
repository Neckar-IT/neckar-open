/**
 * Imports
 */

import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import { PlusIcon } from "lucide-react";
import { useState } from "react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { Button } from "../../primitives/button";

/**
 * Prop Types
 */

interface InjectedProps {
  onOpenChange: (open: boolean) => void;
}

type AddButtonProps<T extends React.ElementType> = {
  dialog: T;
  textKey?: TextKey;
} & Omit<React.ComponentPropsWithoutRef<T>, keyof InjectedProps>;

/**
 * Button that can be used to open a dialog to add a new item to an entity
 */

export function AddButton<T extends React.ElementType>({ textKey, dialog: Dialog, ...dialogProps }: AddButtonProps<T>): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const [open, setOpen] = useState(false);

  return (
    <>
      <Button onClick={() => setOpen(true)}>
        <PlusIcon />
        <span>{t(textKey ?? "addButton.label")}</span>
      </Button>

      {open ? (
        // @ts-expect-error typescript cannot infer that dialogProps are correct here
        <Dialog onOpenChange={(open: boolean) => setOpen(open)} {...(dialogProps as React.ComponentProps<T>)} />
      ) : null}
    </>
  );
}
