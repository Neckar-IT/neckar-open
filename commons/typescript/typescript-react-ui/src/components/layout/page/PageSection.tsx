import { cn } from "cn";

interface PageSectionProps extends React.PropsWithChildren {
  className?: string;
}

export function PageSection({ className, children }: PageSectionProps): React.ReactElement {
  return <section className={cn("flex flex-col gap-2", className)}>{children}</section>;
}
