import { render } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { useDocumentTitle } from "./useDocumentTitle";

function TestScreen({ pageTitle }: { pageTitle: string }) {
  useDocumentTitle(pageTitle);

  return <div>Screen</div>;
}

describe("useDocumentTitle", () => {
  let originalTitle: string;

  beforeEach(() => {
    originalTitle = document.title;
  });

  afterEach(() => {
    document.title = originalTitle;
  });

  it("appends the title found on mount as app name suffix", () => {
    document.title = "My App";

    render(<TestScreen pageTitle="Sign In" />);

    expect(document.title).toBe("Sign In · My App");
  });

  it("does not add a suffix when the page title equals the app name", () => {
    document.title = "My App";

    render(<TestScreen pageTitle="My App" />);

    expect(document.title).toBe("My App");
  });

  it("restores the title found on mount when unmounting", () => {
    document.title = "My App";

    const { unmount } = render(<TestScreen pageTitle="Sign In" />);
    expect(document.title).toBe("Sign In · My App");

    unmount();

    expect(document.title).toBe("My App");
  });

  it("keeps the app name when the page title changes", () => {
    document.title = "My App";

    const { rerender, unmount } = render(<TestScreen pageTitle="Sign In" />);
    rerender(<TestScreen pageTitle="Create Account" />);

    expect(document.title).toBe("Create Account · My App");

    unmount();

    expect(document.title).toBe("My App");
  });
});
