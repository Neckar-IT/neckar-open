import { atom, type PrimitiveAtom, useSetAtom } from "jotai";
import { registerEntityDialog } from "./DialogRegistry";
import type { EntityDialogRender } from "./EntityDialogMount";

/** `onDone` runs after the dialog finished its work, for example to navigate away from an archived entity. */
export type EntityDialogState<T> = { type: "Hidden" } | { type: "Visible"; entity: T; onDone?: () => void };

export type EntityDialogAtom<T> = PrimitiveAtom<EntityDialogState<T>>;

/**
 * Creates the atom of a dialog and registers `render` with `<DialogRegistry />`. The registration is a side effect
 * that lasts for the lifetime of the page, so call it at module level, once per dialog.
 */
export function createEntityDialogAtom<T>(render: EntityDialogRender<T>): EntityDialogAtom<T> {
  const dialogAtom = atom<EntityDialogState<T>>({ type: "Hidden" });
  registerEntityDialog(dialogAtom, render);
  return dialogAtom;
}

export function useOpenEntityDialog<T>(dialogAtom: EntityDialogAtom<T>): (entity: T, onDone?: () => void) => void {
  const setDialog = useSetAtom(dialogAtom);
  return (entity, onDone) => setDialog({ type: "Visible", entity, onDone });
}
