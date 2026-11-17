/**
 * Imports
 */

import { cva } from "class-variance-authority";
import { cn } from "cn";

/**
 * Variants
 */

const optionContainerVariants = cva("bg-card text-card-foreground rounded-xl border", {
  variants: {
    variant: {
      default: null,
      button: "hover:bg-muted w-full text-left hover:cursor-pointer hover:shadow",
      radio: "hover:bg-muted w-full text-left hover:cursor-pointer",
    },
    checked: {
      false: null,
      true: null,
    },
    disabled: {
      false: null,
      true: "pointer-events-none opacity-50",
    },
  },
  compoundVariants: [
    {
      variant: "radio",
      checked: true,
      className: "border-primary ring-primary/30 shadow-none ring-2",
    },
  ],
  defaultVariants: {
    variant: "default",
    disabled: false,
  },
});

/**
 * Prop Types
 */

export type OptionContainerProps =
  | ({ variant: "default" } & React.HTMLAttributes<HTMLDivElement>)
  | ({ variant: "button" } & React.ButtonHTMLAttributes<HTMLButtonElement>)
  | ({ variant: "radio"; checked: boolean } & React.ButtonHTMLAttributes<HTMLButtonElement>);

/**
 * Component to display some option either as a plain div, a button or a radio
 */

export function OptionContainer(props: OptionContainerProps): React.ReactElement {
  switch (props.variant) {
    case "default":
      return <div {...props} className={cn(optionContainerVariants({ variant: props.variant, className: props.className }))} />;

    case "button":
      return (
        <button
          {...props}
          className={cn(optionContainerVariants({ variant: props.variant, disabled: props.disabled, className: props.className }))}
        />
      );

    case "radio":
      return (
        <button
          {...props}
          className={cn(
            optionContainerVariants({
              variant: props.variant,
              checked: props.checked,
              disabled: props.disabled,
              className: props.className,
            }),
          )}
        />
      );
    default:
      throw new Error(`Unexpected value in Switch Statement: props.variant`);
  }
}
