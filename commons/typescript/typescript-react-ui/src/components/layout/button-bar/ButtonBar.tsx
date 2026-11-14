import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "cn";

const buttonBarVariants = cva("flex items-center gap-2", {
  variants: {
    alignment: {
      left: "justify-start",
      center: "justify-center",
      right: "justify-end",
    },
  },
  defaultVariants: {
    alignment: "right",
  },
});

interface ButtonBarProps extends React.PropsWithChildren, VariantProps<typeof buttonBarVariants> {
  className?: string;
}

/**
 * Horizontal bar laying out buttons with configurable alignment.
 */
export function ButtonBar({ children, className, alignment }: ButtonBarProps): React.ReactElement {
  return <div className={cn(buttonBarVariants({ alignment }), className)}>{children}</div>;
}
