/**
 * Imports
 */

import { cn } from "cn";
import type { PropsWithChildren, ReactElement } from "react";

/**
 * Prop Types
 */

type CodeBlockProps = PropsWithChildren<{
  className?: string;
}>;

/**
 * Preformatted block for displaying code or monospace text with a muted background.
 */

export function CodeBlock({ className, children }: CodeBlockProps): ReactElement {
  return <pre className={cn("bg-muted rounded-md px-4 py-2 text-sm", className)}>{children}</pre>;
}
