/**
 * Imports
 */

import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { createQueryClient, QueryClientProvider } from "@neckar-hub/typescript-react-query";
import type { ImpersonationCandidate, ImpersonationCandidatesResponse } from "@neckar-hub/typescript-utils/openapi-types";
import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ImpersonationDialog } from "./ImpersonationDialog";
import { ImpersonationStorageKey } from "./impersonationStorage";

/**
 * Fixture Data
 */

const ada: ImpersonationCandidate = {
  userId: "00000000-0000-0000-0000-000000000002",
  email: "ada@neckar.it",
  firstName: "Ada",
  lastName: "Lovelace",
  tenantId: "00000000-0000-0000-0000-0000000000a1",
  tenantName: null,
};

function candidatesResponse(candidates: ImpersonationCandidate[], totalElementsCount: number): ImpersonationCandidatesResponse {
  return {
    type: "good",
    data: candidates,
    paginationInfo: { pageIndex: 0, pageSize: 20, pageCount: 1, filteredElementsCount: totalElementsCount, totalElementsCount },
    filterState: {},
  };
}

function renderDialog(onSearchCandidates: () => Promise<ImpersonationCandidatesResponse>): void {
  render(
    <I18nProvider>
      <QueryClientProvider client={createQueryClient()}>
        <ImpersonationDialog open onOpenChange={() => {}} onSearchCandidates={onSearchCandidates} />
      </QueryClientProvider>
    </I18nProvider>,
  );
}

/**
 * Tests
 */

describe("ImpersonationDialog", () => {
  const originalLocation = window.location;
  const assign = vi.fn();

  beforeEach(() => {
    window.sessionStorage.clear();
    assign.mockReset();
    Object.defineProperty(window, "location", { value: { assign }, writable: true, configurable: true });
  });

  afterEach(() => {
    Object.defineProperty(window, "location", { value: originalLocation, writable: true, configurable: true });
  });

  it("searches with the typed text and one page of candidates", async () => {
    const onSearchCandidates = vi.fn(() => Promise.resolve(candidatesResponse([ada], 1)));
    renderDialog(onSearchCandidates);

    expect(await screen.findByRole("option", { name: /Ada Lovelace/ })).toBeInTheDocument();
    expect(onSearchCandidates).toHaveBeenCalledWith({ freeText: undefined, pageSize: 20 });

    fireEvent.change(screen.getByPlaceholderText("Name, email address or tenant"), { target: { value: "ada" } });

    await vi.waitFor(() => expect(onSearchCandidates).toHaveBeenCalledWith({ freeText: "ada", pageSize: 20 }));
  });

  it("makes the tab act as the picked candidate", async () => {
    renderDialog(() => Promise.resolve(candidatesResponse([ada], 1)));

    await userEvent.click(await screen.findByRole("option", { name: /Ada Lovelace/ }));
    await userEvent.click(screen.getByRole("button", { name: "Switch" }));

    expect(window.sessionStorage.getItem(ImpersonationStorageKey)).toBe(ada.userId);
    expect(assign).toHaveBeenCalledWith("/");
  });

  it("asks to narrow the search when the page holds fewer candidates than match", async () => {
    renderDialog(() => Promise.resolve(candidatesResponse([ada], 21)));

    expect(await screen.findByText("Not all users are shown. Refine the search.")).toBeInTheDocument();
  });
});
