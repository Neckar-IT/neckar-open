import { cn } from "cn";

interface PageSectionHeaderProps extends React.PropsWithChildren {
  className?: string;
}

export function PageSectionHeader({ className, children }: PageSectionHeaderProps): React.ReactElement {
  return (
    <header
      className={cn(
        "flex flex-col justify-between gap-2 md:flex-row md:items-center md:gap-8 [&>:first-child]:min-w-0 [&>:not(:first-child)]:shrink-0",
        className,
      )}
    >
      {children}
    </header>
  );
}
