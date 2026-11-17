/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { InfoIcon, UndoIcon } from "lucide-react";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { Button } from "../../primitives/button";
import { NumberInput } from "./NumberInput";

/**
 * Meta Information
 */

const meta = {
  title: "Inputs / Number Input",
} satisfies Meta;

export default meta;

/**
 * Args
 */

interface Args {
  min: number;
  max: number;
  step: number;
  fractionDigits: number;
  showButtons: boolean;
  disabled: boolean;
  unit: string;
  unitPosition: "prefix" | "suffix";
  conversionFactor: number;
  hideClearButton: boolean;
}

/**
 * Stories
 */

export const Playground: StoryObj<Args> = {
  args: {
    min: 0,
    max: 10,
    step: 0.1,
    fractionDigits: 1,
    disabled: false,
    showButtons: false,
    unit: "",
    unitPosition: "suffix",
    conversionFactor: 1,
    hideClearButton: false,
  },
  argTypes: {
    unitPosition: {
      control: { type: "radio" },
      options: ["prefix", "suffix"],
    },
  },
  render: (args: Args) => {
    const [value, setValue] = useState<number | undefined>(4.2);

    return (
      <div className="flex w-80 flex-col gap-8">
        <NumberInput {...args} value={value} onChange={setValue} />
        <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};

const showButtonsOptions = [true, false];
const disabledOptions = [true, false];
const unitOptions = [
  { unit: "km", unitPosition: "suffix" as const },
  { unit: "$", unitPosition: "prefix" as const },
  { unit: undefined, unitPosition: undefined },
];
const isInvalidOptions = [true, false];

export const Variants: StoryObj<typeof meta> = {
  render: () => {
    const [value, setValue] = useState<number | undefined>(4.2);

    return (
      <div className="grid grid-cols-3 gap-16">
        {showButtonsOptions.map((showButtons) =>
          disabledOptions.map((disabled) =>
            unitOptions.map(({ unit, unitPosition }) =>
              isInvalidOptions.map((isInvalid) => (
                <div
                  key={`showButtons:${showButtons.toString()}-disabled:${disabled.toString()}-unit:${unit ?? "none"}-position:${unitPosition ?? "none"}-isInvalid:${isInvalid.toString()}`}
                >
                  <p className="text-muted-foreground bg-muted mb-2 rounded-md px-4 py-2 font-mono text-sm">
                    showButtons: {showButtons.toString()}
                    <br />
                    disabled: {disabled.toString()}
                    <br />
                    unit: {unit ?? "undefined"}
                    <br />
                    unitPosition: {unitPosition ?? "undefined"}
                    <br />
                    isInvalid: {isInvalid.toString()}
                  </p>
                  <NumberInput
                    value={value}
                    onChange={setValue}
                    showButtons={showButtons}
                    disabled={disabled}
                    unit={unit}
                    unitPosition={unitPosition}
                    isInvalid={isInvalid}
                  />
                </div>
              )),
            ),
          ),
        )}
      </div>
    );
  },
};

/**
 * Story to demonstrate text alignment behavior.
 *
 * Without +/- buttons, the text should be left-aligned (like a normal text input).
 * With +/- buttons, the text should be centered in the input field.
 */

export const TextAlignment: StoryObj<typeof meta> = {
  render: () => {
    const [value, setValue] = useState<number | undefined>(2026);

    return (
      <div className="flex flex-col gap-8">
        <div className="flex w-80 flex-col gap-2">
          <p className="text-muted-foreground font-mono text-sm">showButtons: false, no unit — should be left-aligned</p>
          <NumberInput value={value} onChange={setValue} />
        </div>

        <div className="flex w-80 flex-col gap-2">
          <p className="text-muted-foreground font-mono text-sm">showButtons: true, no unit — should be centered</p>
          <NumberInput value={value} onChange={setValue} showButtons />
        </div>

        <div className="flex w-80 flex-col gap-2">
          <p className="text-muted-foreground font-mono text-sm">showButtons: false, unit suffix — should be right-aligned</p>
          <NumberInput value={value} onChange={setValue} unit="km" unitPosition="suffix" />
        </div>

        <div className="flex w-80 flex-col gap-2">
          <p className="text-muted-foreground font-mono text-sm">showButtons: false, unit prefix — should be left-aligned</p>
          <NumberInput value={value} onChange={setValue} unit="$" unitPosition="prefix" />
        </div>
      </div>
    );
  },
};

/**
 * Story to demonstrate clear button layout shift.
 *
 * Enter or clear a value and observe whether the text position shifts
 * when the clear button appears/disappears.
 */

export const ClearButtonLayoutShift: StoryObj<typeof meta> = {
  render: () => {
    const [value1, setValue1] = useState<number | undefined>(undefined);
    const [value2, setValue2] = useState<number | undefined>(2026);

    return (
      <div className="flex flex-col gap-8">
        <div className="flex w-80 flex-col gap-2">
          <p className="text-muted-foreground font-mono text-sm">Empty → type a value → observe shift</p>
          <NumberInput value={value1} onChange={setValue1} useGrouping={false} />
          <CodeBlock>{`value: ${JSON.stringify(value1)}`}</CodeBlock>
        </div>

        <div className="flex w-80 flex-col gap-2">
          <p className="text-muted-foreground font-mono text-sm">Has value → clear → observe shift</p>
          <NumberInput value={value2} onChange={setValue2} useGrouping={false} />
          <CodeBlock>{`value: ${JSON.stringify(value2)}`}</CodeBlock>
        </div>

        <div className="flex w-80 flex-col gap-2">
          <p className="text-muted-foreground font-mono text-sm">With showButtons — plus button moves left when the clear button appears</p>
          <NumberInput value={value2} onChange={setValue2} showButtons useGrouping={false} />
        </div>
      </div>
    );
  },
};

/**
 * Story to demonstrate the order of add-ons and step buttons: addOnLeft, minus, input, plus, addOnRight.
 */

export const AddOns: StoryObj<typeof meta> = {
  render: () => {
    const [value, setValue] = useState<number | undefined>(4.2);

    return (
      <div className="flex w-80 flex-col gap-8">
        <NumberInput
          value={value}
          onChange={setValue}
          showButtons
          addOnLeft={
            <Button variant="ghost" size="icon" aria-label="Info" className="rounded-e-none">
              <InfoIcon aria-hidden className="text-muted-foreground" />
            </Button>
          }
          addOnRight={
            <Button variant="ghost" size="icon" aria-label="Undo" className="rounded-s-none">
              <UndoIcon aria-hidden />
            </Button>
          }
        />
        <NumberInput
          value={value}
          onChange={setValue}
          showButtons
          addOnLeft={
            <Button variant="ghost" size="icon" aria-label="Info" className="rounded-e-none">
              <InfoIcon aria-hidden className="text-muted-foreground" />
            </Button>
          }
        />
        <CodeBlock>{`value: ${JSON.stringify(value)}`}</CodeBlock>
      </div>
    );
  },
};
