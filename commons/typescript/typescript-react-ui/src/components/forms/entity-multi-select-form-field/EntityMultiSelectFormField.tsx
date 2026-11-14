import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import { useTranslation } from "@neckar-hub/typescript-react-i18n";
import { PlusIcon, XIcon } from "lucide-react";
import { useState, type ReactElement, type ReactNode } from "react";
import type { ZodObject } from "zod";
import { Badge } from "../../primitives/badge";
import { Button } from "../../primitives/button";
import type { Identifiable } from "../entity-select-form-field";
import { FormFieldBase, type FormFieldBaseProps } from "../form";

export interface EntityMultiSelectFormFieldProps<T extends ZodObject, TEntity extends Identifiable> extends FormFieldBaseProps<T> {
  /** Entities the field is initialized with — rendered as the initial badges. */
  initialEntities: TEntity[];
  addButtonLabel: TextKey;
  itemRenderer: (entity: TEntity) => ReactNode;
  /**
   * The dialog always opens to add a further entity, never to revise one selection — so it is
   * handed the whole current selection rather than a single entity. Pass [selectedEntityIds] on so
   * the dialog can disable the entries that are already picked: adding one of them is a no-op, and
   * without the ids that would be invisible to the user.
   */
  renderDialog: (props: {
    onOpenChange: (open: boolean) => void;
    selectedEntityIds: string[];
    onSelectEntity: (entity: TEntity | undefined) => void;
  }) => ReactElement;
}

/**
 * Multi-value variant of [EntitySelectFormField]: the field value is an array of entity ids.
 *
 * Selected entities are kept in local state so the badges can render names without a re-fetch;
 * the react-hook-form leaf holds the id array. Picking an entity already in the list is a no-op.
 * Mirrors the single-select picker, composed with the same select dialog building blocks.
 *
 * [TEntity] is typically the entity's `*Preview`, so a caller that already holds previews can pass
 * them straight in — the select dialog's table entry satisfies the preview shape.
 */
export function EntityMultiSelectFormField<T extends ZodObject, TEntity extends Identifiable>({
  initialEntities,
  addButtonLabel,
  itemRenderer,
  renderDialog,
  ...rest
}: EntityMultiSelectFormFieldProps<T, TEntity>): ReactElement {
  const { t } = useTranslation();

  const [open, setOpen] = useState(false);
  const [selectedEntities, setSelectedEntities] = useState<TEntity[]>(initialEntities);

  return (
    <FormFieldBase
      {...rest}
      render={({ field }) => {
        const commit = (entities: TEntity[]) => {
          setSelectedEntities(entities);
          field.onChange(entities.map((entity) => entity.id));
        };

        const addEntity = (entity: TEntity | undefined) => {
          if (entity === undefined) return;
          if (selectedEntities.some((existing) => existing.id === entity.id)) return;
          commit([...selectedEntities, entity]);
        };

        const removeEntity = (id: string) => {
          commit(selectedEntities.filter((entity) => entity.id !== id));
        };

        return (
          <EntityMultiSelectControl
            onClick={() => setOpen(true)}
            addButtonLabel={t(addButtonLabel)}
            dialog={
              open
                ? renderDialog({
                    onOpenChange: setOpen,
                    selectedEntityIds: selectedEntities.map((entity) => entity.id),
                    onSelectEntity: (entity) => {
                      addEntity(entity);
                      setOpen(false);
                    },
                  })
                : null
            }
          >
            {selectedEntities.map((entity) => (
              <Badge key={entity.id} variant="secondary" className="gap-1">
                {itemRenderer(entity)}
                <button type="button" onClick={() => removeEntity(entity.id)} className="hover:text-destructive">
                  <XIcon className="size-3" />
                </button>
              </Badge>
            ))}
          </EntityMultiSelectControl>
        );
      }}
    />
  );
}

interface EntityMultiSelectControlProps extends React.ComponentProps<"button"> {
  addButtonLabel: string;
  /** Rendered next to the badge row; the select dialogs portal, so this position never shows. */
  dialog: ReactNode;
}

/** The element `FormControl` slots onto — it passes those props on to the add button. */
function EntityMultiSelectControl({ addButtonLabel, dialog, children, ...addButtonProps }: EntityMultiSelectControlProps): ReactElement {
  return (
    <>
      <div className="flex flex-wrap items-center gap-2">
        {children}
        <Button type="button" size="sm" variant="outline" {...addButtonProps}>
          <PlusIcon className="size-4" />
          {addButtonLabel}
        </Button>
      </div>

      {dialog}
    </>
  );
}
