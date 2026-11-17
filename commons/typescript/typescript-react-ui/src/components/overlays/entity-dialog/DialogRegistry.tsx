import { type ReactElement, useSyncExternalStore } from "react";
import type { EntityDialogAtom } from "./entityDialogAtom";
import { type EntityDialogRender, EntityDialogMount } from "./EntityDialogMount";

interface RegisteredEntityDialog {
  atom: EntityDialogAtom<never>;
  render: EntityDialogRender<never>;
}

let registeredDialogs: readonly RegisteredEntityDialog[] = [];
const listeners = new Set<() => void>();

/**
 * Makes `<DialogRegistry />` render the dialog of `atom`, also after the registry rendered, as a lazily loaded
 * module registers at load. Called by {@link createEntityDialogAtom} only, so every atom has exactly one dialog.
 */
export function registerEntityDialog<T>(atom: EntityDialogAtom<T>, render: EntityDialogRender<T>): void {
  registeredDialogs = [...registeredDialogs, { atom, render } as unknown as RegisteredEntityDialog];
  listeners.forEach((listener) => listener());
}

function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

function getRegisteredDialogs(): readonly RegisteredEntityDialog[] {
  return registeredDialogs;
}

/**
 * Renders the dialog of every atom created through {@link createEntityDialogAtom}. Rendered once at the app root, so a
 * dialog outlives the page that opened it.
 */
export function DialogRegistry(): ReactElement {
  const dialogs = useSyncExternalStore(subscribe, getRegisteredDialogs);
  return (
    <>
      {dialogs.map((dialog) => (
        // A jotai atom's toString() is unique per atom.
        <EntityDialogMount key={dialog.atom.toString()} atom={dialog.atom} render={dialog.render} />
      ))}
    </>
  );
}
