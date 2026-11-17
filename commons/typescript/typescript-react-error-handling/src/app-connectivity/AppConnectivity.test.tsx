import { createQueryClient, type QueryClient, QueryClientProvider, useQuery } from "@neckar-hub/typescript-react-query";
import { ActionButton } from "@neckar-hub/typescript-react-ui/actions";
import { mutationFailedToastId, toast } from "@neckar-hub/typescript-react-ui/feedback";
import type { HealthResponse, ServiceStateDto } from "@neckar-hub/typescript-utils/openapi-types";
import { act, render, renderHook, screen, waitFor } from "@test/test-utils";
import { Provider } from "jotai";
import { ErrorBoundary } from "react-error-boundary";
import { afterEach, describe, expect, it, vi } from "vitest";
import { makeAxiosError } from "../global-error-handler/axiosErrorFixture";
import { AppConnectivityProvider } from "./AppConnectivityProvider";
import {
  failureToServiceAvailability,
  HealthCheckQueryKey,
  pollFailureToServiceAvailability,
  pollStateToServiceAvailability,
  type ServiceAvailability,
  toServiceAvailability,
  useDeviceIsOnline,
  useDeviceOnlineListener,
} from "./appConnectivityState";

const setOnlineStatus = (isOnline: boolean) => {
  Object.defineProperty(window.navigator, "onLine", {
    value: isOnline,
    configurable: true,
  });
};

// The connectivity atoms live at module scope, so every test needs its own jotai store to start from their initial values.
const withStore = ({ children }: { children: React.ReactNode }) => <Provider>{children}</Provider>;

describe("useDeviceIsOnline", () => {
  it("should return true when the device is online", () => {
    setOnlineStatus(true);
    const { result } = renderHook(
      () => {
        useDeviceOnlineListener();
        return useDeviceIsOnline();
      },
      { wrapper: withStore },
    );
    expect(result.current).toBe(true);
  });

  it("should return false when the device is offline", () => {
    setOnlineStatus(false);
    const { result } = renderHook(
      () => {
        useDeviceOnlineListener();
        return useDeviceIsOnline();
      },
      { wrapper: withStore },
    );
    expect(result.current).toBe(false);
  });

  it("should follow the window's offline and online events", () => {
    setOnlineStatus(true);
    const { result } = renderHook(
      () => {
        useDeviceOnlineListener();
        return useDeviceIsOnline();
      },
      { wrapper: withStore },
    );
    expect(result.current).toBe(true);

    act(() => {
      setOnlineStatus(false);
      window.dispatchEvent(new Event("offline"));
    });
    expect(result.current).toBe(false);

    act(() => {
      setOnlineStatus(true);
      window.dispatchEvent(new Event("online"));
    });
    expect(result.current).toBe(true);
  });
});

describe("toServiceAvailability", () => {
  it.each<[ServiceStateDto, ServiceAvailability]>([
    ["ready", { type: "available", state: "ready" }],
    ["degraded", { type: "available", state: "degraded" }],
    ["starting", { type: "notReady", state: "starting" }],
    ["migrating", { type: "notReady", state: "migrating" }],
    ["dependency-unavailable", { type: "notReady", state: "dependency-unavailable" }],
    ["schema-incompatible", { type: "notReady", state: "schema-incompatible" }],
    ["migration-failed", { type: "notReady", state: "migration-failed" }],
    ["fatal", { type: "notReady", state: "fatal" }],
  ])("maps the state %s", (state, expected) => {
    expect(toServiceAvailability({ state })).toEqual(expected);
  });
});

describe("pollStateToServiceAvailability", () => {
  it("ends the outage on a same-origin 401 of the poll, also over the state of an earlier answer", () => {
    expect(pollStateToServiceAvailability({ data: healthResponse("migrating"), error: makeAxiosError({ status: 401 }) })).toEqual({
      type: "available",
      state: "ready",
    });
  });

  it.each<[string, unknown]>([
    ["a foreign-origin 401", makeAxiosError({ status: 401, url: "https://auth.example.com/token" })],
    ["a 403", makeAxiosError({ status: 403 })],
  ])("takes %s of the poll for a defect", (_name, error) => {
    expect(pollStateToServiceAvailability({ data: healthResponse("migrating"), error })).toBeUndefined();
  });
});

describe("failureToServiceAvailability", () => {
  it.each<[string, unknown, ServiceAvailability | undefined]>([
    ["502", makeAxiosError({ status: 502 }), { type: "unreachable" }],
    ["503", makeAxiosError({ status: 503 }), { type: "unreachable" }],
    ["504", makeAxiosError({ status: 504 }), { type: "unreachable" }],
    ["no status", makeAxiosError(), { type: "noConnection" }],
    ["301", makeAxiosError({ status: 301 }), undefined],
    ["404", makeAxiosError({ status: 404 }), undefined],
    ["500", makeAxiosError({ status: 500 }), undefined],
  ])("maps a request failing with %s", (_name, error, expected) => {
    expect(failureToServiceAvailability(error)).toEqual(expected);
  });
});

describe("pollFailureToServiceAvailability", () => {
  it.each<[string, unknown, ServiceAvailability | undefined]>([
    ["500", makeAxiosError({ status: 500 }), { type: "unreachable" }],
    ["505", makeAxiosError({ status: 505 }), { type: "unreachable" }],
    ["599", makeAxiosError({ status: 599 }), { type: "unreachable" }],
    ["no status", makeAxiosError(), { type: "noConnection" }],
    ["403", makeAxiosError({ status: 403 }), undefined],
    ["499", makeAxiosError({ status: 499 }), undefined],
  ])("maps a poll failing with %s", (_name, error, expected) => {
    expect(pollFailureToServiceAvailability(error)).toEqual(expected);
  });
});

function healthResponse(state: ServiceStateDto): HealthResponse {
  return { state, errorCount: 0, backgroundJobs: [] } as HealthResponse;
}

function renderProvider(
  healthCheckFn: () => Promise<HealthResponse>,
  children: React.ReactNode,
  queryClient: QueryClient = createQueryClient(),
) {
  return render(
    <Provider>
      <QueryClientProvider client={queryClient}>
        <ErrorBoundary fallbackRender={() => <div>error boundary</div>}>
          <AppConnectivityProvider healthCheckFn={healthCheckFn}>{children}</AppConnectivityProvider>
        </ErrorBoundary>
      </QueryClientProvider>
    </Provider>,
  );
}

describe("AppConnectivityProvider", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("should render the offline message if the device is offline", () => {
    setOnlineStatus(false);
    renderProvider(() => Promise.resolve(healthResponse("ready")), "App");
    expect(screen.getByText("You are offline.")).toBeInTheDocument();
  });

  it.each<[ServiceStateDto, string]>([
    ["degraded", "Some features are limited right now."],
    ["starting", "The application is starting."],
    ["migrating", "Maintenance is in progress."],
    ["dependency-unavailable", "The application is briefly unavailable."],
    ["schema-incompatible", "The application is unavailable right now. We have been notified."],
    ["migration-failed", "The application is unavailable right now. We have been notified."],
    ["fatal", "The application is unavailable right now. We have been notified."],
  ])("should show the banner of the state %s", async (state, text) => {
    setOnlineStatus(true);
    renderProvider(() => Promise.resolve(healthResponse(state)), "App");
    expect(await screen.findByText(text)).toBeInTheDocument();
  });

  it.each<[string, unknown, string]>([
    ["502", makeAxiosError({ status: 502 }), "The application cannot be reached right now."],
    ["503", makeAxiosError({ status: 503 }), "The application cannot be reached right now."],
    ["504", makeAxiosError({ status: 504 }), "The application cannot be reached right now."],
    ["a network error", makeAxiosError(), "No connection to the application."],
  ])("should show the banner for a poll failing with %s", async (_name, error, text) => {
    setOnlineStatus(true);
    renderProvider(() => Promise.reject(error), "App");
    expect(await screen.findByText(text)).toBeInTheDocument();
  });

  it("should show no banner while the service is ready", async () => {
    setOnlineStatus(true);
    const queryClient = createQueryClient();
    renderProvider(() => Promise.resolve(healthResponse("ready")), "App", queryClient);
    await waitFor(() => expect(queryClient.getQueryState(HealthCheckQueryKey)?.status).toBe("success"));
    expect(screen.queryByText("The application cannot be reached right now.")).not.toBeInTheDocument();
    expect(screen.queryByText("Some features are limited right now.")).not.toBeInTheDocument();
  });

  it("should leave a same-origin 401 of the poll to the auth provider under unauthorizedEndsSession", async () => {
    setOnlineStatus(true);
    const queryClient = createQueryClient({ unauthorizedEndsSession: true });
    renderProvider(() => Promise.reject(makeAxiosError({ status: 401 })), "App", queryClient);

    await waitFor(() => expect(queryClient.getQueryState(HealthCheckQueryKey)?.status).toBe("error"));
    expect(screen.queryByText("error boundary")).not.toBeInTheDocument();
    expect(screen.getByText("App")).toBeInTheDocument();
  });

  it.each([301, 401, 403, 404])("should hand a poll failing with %i to the error boundary", async (status) => {
    setOnlineStatus(true);
    renderProvider(() => Promise.reject(makeAxiosError({ status })), "App");
    expect(await screen.findByText("error boundary")).toBeInTheDocument();
  });

  it.each([500, 501])("should show the service as unreachable for a poll failing with %i, as from a login gateway", async (status) => {
    setOnlineStatus(true);
    renderProvider(() => Promise.reject(makeAxiosError({ status })), "App");
    expect(await screen.findByText("The application cannot be reached right now.")).toBeInTheDocument();
    expect(screen.queryByText("error boundary")).not.toBeInTheDocument();
  });

  it("should lock every action below while the device is offline", () => {
    setOnlineStatus(false);
    const onTrigger = vi.fn();
    renderProvider(() => Promise.resolve(healthResponse("ready")), <ActionButton action={{ icon: undefined, label: "Save", onTrigger }} />);

    const button = screen.getByRole("button", { name: "Save" });
    button.click();

    expect(button).toBeDisabled();
    expect(onTrigger).not.toHaveBeenCalled();
  });

  it("should lock every action below while the service is not ready", async () => {
    setOnlineStatus(true);
    renderProvider(
      () => Promise.resolve(healthResponse("migrating")),
      <ActionButton action={{ icon: undefined, label: "Save", onTrigger: vi.fn() }} />,
    );

    await waitFor(() => expect(screen.getByRole("button", { name: "Save" })).toBeDisabled());
  });

  it("should leave actions enabled while the service is degraded", async () => {
    setOnlineStatus(true);
    renderProvider(
      () => Promise.resolve(healthResponse("degraded")),
      <ActionButton action={{ icon: undefined, label: "Save", onTrigger: vi.fn() }} />,
    );

    expect(await screen.findByText("Some features are limited right now.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Save" })).toBeEnabled();
  });

  it.each([502, 503])("should name the health state in the toast of a mutation failing with %i", async (status) => {
    setOnlineStatus(true);
    const toastError = vi.spyOn(toast, "error").mockImplementation(() => "toast");
    const queryClient = createQueryClient();
    renderProvider(() => Promise.resolve(healthResponse("migrating")), null, queryClient);

    const mutation = queryClient.getMutationCache().build(queryClient, { mutationFn: () => Promise.reject(makeAxiosError({ status })) });
    await mutation.execute(undefined).catch(() => undefined);

    await waitFor(() => expect(toastError).toHaveBeenCalledWith("Not saved. Maintenance is in progress.", { id: mutationFailedToastId }));
  });

  it("should say only that a mutation was not saved while the poll answers ready", async () => {
    setOnlineStatus(true);
    const toastError = vi.spyOn(toast, "error").mockImplementation(() => "toast");
    const queryClient = createQueryClient();
    renderProvider(() => Promise.resolve(healthResponse("ready")), null, queryClient);

    const mutation = queryClient
      .getMutationCache()
      .build(queryClient, { mutationFn: () => Promise.reject(makeAxiosError({ status: 503 })) });
    await mutation.execute(undefined).catch(() => undefined);

    await waitFor(() => expect(toastError).toHaveBeenCalledWith("Not saved.", { id: mutationFailedToastId }));
  });

  it("should keep a newer mutation failure from the late toast of a mutation that was not executed", async () => {
    setOnlineStatus(true);
    const toastError = vi.spyOn(toast, "error").mockImplementation(() => "toast");
    let answerPoll: (response: HealthResponse) => void = () => undefined;
    const healthCheckFn = vi
      .fn<() => Promise<HealthResponse>>()
      .mockResolvedValueOnce(healthResponse("ready"))
      .mockImplementation(() => new Promise((resolve) => (answerPoll = resolve)));
    const queryClient = createQueryClient();
    renderProvider(healthCheckFn, null, queryClient);
    await waitFor(() => expect(healthCheckFn).toHaveBeenCalledOnce());

    const fail = (status: number) =>
      queryClient
        .getMutationCache()
        .build(queryClient, { mutationFn: () => Promise.reject(makeAxiosError({ status })) })
        .execute(undefined)
        .catch(() => undefined);
    await fail(503);
    await waitFor(() => expect(healthCheckFn).toHaveBeenCalledTimes(2));
    await fail(500);
    await act(async () => answerPoll(healthResponse("migrating")));

    expect(toastError).not.toHaveBeenCalled();
  });

  it("should poll before the interval when a mutation has an unknown outcome", async () => {
    setOnlineStatus(true);
    const healthCheckFn = vi.fn(() => Promise.resolve(healthResponse("ready")));
    const queryClient = createQueryClient();
    renderProvider(healthCheckFn, null, queryClient);
    await waitFor(() => expect(healthCheckFn).toHaveBeenCalledOnce());

    const mutation = queryClient
      .getMutationCache()
      .build(queryClient, { mutationFn: () => Promise.reject(makeAxiosError({ status: 504 })) });
    await mutation.execute(undefined).catch(() => undefined);

    await waitFor(() => expect(healthCheckFn).toHaveBeenCalledTimes(2));
  });

  it("should say only that a mutation was not saved while the poll answers degraded", async () => {
    setOnlineStatus(true);
    const toastError = vi.spyOn(toast, "error").mockImplementation(() => "toast");
    const queryClient = createQueryClient();
    renderProvider(() => Promise.resolve(healthResponse("degraded")), null, queryClient);

    const mutation = queryClient
      .getMutationCache()
      .build(queryClient, { mutationFn: () => Promise.reject(makeAxiosError({ status: 503 })) });
    await mutation.execute(undefined).catch(() => undefined);

    await waitFor(() => expect(toastError).toHaveBeenCalledWith("Not saved.", { id: mutationFailedToastId }));
  });

  it("should poll before the interval when a query fails with 503", async () => {
    setOnlineStatus(true);
    const healthCheckFn = vi.fn(() => Promise.resolve(healthResponse("ready")));
    function FailingQuery() {
      useQuery({ queryKey: ["books"], queryFn: () => Promise.reject(makeAxiosError({ status: 503 })), retry: false });
      return null;
    }

    renderProvider(healthCheckFn, <FailingQuery />);

    await waitFor(() => expect(healthCheckFn.mock.calls.length).toBeGreaterThanOrEqual(2));
  });

  it("should fetch a query that keeps failing with 503 again at most once per half poll interval while the poll answers ready", async () => {
    setOnlineStatus(true);
    const healthCheckFn = vi.fn(() => Promise.resolve(healthResponse("ready")));
    const queryFn = vi.fn(() => Promise.reject(makeAxiosError({ status: 503 })));
    const queryClient = createQueryClient();
    queryClient.setQueryData(["books"], "cached books");
    function Books() {
      const result = useQuery({ queryKey: ["books"], queryFn, retry: false });
      return <div>{result.response}</div>;
    }

    renderProvider(healthCheckFn, <Books />, queryClient);
    await waitFor(() => expect(queryFn).toHaveBeenCalled());
    await act(() => new Promise((resolve) => setTimeout(resolve, 300)));

    // The default poll interval is 5000 ms: the first fetch plus at most one recovery round.
    expect(queryFn.mock.calls.length).toBeLessThanOrEqual(2);
    expect(screen.getByText("cached books")).toBeInTheDocument();
  });

  it("should render `ServiceUnavailablePage` for a query without data of an unavailable service and fetch again once the service is ready", async () => {
    setOnlineStatus(true);
    let state: ServiceStateDto = "migrating";
    let failing = true;
    const queryClient = createQueryClient();
    function Books() {
      const result = useQuery({
        queryKey: ["books"],
        queryFn: () => (failing ? Promise.reject(makeAxiosError({ status: 503 })) : Promise.resolve("books loaded")),
        retry: false,
      });
      return <div>{result.response}</div>;
    }

    render(
      <Provider>
        <QueryClientProvider client={queryClient}>
          <AppConnectivityProvider healthCheckFn={() => Promise.resolve(healthResponse(state))} refetchInterval={50}>
            <Books />
          </AppConnectivityProvider>
        </QueryClientProvider>
      </Provider>,
    );

    expect(await screen.findByRole("button", { name: "Back" })).toBeInTheDocument();
    expect(await screen.findAllByText("Maintenance is in progress.")).not.toHaveLength(0);

    failing = false;
    state = "ready";

    // The data shows up in the same document: the recovery needs no page reload.
    expect(await screen.findByText("books loaded")).toBeInTheDocument();
  });

  it("should let a slow refetch of a query with data through the following recovery rounds", async () => {
    setOnlineStatus(true);
    const queryFn = vi
      .fn<() => Promise<string>>()
      .mockRejectedValueOnce(makeAxiosError({ status: 503 }))
      .mockImplementation(() => new Promise((resolve) => setTimeout(() => resolve("fresh books"), 300)));
    const queryClient = createQueryClient();
    queryClient.setQueryData(["books"], "cached books");
    function Books() {
      const result = useQuery({ queryKey: ["books"], queryFn, retry: false });
      return <div>{result.response}</div>;
    }

    render(
      <Provider>
        <QueryClientProvider client={queryClient}>
          <AppConnectivityProvider healthCheckFn={() => Promise.resolve(healthResponse("ready"))} refetchInterval={50}>
            <Books />
          </AppConnectivityProvider>
        </QueryClientProvider>
      </Provider>,
    );

    // The answer takes six poll intervals; a round cancelling it would never show the fresh data.
    expect(await screen.findByText("fresh books", undefined, { timeout: 2000 })).toBeInTheDocument();
    expect(queryFn).toHaveBeenCalledTimes(2);
  });

  it("should leave `ServiceUnavailablePage` once the poll answers with the 401 of an ended session", async () => {
    setOnlineStatus(true);
    let pollAnswer: () => Promise<HealthResponse> = () => Promise.resolve(healthResponse("migrating"));
    let failing = true;
    function Books() {
      const result = useQuery({
        queryKey: ["books"],
        queryFn: () => (failing ? Promise.reject(makeAxiosError({ status: 503 })) : Promise.resolve("books loaded")),
        retry: false,
      });
      return <div>{result.response}</div>;
    }

    render(
      <Provider>
        <QueryClientProvider client={createQueryClient({ unauthorizedEndsSession: true })}>
          <AppConnectivityProvider healthCheckFn={() => pollAnswer()} refetchInterval={50}>
            <Books />
          </AppConnectivityProvider>
        </QueryClientProvider>
      </Provider>,
    );
    expect(await screen.findByRole("button", { name: "Back" })).toBeInTheDocument();

    failing = false;
    pollAnswer = () => Promise.reject(makeAxiosError({ status: 401 }));

    // The children mount again, so an auth provider among them can show the sign-in-again screen.
    expect(await screen.findByText("books loaded")).toBeInTheDocument();
  });

  it("should keep the data of a query with data of an unavailable service and render no page", async () => {
    setOnlineStatus(true);
    const queryClient = createQueryClient();
    queryClient.setQueryData(["books"], "cached books");
    function Books() {
      const result = useQuery({ queryKey: ["books"], queryFn: () => Promise.reject(makeAxiosError({ status: 502 })), retry: false });
      return <div>{result.response}</div>;
    }

    renderProvider(() => Promise.reject(makeAxiosError({ status: 502 })), <Books />, queryClient);

    expect(await screen.findByText("The application cannot be reached right now.")).toBeInTheDocument();
    expect(screen.getByText("cached books")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Back" })).not.toBeInTheDocument();
  });
});
