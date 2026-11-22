import { cn } from "cn";

interface PageSectionHeadingProps extends React.PropsWithChildren {
  className?: string;

  /**
   * Defines the element and the size of the heading.
   * `primary` for the first heading on a page (i.e. page title)
   * `secondary` for other sections
   */
  level: "primary" | "secondary";
}

/**
 * Displays a section heading whose font size steps up at the `sm` breakpoint (640 px):
 * `primary` 24 px below `sm` and 30 px from `sm`, `secondary` 20 px below `sm` and 24 px from `sm`.
 */
export function PageSectionHeading({ className, children, level }: PageSectionHeadingProps): React.ReactElement {
  switch (level) {
    case "primary":
      return <h1 className={cn("text-2xl font-bold sm:text-3xl", className)}>{children}</h1>;
    case "secondary":
      return <h2 className={cn("text-xl font-semibold sm:text-2xl", className)}>{children}</h2>;
    default:
      throw new Error(`Unexpected value in Switch Statement: level`);
  }
}
