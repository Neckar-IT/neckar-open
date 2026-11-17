/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { makeAxiosError } from "../axiosErrorFixture";
import { PermissionDeniedPage, type PermissionDeniedPageProps } from "./missingCapabilityErrorHandler";

/**
 * Meta Information
 */

const meta = {
  title: "Error Handling / Default Error Handlers",
} satisfies Meta;

export default meta;

/**
 * Stories
 */

/**
 * The page a user lands on when the server refuses a request for lack of a capability.
 *
 * `missingCapability` is shown through the i18n key `capabilities.<namespace>.<key>`; the story
 * uses a capability no product translates, so what appears is the raw key — the fallback a user
 * sees whenever a product has not shipped a label yet.
 */
export const MissingCapabilityErrorHandler: StoryObj<PermissionDeniedPageProps> = {
  args: {
    errorCaughtBy: "global-error-boundary",
    missingCapability: "core:operate-product",
    errorData: {
      errorCode: "AUTHZ-1",
      fallbackMessage: "Missing capability: core:operate-product",
      attributes: { missingCapability: "core:operate-product" },
    },
    error: makeAxiosError({
      status: 403,
      method: "delete",
      url: "/api/admin/tenants/01937f4c-8a21-7c3e-9f11-2b6d4e8a0c55",
      traceId: "0af7651916cd43dd8448eb211c80319c",
    }),
  },
  render: (props: PermissionDeniedPageProps) => <PermissionDeniedPage {...props} />,
};
