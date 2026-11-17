import { cn } from "cn";
import { FormBody } from "./FormBody";
import { FormSectionHeader } from "./FormSectionHeader";

interface FormFieldsetProps extends React.PropsWithChildren {
  /**
   * The legend of the group: the name of the value the fields together make up.
   */
  legend: React.ReactNode;
  className?: string;
}

/**
 * A group of form fields that together enter one value of the record, such as the payment terms
 * of the quote settings. The border and the legend keep the fields of the value apart from the
 * record's own fields around them; the fields inside stack like every other [FormBody].
 */
export function FormFieldset({ legend, className, children }: FormFieldsetProps): React.ReactElement {
  return (
    <fieldset className={cn("rounded-md border p-4", className)}>
      <FormSectionHeader as="legend" className="px-1">
        {legend}
      </FormSectionHeader>
      <FormBody>{children}</FormBody>
    </fieldset>
  );
}
