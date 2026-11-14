import { useAtom } from "jotai";
import type { ReactElement } from "react";
import type { EntityDialogAtom } from "./entityDialogAtom";

export interface EntityDialogRenderProps<T> {
  entity: T;
  onOpenChange: (open: boolean) => void;
  onDone?: () => void;
}

/** Rendered as a component, so it may call hooks; a new function per render remounts the dialog. */
export type EntityDialogRender<T> = (props: EntityDialogRenderProps<T>) => ReactElement;

interface EntityDialogMountProps<T> {
  atom: EntityDialogAtom<T>;
  render: EntityDialogRender<T>;
}

/**
 * Renders the dialog of `atom` while it is visible; closing the dialog hides it.
 */
export function EntityDialogMount<T>({ atom, render }: EntityDialogMountProps<T>): ReactElement | null {
  const [dialog, setDialog] = useAtom(atom);

  switch (dialog.type) {
    case "Hidden":
      return null;
    case "Visible": {
      const Dialog = render;
      return (
        <Dialog
          entity={dialog.entity}
          onOpenChange={(isOpen) => {
            if (!isOpen) setDialog({ type: "Hidden" });
          }}
          onDone={dialog.onDone}
        />
      );
    }
    default:
      throw new Error(`Unexpected value in Switch Statement: dialog.type`);
  }
}
