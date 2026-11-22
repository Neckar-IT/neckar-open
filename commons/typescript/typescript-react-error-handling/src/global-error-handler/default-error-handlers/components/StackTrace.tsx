/**
 * Imports
 */

import { CollapsibleSection } from "./CollapsibleSection";

/**
 * Prop Types
 */

interface StackTraceProps {
  stackTrace: string;
  initiallyOpen?: boolean | undefined;
  className?: string | undefined;
}

export function StackTrace(props: StackTraceProps) {
  return (
    <CollapsibleSection className={props.className} heading="Stack Trace" initiallyOpen={props.initiallyOpen}>
      <pre className="bg-muted w-full overflow-auto rounded-md border px-4 py-2 text-xs">{props.stackTrace}</pre>
    </CollapsibleSection>
  );
}
