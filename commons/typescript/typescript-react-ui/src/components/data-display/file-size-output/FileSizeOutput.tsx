/**
 * Imports
 */

import { useFileSizeFormatter } from "@neckar-hub/typescript-react-i18n";
import { isNotDefined } from "@neckar-hub/typescript-utils/lang";
import { cn } from "cn";
import { NoValue } from "../../text/no-value";

/**
 * Prop Types
 */

export interface FileSizeOutputProps extends Omit<React.ComponentProps<"span">, "children"> {
  /**
   * The size in bytes; `null` and `undefined` show the no-value placeholder
   */
  bytes: number | null | undefined;
}

/**
 * Component to display a file size in the unit that fits it, e.g. `2,5 MB`
 *
 * @prop bytes - The size in bytes
 */
export function FileSizeOutput({ bytes, className, ...rest }: FileSizeOutputProps): React.ReactElement {
  const formatFileSize = useFileSizeFormatter();

  if (isNotDefined(bytes)) {
    return (
      <span {...rest} className={cn("text-nowrap whitespace-nowrap", className)}>
        <NoValue />
      </span>
    );
  }

  return (
    <span {...rest} className={cn("text-nowrap whitespace-nowrap", className)}>
      {formatFileSize(bytes)}
    </span>
  );
}
