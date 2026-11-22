import { isDefined } from "@neckar-hub/typescript-utils/lang";
import { Button, type ButtonProps } from "../primitives/button";
import { useActionLock } from "./ActionLockProvider";
import { DisabledReasonTooltip } from "./DisabledReasonTooltip";

/**
 * `Button` that a lock of an `ActionLockProvider` above disables; the lock reason is its tooltip.
 */
export function LockableButton({ disabled, ...rest }: ButtonProps): React.ReactElement {
  const lockReason = useActionLock();

  return (
    <DisabledReasonTooltip reason={lockReason}>
      <Button disabled={(disabled ?? false) || isDefined(lockReason)} {...rest} />
    </DisabledReasonTooltip>
  );
}
