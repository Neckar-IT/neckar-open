import { isDefined } from "@neckar-hub/typescript-utils/lang";
import { cn } from "cn";
interface EMailProps {
  value: string | null | undefined;
  className?: string;
}

/**
 * Renders an email address as a mailto link, returns null if the value is not defined
 */
export function EMail({ value, className }: EMailProps): React.ReactNode {
  if (!isDefined(value)) {
    return null;
  }

  return (
    <a href={`mailto:${value}`} className={cn("text-primary hover:underline", className)}>
      {value}
    </a>
  );
}
