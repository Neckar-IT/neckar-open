import { Button } from "../../primitives/button";
import type { ResponsiveColumnDefinition } from "./ResponsiveTable";

/**
 * Column helper for entity select dialogs: a right-aligned link button per row that reports the
 * row via `onSelect` and is disabled while `isSelected` holds for it.
 *
 * `isSelected` is a predicate rather than a value comparison so a call site can match on whatever
 * shape its selection state has — an entity, a nullable preview, a union of views, or a list of ids.
 */
export function selectEntityColumn<ItemType, SortCriteria>({
  isSelected,
  onSelect,
  selectLabel,
}: {
  isSelected: (item: ItemType) => boolean;
  onSelect: (item: ItemType) => void;
  selectLabel: string;
}): ResponsiveColumnDefinition<ItemType, SortCriteria> {
  return {
    id: "select",
    // The column has no header, so on mobile it belongs into the card header next to the entity
    // name — as a detail row it would render against an empty `<dt>`.
    viewOnMobile: "secondary-header",
    cell: (item) => (
      <div className="text-right">
        <Button size="sm" variant="link" disabled={isSelected(item)} onClick={() => onSelect(item)}>
          {selectLabel}
        </Button>
      </div>
    ),
  };
}
