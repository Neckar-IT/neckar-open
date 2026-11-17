import type { Context } from "@opentelemetry/api";
import type { ReadableSpan, Span, SpanProcessor } from "@opentelemetry/sdk-trace";

/**
 * Identity attributes to propagate on every span.
 *
 * All fields are optional — attributes are only set when a value is present.
 * Typically, sessionId is available immediately, while userId and tenantId
 * become available after authentication.
 */
export interface OtelIdentity {
  /** Pseudonymous user identifier (e.g., login name). No PII. */
  userId?: string;
  /** Tenant/client identifier (UUID). */
  tenantId?: string;
}

/**
 * SpanProcessor that injects client identity attributes into every span.
 *
 * Since OpenTelemetry Resources are immutable after TracerProvider creation,
 * this processor dynamically adds identity attributes as span attributes.
 * Identity can be updated at any time (e.g., after login) via {@link setIdentity}.
 */
export class IdentitySpanProcessor implements SpanProcessor {
  private identity: OtelIdentity = {};

  /**
   * Update the identity attributes. Call this after successful authentication
   * to start adding user and tenant information to all subsequent spans.
   */
  setIdentity(identity: OtelIdentity): void {
    this.identity = identity;
  }

  /**
   * Clear identity attributes (e.g., on logout).
   */
  clearIdentity(): void {
    this.identity = {};
  }

  onStart(span: Span, _parentContext: Context): void {
    if (this.identity.userId != null) {
      span.setAttribute("enduser.id", this.identity.userId);
    }
    if (this.identity.tenantId != null) {
      span.setAttribute("tenant.id", this.identity.tenantId);
    }
  }

  onEnd(_span: ReadableSpan): void {
    // No-op — attributes are set on start
  }

  async shutdown(): Promise<void> {
    // No-op — no resources to release
  }

  async forceFlush(): Promise<void> {
    // No-op — no buffered data
  }
}
