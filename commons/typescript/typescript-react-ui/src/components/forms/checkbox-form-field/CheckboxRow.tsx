import { Checkbox } from "../../primitives/checkbox";
import { FormControl, FormFieldAnnotation, type FormFieldAnnotationProps, FormFieldLabel, type FormFieldLabelProps } from "../form";

interface CheckboxRowProps {
  checked: boolean | undefined;
  disabled?: boolean;
  name: string;
  onBlur: () => void;
  onCheckedChange: (checked: boolean) => void;
  label?: FormFieldLabelProps["label"];
  labelComponent?: FormFieldLabelProps["labelComponent"];
  description?: FormFieldAnnotationProps["description"];
  descriptionComponent?: FormFieldAnnotationProps["descriptionComponent"];
}

/**
 * The box and the label beside it, the one line every checkbox field is read on.
 *
 * The indent {@link CheckboxDependentFields} applies is measured against this layout, which is why
 * the three components live together: a change to the box's size moves that indent with it.
 */
export function CheckboxRow({
  checked,
  disabled,
  name,
  onBlur,
  onCheckedChange,
  label,
  labelComponent,
  description,
  descriptionComponent,
}: CheckboxRowProps): React.ReactElement {
  return (
    <div className="flex items-start gap-2">
      <FormControl>
        <Checkbox
          checked={checked}
          disabled={disabled}
          name={name}
          onBlur={onBlur}
          onCheckedChange={(state) => onCheckedChange(state === true)}
        />
      </FormControl>
      <div className="mt-px">
        <FormFieldLabel disabled={disabled} label={label} labelComponent={labelComponent} />
        <FormFieldAnnotation disabled={disabled} description={description} descriptionComponent={descriptionComponent} className="mt-0.5" />
      </div>
    </div>
  );
}
