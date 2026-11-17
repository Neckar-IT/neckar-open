import { InMemorySpanExporter, SimpleSpanProcessor, TracerProvider } from "@opentelemetry/sdk-trace";
import { afterEach, describe, expect, it } from "vitest";
import { IdentitySpanProcessor } from "./IdentitySpanProcessor";

describe("IdentitySpanProcessor", () => {
  let provider: TracerProvider | null = null;

  afterEach(async () => {
    if (provider != null) {
      await provider.shutdown();
      provider = null;
    }
  });

  function setup(): { identityProcessor: IdentitySpanProcessor; exporter: InMemorySpanExporter; provider: TracerProvider } {
    const exporter = new InMemorySpanExporter();
    const identityProcessor = new IdentitySpanProcessor();
    const p = new TracerProvider({
      spanProcessors: [identityProcessor, new SimpleSpanProcessor({ exporter })],
    });
    provider = p;
    return { identityProcessor, exporter, provider: p };
  }

  it("adds no identity attributes when identity is not set", () => {
    const { exporter, provider } = setup();
    const tracer = provider.getTracer("test");
    const span = tracer.startSpan("test-span");
    span.end();

    const exportedSpans = exporter.getFinishedSpans();
    expect(exportedSpans).toHaveLength(1);

    const attributes = exportedSpans[0]?.attributes;
    expect(attributes?.["enduser.id"]).toBeUndefined();
    expect(attributes?.["tenant.id"]).toBeUndefined();
  });

  it("adds identity attributes when identity is set", () => {
    const { identityProcessor, exporter, provider } = setup();
    identityProcessor.setIdentity({ userId: "demo-user", tenantId: "tenant-123" });

    const tracer = provider.getTracer("test");
    const span = tracer.startSpan("test-span");
    span.end();

    const exportedSpans = exporter.getFinishedSpans();
    expect(exportedSpans).toHaveLength(1);

    const attributes = exportedSpans[0]?.attributes;
    expect(attributes?.["enduser.id"]).toBe("demo-user");
    expect(attributes?.["tenant.id"]).toBe("tenant-123");
  });

  it("supports partial identity (only userId)", () => {
    const { identityProcessor, exporter, provider } = setup();
    identityProcessor.setIdentity({ userId: "demo-user" });

    const tracer = provider.getTracer("test");
    const span = tracer.startSpan("test-span");
    span.end();

    const exportedSpans = exporter.getFinishedSpans();
    expect(exportedSpans).toHaveLength(1);

    const attributes = exportedSpans[0]?.attributes;
    expect(attributes?.["enduser.id"]).toBe("demo-user");
    expect(attributes?.["tenant.id"]).toBeUndefined();
  });

  it("clears identity attributes after clearIdentity()", () => {
    const { identityProcessor, exporter, provider } = setup();
    identityProcessor.setIdentity({ userId: "demo-user", tenantId: "tenant-123" });
    identityProcessor.clearIdentity();

    const tracer = provider.getTracer("test");
    const span = tracer.startSpan("test-span");
    span.end();

    const exportedSpans = exporter.getFinishedSpans();
    expect(exportedSpans).toHaveLength(1);

    const attributes = exportedSpans[0]?.attributes;
    expect(attributes?.["enduser.id"]).toBeUndefined();
    expect(attributes?.["tenant.id"]).toBeUndefined();
  });

  it("updates identity for subsequent spans", () => {
    const { identityProcessor, exporter, provider } = setup();

    const tracer = provider.getTracer("test");

    // First span without identity
    const span1 = tracer.startSpan("span-1");
    span1.end();

    // Set identity
    identityProcessor.setIdentity({ userId: "user-a", tenantId: "tenant-a" });

    // Second span with identity
    const span2 = tracer.startSpan("span-2");
    span2.end();

    const exportedSpans = exporter.getFinishedSpans();
    expect(exportedSpans).toHaveLength(2);

    expect(exportedSpans[0]?.attributes["enduser.id"]).toBeUndefined();
    expect(exportedSpans[1]?.attributes["enduser.id"]).toBe("user-a");
    expect(exportedSpans[1]?.attributes["tenant.id"]).toBe("tenant-a");
  });
});
