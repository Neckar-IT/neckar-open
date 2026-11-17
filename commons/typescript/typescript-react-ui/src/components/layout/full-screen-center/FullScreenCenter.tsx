import { cn } from "cn";

interface FullScreenCenterProps extends React.PropsWithChildren {
  className?: string;
  "data-testid"?: string;
}

/**
 * Screen-height area centering its content - the layout of every screen that stands in for the
 * application: auth loading and error states, login and registration cards.
 */
export function FullScreenCenter({ children, className, "data-testid": dataTestId }: FullScreenCenterProps): React.ReactElement {
  return (
    <div className={cn("flex h-screen items-center justify-center", className)} data-testid={dataTestId}>
      {children}
    </div>
  );
}
