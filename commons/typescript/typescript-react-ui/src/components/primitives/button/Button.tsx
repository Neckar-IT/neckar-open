/**
 * Clickable button with multiple style variants, sizes, and an optional built-in loading spinner.
 */

import { Button as ShadcnButton, buttonVariants as shadcnButtonVariants } from "@neckar-hub/typescript-react-ui-primitives/ui/button";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "cn";
import { Loader2Icon } from "lucide-react";
import { Slot } from "radix-ui";
import { cloneElement, isValidElement } from "react";

type ShadcnButtonVariant = NonNullable<VariantProps<typeof shadcnButtonVariants>["variant"]>;

/**
 * Classes on top of the primitive's variants:
 * - `confirm` saves a confirmed state in the `confirmed` color, next to the primitive's variants
 * - `secondary` hovers at `/60` of its color, the hover the product forms were designed with
 */
const ownButtonVariants = cva("", {
  variants: {
    variant: {
      default: "",
      confirm: "bg-confirmed text-confirmed-foreground hover:bg-confirmed/80",
      destructive: "",
      outline: "",
      secondary: "hover:bg-secondary/60",
      ghost: "",
      link: "",
    } satisfies Record<ShadcnButtonVariant | "confirm", string>,
  },
});

type ButtonVariantProps = Omit<VariantProps<typeof shadcnButtonVariants>, "variant"> & VariantProps<typeof ownButtonVariants>;

/**
 * The primitive's variant under the own one: `confirm` builds on the primitive's `default` and recolors it.
 */
function shadcnVariantOf(variant: ButtonVariantProps["variant"]): ShadcnButtonVariant | null | undefined {
  return variant === "confirm" ? "default" : variant;
}

/**
 * Class string of a button in the given variant and size, for elements styled as a button (links, triggers).
 */
export function buttonVariants({ variant, size, className }: ButtonVariantProps & { className?: string | undefined } = {}): string {
  return cn(shadcnButtonVariants({ variant: shadcnVariantOf(variant), size }), ownButtonVariants({ variant }), className);
}

export interface ButtonProps extends Omit<React.ComponentProps<typeof ShadcnButton>, "variant">, VariantProps<typeof ownButtonVariants> {
  loading?: boolean;
  /**
   * By default every icon is set to size-4. Set this prop to true to size an icon by its own className.
   */
  customIconSize?: boolean;
}

// Adds the loading spinner, `type="button"` against accidental form submits, and truncated string children.
// `Slot.Slottable` marks the content as the child `asChild` merges into; without it the spinner makes `Slot.Root` throw.
// While loading, the content sits in an invisible `contents` wrapper: `visibility` also hides text nodes, `contents` keeps the layout.
export function Button({
  className,
  variant = "default",
  size,
  type = "button",
  loading = false,
  children,
  disabled,
  customIconSize = false,
  asChild,
  ...props
}: ButtonProps): React.ReactElement {
  const wrapContent = (content: React.ReactNode): React.ReactElement => (
    <span className={cn("contents", { invisible: loading })}>
      {typeof content === "string" ? <span className="truncate">{content}</span> : content}
    </span>
  );

  // With `asChild` the wrapper goes into the child element, so that the text of an `<a>` is hidden too.
  const slottedContent =
    asChild && isValidElement<React.PropsWithChildren>(children)
      ? cloneElement(children, undefined, wrapContent(children.props.children))
      : wrapContent(children);

  return (
    <ShadcnButton
      asChild={asChild}
      variant={shadcnVariantOf(variant)}
      // The own variant, so `data-variant` names `confirm` rather than the primitive variant it builds on.
      data-variant={variant}
      size={size}
      className={cn(
        ownButtonVariants({ variant }),
        {
          relative: loading,
          "[&_svg]:size-4": !customIconSize,
        },
        className,
      )}
      disabled={loading || disabled}
      type={type}
      {...props}
    >
      <Slot.Slottable>{slottedContent}</Slot.Slottable>
      <div className={cn("absolute inset-0 flex items-center justify-center", { hidden: !loading })}>
        <Loader2Icon className="size-5! animate-spin" />
      </div>
    </ShadcnButton>
  );
}
