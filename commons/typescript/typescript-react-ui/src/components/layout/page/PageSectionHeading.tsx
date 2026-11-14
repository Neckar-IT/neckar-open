import { cn } from "cn";

interface PageSectionHeadingProps extends React.PropsWithChildren {
  className?: string;

  /**
   * Defines the size of the heading.
   * `primary` for the first heading on a page (i.e. page title)
   * `secondary` for other sections
   */
  level: "primary" | "secondary";
}

/**
 * Displays a section heading with text truncation for long content.
 */
export function PageSectionHeading({ className, children, level }: PageSectionHeadingProps): React.ReactElement {
  switch (level) {
    case "primary":
      return <h1 className={cn("text-3xl font-bold", className)}>{children}</h1>;
    case "secondary":
      return <h2 className={cn("text-2xl font-semibold", className)}>{children}</h2>;
    default:
      throw new Error(`Unexpected value in Switch Statement: level`);
  }
}
