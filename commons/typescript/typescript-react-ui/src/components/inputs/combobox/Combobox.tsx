/* eslint-disable @typescript-eslint/no-explicit-any */

/**
 * Imports
 */

import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import { cn } from "cn";
import { CheckIcon, ChevronRightIcon, ChevronsUpDownIcon, XIcon } from "lucide-react";
import { useRef, useState } from "react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { Empty } from "../../feedback/empty";
import { Button } from "../../primitives/button";
import { Command, CommandGroup, CommandInput, CommandItem, CommandList } from "../../primitives/command";
import { Popover, PopoverContent, PopoverTrigger } from "../../primitives/popover";
import { ScrollArea } from "../../primitives/scroll-area";

/**
 * Prop Types
 */

export interface ComboboxProps<T> {
  variant?: "flat" | "popover";
  className?: string | undefined;
  items: T[];
  filter: string | undefined;
  onFilterChange: (filter: string) => void;
  selectedItem: T | undefined;
  setSelectedItem: (item: T | undefined) => void;
  itemRenderer: (item: T) => React.ReactNode;
  disabled?: boolean | undefined;
  bottomOfTheList?: React.ReactNode | undefined;
  textKeys?: {
    placeholder?: TextKey;
    search?: TextKey;
    empty?: TextKey;
  };
}

/**
 * Searchable single-select dropdown with flat (inline) and popover variants, supporting custom item rendering.
 *
 * @prop {flat | popover} [variant] - The variant of the combobox
 * @prop {T} [items] - The items to show in the combobox
 * @prop {string} [filter] - The filter value
 * @prop {(filter: string) => void} [onFilterChange] - The filter change handler
 * @prop {T | undefined} [selectedItem] - The selected item
 * @prop {(item: T | undefined) => void} [setSelectedItem] - The selected item change handler
 * @prop {(item: T) => React.ReactNode} [itemRenderer] - How to render each item in the list
 */

export function Combobox<T extends string | Record<string, any>>(props: ComboboxProps<T>): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const textKeysWithDefaults = {
    placeholder: props.textKeys?.placeholder ?? "combobox.placeholder",
    search: props.textKeys?.search ?? "combobox.search",
  };

  const [popoverIsOpen, setPopoverIsOpen] = useState(false);

  const inputRef = useRef<HTMLInputElement>(null);

  const handleDeselect = () => {
    props.setSelectedItem(undefined);
    inputRef.current?.focus();
  };

  const handleSelect = (item: T) => {
    if (props.selectedItem === item) {
      handleDeselect();
    } else {
      props.setSelectedItem(item);
      setPopoverIsOpen(false);
    }
  };

  switch (props.variant) {
    case "flat":
      return (
        <Command className={cn("relative rounded-lg border", props.className)} shouldFilter={false}>
          <CommandInput
            ref={inputRef}
            disabled={props.disabled}
            placeholder={t(textKeysWithDefaults.search)}
            value={props.filter}
            onValueChange={props.onFilterChange}
          />

          {props.selectedItem !== undefined ? (
            <div className="absolute top-0 right-0 left-0 border-b py-1.5 pr-4 pl-1 [&_div]:border-b-0">
              <button
                className="bg-accent flex w-full items-center rounded-sm px-2 py-1.5 text-sm"
                disabled={props.disabled}
                type="button"
                onClick={handleDeselect}
              >
                <div className="mr-2">
                  <XIcon className="h-4 w-4" />
                </div>
                <div className="flex-1 font-semibold">{props.itemRenderer(props.selectedItem)}</div>
              </button>
            </div>
          ) : null}

          <ComboboxOptions
            bottomOfTheList={props.bottomOfTheList}
            disabled={props.disabled}
            itemRenderer={props.itemRenderer}
            items={props.items}
            selectedItem={props.selectedItem}
            onSelect={handleSelect}
            textKeys={props.textKeys}
          />
        </Command>
      );

    case "popover":
    case undefined:
      return (
        <Popover open={props.disabled ? false : popoverIsOpen} onOpenChange={setPopoverIsOpen}>
          <PopoverTrigger asChild>
            {props.selectedItem !== undefined ? (
              <div className={cn("flex h-10 items-center rounded-lg border pr-2.5 pl-1", props.className)}>
                <div className="mr-2 w-full py-1.5">
                  <div className="bg-accent flex w-full items-center rounded-sm px-3 py-1.5 text-sm">
                    <div className="flex-1 font-semibold">{props.itemRenderer(props.selectedItem)}</div>
                  </div>
                </div>
                <Button
                  className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full p-0"
                  disabled={props.disabled}
                  type="button"
                  variant="ghost"
                  onClick={handleDeselect}
                >
                  <XIcon className="h-4 w-4" />
                </Button>
                <Button
                  className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full p-0"
                  disabled={props.disabled}
                  type="button"
                  variant="ghost"
                  onClick={() => setPopoverIsOpen(true)}
                >
                  <ChevronsUpDownIcon className="h-4 w-4" />
                </Button>
              </div>
            ) : (
              <Button className={cn("justify-between", props.className)} disabled={props.disabled} role="combobox" variant="outline">
                <span className="text-muted-foreground font-normal">{t(textKeysWithDefaults.placeholder)}</span>
                <ChevronsUpDownIcon className="ml-4 h-4 w-4 shrink-0" />
              </Button>
            )}
          </PopoverTrigger>
          <PopoverContent className="p-0" style={{ width: "var(--radix-popover-trigger-width)" }}>
            <Command className="relative" shouldFilter={false}>
              <CommandInput
                ref={inputRef}
                placeholder={t(textKeysWithDefaults.search)}
                value={props.filter}
                onValueChange={props.onFilterChange}
              />

              <ComboboxOptions
                bottomOfTheList={props.bottomOfTheList}
                disabled={props.disabled}
                itemRenderer={props.itemRenderer}
                items={props.items}
                selectedItem={props.selectedItem}
                onSelect={handleSelect}
                textKeys={props.textKeys}
              />
            </Command>
          </PopoverContent>
        </Popover>
      );
    default:
      throw new Error(`Unexpected value in Switch Statement: props.variant`);
  }
}

/**
 * Options Component
 */

interface ComboboxOptionsProps<T> {
  items: T[];
  selectedItem: T | undefined;
  itemRenderer: (item: T) => React.ReactNode;
  onSelect: (item: T) => void;
  disabled?: boolean | undefined;
  bottomOfTheList?: React.ReactNode | undefined;
  textKeys?: {
    empty?: TextKey;
  };
}

function ComboboxOptions<T extends string | Record<string, any>>(props: ComboboxOptionsProps<T>): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const textKeysWithDefaults = {
    empty: props.textKeys?.empty ?? "combobox.empty",
  };

  return (
    <CommandList>
      <CommandGroup>
        {props.items.length > 0 ? (
          <>
            <ScrollArea className="h-56 pr-3" type="auto">
              {props.items.map((item, index) => {
                const isSelected = props.selectedItem === item;

                return (
                  <CommandItem
                    key={index}
                    className="group hover:cursor-pointer"
                    disabled={props.disabled}
                    onSelect={() => props.onSelect(item)}
                  >
                    <div
                      className={cn("mr-2 opacity-0 group-data-[selected=true]:opacity-100", {
                        "opacity-100": isSelected,
                      })}
                    >
                      {isSelected ? (
                        <>
                          <XIcon className="hidden h-4 w-4 group-data-[selected=true]:block" />
                          <CheckIcon className="block h-4 w-4 group-data-[selected=true]:hidden" />
                        </>
                      ) : (
                        <ChevronRightIcon className="h-4 w-4" />
                      )}
                    </div>
                    <div className={cn("flex-1", { "font-semibold": isSelected })}>{props.itemRenderer(item)}</div>
                  </CommandItem>
                );
              })}
              {props.bottomOfTheList ?? null}
            </ScrollArea>
          </>
        ) : (
          <div className="flex h-56 items-center justify-center">
            <div className="flex flex-col items-center">
              <Empty className="h-16 w-16" />
              <span className="text-muted-foreground text-sm">{t(textKeysWithDefaults.empty)}</span>
            </div>
          </div>
        )}
      </CommandGroup>
    </CommandList>
  );
}
