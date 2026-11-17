/**
 * Imports
 */

import { serviceHost } from "@neckar-hub/typescript-utils/runtime-context";
import { useState } from "react";
import { useObservability } from "../../ObservabilityContext";
import { useVersionInfo } from "../../VersionInfoContext";
import { ContactSupportButton } from "./ContactSupportButton";
import { CopyErrorInfoButton } from "./CopyErrorInfoButton";
import { buildErrorInfoText, type ErrorInfoSource } from "./errorInfoText";

/**
 * Prop Types
 */

interface ErrorSupportActionsProps extends ErrorInfoSource {
  /** Tells the user when to turn to support. */
  hint: string;
}

/**
 * Error Support Actions
 *
 * Footer of an error page: the hint and the two ways to hand the error to support.
 */

export function ErrorSupportActions({ hint, ...errorInfoSource }: ErrorSupportActionsProps) {
  // The page renders right after the catch, so its mount time is the time of the error.
  const [occurredAt] = useState(() => new Date().toISOString());
  const versionInfo = useVersionInfo();
  const observability = useObservability();

  const errorInfo = buildErrorInfoText({ ...errorInfoSource, occurredAt, serviceHost: serviceHost(), versionInfo, observability });

  return (
    <div className="flex w-full flex-col items-center border-t pt-4">
      <p className="text-center text-sm">{hint}</p>
      <div className="mt-2 flex gap-2">
        <ContactSupportButton errorInfo={errorInfo} />
        <CopyErrorInfoButton errorInfo={errorInfo} />
      </div>
    </div>
  );
}
