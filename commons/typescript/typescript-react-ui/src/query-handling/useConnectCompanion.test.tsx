import { QueryClient, QueryClientProvider } from "@neckar-hub/typescript-react-query";
import { act, renderHook, waitFor } from "@testing-library/react";
import { useState } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useConnectCompanion } from "./useConnectCompanion";

const fetchSpy = vi.fn();
const applySpy = vi.fn();

function renderPreview(initialInput: string, enabled = true) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: Infinity } } });

  return renderHook(
    () => {
      const [input, setInput] = useState(initialInput);
      useConnectCompanion<string, string | null>({
        input,
        inputEquals: (left, right) => left === right,
        enabled,
        debounceMs: 10,
        queryKey: (current) => ["preview-test", current],
        fetchValue: (current) => fetchSpy(current),
        applyValue: (derived) => applySpy(derived),
      });
      return { setInput };
    },
    { wrapper: ({ children }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider> },
  );
}

describe("useConnectCompanion", () => {
  beforeEach(() => {
    fetchSpy.mockReset();
    applySpy.mockReset();
  });

  it("fetches debounced on input changes and applies the derived value", async () => {
    fetchSpy.mockResolvedValue("derived for b");
    const { result } = renderPreview("a");

    act(() => {
      result.current.setInput("b");
    });

    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalledWith("b");
      expect(applySpy).toHaveBeenCalledWith("derived for b");
    });
  });

  it("applies null - a calculation without result is a valid derived value", async () => {
    fetchSpy.mockResolvedValue(null);
    const { result } = renderPreview("a");

    act(() => {
      result.current.setInput("b");
    });

    await waitFor(() => {
      expect(applySpy).toHaveBeenCalledWith(null);
    });
  });

  it("ignores undefined - the fetch asked for the response to be dropped", async () => {
    fetchSpy.mockResolvedValue(undefined);
    const { result } = renderPreview("a");

    act(() => {
      result.current.setInput("b");
    });

    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalled();
    });
    expect(applySpy).not.toHaveBeenCalled();
  });

  it("does not fetch while disabled", async () => {
    fetchSpy.mockResolvedValue("irrelevant");
    const { result } = renderPreview("a", false);

    act(() => {
      result.current.setInput("b");
    });

    await new Promise((resolve) => setTimeout(resolve, 50));

    expect(fetchSpy).not.toHaveBeenCalled();
  });
});
