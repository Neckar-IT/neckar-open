import { cn } from "cn";
import { SquareMousePointerIcon } from "lucide-react";
import { type ReactNode, useEffect, useId, useRef, useState } from "react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { SelectedEntityButton } from "../../inputs/selected-entity-button";
import { FilterTrigger } from "../filter-trigger";

export interface FilterByEntityProps {
  /** Translated. */
  label: string;
  /** The selected entity, e.g. its display name, or the pending text; `undefined` while nothing is selected. */
  selected: ReactNode;
  /** Whether an entity is selected. */
  active: boolean;
  onClear: () => void;
  /** Renders the select dialog of the entity; it closes through `onOpenChange(false)`. */
  renderSelectDialog: (onOpenChange: (open: boolean) => void) => ReactNode;
  /** `Inline` is a filter button that opens the dialog, `Panel` a field-like button that shows the selection. Defaults to `Inline`. */
  form?: "Inline" | "Panel";
}

/**
 * Filter control for one entity, picked in its select dialog.
 */
export function FilterByEntity({
  label,
  selected,
  active,
  onClear,
  renderSelectDialog,
  form = "Inline",
}: FilterByEntityProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const [dialogOpen, setDialogOpen] = useState(false);
  // The select dialog has no DialogTrigger, so Radix returns the focus to nobody; the trigger takes it back here.
  const triggerElement = useRef<HTMLButtonElement>(null);
  const wasOpen = useRef(false);
  useEffect(() => {
    if (wasOpen.current && !dialogOpen) {
      triggerElement.current?.focus();
    }
    wasOpen.current = dialogOpen;
  }, [dialogOpen]);
  const labelId = useId();
  const valueId = useId();

  const trigger = (() => {
    switch (form) {
      case "Inline":
        return (
          <FilterTrigger
            ref={triggerElement}
            icon={SquareMousePointerIcon}
            label={label}
            valueText={selected}
            active={active}
            onClear={onClear}
            aria-haspopup="dialog"
            onClick={() => setDialogOpen(true)}
          />
        );
      case "Panel":
        return (
          <SelectedEntityButton
            ref={triggerElement}
            aria-haspopup="dialog"
            // Label and value name the button, also while the value is a React node.
            aria-labelledby={`${labelId} ${valueId}`}
            hideClearButton={!active}
            marked={active}
            onClear={onClear}
            onClick={() => setDialogOpen(true)}
          >
            <span id={labelId} className="sr-only">{`${label}:`}</span>
            <span id={valueId} className={cn("truncate", { "text-muted-foreground": !active })}>
              {active ? selected : t("filterBar.selectEntity")}
            </span>
          </SelectedEntityButton>
        );
      default:
        throw new Error(`Unexpected value in Switch Statement: form`);
    }
  })();

  return (
    <>
      {trigger}
      {dialogOpen ? renderSelectDialog(setDialogOpen) : null}
    </>
  );
}
