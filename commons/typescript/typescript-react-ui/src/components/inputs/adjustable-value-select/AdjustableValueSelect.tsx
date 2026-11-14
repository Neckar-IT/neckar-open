import { type TextKey } from "@neckar-hub/typescript-react-i18n";
import {
  AdjustableValueAdjustedStringWithTypeType,
  AdjustableValueAutoStringWithTypeType,
  AdjustableValueManualStringWithTypeType,
} from "@neckar-hub/typescript-utils/openapi-types";
import { InfoIcon, PencilIcon, UndoIcon } from "lucide-react";
import { useState } from "react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { Button } from "../../primitives/button";
import { Popover, PopoverContent, PopoverTrigger } from "../../primitives/popover";
import { SelectFromList, type SelectFromListProps } from "../select-from-list";

/**
 * Destructure type variables to enhance readability. The discriminant literals are shared across every
 * monomorphized `AdjustableValue<T>` wire type, so the `String` constants stand in for any value type.
 */
const { Adjusted } = AdjustableValueAdjustedStringWithTypeType;
const { Auto } = AdjustableValueAutoStringWithTypeType;
const { Manual } = AdjustableValueManualStringWithTypeType;

/**
 * The value an [AdjustableValueSelect] edits: the read-only `AdjustableValue<T>` shape, extended so the
 * override may be empty (`value: null`) while the user picks one, and so the whole field may be `null`
 * — the inherited state with nothing to inherit, e.g. a "follow the source" choice whose source has no
 * value yet. The two-axis meaning is: `Auto`/`null` inherit, `Adjusted`/`Manual` override; `Auto`/
 * `Adjusted` have a calculated counterpart, `null`/`Manual` do not.
 */
export type AdjustableValueSelectValue<T> =
  | { type: typeof Auto; value: T }
  | { type: typeof Adjusted; calculated: T; value: T | null }
  | { type: typeof Manual; value: T | null };

/**
 * Prop Types
 */
export interface AdjustableValueSelectProps<T> extends Omit<SelectFromListProps<T>, "value" | "onChange" | "addOnLeft" | "addOnRight"> {
  value: AdjustableValueSelectValue<T> | null;
  onChange: (value: AdjustableValueSelectValue<T> | null) => void;
  textKeys?: {
    hint?: TextKey;
    calculatedValueLabel?: TextKey;
  };
}

/**
 * Select counterpart of [AdjustableValueTextInput]: edits an `AdjustableValue<T>` whose effective value
 * is one of a fixed option list rather than free text.
 *
 * The right-hand toggle flips the override on and off along whichever axis the calculated counterpart
 * allows: `Auto ↔ Adjusted` while a calculated value exists, `null ↔ Manual` while it does not. Crossing
 * between those axes — the calculated value appearing or disappearing — is not a user action but a
 * consequence of the source changing; apply [recalculatedAdjustableValueSelect] for that. An overridden
 * value discloses its calculated counterpart in the left-hand info popover, exactly as the display and
 * the text input do.
 */
export function AdjustableValueSelect<T>({
  value,
  onChange,
  isInvalid,
  disabled,
  textKeys,
  itemRenderer,
  ...rest
}: AdjustableValueSelectProps<T>): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const textKeysWithDefaults = {
    hint: textKeys?.hint ?? "",
    calculatedValueLabel: textKeys?.calculatedValueLabel ?? "adjustableValueFormField.calculatedValueLabel",
  };

  /**
   * The last value the user picked, used to re-seed the override when they toggle it back on. Seeded
   * from the initial `value` and only updated on a pick — deliberately not re-synced when the parent
   * replaces `value`: seeding from `value?.value` is what lets a field that mounts as an override
   * survive a toggle-off/toggle-on round-trip. The trade-off is that toggling an inherited field to an
   * override after its source recalculated re-seeds the remembered pick, not the freshly calculated
   * value; it self-heals on the next pick. The display always reads `value`, never this.
   */
  const [lastOverride, setLastOverride] = useState<T | null>(value?.value ?? null);

  const isInherited = value === null || value.type === Auto;
  const isOverride = value !== null && (value.type === Adjusted || value.type === Manual);

  const handleValueChange = (newValue: T | null) => {
    setLastOverride(newValue);
    // The select is disabled while inheriting, so this fires only for an override.
    onChange(
      value !== null && value.type === Adjusted
        ? { type: Adjusted, value: newValue, calculated: value.calculated }
        : { type: Manual, value: newValue },
    );
  };

  const handleToggleType = () => {
    switch (value?.type) {
      case Auto:
        // Turn the override on: keep the calculated counterpart, seed with the last picked override.
        onChange({ type: Adjusted, value: lastOverride ?? value.value, calculated: value.value });
        break;
      case Adjusted:
        // Turn the override off: fall back to the calculated counterpart.
        onChange({ type: Auto, value: value.calculated });
        break;
      case Manual:
        // Turn the override off while no calculated counterpart exists: inherit nothing.
        onChange(null);
        break;
      case undefined:
        // Turn the override on while no calculated counterpart exists: an empty override to pick into.
        onChange({ type: Manual, value: lastOverride });
        break;
      default:
        throw new Error(`Unexpected value in Switch Statement: value.type`);
    }
  };

  return (
    <SelectFromList
      {...rest}
      itemRenderer={itemRenderer}
      value={value?.value ?? null}
      onChange={handleValueChange}
      isInvalid={isInvalid}
      disabled={isInherited || disabled}
      hideClearButton={true}
      addOnLeft={
        value !== null && value.type === Adjusted ? (
          <Popover>
            <PopoverTrigger asChild>
              <Button variant="ghost" size="icon">
                <InfoIcon className="text-muted-foreground" />
              </Button>
            </PopoverTrigger>
            <PopoverContent className="text-sm">
              <div className="flex items-center justify-between gap-2 overflow-hidden">
                <div className="text-muted-foreground truncate">{t(textKeysWithDefaults.calculatedValueLabel)}</div>
                <div className="text-nowrap">{itemRenderer(value.calculated)}</div>
              </div>
              {textKeys?.hint ? (
                <div className="text-muted-foreground mt-1 text-justify text-xs">{t(textKeysWithDefaults.hint)}</div>
              ) : null}
            </PopoverContent>
          </Popover>
        ) : null
      }
      addOnRight={
        <Button variant="ghost" size="icon" onClick={handleToggleType} disabled={disabled} className="z-50 rounded-s-none">
          {isOverride ? <UndoIcon /> : <PencilIcon />}
        </Button>
      }
    />
  );
}
