import { render, screen } from "@test/test-utils";
import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { toast } from "./toast";

const toastWarningMock = vi.fn();
beforeEach(() => {
  toastWarningMock.mockReset();
  vi.spyOn(toast, "warning").mockImplementation(toastWarningMock);
});

import { warnAboutActiveUsage } from "./warnAboutActiveUsage";

describe("warnAboutActiveUsage", () => {
  beforeEach(() => {
    toastWarningMock.mockClear();
  });

  it("warns with the sum of the active counts", () => {
    warnAboutActiveUsage({ counts: [{ activeCount: 2 }, { activeCount: 1 }] });

    expect(toastWarningMock).toHaveBeenCalledOnce();
    render(toastWarningMock.mock.calls[0]?.[0] as ReactElement);
    expect(screen.getByText("3 active records still depend on the archived record.")).toBeInTheDocument();
  });

  it("warns in the singular for one active record", () => {
    warnAboutActiveUsage({ counts: [{ activeCount: 1 }] });

    render(toastWarningMock.mock.calls[0]?.[0] as ReactElement);
    expect(screen.getByText("1 active record still depends on the archived record.")).toBeInTheDocument();
  });

  it("stays silent for an empty usedBy", () => {
    warnAboutActiveUsage({ counts: [] });

    expect(toastWarningMock).not.toHaveBeenCalled();
  });

  it("stays silent when every count is zero", () => {
    warnAboutActiveUsage({ counts: [{ activeCount: 0 }] });

    expect(toastWarningMock).not.toHaveBeenCalled();
  });
});
