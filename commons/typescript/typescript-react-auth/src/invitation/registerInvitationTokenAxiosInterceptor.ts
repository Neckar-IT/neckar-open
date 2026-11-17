import type { AxiosInstance } from "axios";
import { InvitationTokenHeader, readPendingInvitationToken } from "./pendingInvitationToken";

/**
 * Replays a parked invitation token as the [InvitationTokenHeader] on every request of [instance].
 *
 * The header rides along until onboarding completes, because the registration call is not the only
 * request in flight during it — the `/me` round trip that decides whether the registration gate is
 * shown at all happens first. Cleared by the app once registration succeeded (see
 * `clearPendingInvitationToken`).
 */
export function registerInvitationTokenAxiosInterceptor(instance: AxiosInstance): void {
  instance.interceptors.request.use((config) => {
    const invitationToken = readPendingInvitationToken();
    if (invitationToken !== null) {
      config.headers.set(InvitationTokenHeader, invitationToken);
    }
    return config;
  });
}
