import { fireEvent, render, screen } from "@test/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { toast } from "../../feedback/toast";

const toastErrorMock = vi.fn();
beforeEach(() => {
  toastErrorMock.mockReset();
  vi.spyOn(toast, "error").mockImplementation(toastErrorMock);
});

import { useInvalidFormSubmitHandler } from "./useInvalidFormSubmitHandler";

function Probe(): React.ReactElement {
  const onInvalidSubmit = useInvalidFormSubmitHandler();
  return (
    <button type="button" onClick={() => onInvalidSubmit({})}>
      trigger
    </button>
  );
}

describe("useInvalidFormSubmitHandler", () => {
  it("shows an error toast when invoked", () => {
    render(<Probe />);
    fireEvent.click(screen.getByText("trigger"));
    expect(toastErrorMock).toHaveBeenCalledTimes(1);
  });
});
