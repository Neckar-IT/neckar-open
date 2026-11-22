/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { storePendingInvitationToken } from "../../src/invitation/pendingInvitationToken";
import { OnboardingGate, type OnboardingRegistrationProps } from "../../src/screens/OnboardingGate";

const meta = {
  title: "Onboarding Gate",
  component: OnboardingGate,
} satisfies Meta;

export default meta;

/**
 * The gate reads the parked invitation token from `sessionStorage`, so each story sets the storage
 * up before rendering rather than taking a prop for it — that is the very state the component has
 * to resolve, and faking it through a prop would test something the app never runs. The registration
 * page itself belongs to the product; the stories render a stand-in that names the branch.
 */

function withParkedInvitation(): void {
  storePendingInvitationToken("c3d4e5f6-a7b8-9012-cdef-123456789012");
}

function withoutParkedInvitation(): void {
  window.sessionStorage.clear();
}

function productRegistrationPage({ joinsInvitingTenant }: OnboardingRegistrationProps): React.ReactElement {
  return (
    <div className="p-8">
      {joinsInvitingTenant ? "Product registration page: join confirmation" : "Product registration page: self-signup form"}
    </div>
  );
}

/** Self-signup is allowed and no invitation is parked: the product's registration form. */
export const SelfSignupStory: StoryObj = {
  render: () => {
    withoutParkedInvitation();

    return (
      <OnboardingGate
        selfSignupAllowed={true}
        renderRegistration={productRegistrationPage}
        onRegistered={() => {}}
        onLogout={() => {}}
        logoutPending={false}
        contactEmail="info@neckar.it"
      />
    );
  },
};

/** An invitation is parked: the product's join confirmation, even though self-signup is rejected. */
export const JoinInvitingTenantStory: StoryObj = {
  render: () => {
    withParkedInvitation();

    return (
      <OnboardingGate
        selfSignupAllowed={false}
        renderRegistration={productRegistrationPage}
        onRegistered={() => {}}
        onLogout={() => {}}
        logoutPending={false}
        contactEmail="info@neckar.it"
      />
    );
  },
};

/** Neither an invitation nor self-signup: the access-denied screen. */
export const RejectedStory: StoryObj = {
  render: () => {
    withoutParkedInvitation();

    return (
      <OnboardingGate
        selfSignupAllowed={false}
        renderRegistration={productRegistrationPage}
        onRegistered={() => {}}
        onLogout={() => {}}
        logoutPending={false}
        contactEmail="info@neckar.it"
      />
    );
  },
};
