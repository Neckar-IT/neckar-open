import { Alert as ShadcnAlert } from "@neckar-hub/typescript-react-ui-primitives/ui/alert";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "cn";

export { AlertDescription, AlertTitle } from "@neckar-hub/typescript-react-ui-primitives/ui/alert";

/**
 * Adds `info` (a hint in the primary color) and `muted` (a secondary remark) to the primitive's variants.
 */
const ownAlertVariants = cva("", {
  variants: {
    variant: {
      default: "",
      destructive: "",
      info: "border-primary/50 text-primary dark:border-primary *:data-[slot=alert-description]:text-primary/90",
      muted: "bg-muted/50 text-muted-foreground",
    },
  },
});

type AlertProps = Omit<React.ComponentProps<typeof ShadcnAlert>, "variant"> & VariantProps<typeof ownAlertVariants>;

/**
 * Inline alert/callout box with severity variants.
 */
export function Alert({ className, variant, ...props }: AlertProps): React.ReactElement {
  return (
    <ShadcnAlert
      variant={variant === "destructive" ? "destructive" : "default"}
      className={cn(ownAlertVariants({ variant }), className)}
      {...props}
    />
  );
}
