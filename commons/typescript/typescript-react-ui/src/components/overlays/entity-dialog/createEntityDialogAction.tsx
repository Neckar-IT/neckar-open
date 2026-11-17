import { type ParseKeys, useTranslation } from "@neckar-hub/typescript-react-i18n";
import type { ReactNode } from "react";
import type { Action } from "../../actions/Action";
import { createEntityDialogAtom, useOpenEntityDialog } from "./entityDialogAtom";
import type { EntityDialogRender } from "./EntityDialogMount";

export interface EntityDialogActionOptions<T> {
  dialog: EntityDialogRender<T>;
  icon: ReactNode;
  labelKey: ParseKeys;
  variant: NonNullable<Action["variant"]>;
  /** Hook answering whether the current user may run the action; the action is hidden while it answers `false`. */
  useAllowed: () => boolean;
}

/**
 * Registers `dialog` with `<DialogRegistry />` and returns the hook of the action that opens it. Call it at module
 * level, so the dialog is registered when the module loads.
 */
export function createEntityDialogAction<T>({
  dialog,
  icon,
  labelKey,
  variant,
  useAllowed,
}: EntityDialogActionOptions<T>): (entity: T, onDone?: () => void) => Action {
  const dialogAtom = createEntityDialogAtom(dialog);

  return function useEntityDialogAction(entity, onDone) {
    const { t } = useTranslation();
    const open = useOpenEntityDialog(dialogAtom);
    const allowed = useAllowed();

    return { icon, label: t(labelKey), variant, hidden: !allowed, onTrigger: () => open(entity, onDone) };
  };
}
