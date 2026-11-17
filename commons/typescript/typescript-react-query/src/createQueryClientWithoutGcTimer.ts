import type { QueryClient } from "@tanstack/react-query";
import { createQueryClient } from "./createQueryClient";

/**
 * A query client whose queries and mutations start no garbage-collection timer, so no timer outlives a test.
 */
export function createQueryClientWithoutGcTimer(): QueryClient {
  const queryClient = createQueryClient();
  const defaultOptions = queryClient.getDefaultOptions();
  queryClient.setDefaultOptions({
    queries: { ...defaultOptions.queries, gcTime: Infinity },
    mutations: { ...defaultOptions.mutations, gcTime: Infinity },
  });

  return queryClient;
}
