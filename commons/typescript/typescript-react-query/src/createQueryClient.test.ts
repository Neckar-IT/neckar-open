/**
 * Imports
 */

import { MutationObserver, type QueryClient, QueryObserver } from "@tanstack/react-query";
import axios, { AxiosError, AxiosHeaders, type AxiosInstance, CanceledError, type InternalAxiosRequestConfig } from "axios";
import { describe, expect, it, vi } from "vitest";
import { createQueryClient } from "./createQueryClient";

/**
 * Fires a single mutation via the raw `MutationObserver` API and waits for it to settle.
 *
 * `throwOnError: true` is set globally in `createQueryClient`, so failing mutations reject —
 * we swallow those rejections here because the tests only care about whether the global
 * `onSettled` handler triggered a cache reset.
 */
async function runMutation<TData>(
  client: QueryClient,
  options: {
    mutationFn: () => Promise<TData>;
    meta?: Record<string, unknown>;
  },
): Promise<void> {
  const observer = new MutationObserver(client, options);
  try {
    await observer.mutate(undefined);
  } catch {
    // Swallow — throwOnError is true for mutations; errors are observed via the onSettled callback.
  }
}

/**
 * An axios instance whose adapter answers every request with HTTP 401 and records the request URLs.
 */
function createUnauthorizedHttpClient(): { httpClient: AxiosInstance; requests: string[] } {
  const requests: string[] = [];
  const httpClient = axios.create({
    adapter: (config: InternalAxiosRequestConfig) => {
      requests.push(axios.getUri(config));
      const response = { data: {}, status: 401, statusText: "Unauthorized", headers: {}, config };
      return Promise.reject(new AxiosError("unauthorized", AxiosError.ERR_BAD_REQUEST, config, undefined, response));
    },
  });
  return { httpClient, requests };
}

/**
 * Runs one query through the client's defaults until it fails; `retryDelay: 0` keeps the retries instant.
 */
async function runFailingQuery(client: QueryClient, queryFn: () => Promise<unknown>): Promise<unknown> {
  const observer = new QueryObserver(client, { queryKey: ["failing"], queryFn, retryDelay: 0 });
  return new Promise((resolve) => {
    const unsubscribe = observer.subscribe((result) => {
      if (result.status === "error") {
        unsubscribe();
        resolve(result.error);
      }
    });
  });
}

/**
 * Tests
 */

describe("createQueryClient onSettled cache reset", () => {
  it("resets queries when a mutation resolves with void", async () => {
    const client = createQueryClient();
    const resetSpy = vi.spyOn(client, "resetQueries");

    await runMutation(client, { mutationFn: async () => undefined });

    expect(resetSpy).toHaveBeenCalledOnce();
  });

  it("resets queries when a mutation resolves with a good RestResponse", async () => {
    const client = createQueryClient();
    const resetSpy = vi.spyOn(client, "resetQueries");

    await runMutation(client, {
      mutationFn: async () => ({ type: "good", data: { id: 1 } }),
    });

    expect(resetSpy).toHaveBeenCalledOnce();
  });

  it("resets queries when a mutation resolves with a primitive value", async () => {
    const client = createQueryClient();
    const resetSpy = vi.spyOn(client, "resetQueries");

    await runMutation(client, { mutationFn: async () => 42 });

    expect(resetSpy).toHaveBeenCalledOnce();
  });

  it("resets queries when a mutation resolves with an object that has no type discriminator", async () => {
    const client = createQueryClient();
    const resetSpy = vi.spyOn(client, "resetQueries");

    await runMutation(client, { mutationFn: async () => ({ id: 7, name: "something" }) });

    expect(resetSpy).toHaveBeenCalledOnce();
  });

  it("does NOT reset queries when a mutation resolves with a bad/ValidationFailure RestResponse", async () => {
    const client = createQueryClient();
    const resetSpy = vi.spyOn(client, "resetQueries");

    await runMutation(client, {
      mutationFn: async () => ({ type: "bad/ValidationFailure", errors: [] }),
    });

    expect(resetSpy).not.toHaveBeenCalled();
  });

  it("does NOT reset queries when a mutation resolves with a bad/NotFound RestResponse", async () => {
    const client = createQueryClient();
    const resetSpy = vi.spyOn(client, "resetQueries");

    await runMutation(client, {
      mutationFn: async () => ({ type: "bad/NotFound" }),
    });

    expect(resetSpy).not.toHaveBeenCalled();
  });

  it("does NOT reset queries when a mutation rejects", async () => {
    const client = createQueryClient();
    const resetSpy = vi.spyOn(client, "resetQueries");

    await runMutation(client, {
      mutationFn: async () => {
        throw new Error("boom");
      },
    });

    expect(resetSpy).not.toHaveBeenCalled();
  });

  it("does NOT reset queries when meta.skipGlobalQueryResets is true, even on success", async () => {
    const client = createQueryClient();
    const resetSpy = vi.spyOn(client, "resetQueries");

    await runMutation(client, {
      mutationFn: async () => ({ type: "good", data: null }),
      meta: { skipGlobalQueryResets: true },
    });

    expect(resetSpy).not.toHaveBeenCalled();
  });

  it("resets queries when a bad response type is listed in meta.resetQueriesOnBadResponses", async () => {
    const client = createQueryClient();
    const resetSpy = vi.spyOn(client, "resetQueries");

    await runMutation(client, {
      mutationFn: async () => ({ type: "bad/NotFound" }),
      meta: { resetQueriesOnBadResponses: ["bad/NotFound"] },
    });

    expect(resetSpy).toHaveBeenCalledOnce();
  });

  it("does NOT reset queries when a bad response type is missing from meta.resetQueriesOnBadResponses", async () => {
    const client = createQueryClient();
    const resetSpy = vi.spyOn(client, "resetQueries");

    await runMutation(client, {
      mutationFn: async () => ({ type: "bad/ValidationFailure", errors: [] }),
      meta: { resetQueriesOnBadResponses: ["bad/NotFound"] },
    });

    expect(resetSpy).not.toHaveBeenCalled();
  });

  it("does NOT reset queries when meta.skipGlobalQueryResets is true, even for a listed bad response type", async () => {
    const client = createQueryClient();
    const resetSpy = vi.spyOn(client, "resetQueries");

    await runMutation(client, {
      mutationFn: async () => ({ type: "bad/NotFound" }),
      meta: { skipGlobalQueryResets: true, resetQueriesOnBadResponses: ["bad/NotFound"] },
    });

    expect(resetSpy).not.toHaveBeenCalled();
  });

  it("calls resetQueries with a predicate that respects preventBeingResetByMutations", async () => {
    const client = createQueryClient();
    const resetSpy = vi.spyOn(client, "resetQueries");

    await runMutation(client, { mutationFn: async () => undefined });

    expect(resetSpy).toHaveBeenCalledOnce();
    const callArgs = resetSpy.mock.calls[0]?.[0] as { predicate: (query: { meta?: Record<string, unknown> }) => boolean } | undefined;
    expect(callArgs?.predicate).toBeTypeOf("function");

    const predicate = callArgs?.predicate;
    if (predicate != null) {
      expect(predicate({ meta: {} })).toBe(true);
      expect(predicate({})).toBe(true);
      expect(predicate({ meta: { preventBeingResetByMutations: false } })).toBe(true);
      expect(predicate({ meta: { preventBeingResetByMutations: true } })).toBe(false);
    }
  });
});

describe("createQueryClient onMutationError", () => {
  it("calls onMutationError for every failed mutation, even when the caller swallows the rejection", async () => {
    const onMutationError = vi.fn();
    const client = createQueryClient({ onMutationError });
    const failure = new Error("save failed");

    await runMutation(client, {
      mutationFn: async () => {
        throw failure;
      },
    });

    expect(onMutationError).toHaveBeenCalledTimes(1);
    expect(onMutationError).toHaveBeenCalledWith(failure);
  });

  it("does not call onMutationError for successful mutations", async () => {
    const onMutationError = vi.fn();
    const client = createQueryClient({ onMutationError });

    await runMutation(client, { mutationFn: async () => "ok" });

    expect(onMutationError).not.toHaveBeenCalled();
  });

  it("does not call onMutationError for a cancelled request", async () => {
    const onMutationError = vi.fn();
    const client = createQueryClient({ onMutationError });

    await runMutation(client, {
      mutationFn: async () => {
        throw new CanceledError("canceled");
      },
    });

    expect(onMutationError).not.toHaveBeenCalled();
  });

  it("does not render-throw a cancelled request, but a failed one", () => {
    const throwOnError = createQueryClient().getDefaultOptions().mutations?.throwOnError;
    if (typeof throwOnError !== "function") throw new Error("throwOnError is expected to be a function");

    expect(throwOnError(new CanceledError("canceled"))).toBe(false);
    expect(throwOnError(new Error("save failed"))).toBe(true);
  });
});

describe("createQueryClient unauthorizedEndsSession", () => {
  it("runs a same-origin 401 query once and does not render-throw it", async () => {
    const client = createQueryClient({ unauthorizedEndsSession: true });
    const { httpClient, requests } = createUnauthorizedHttpClient();

    const error = await runFailingQuery(client, () => httpClient.get("/api/books/42"));

    expect(requests).toHaveLength(1);
    const throwOnError = client.getDefaultOptions().queries?.throwOnError;
    if (typeof throwOnError !== "function") throw new Error("throwOnError is expected to be a function");
    expect(throwOnError(error as Error, { state: { data: undefined } } as never)).toBe(false);
  });

  it("retries a foreign 401 query twice and render-throws it", async () => {
    const client = createQueryClient({ unauthorizedEndsSession: true });
    const { httpClient, requests } = createUnauthorizedHttpClient();

    const error = await runFailingQuery(client, () => httpClient.get("https://other.example/api/x"));

    expect(requests).toHaveLength(3);
    const throwOnError = client.getDefaultOptions().queries?.throwOnError;
    if (typeof throwOnError !== "function") throw new Error("throwOnError is expected to be a function");
    expect(throwOnError(error as Error, { state: { data: undefined } } as never)).toBe(true);
  });

  it("retries and render-throws a same-origin 401 query when the option is off", async () => {
    const client = createQueryClient();
    const { httpClient, requests } = createUnauthorizedHttpClient();

    await runFailingQuery(client, () => httpClient.get("/api/books/42"));

    expect(requests).toHaveLength(3);
  });

  it("skips onMutationError and the render-throw for a same-origin 401 mutation", async () => {
    const onMutationError = vi.fn();
    const client = createQueryClient({ onMutationError, unauthorizedEndsSession: true });
    const { httpClient } = createUnauthorizedHttpClient();

    await runMutation(client, { mutationFn: () => httpClient.put("/api/books/42") });

    expect(onMutationError).not.toHaveBeenCalled();
    const throwOnError = client.getDefaultOptions().mutations?.throwOnError;
    if (typeof throwOnError !== "function") throw new Error("throwOnError is expected to be a function");
    expect(throwOnError(client.getMutationCache().getAll()[0]?.state.error as Error)).toBe(false);
  });

  it("reports a foreign 401 mutation to onMutationError", async () => {
    const onMutationError = vi.fn();
    const client = createQueryClient({ onMutationError, unauthorizedEndsSession: true });
    const { httpClient } = createUnauthorizedHttpClient();

    await runMutation(client, { mutationFn: () => httpClient.put("https://other.example/api/x") });

    expect(onMutationError).toHaveBeenCalledOnce();
  });
});

describe("createQueryClient service unavailable", () => {
  const config: InternalAxiosRequestConfig = { headers: new AxiosHeaders() };
  const errorWithStatus = (status: number) =>
    new AxiosError("failed", AxiosError.ERR_BAD_RESPONSE, config, undefined, {
      data: undefined,
      status,
      statusText: "",
      headers: {},
      config,
    });
  const unavailableErrors = [errorWithStatus(502), errorWithStatus(503), errorWithStatus(504), new TypeError("Failed to fetch")];
  const queryWithData = (data: unknown) => ({ state: { data } }) as never;

  it.each(unavailableErrors)("keeps a background query with data from render-throwing %s", (error) => {
    const throwOnError = createQueryClient().getDefaultOptions().queries?.throwOnError;
    if (typeof throwOnError !== "function") throw new Error("throwOnError is expected to be a function");

    expect(throwOnError(error, queryWithData(["book"]))).toBe(false);
  });

  it.each(unavailableErrors)("render-throws %s for a foreground query without data", (error) => {
    const throwOnError = createQueryClient().getDefaultOptions().queries?.throwOnError;
    if (typeof throwOnError !== "function") throw new Error("throwOnError is expected to be a function");

    expect(throwOnError(error, queryWithData(undefined))).toBe(true);
  });

  it.each([301, 404, 409, 500, 501])("render-throws HTTP %i for a query that holds data", (status) => {
    const throwOnError = createQueryClient().getDefaultOptions().queries?.throwOnError;
    if (typeof throwOnError !== "function") throw new Error("throwOnError is expected to be a function");

    expect(throwOnError(errorWithStatus(status), queryWithData(["book"]))).toBe(true);
  });

  it("render-throws a mutation failing with 404 or 500, but not with 502, 503, 504 or a network error", () => {
    const throwOnError = createQueryClient().getDefaultOptions().mutations?.throwOnError;
    if (typeof throwOnError !== "function") throw new Error("throwOnError is expected to be a function");

    expect([errorWithStatus(404), errorWithStatus(500)].map((error) => throwOnError(error))).toEqual([true, true]);
    expect(unavailableErrors.map((error) => throwOnError(error))).toEqual([false, false, false, false]);
  });

  it("runs a mutation failing with 502 once and reports it to onMutationError", async () => {
    const onMutationError = vi.fn();
    const mutationFn = vi.fn(() => Promise.reject(errorWithStatus(502)));

    await runMutation(createQueryClient({ onMutationError }), { mutationFn });

    expect(mutationFn).toHaveBeenCalledOnce();
    expect(onMutationError).toHaveBeenCalledOnce();
  });
});
