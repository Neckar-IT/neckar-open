import type {
  PlatformHeaderClientId,
  PlatformHeaderClientRole,
  PlatformHeaderFeatureFlags,
  PlatformHeaderProtocolVersion,
  PlatformHeaderSessionId,
  PlatformHeaderTraceId,
} from "../../build/generated/open-api/openapi-types.schemas";

/**
 * HTTP header names that flow between Ktor backends and React frontends.
 *
 * Single source of truth is the Kotlin enum `it.neckar.rest.PlatformHeader` in `rest-model`.
 * The values below are validated against the generated schemas via `satisfies`, so renaming
 * a header on the backend breaks this file at TypeScript compile time.
 */
export const PlatformHeader = {
  SessionId: "NeckarIT-Session-Id",
  ClientId: "NeckarIT-Client-Id",
  ClientRole: "NeckarIT-Client-Role",
  ProtocolVersion: "NeckarIT-Protocol-Version",
  FeatureFlags: "NeckarIT-Feature-Flags",
  TraceId: "NeckarIT-Trace-Id",
} as const satisfies {
  SessionId: PlatformHeaderSessionId["headerName"];
  ClientId: PlatformHeaderClientId["headerName"];
  ClientRole: PlatformHeaderClientRole["headerName"];
  ProtocolVersion: PlatformHeaderProtocolVersion["headerName"];
  FeatureFlags: PlatformHeaderFeatureFlags["headerName"];
  TraceId: PlatformHeaderTraceId["headerName"];
};

export type PlatformHeaderName = (typeof PlatformHeader)[keyof typeof PlatformHeader];
