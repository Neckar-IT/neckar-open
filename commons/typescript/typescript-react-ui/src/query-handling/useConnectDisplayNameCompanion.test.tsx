import { QueryClientForTest } from "@neckar-hub/typescript-react-query";
import { act, renderHook, waitFor } from "@testing-library/react";
import { useForm, useWatch } from "react-hook-form";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { AdjustableValueTextExtended } from "../components/inputs/adjustable-value-text-input/AdjustableValueTextInput";
import { type DisplayNameCompanionResponse, useConnectDisplayNameCompanion } from "./useConnectDisplayNameCompanion";
import { companionQueryKey } from "./useConnectFormCompanion";

const fetchSpy = vi.fn<(input: { firstName: string | null; lastName: string | null }) => Promise<DisplayNameCompanionResponse>>();

interface TestSchema {
  id: string;
  firstName: string | null;
  lastName: string | null;
  displayName: AdjustableValueTextExtended | null;
}

function renderDisplayNameCompanion(displayName: AdjustableValueTextExtended | null, onRender: () => void = () => {}) {
  return renderHook(
    () => {
      onRender();
      const form = useForm<TestSchema>({ defaultValues: { id: "a1", firstName: "Jane", lastName: null, displayName } });
      const [firstName, lastName] = useWatch({ control: form.control, name: ["firstName", "lastName"] });
      useConnectDisplayNameCompanion(form, {
        input: { firstName, lastName },
        queryKey: (input) => companionQueryKey("tests", form.getValues("id"), "display-name", input),
        fetch: fetchSpy,
      });
      return form;
    },
    { wrapper: QueryClientForTest },
  );
}

function typeLastName(form: ReturnType<typeof renderDisplayNameCompanion>["result"]["current"], lastName: string) {
  act(() => {
    form.setValue("lastName", lastName, { shouldDirty: true });
  });
}

describe("useConnectDisplayNameCompanion", () => {
  beforeEach(() => {
    fetchSpy.mockReset();
  });

  it("writes Auto into an empty display name", async () => {
    fetchSpy.mockResolvedValue({ type: "good", data: { calculated: "Jane Austen" } });
    const { result } = renderDisplayNameCompanion(null);

    typeLastName(result.current, "Austen");

    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalledWith({ firstName: "Jane", lastName: "Austen" });
      expect(result.current.getValues("displayName")).toEqual({ type: "Auto", value: "Jane Austen" });
    });
  });

  it("keeps the typed value of a Manual display name and records the calculation as Adjusted", async () => {
    fetchSpy.mockResolvedValue({ type: "good", data: { calculated: "Jane Austen" } });
    const { result } = renderDisplayNameCompanion({ type: "Manual", value: "J. A." });

    typeLastName(result.current, "Austen");

    await waitFor(() => {
      expect(result.current.getValues("displayName")).toEqual({ type: "Adjusted", calculated: "Jane Austen", value: "J. A." });
    });
  });

  it("ignores a bad response", async () => {
    // A bad response with a readable name proves the guard, not a swallowed TypeError on `data`.
    const badResponse = { type: "bad/AuthorNotFound", data: { calculated: "Jane Austen" } } as const;
    fetchSpy.mockResolvedValue(badResponse);
    const { result } = renderDisplayNameCompanion({ type: "Auto", value: "Jane" });

    typeLastName(result.current, "Austen");

    await waitFor(() => expect(fetchSpy).toHaveBeenCalled());
    await new Promise((resolve) => setTimeout(resolve, 50));
    expect(result.current.getValues("displayName")).toEqual({ type: "Auto", value: "Jane" });
  });

  it("writes nothing when the recalculated value equals the current one", async () => {
    fetchSpy.mockResolvedValue({ type: "good", data: { calculated: "Jane" } });
    const { result } = renderDisplayNameCompanion({ type: "Auto", value: "Jane" });
    const setValueSpy = vi.spyOn(result.current, "setValue");

    typeLastName(result.current, "Austen");

    await waitFor(() => expect(fetchSpy).toHaveBeenCalled());
    await new Promise((resolve) => setTimeout(resolve, 50));
    expect(setValueSpy).not.toHaveBeenCalledWith("displayName", expect.anything());
  });

  it("settles once the debounced name fields equal the watched ones", async () => {
    fetchSpy.mockResolvedValue({ type: "good", data: { calculated: "Jane Austen" } });
    let renders = 0;
    const { result } = renderDisplayNameCompanion(null, () => renders++);

    typeLastName(result.current, "Austen");
    await waitFor(() => expect(result.current.getValues("displayName")).toEqual({ type: "Auto", value: "Jane Austen" }));
    await new Promise((resolve) => setTimeout(resolve, 400));
    const rendersAfterDebounce = renders;
    await new Promise((resolve) => setTimeout(resolve, 700));

    expect(renders).toBe(rendersAfterDebounce);
  });

  it("applies a null calculation instead of ignoring it", async () => {
    fetchSpy.mockResolvedValue({ type: "good", data: { calculated: null } });
    const { result } = renderDisplayNameCompanion({ type: "Adjusted", calculated: "Jane", value: "J." });

    typeLastName(result.current, "Austen");

    await waitFor(() => {
      expect(result.current.getValues("displayName")).toEqual({ type: "Manual", value: "J." });
    });
  });
});
