import { appImageInfo } from "@neckar-hub/typescript-utils/deploy-metadata";
import { installClientErrorReporter } from "./clientErrorReporter";
import { initializeOtel, type OtelConfig } from "./initializeOtel";
import { installConsoleLogger } from "./installConsoleLogger";
import { installWindowErrorLogger } from "./windowErrorLogger";

export interface BootstrapObservabilityOptions {
  /** Matches the docker-compose service/container name of the UI, e.g. `patterns`. */
  serviceName: string;
  exportHeaders?: OtelConfig["exportHeaders"];
}

/**
 * Call once at the top of `main.tsx`, before anything issues a request, so the HTTP client interceptor has an active
 * span context to inject as `traceparent`.
 */
export function bootstrapObservability(options: BootstrapObservabilityOptions): void {
  const serviceVersion = appImageInfo().imageDigestShort;
  installClientErrorReporter({ serviceVersion });
  initializeOtel({ serviceName: options.serviceName, serviceVersion, exportHeaders: options.exportHeaders });
  installWindowErrorLogger();
  installConsoleLogger();
}
