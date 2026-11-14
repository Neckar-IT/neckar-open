import { isDefined } from "@neckar-hub/typescript-utils/lang";
import type { Action } from "./Action";
import { useActionLock } from "./ActionLockProvider";

export interface ActionState {
  disabled: boolean;
  loading: boolean;
  /** Ignore every trigger: the action is disabled or already running. */
  blocked: boolean;
  /** Why the action is disabled: the lock reason of an `ActionLockProvider`, else `action.disabledReason`. */
  disabledReason: React.ReactNode;
}

interface ActionStateOverrides {
  disabled?: boolean;
  loading?: boolean;
}

/**
 * The state every component rendering an `Action` shows and obeys. An `ActionLockProvider` lock wins over the usage-site
 * override, which wins over the action's own `disabled` and `loading`.
 */
export function useActionState(action: Action, overrides: ActionStateOverrides = {}): ActionState {
  const lockReason = useActionLock();
  const locked = isDefined(lockReason);
  const disabled = locked || (overrides.disabled ?? action.disabled ?? false);
  const loading = overrides.loading ?? action.loading ?? false;

  return {
    disabled,
    loading,
    blocked: disabled || loading,
    disabledReason: locked ? lockReason : action.disabledReason,
  };
}
