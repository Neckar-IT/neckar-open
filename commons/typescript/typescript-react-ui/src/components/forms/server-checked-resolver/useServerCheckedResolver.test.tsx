import { createQueryClient, type QueryClient, QueryClientProvider } from "@neckar-hub/typescript-react-query";
import { renderHook } from "@testing-library/react";
import type { ReactNode } from "react";
import { describe, expect, it, vi } from "vitest";
import { z } from "zod";
import { useServerCheckedResolver } from "./useServerCheckedResolver";

const schema = z.object({ id: z.string(), name: z.string().max(5).nullable(), note: z.string().min(1) });
type Values = z.infer<typeof schema>;
type Answer = "taken" | "free";

const options = { fields: {}, shouldUseNativeValidation: false };

function wrapperWith(queryClient: QueryClient) {
  return ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

function resolveWith(
  ask: (value: string, id: string) => Promise<Answer>,
  verdict: (answer: Answer) => string | null = (answer) => (answer === "taken" ? "taken" : null),
) {
  const { result } = renderHook(
    () => useServerCheckedResolver(schema, { field: "name", queryKey: (name, id) => ["things", "check", "name", name, id], ask, verdict }),
    { wrapper: wrapperWith(createQueryClient()) },
  );
  return (values: Values) => result.current(values, undefined, options);
}

describe("useServerCheckedResolver", () => {
  it("returns the Zod error without asking the server when Zod rejects the field", async () => {
    const ask = vi.fn(async (): Promise<Answer> => "taken");

    const result = await resolveWith(ask)({ id: "own", name: "far too long", note: "x" });

    expect(ask).not.toHaveBeenCalled();
    expect(result.errors.name?.type).toBe("too_big");
  });

  it("asks the server nothing for a blank or null field", async () => {
    const ask = vi.fn(async (): Promise<Answer> => "taken");
    const resolve = resolveWith(ask);

    const blank = await resolve({ id: "own", name: "  ", note: "x" });
    const absent = await resolve({ id: "own", name: null, note: "x" });

    expect(ask).not.toHaveBeenCalled();
    expect(blank.errors).toEqual({});
    expect(absent.errors).toEqual({});
  });

  it("adds the verdict as a server error beside the other Zod errors", async () => {
    const ask = vi.fn(async (): Promise<Answer> => "taken");

    const result = await resolveWith(ask)({ id: "own", name: "Anna", note: "" });

    expect(result.errors.name).toEqual({ type: "server", message: "taken" });
    expect(result.errors.note?.type).toBe("too_small");
  });

  it("returns the Zod result when the server accepts the value", async () => {
    const values = { id: "own", name: "Anna", note: "x" };

    const result = await resolveWith(async () => "free")(values);

    expect(result).toEqual({ values, errors: {} });
  });

  it("asks the server once for a value checked again within the stale time, and maps the cached answer anew", async () => {
    const ask = vi.fn(async (): Promise<Answer> => "taken");
    let language = "en";
    const resolve = resolveWith(ask, () => (language === "en" ? "taken" : "vergeben"));

    await resolve({ id: "own", name: "Anna", note: "x" });
    language = "de";
    const second = await resolve({ id: "own", name: "Anna", note: "y" });

    expect(ask).toHaveBeenCalledTimes(1);
    expect(second).toEqual({ values: {}, errors: { name: { type: "server", message: "vergeben" } } });
  });

  it("asks the server again for another value or another id", async () => {
    const ask = vi.fn(async (): Promise<Answer> => "free");
    const resolve = resolveWith(ask);

    await resolve({ id: "own", name: "Anna", note: "x" });
    await resolve({ id: "own", name: "Berta", note: "x" });
    await resolve({ id: "other", name: "Berta", note: "x" });

    expect(ask.mock.calls).toEqual([
      ["Anna", "own"],
      ["Berta", "own"],
      ["Berta", "other"],
    ]);
  });

  it("reads and writes a nested field at its path", async () => {
    const nestedSchema = z.object({ id: z.string(), owner: z.object({ name: z.string().max(5) }) });
    const ask = vi.fn(async () => "taken");
    const { result } = renderHook(
      () =>
        useServerCheckedResolver(nestedSchema, {
          field: "owner.name",
          queryKey: (name, id) => ["owners", name, id],
          ask,
          verdict: (answer) => answer,
        }),
      { wrapper: wrapperWith(createQueryClient()) },
    );
    const resolve = (name: string) => result.current({ id: "own", owner: { name } }, undefined, options);

    expect((await resolve("far too long")).errors.owner?.name?.type).toBe("too_big");
    expect(ask).not.toHaveBeenCalled();
    expect((await resolve("Anna")).errors.owner?.name).toEqual({ type: "server", message: "taken" });
  });
});
