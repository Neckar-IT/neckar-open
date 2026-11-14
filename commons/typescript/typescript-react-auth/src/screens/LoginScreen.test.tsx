/**
 * Imports
 */

import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { createQueryClient, QueryClientProvider } from "@neckar-hub/typescript-react-query";
import type { LoginResponse } from "@neckar-hub/typescript-utils/openapi-types";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { LoginScreen } from "./LoginScreen";

/**
 * Tests
 */

async function submitLoginAnswering(response: LoginResponse): Promise<void> {
  render(
    <I18nProvider>
      <QueryClientProvider client={createQueryClient()}>
        <LoginScreen onLogin={vi.fn().mockResolvedValue(response)} />
      </QueryClientProvider>
    </I18nProvider>,
  );

  await userEvent.type(screen.getByLabelText(/Email address/), "mmueller");
  await userEvent.type(screen.getByLabelText(/Password/), "secret");
  await userEvent.click(screen.getByRole("button", { name: "Sign In" }));
}

describe("LoginScreen", () => {
  it("shows the invalid credentials message for bad/InvalidCredentials", async () => {
    await submitLoginAnswering({ type: "bad/InvalidCredentials" });

    expect(await screen.findByText("Invalid username or password.")).toBeInTheDocument();
  });

  it("shows the disabled user message for bad/UserDisabled", async () => {
    await submitLoginAnswering({ type: "bad/UserDisabled" });

    expect(await screen.findByText("User is disabled.")).toBeInTheDocument();
    expect(screen.queryByText("Invalid username or password.")).not.toBeInTheDocument();
  });
});
