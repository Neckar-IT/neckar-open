import { cn } from "cn";
import { OrNoValue } from "../../text/no-value";

/**
 * Definition list (`<dl>`) for label/value pairs.
 */
export function DescriptionList({ className, children, ...rest }: React.ComponentProps<"dl">): React.ReactElement {
  return (
    <dl className={cn("@container", className)} {...rest}>
      <div className="grid grid-cols-1 @3xl:grid-cols-[max-content_1fr]">{children}</div>
    </dl>
  );
}

export function DescriptionListRow({ className, ...rest }: React.ComponentProps<"div">): React.ReactElement {
  return (
    <div
      className={cn(
        "border-border/40 border-b py-3 first:pt-0 last:border-b-0 last:pb-0 @3xl:col-span-2 @3xl:grid @3xl:grid-cols-subgrid @3xl:items-baseline @3xl:gap-x-8",
        className,
      )}
      {...rest}
    />
  );
}

export function DescriptionListLabel({ className, ...rest }: React.ComponentProps<"dt">): React.ReactElement {
  return <dt className={cn("text-muted-foreground mb-1 text-xs tracking-tight uppercase @3xl:mb-0", className)} {...rest} />;
}

export function DescriptionListValue({
  className,
  fallback,
  children,
  ...rest
}: React.ComponentProps<"dd"> & { fallback?: React.ReactNode }): React.ReactElement {
  return (
    <dd className={cn("text-sm font-medium", className)} {...rest}>
      <OrNoValue fallback={fallback}>{children}</OrNoValue>
    </dd>
  );
}

/**
 * The several values of one [DescriptionListValue], one per line.
 *
 * A row that holds a list — the contact persons of a publisher, the inverter manufacturers a device
 * is compatible with — puts each entry on its own line rather than running them together in one.
 * Render nothing for an empty list, so [DescriptionListValue] shows its no-value fallback.
 */
export function DescriptionListValueStack({ className, ...rest }: React.ComponentProps<"div">): React.ReactElement {
  return <div className={cn("flex flex-col gap-1", className)} {...rest} />;
}
