import { QueryClient, QueryClientProvider } from "@neckar-hub/typescript-react-query";
import { act, renderHook, waitFor } from "@testing-library/react";
import { useForm } from "react-hook-form";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { companionQueryKey, useConnectFormCompanion } from "./useConnectFormCompanion";

const fetchSpy = vi.fn();
const applySpy = vi.fn();

interface TestSchema {
  name: string;
}

function renderFormCompanion() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: Infinity } } });

  return renderHook(
    () => {
      const form = useForm<TestSchema>({ defaultValues: { name: "initial" } });
      useConnectFormCompanion<TestSchema, string, string | null>(form, {
        input: form.watch("name"),
        inputEquals: (left, right) => left === right,
        debounceMs: 10,
        queryKey: (input) => companionQueryKey("tests", "test-id", "name", input),
        fetchValue: (input) => fetchSpy(input),
        applyValue: (derived) => applySpy(derived),
      });
      return form;
    },
    { wrapper: ({ children }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider> },
  );
}

describe("useConnectFormCompanion", () => {
  beforeEach(() => {
    fetchSpy.mockReset();
    applySpy.mockReset();
  });

  it("connects once the form is dirty", async () => {
    fetchSpy.mockResolvedValue("derived");
    const { result } = renderFormCompanion();

    act(() => {
      result.current.setValue("name", "changed", { shouldDirty: true });
    });

    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalledWith("changed");
      expect(applySpy).toHaveBeenCalledWith("derived");
    });
  });

  it("stays silent while the form is pristine", async () => {
    fetchSpy.mockResolvedValue("irrelevant");
    renderFormCompanion();

    await new Promise((resolve) => setTimeout(resolve, 50));

    expect(fetchSpy).not.toHaveBeenCalled();
  });
});

describe("companionQueryKey", () => {
  it("follows the entity-id-companion-aspect-input convention", () => {
    expect(companionQueryKey("authors", "a1", "display-name", { firstName: "x" })).toEqual([
      "authors",
      "a1",
      "companion",
      "display-name",
      { firstName: "x" },
    ]);
  });
});
