export { initializeOtel, injectTraceContext, getTracer, getIdentitySpanProcessor, shutdownOtel, type OtelConfig } from "./initializeOtel";
export { IdentitySpanProcessor, type OtelIdentity } from "./IdentitySpanProcessor";
export { registerAxiosTracingInterceptors } from "./axiosTracingInterceptor";
export { captureError, type ErrorSource } from "./errorLogging";
export { installWindowErrorLogger } from "./windowErrorLogger";
export {
  installClientErrorReporter,
  reportClientError,
  endClientErrorReporter,
  type ClientErrorKind,
  type ClientErrorReporterConfig,
} from "./clientErrorReporter";
export { installConsoleLogger } from "./installConsoleLogger";
export { bootstrapObservability, type BootstrapObservabilityOptions } from "./bootstrapObservability";
export { grafanaTraceObservabilityConfig } from "./grafanaTraceObservabilityConfig";
