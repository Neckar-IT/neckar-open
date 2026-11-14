/**
 * Imports
 */

import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import type { Tag } from "@neckar-hub/typescript-utils/openapi-types";
import { cn } from "cn";
import { CheckIcon, ChevronDownIcon, MinusIcon, PlusIcon, XIcon } from "lucide-react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { Badge } from "../../primitives/badge";
import { Button } from "../../primitives/button";
import { Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList } from "../../primitives/command";
import { Popover, PopoverContent, PopoverTrigger } from "../../primitives/popover";

/**
 * Prop Types
 */

interface TagPickerTextKeys {
  placeholder: TextKey;
  search: TextKey;
  empty: TextKey;
}

export interface TagPickerProps {
  options: Tag[];
  value: Tag[] | undefined | null;
  onChange: (value: Tag[] | null) => void;
  className?: string;
  disabled?: boolean;
  textKeys?: Partial<TagPickerTextKeys>;
}

/**
 * Multi-select popover for picking tags from a list, showing selected tags as removable badges.
 */

export function TagPicker(props: TagPickerProps): React.ReactElement {
  const selectedTagIds = new Set<string>(props.value ? props.value.map((tag) => tag.id) : []);

  const { t } = useEmbeddedTranslation();
  const textKeysWithDefaults: TagPickerTextKeys = {
    placeholder: props.textKeys?.placeholder ?? "tagPicker.placeholder",
    search: props.textKeys?.search ?? "tagPicker.search",
    empty: props.textKeys?.empty ?? "tagPicker.empty",
  };

  const handleSelect = (tag: Tag) => {
    if (selectedTagIds.has(tag.id)) {
      const newValue = props.value?.filter((t) => t.id !== tag.id) ?? null;

      if (newValue?.length === 0) {
        props.onChange(null);
      } else {
        props.onChange(newValue);
      }
    } else {
      props.onChange([...(props.value ?? []), tag]);
    }
  };

  return (
    <Popover>
      <PopoverTrigger asChild>
        <div className={cn("inline-flex min-h-10 items-stretch justify-between gap-2 rounded-md border", props.className)}>
          {props.value && props.value.length > 0 ? (
            <div className="flex flex-wrap gap-1 py-2 pl-4">
              {props.value.map((tag) => (
                <Badge key={tag.id} className="flex items-center gap-1" variant="outline">
                  <span className={cn({ "text-muted-foreground": props.disabled })}>{tag.name}</span>
                  <Button
                    className="h-3.5 w-3.5 p-0"
                    disabled={props.disabled}
                    type="button"
                    variant="ghost"
                    onClick={(event) => {
                      handleSelect(tag);
                      event.stopPropagation();
                    }}
                  >
                    <XIcon className="h-3.5 w-3.5" />
                  </Button>
                </Badge>
              ))}
            </div>
          ) : null}
          <Button
            className={cn("flex h-auto justify-between gap-4", { "w-full": selectedTagIds.size === 0 })}
            disabled={props.disabled}
            variant="ghost"
          >
            {selectedTagIds.size === 0 ? (
              <span className="text-muted-foreground font-normal">{t(textKeysWithDefaults.placeholder)}</span>
            ) : null}
            <ChevronDownIcon className="text-muted-foreground" />
          </Button>
        </div>
      </PopoverTrigger>
      <PopoverContent align="start" className="min-w-56 p-0" style={{ width: "var(--radix-popover-trigger-width)" }}>
        <Command>
          <CommandInput placeholder={t(textKeysWithDefaults.search)} />
          <CommandList>
            <CommandEmpty>{t(textKeysWithDefaults.empty)}</CommandEmpty>
            <CommandGroup>
              {props.options.map((option) => {
                const isSelected = selectedTagIds.has(option.id);

                return (
                  <CommandItem key={option.id} className="group hover:cursor-pointer" onSelect={() => handleSelect(option)}>
                    <div
                      className={cn("mr-2 opacity-0 group-data-[selected=true]:opacity-100", {
                        "opacity-100": isSelected,
                      })}
                    >
                      {isSelected ? (
                        <>
                          <MinusIcon className="hidden h-4 w-4 group-data-[selected=true]:block" />
                          <CheckIcon className="block h-4 w-4 group-data-[selected=true]:hidden" />
                        </>
                      ) : (
                        <PlusIcon className="h-4 w-4" />
                      )}
                    </div>
                    <Badge variant="outline">{option.name}</Badge>
                  </CommandItem>
                );
              })}
            </CommandGroup>
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  );
}
