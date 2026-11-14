import { afterAll, vi } from "vitest";

// A worker that does not isolate test files keeps the modules a file mocked in the module cache for the next file.
afterAll(() => {
  vi.resetModules();
});
