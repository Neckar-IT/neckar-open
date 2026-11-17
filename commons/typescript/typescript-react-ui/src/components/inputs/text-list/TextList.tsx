/**
 * Imports
 */

import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { FieldArray, FieldArrayAddButton, FieldArrayRemoveButton } from "../../layout/field-array";
import { TextInput } from "../text-input";

/**
 * Prop Types
 */

interface TextListTextKeys {
  add: TextKey;
  remove: TextKey;
}

export interface TextListProps {
  value: string[] | undefined | null;
  onChange: (value: string[]) => void;
  /** How many entries the list may hold; the add button is disabled from there on. */
  maxEntries?: number;
  /** How many characters one entry may hold. */
  maxLength?: number;
  placeholder?: string;
  disabled?: boolean;
  className?: string;
  textKeys?: Partial<TextListTextKeys>;
}

/**
 * A list of free texts: one input per entry with a remove button beside it, and an add button below.
 *
 * Two entries may hold the same text, so an entry is identified by its position rather than by what
 * it holds. Emptying a line leaves the entry in the list — the button beside it is what removes one.
 */

export function TextList(props: TextListProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const entries = props.value ?? [];
  const textKeys: TextListTextKeys = {
    add: props.textKeys?.add ?? "textList.add",
    remove: props.textKeys?.remove ?? "textList.remove",
  };

  const replaceAt = (index: number, entry: string) => {
    props.onChange(entries.map((existing, position) => (position === index ? entry : existing)));
  };

  const removeAt = (index: number) => {
    props.onChange(entries.filter((_, position) => position !== index));
  };

  return (
    <FieldArray className={props.className}>
      {entries.map((entry, index) => (
        // Keyed by position: two entries may hold the same text, so nothing else identifies one.
        <div key={index} className="flex items-start gap-2">
          <TextInput
            className="w-full"
            value={entry}
            onChange={(changed) => replaceAt(index, changed ?? "")}
            maxLength={props.maxLength}
            placeholder={props.placeholder}
            disabled={props.disabled}
          />
          <FieldArrayRemoveButton onClick={() => removeAt(index)} label={t(textKeys.remove)} disabled={props.disabled} />
        </div>
      ))}
      <FieldArrayAddButton
        onClick={() => props.onChange([...entries, ""])}
        label={t(textKeys.add)}
        disabled={props.disabled || (props.maxEntries !== undefined && entries.length >= props.maxEntries)}
      />
    </FieldArray>
  );
}
