import type { Story, StoryDefault } from "@ladle/react";
import { createQueryClient, QueryClientProvider } from "@neckar-hub/typescript-react-query";
import type { HealthResponse } from "@neckar-hub/typescript-utils/openapi-types";
import { Provider } from "jotai";
import { ServiceUnavailablePage } from "../global-error-handler/default-error-handlers/serviceUnavailableErrorHandler";
import { AppConnectivityProvider } from "./AppConnectivityProvider";
import type { ServiceAvailability } from "./appConnectivityState";

export default {
  title: "App Connectivity",
} satisfies StoryDefault;

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

export const DeviceOffline: Story = () => <Banner deviceOffline={true} />;
export const Degraded: Story = () => <Banner availability={{ type: "available", state: "degraded" }} />;
export const Starting: Story = () => <Banner availability={{ type: "notReady", state: "starting" }} />;
export const Migrating: Story = () => <Banner availability={{ type: "notReady", state: "migrating" }} />;
export const DependencyUnavailable: Story = () => <Banner availability={{ type: "notReady", state: "dependency-unavailable" }} />;
export const SchemaIncompatible: Story = () => <Banner availability={{ type: "notReady", state: "schema-incompatible" }} />;
export const MigrationFailed: Story = () => <Banner availability={{ type: "notReady", state: "migration-failed" }} />;
export const Fatal: Story = () => <Banner availability={{ type: "notReady", state: "fatal" }} />;
export const Unreachable: Story = () => <Banner availability={{ type: "unreachable" }} />;
export const NoConnection: Story = () => <Banner availability={{ type: "noConnection" }} />;

/**
 * `ServiceUnavailablePage` before the first health poll answers: the text of the failed request itself.
 */
export const ServiceUnavailablePageUnreachable: Story = () => (
  <Provider>
    <ServiceUnavailablePage fallbackAvailability={{ type: "unreachable" }} />
  </Provider>
);

/**
 * `ServiceUnavailablePage` with the retry button a recoverable caller offers.
 */
export const ServiceUnavailablePageWithRetry: Story = () => (
  <Provider>
    <ServiceUnavailablePage fallbackAvailability={{ type: "noConnection" }} onRecover={() => console.info("recover")} />
  </Provider>
);
