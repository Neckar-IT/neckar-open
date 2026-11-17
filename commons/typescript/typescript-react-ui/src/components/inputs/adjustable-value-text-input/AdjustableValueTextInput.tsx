import { type TextKey } from "@neckar-hub/typescript-react-i18n";
import {
  type AdjustableValueAdjustedStringWithType,
  AdjustableValueAdjustedStringWithTypeType,
  type AdjustableValueAutoStringWithType,
  AdjustableValueAutoStringWithTypeType,
  type AdjustableValueManualStringWithType,
  AdjustableValueManualStringWithTypeType,
} from "@neckar-hub/typescript-utils/openapi-types";
import { InfoIcon, PencilIcon, UndoIcon } from "lucide-react";
import { useState } from "react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { Button } from "../../primitives/button";
import { Popover, PopoverContent, PopoverTrigger } from "../../primitives/popover";
import { TextInput, type TextInputProps } from "../text-input";

/**
 * Destructure type variables to enhance readability
 */
const { Adjusted } = AdjustableValueAdjustedStringWithTypeType;
const { Auto } = AdjustableValueAutoStringWithTypeType;
const { Manual } = AdjustableValueManualStringWithTypeType;

/**
 * Extends the Adjustable Value Interface to enable removing the overridden value with backspaces (i.e. set it to null)
 */
export type AdjustableValueTextExtended =
  | AdjustableValueAutoStringWithType
  | (Omit<AdjustableValueAdjustedStringWithType, "value"> & {
      value: string | null;
    })
  | (Omit<AdjustableValueManualStringWithType, "value"> & {
      value: string | null;
    });

/**
 * Prop Types
 *
 * The whole field may be `null` — nothing stored and nothing calculated, the state a field of a freshly
 * created entity starts in. The two-axis meaning is: `Auto`/`null` inherit, `Adjusted`/`Manual` override;
 * `Auto`/`Adjusted` have a calculated counterpart, `null`/`Manual` do not.
 */
export interface AdjustableValueTextInputProps extends Omit<TextInputProps, "value" | "onChange"> {
  value: AdjustableValueTextExtended | null;
  onChange: (value: AdjustableValueTextExtended | null) => void;
  textKeys?: {
    hint?: TextKey;
    calculatedValueLabel?: TextKey;
  };
}

/**
 * Input component for adjustable text values
 *
 * The right-hand toggle flips the override on and off along whichever axis the calculated counterpart
 * allows: `Auto ↔ Adjusted` while a calculated value exists, `null ↔ Manual` while it does not. Crossing
 * between those axes — the calculated value appearing or disappearing — is not a user action but a
 * consequence of the calculation's inputs changing; apply [recalculatedAdjustableValueText] for that.
 */
export function AdjustableValueTextInput({
  value,
  onChange,
  isInvalid,
  disabled,
  textKeys,
  ...rest
}: AdjustableValueTextInputProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const textKeysWithDefaults = {
    hint: textKeys?.hint ?? "",
    calculatedValueLabel: textKeys?.calculatedValueLabel ?? "adjustableValueFormField.calculatedValueLabel",
  };

  const [tempValue, setTempValue] = useState<string | null>(value?.value ?? null);

  const isInherited = value === null || value.type === Auto;
  const isOverride = value !== null && (value.type === Adjusted || value.type === Manual);

  const handleValueChange = (newValue: string | null) => {
    setTempValue(newValue);
    // The input is disabled while inheriting, so this fires only for an override.
    onChange(
      value !== null && value.type === Adjusted
        ? {
            type: Adjusted,
            value: newValue,
            calculated: value.calculated,
          }
        : // A manual value has no calculated counterpart to adjust against, so editing it keeps it manual
          { type: Manual, value: newValue },
    );
  };

  const handleToggleType = () => {
    switch (value?.type) {
      case Adjusted:
        onChange({
          type: Auto,
          // When switching to "Auto", we use the calculated value
          value: value.calculated,
        });
        break;
      case Auto:
        onChange({
          type: Adjusted,
          value: tempValue ?? value.value,
          // Keep the calculated value so the popover can show it and restore it when switching back to "Auto".
          calculated: value.value,
        });
        break;
      case Manual:
        // Turn the override off while no calculated counterpart exists: nothing stored.
        onChange(null);
        break;
      case undefined:
        // Turn the override on while no calculated counterpart exists: an empty override to type into.
        onChange({ type: Manual, value: tempValue });
        break;
      default:
        throw new Error(`Unexpected value in Switch Statement: ${JSON.stringify(value)}`);
    }
  };

  return (
    <TextInput
      {...rest}
      value={isInherited ? (value?.value ?? null) : tempValue}
      onChange={handleValueChange}
      isInvalid={isInvalid}
      disabled={isInherited || disabled}
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
                <div className="text-nowrap">{value.calculated}</div>
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
