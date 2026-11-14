import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "cn";

const badgeBarVariants = cva("flex min-w-0 items-center gap-2", {
  variants: {
    alignment: {
      left: "justify-start",
      center: "justify-center",
      right: "justify-end",
    },
  },
  defaultVariants: {
    alignment: "left",
  },
});

interface BadgeBarProps extends React.PropsWithChildren, VariantProps<typeof badgeBarVariants> {
  className?: string;
}

/**
 * Horizontal bar laying out badges with configurable alignment; it shrinks below its content inside a flex cell, so a text item in it can truncate.
 */
export function BadgeBar({ children, className, alignment }: BadgeBarProps): React.ReactElement {
  return <div className={cn(badgeBarVariants({ alignment, className }))}>{children}</div>;
}
