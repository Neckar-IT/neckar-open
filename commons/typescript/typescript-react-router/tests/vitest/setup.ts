/**
 * Imports
 */

import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";

/**
 * Setup Tests
 */

// run a cleanup after each test
afterEach(() => {
  cleanup();
});
