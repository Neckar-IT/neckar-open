import { cn } from "cn";
import type { ZodObject } from "zod";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { ToggleGroup, ToggleGroupItem } from "../../primitives/toggle-group";
import { FormFieldBase, type FormFieldBaseProps } from "../form";

type NullableBooleanItem = "yes" | "no" | "empty";

/** `""` presses no item: a form without a start value holds `undefined`, which the schema refuses. */
function toItem(value: unknown): NullableBooleanItem | "" {
  if (value === true) {
    return "yes";
  }
  if (value === false) {
    return "no";
  }
  if (value === null) {
    return "empty";
  }
  return "";
}

const itemValues: Record<NullableBooleanItem, boolean | null> = {
  yes: true,
  no: false,
  empty: null,
};

/**
 * Yes, no or empty for a `boolean | null` form field; each state is reachable from each other one.
 *
 * Once pressed, one item stays pressed: a click on the pressed item writes nothing, and `empty` writes `null`.
 */
export function NullableBooleanFormField<T extends ZodObject>({
  className,
  ...props
}: FormFieldBaseProps<T, boolean | null>): React.ReactElement {
  const { t } = useEmbeddedTranslation();

  return (
    <FormFieldBase
      {...props}
      className={cn("gap-2", className)}
      render={({ field: { onChange, value, ...fieldRest } }) => (
        <ToggleGroup
          type="single"
          variant="outline"
          className="justify-start"
          value={toItem(value)}
          onValueChange={(newValue) => {
            if (Object.hasOwn(itemValues, newValue)) {
              onChange(itemValues[newValue as NullableBooleanItem]);
            }
          }}
          {...fieldRest}
        >
          <ToggleGroupItem value="yes" className="px-4">
            {t("nullableBooleanFormField.yes")}
          </ToggleGroupItem>
          <ToggleGroupItem value="no" className="px-4">
            {t("nullableBooleanFormField.no")}
          </ToggleGroupItem>
          <ToggleGroupItem value="empty" className="px-4">
            {t("nullableBooleanFormField.empty")}
          </ToggleGroupItem>
        </ToggleGroup>
      )}
    />
  );
}
