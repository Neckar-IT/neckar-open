import { createContext, useContext } from "react";

type ActionLockReason = string | React.ReactElement | undefined;

const ActionLockContext = createContext<ActionLockReason>(undefined);

interface ActionLockProviderProps {
  /** Locks every action below while defined, and is shown as the reason; `undefined` adds no lock. */
  reason: ActionLockReason;
  children: React.ReactNode;
}

/**
 * Locks every `ActionButton`, `FormSubmitActionButton`, `ActionsDropdownItem` and `ConfirmationDialog` below, e.g. while the
 * device is offline. The lock of an outer provider stays in force below an inner one.
 */
export function ActionLockProvider({ reason, children }: ActionLockProviderProps): React.ReactElement {
  const outerReason = useContext(ActionLockContext);
  return <ActionLockContext value={outerReason ?? reason}>{children}</ActionLockContext>;
}

/**
 * The reason every action is locked for, or `undefined` while actions are allowed.
 */
export function useActionLock(): ActionLockReason {
  return useContext(ActionLockContext);
}
