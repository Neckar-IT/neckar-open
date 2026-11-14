import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { FileSizeOutput } from "./FileSizeOutput";

describe("FileSizeOutput", () => {
  it("should render the size in the unit that fits it", () => {
    render(
      <>
        <FileSizeOutput data-testid="bytes" bytes={512} />
        <FileSizeOutput data-testid="kilobytes" bytes={12 * 1024} />
        <FileSizeOutput data-testid="megabytes" bytes={2.5 * 1024 * 1024} />
      </>,
    );
    expect(screen.getByTestId("bytes").textContent).toBe("512 B");
    expect(screen.getByTestId("kilobytes").textContent).toBe("12 KB");
    expect(screen.getByTestId("megabytes").textContent).toBe("2.5 MB");
  });

  it("should render NoValue when bytes is null or undefined", () => {
    render(
      <>
        <FileSizeOutput data-testid="null-value" bytes={null} />
        <FileSizeOutput data-testid="undefined-value" bytes={undefined} />
      </>,
    );
    expect(screen.getByTestId("null-value").textContent).toBe("-");
    expect(screen.getByTestId("undefined-value").textContent).toBe("-");
  });
});
