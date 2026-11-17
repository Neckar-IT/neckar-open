import { createQueryClient, QueryClientProvider } from "@neckar-hub/typescript-react-query";
import type { HealthResponse } from "@neckar-hub/typescript-utils/openapi-types";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { Provider } from "jotai";
import { ServiceUnavailablePage } from "../global-error-handler/default-error-handlers/serviceUnavailableErrorHandler";
import { AppConnectivityProvider } from "./AppConnectivityProvider";
import type { ServiceAvailability } from "./appConnectivityState";

const meta = {
  title: "App Connectivity",
} satisfies Meta;

export default meta;

const readyHealth = (): Promise<HealthResponse> => Promise.resolve({ state: "ready", errorCount: 0, backgroundJobs: [] });

/**
 * The banner of one availability, with a fresh jotai store and query client per story.
 */
function Banner(props: { availability?: ServiceAvailability; deviceOffline?: boolean }) {
  return (
    <Provider>
      <QueryClientProvider client={createQueryClient()}>
        <AppConnectivityProvider
          healthCheckFn={readyHealth}
          mockDeviceOffline={props.deviceOffline}
          mockServiceAvailability={props.availability}
        >
          This is the application!
        </AppConnectivityProvider>
      </QueryClientProvider>
    </Provider>
  );
}

export const DeviceOffline: StoryObj<typeof meta> = {
  render: () => <Banner deviceOffline={true} />,
};
export const Degraded: StoryObj<typeof meta> = {
  render: () => <Banner availability={{ type: "available", state: "degraded" }} />,
};
export const Starting: StoryObj<typeof meta> = {
  render: () => <Banner availability={{ type: "notReady", state: "starting" }} />,
};
export const Migrating: StoryObj<typeof meta> = {
  render: () => <Banner availability={{ type: "notReady", state: "migrating" }} />,
};
export const DependencyUnavailable: StoryObj<typeof meta> = {
  render: () => <Banner availability={{ type: "notReady", state: "dependency-unavailable" }} />,
};
export const SchemaIncompatible: StoryObj<typeof meta> = {
  render: () => <Banner availability={{ type: "notReady", state: "schema-incompatible" }} />,
};
export const MigrationFailed: StoryObj<typeof meta> = {
  render: () => <Banner availability={{ type: "notReady", state: "migration-failed" }} />,
};
export const Fatal: StoryObj<typeof meta> = {
  render: () => <Banner availability={{ type: "notReady", state: "fatal" }} />,
};
export const Unreachable: StoryObj<typeof meta> = {
  render: () => <Banner availability={{ type: "unreachable" }} />,
};
export const NoConnection: StoryObj<typeof meta> = {
  render: () => <Banner availability={{ type: "noConnection" }} />,
};

/**
 * `ServiceUnavailablePage` before the first health poll answers: the text of the failed request itself.
 */
export const ServiceUnavailablePageUnreachable: StoryObj<typeof meta> = {
  render: () => (
    <Provider>
      <ServiceUnavailablePage fallbackAvailability={{ type: "unreachable" }} />
    </Provider>
  ),
};

/**
 * `ServiceUnavailablePage` with the retry button a recoverable caller offers.
 */
export const ServiceUnavailablePageWithRetry: StoryObj<typeof meta> = {
  render: () => (
    <Provider>
      <ServiceUnavailablePage fallbackAvailability={{ type: "noConnection" }} onRecover={() => console.info("recover")} />
    </Provider>
  ),
};
