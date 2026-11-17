import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import { useTranslation } from "@neckar-hub/typescript-react-i18n";
import { useState, type ReactElement, type ReactNode } from "react";
import type { ZodObject } from "zod";
import { SelectedEntityButton } from "../../inputs/selected-entity-button";
import { FormFieldBase, type FormFieldBaseProps } from "../form";

export interface Identifiable {
  id: string;
}

/**
 * [TEntity] is what the dialog selects — typically the entity's table entry. [TInitialEntity] is what
 * the already-selected value arrives as, which is often a lighter projection (a preview embedded in a
 * detail view) rather than a full table entry. It defaults to [TEntity] for the fields whose initial
 * value is a table entry.
 *
 * The currently selected value handed to [renderDialog] is therefore either type — dialogs that can
 * receive a [TInitialEntity] identify the selected row by its `id`.
 */
export interface EntitySelectFormFieldProps<
  T extends ZodObject,
  TEntity extends Identifiable,
  TInitialEntity extends Identifiable = TEntity,
> extends FormFieldBaseProps<T> {
  initialEntity: TInitialEntity | undefined;
  placeholder?: TextKey;
  itemRenderer: (entity: TEntity | TInitialEntity) => ReactNode;

  renderDialog: (props: {
    onOpenChange: (open: boolean) => void;
    selectedEntity: TEntity | TInitialEntity | undefined;
    onSelectEntity: (entity: TEntity | undefined) => void;
  }) => ReactElement;

  /**
   * Notified whenever the selection changes, with the picked entity — or `undefined` when the
   * selection was cleared. The form itself only stores the entity's id, so this is how a caller that
   * needs more than the id (a projection derived from the selected entity, say) follows along without
   * a second fetch.
   */
  onEntitySelected?: (entity: TEntity | TInitialEntity | undefined) => void;
}

/**
 * The picker-layer state and dialog callbacks use `undefined` for "no selection".
 * The seam to react-hook-form lives in `handleEntitySelect` — `field.onChange`
 * receives `null` so the form object keeps an explicit leaf value.
 */
export function EntitySelectFormField<T extends ZodObject, TEntity extends Identifiable, TInitialEntity extends Identifiable = TEntity>({
  initialEntity,
  placeholder,
  itemRenderer,
  renderDialog,
  onEntitySelected = () => {},
  ...rest
}: EntitySelectFormFieldProps<T, TEntity, TInitialEntity>): ReactElement {
  const { t } = useTranslation();

  const [open, setOpen] = useState(false);
  const [selectedEntity, setSelectedEntity] = useState<TEntity | TInitialEntity | undefined>(initialEntity);

  return (
    <FormFieldBase
      {...rest}
      render={({ field }) => {
        const handleEntitySelect = (entity: TEntity | TInitialEntity | undefined) => {
          setSelectedEntity(entity);
          field.onChange(entity?.id ?? null);
          onEntitySelected(entity);
        };

        return (
          <EntitySelectControl
            onClick={() => setOpen(true)}
            onClear={() => handleEntitySelect(undefined)}
            hideClearButton={selectedEntity === undefined}
            dialog={
              open
                ? renderDialog({
                    onOpenChange: setOpen,
                    selectedEntity,
                    onSelectEntity: (entity) => {
                      handleEntitySelect(entity);
                      if (entity !== undefined) {
                        setOpen(false);
                      }
                    },
                  })
                : null
            }
          >
            <span className={selectedEntity !== undefined ? undefined : "text-muted-foreground"}>
              {selectedEntity !== undefined ? itemRenderer(selectedEntity) : placeholder !== undefined ? t(placeholder) : undefined}
            </span>
          </EntitySelectControl>
        );
      }}
    />
  );
}

interface EntitySelectControlProps extends React.ComponentProps<"button"> {
  onClear: () => void;
  hideClearButton: boolean;
  /** Rendered next to the trigger; the select dialogs portal, so this position never shows. */
  dialog: ReactNode;
}

/** The element `FormControl` slots onto — it passes those props on to the trigger. */
function EntitySelectControl({ onClear, hideClearButton, dialog, children, ...triggerProps }: EntitySelectControlProps): ReactElement {
  return (
    <>
      <SelectedEntityButton onClear={onClear} hideClearButton={hideClearButton} {...triggerProps}>
        {children}
      </SelectedEntityButton>

      {dialog}
    </>
  );
}
