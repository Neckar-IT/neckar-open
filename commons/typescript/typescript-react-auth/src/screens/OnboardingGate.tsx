import { type ReactElement, useState } from "react";
import { clearPendingInvitationToken, readPendingInvitationToken } from "../invitation/pendingInvitationToken";
import { OnboardingRejectedScreen } from "./AuthScreens";

/**
 * What the product's registration page gets from the gate. The page answers each outcome of its registration
 * response with the matching callback.
 */
export interface OnboardingRegistrationProps {
  /** A pending invitation leads into an existing tenant; the page shows a join confirmation instead of the form. */
  joinsInvitingTenant: boolean;
  /** The user is registered — the provider re-evaluates the session. */
  onRegistered: () => void;
  /** `bad/SelfSignupRejected`: the policy changed between `/me` and the attempt. */
  onSelfSignupRejected: () => void;
  /** `bad/InvitationNotAcceptable`: the gate drops the dead token and falls back to self-signup. */
  onInvitationNotAcceptable: () => void;
}

export interface OnboardingGateProps {
  /**
   * Whether registering without an invitation is possible on this service — from the
   * `bad/NotRegistered` response of `/me`.
   */
  selfSignupAllowed: boolean;

  /**
   * Renders the product's registration page. The pending invitation token rides along on its requests as a header
   * (see `registerInvitationTokenAxiosInterceptor`).
   */
  renderRegistration: (props: OnboardingRegistrationProps) => ReactElement;

  /** Invalidates the me query so the provider re-evaluates the session. */
  onRegistered: () => void;

  /** Ends the session — offered on the rejection screen, the only action left there. */
  onLogout: () => void;

  /** Whether a logout is already on its way — the rejection screen disables its button then. */
  logoutPending: boolean;

  /** Address a user without access can write to, shown on the rejection screen. */
  contactEmail?: string;
}

/**
 * The registration gate: rendered when `/me` answered `bad/NotRegistered` — the identity is
 * authenticated but has no local account in this service yet.
 *
 * It resolves the three states that combination can be in:
 * - a pending invitation token is parked: the product's registration page as a join confirmation, which puts the
 *   user into the inviting tenant
 * - none, and self-signup is allowed: the product's registration page as a form, which creates a fresh tenant with
 *   this identity as its owner
 * - none, and self-signup is rejected: the access-denied screen, because a registration attempt
 *   would only answer `bad/SelfSignupRejected`
 *
 * Wire it through `renderNotRegistered` of the auth config in use.
 */
export function OnboardingGate({
  selfSignupAllowed,
  renderRegistration,
  onRegistered,
  onLogout,
  logoutPending,
  contactEmail,
}: OnboardingGateProps): ReactElement {
  // A dead token answers `bad/InvitationNotAcceptable`. Dropping it falls back to the self-signup
  // branch instead of retrying the join forever.
  const [invitationInvalid, setInvitationInvalid] = useState(false);
  // The policy changed between /me and the attempt.
  const [selfSignupRejected, setSelfSignupRejected] = useState(false);
  const hasInvitation = !invitationInvalid && readPendingInvitationToken() !== null;

  if (selfSignupRejected || (!hasInvitation && !selfSignupAllowed)) {
    return <OnboardingRejectedScreen onLogout={onLogout} logoutPending={logoutPending} contactEmail={contactEmail} />;
  }

  return renderRegistration({
    joinsInvitingTenant: hasInvitation,
    onRegistered,
    onSelfSignupRejected: () => setSelfSignupRejected(true),
    onInvitationNotAcceptable: () => {
      clearPendingInvitationToken();
      setInvitationInvalid(true);
    },
  });
}
