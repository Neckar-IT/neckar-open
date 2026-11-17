/**
 * Single-thumb slider for selecting one numeric value along a track.
 */

import { Slider as ShadcnSlider } from "@neckar-hub/typescript-react-ui-primitives/ui/slider";
import { isDefined } from "@neckar-hub/typescript-utils/lang";

type SliderProps = Omit<React.ComponentProps<typeof ShadcnSlider>, "defaultValue" | "value" | "onValueChange" | "onValueCommit"> & {
  defaultValue?: number | undefined;
  value?: number | undefined;
  onValueChange?: (value: number) => void;
  onValueCommit?: (value: number) => void;
};

/**
 * The slider renders exactly one thumb, so Radix always reports exactly one value.
 */
function singleValueOf(values: number[]): number {
  const [singleValue] = values;

  if (singleValue === undefined) {
    throw new Error("Slider reported an empty value array");
  }

  return singleValue;
}

// Single number instead of the primitive's `number[]`; the one-element array also keeps the primitive from drawing a second
// thumb, which it does at `min` and `max` without a value.
export function Slider({ defaultValue, value, onValueChange, onValueCommit, min = 0, ...props }: SliderProps): React.ReactElement {
  return (
    <ShadcnSlider
      min={min}
      {...props}
      defaultValue={isDefined(value) ? undefined : [defaultValue ?? min]}
      value={isDefined(value) ? [value] : undefined}
      onValueChange={(values) => onValueChange?.(singleValueOf(values))}
      onValueCommit={(values) => onValueCommit?.(singleValueOf(values))}
    />
  );
}
