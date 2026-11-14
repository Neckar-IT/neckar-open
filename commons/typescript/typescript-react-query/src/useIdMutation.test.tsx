/**
 * Imports
 */

import { QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook } from "@testing-library/react";
import type { ReactNode } from "react";
import { describe, expect, it, vi } from "vitest";
import { createQueryClient } from "./createQueryClient";
import { useIdMutation } from "./useIdMutation";

/**
 * Test Helpers
 */

function createWrapper() {
  const queryClient = createQueryClient();
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

/**
 * Tests
 */

describe("useIdMutation", () => {
  it("calls the function with the id alone and resolves its result", async () => {
    const archive = vi.fn((id: string) => Promise.resolve({ archived: id }));
    const { result } = renderHook(() => useIdMutation(archive), { wrapper: createWrapper() });

    let response: unknown;
    await act(async () => {
      response = await result.current.mutateAsync("id-1");
    });

    expect(archive).toHaveBeenCalledOnce();
    expect(archive.mock.calls[0]).toEqual(["id-1"]);
    expect(response).toEqual({ archived: "id-1" });
  });
});
