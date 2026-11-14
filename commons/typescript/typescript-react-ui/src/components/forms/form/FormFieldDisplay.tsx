import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import { useTranslation } from "@neckar-hub/typescript-react-i18n";
import { cn } from "cn";
import { Label } from "../../primitives/label";
import { FormItem } from "./Form";

interface FormFieldDisplayProps {
  /**
   * Label TextKey. Resolved through the i18n translator. Pass `labelComponent` instead
   * to render a custom label.
   */
  label?: TextKey;
  labelComponent?: React.ReactNode;
  /**
   * Optional description rendered below the value, in the same style as a
   * FormFieldAnnotation description.
   */
  description?: TextKey;
  descriptionComponent?: React.ReactNode;
  children: React.ReactNode;
  className?: string;
}

/**
 * Read-only row inside a FormCardContent.
 *
 * Renders the same `<Label>` + value + description rhythm as a `FormFieldBase` row,
 * without binding to react-hook-form. Use when the displayed value is part of the
 * calling context (URL, parent route, fixed by the surrounding workflow) and therefore
 * not editable on this page. The editable counterpart typically lives on a different
 * route or in a different mode.
 *
 * Place inline in a `FormCardContent` next to editable fields — the `text-sm` set on
 * `FormCardContent` makes the value visually align with the surrounding inputs.
 */
export function FormFieldDisplay({
  label,
  labelComponent,
  description,
  descriptionComponent,
  children,
  className,
}: FormFieldDisplayProps): React.ReactElement {
  const { t } = useTranslation();
  return (
    <FormItem className={cn(className)}>
      {label !== undefined ? <Label>{t(label)}</Label> : labelComponent}
      <div>{children}</div>
      {description !== undefined ? <p className="text-muted-foreground text-sm">{t(description)}</p> : descriptionComponent}
    </FormItem>
  );
}
