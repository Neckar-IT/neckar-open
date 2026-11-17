// TODO: These types should be generated from OpenAPI (see FeatureFlagsResponse.kt in ktor-server)
// For now, they are defined manually to match the backend response structure.

/**
 * Response type from the /feature-flags/available endpoint
 */
export interface AvailableFeatureFlagsResponse {
  type: "good";
  data: AvailableFeatureFlag[];
}

/**
 * A feature flag as returned by the API
 */
export interface AvailableFeatureFlag {
  key: string;
  description: string | null;
}

/**
 * Fetches the available feature flags from the server.
 *
 * @param baseUrl - The base URL of the service API (e.g., "/api/elektromeister")
 * @param fetchFn - Optional fetch function (defaults to global fetch). Pass a custom fetch for testing or SSR.
 * @returns List of available feature flags
 *
 * @example
 * ```typescript
 * const flags = await fetchAvailableFeatureFlags("/api/elektromeister");
 * // flags = [{ key: "slowUI", description: "Delay actions in the UI" }, ...]
 * ```
 */
export async function fetchAvailableFeatureFlags(
  baseUrl: string,
  fetchFn: typeof globalThis.fetch = globalThis.fetch,
): Promise<AvailableFeatureFlag[]> {
  const url = `${baseUrl}/feature-flags/available`;
  const response = await fetchFn(url);

  if (!response.ok) {
    throw new Error(`Failed to fetch feature flags: ${String(response.status)} ${response.statusText}`);
  }

  const json = (await response.json()) as AvailableFeatureFlagsResponse;
  return json.data;
}
