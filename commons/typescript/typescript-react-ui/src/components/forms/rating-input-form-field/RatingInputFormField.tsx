import { cn } from "cn";
import { StarIcon } from "lucide-react";
import { useRef, useState } from "react";
import type { ZodObject } from "zod";
import { FormFieldBase, type FormFieldBaseProps } from "../form";

interface RatingInputFormFieldProps<T extends ZodObject> extends FormFieldBaseProps<T> {
  /**
   * Maximum rating value (number of stars). Defaults to 5.
   */
  maxRating?: number;
}

/**
 * A star-rating form field for selecting an integer rating (1–N).
 *
 * Renders clickable star icons with hover preview and keyboard navigation.
 * Integrates with react-hook-form via FormFieldBase.
 */
export function RatingInputFormField<T extends ZodObject>({ maxRating = 5, ...rest }: RatingInputFormFieldProps<T>) {
  const [hoveredValue, setHoveredValue] = useState<number | null>(null);
  const buttonRefs = useRef<(HTMLButtonElement | null)[]>([]);

  return (
    <FormFieldBase
      {...rest}
      render={({ field: { onChange, value, disabled, ref, name: _name, onBlur } }) => {
        const currentValue = (value as number | undefined) ?? 0;
        const displayValue = hoveredValue ?? currentValue;

        return (
          <div
            className="inline-flex gap-1"
            role="radiogroup"
            tabIndex={-1}
            aria-label={typeof rest.label === "string" ? rest.label : undefined}
            onMouseLeave={() => setHoveredValue(null)}
          >
            {Array.from({ length: maxRating }, (_, i) => {
              const starValue = i + 1;
              const isFilled = starValue <= displayValue;
              const isSelected = starValue === currentValue;

              return (
                <button
                  key={starValue}
                  type="button"
                  role="radio"
                  aria-checked={isSelected}
                  aria-label={`${starValue} / ${maxRating}`}
                  disabled={disabled}
                  tabIndex={isSelected || (currentValue === 0 && starValue === 1) ? 0 : -1}
                  ref={(el) => {
                    buttonRefs.current[i] = el;
                    // Forward react-hook-form ref to the selected (or first) button
                    if (isSelected || (currentValue === 0 && starValue === 1)) {
                      if (typeof ref === "function") {
                        ref(el);
                      }
                    }
                  }}
                  onBlur={onBlur}
                  className={cn(
                    "focus-visible:ring-ring cursor-pointer rounded-sm p-0.5 transition-colors focus-visible:ring-2 focus-visible:outline-none",
                    disabled && "cursor-not-allowed opacity-50",
                  )}
                  onClick={() => {
                    onChange(starValue);
                    buttonRefs.current[i]?.focus();
                  }}
                  onMouseEnter={() => setHoveredValue(starValue)}
                  onKeyDown={(e) => {
                    if (e.key === "ArrowRight" || e.key === "ArrowUp") {
                      e.preventDefault();
                      const nextValue = Math.min(currentValue + 1, maxRating);
                      onChange(nextValue);
                      buttonRefs.current[nextValue - 1]?.focus();
                    } else if (e.key === "ArrowLeft" || e.key === "ArrowDown") {
                      e.preventDefault();
                      const prevValue = Math.max(currentValue - 1, 1);
                      onChange(prevValue);
                      buttonRefs.current[prevValue - 1]?.focus();
                    }
                  }}
                >
                  <StarIcon
                    className={cn(
                      "size-6 transition-colors",
                      isFilled ? "fill-yellow-400 text-yellow-400" : "text-muted-foreground/30",
                      hoveredValue !== null && isFilled && "fill-yellow-300 text-yellow-300",
                    )}
                  />
                </button>
              );
            })}
          </div>
        );
      }}
    />
  );
}
