import { render, waitFor } from "@testing-library/react";
import { StrictMode, useRef } from "react";
import { describe, expect, it, beforeEach, afterEach } from "vitest";
import { useAutoPageTitle } from "./useAutoPageTitle";

function TestComponent({ appName, htmlTitle }: { appName: string; htmlTitle?: string }) {
  const containerRef = useRef<HTMLDivElement>(null);
  useAutoPageTitle(containerRef, { appName });

  return (
    <div ref={containerRef}>
      <nav aria-label="breadcrumb">
        <ol>
          <li>Home</li>
          <li>Books</li>
        </ol>
      </nav>
      {htmlTitle !== undefined && <span data-explicit-page-title={htmlTitle} hidden />}
      <h1>Page Heading</h1>
    </div>
  );
}

function TestComponentNoBreadcrumbs({ appName }: { appName: string }) {
  const containerRef = useRef<HTMLDivElement>(null);
  useAutoPageTitle(containerRef, { appName });

  return (
    <div ref={containerRef}>
      <h1>Only Heading</h1>
    </div>
  );
}

function TestComponentNoHeading({ appName }: { appName: string }) {
  const containerRef = useRef<HTMLDivElement>(null);
  useAutoPageTitle(containerRef, { appName });

  return (
    <div ref={containerRef}>
      <nav aria-label="breadcrumb">
        <ol>
          <li aria-hidden="true">/</li>
          <li>Last Crumb</li>
        </ol>
      </nav>
    </div>
  );
}

function TestComponentEmpty({ appName }: { appName: string }) {
  const containerRef = useRef<HTMLDivElement>(null);
  useAutoPageTitle(containerRef, { appName });

  return <div ref={containerRef}></div>;
}

function TestPage({ appName, heading }: { appName: string; heading: string }) {
  const containerRef = useRef<HTMLDivElement>(null);
  useAutoPageTitle(containerRef, { appName });

  return (
    <div ref={containerRef}>
      <h1>{heading}</h1>
    </div>
  );
}

describe("useAutoPageTitle", () => {
  let originalTitle: string;

  beforeEach(() => {
    originalTitle = document.title;
  });

  afterEach(() => {
    document.title = originalTitle;
  });

  it("derives title from h1 with app name suffix", async () => {
    render(<TestComponent appName="My App" />);
    await waitFor(() => {
      expect(document.title).toBe("Page Heading · My App");
    });
  });

  it("explicit title takes precedence over h1", async () => {
    render(<TestComponent appName="My App" htmlTitle="Explicit Title" />);
    await waitFor(() => {
      expect(document.title).toBe("Explicit Title · My App");
    });
  });

  it("falls back to last breadcrumb when no h1 exists", async () => {
    render(<TestComponentNoHeading appName="My App" />);
    await waitFor(() => {
      expect(document.title).toBe("Last Crumb · My App");
    });
  });

  it("falls back to appName when no h1 and no breadcrumbs exist", async () => {
    render(<TestComponentEmpty appName="My App" />);
    await waitFor(() => {
      expect(document.title).toBe("My App");
    });
  });

  it("does not add suffix when derived title equals appName", async () => {
    render(<TestComponentEmpty appName="Fallback" />);
    await waitFor(() => {
      expect(document.title).toBe("Fallback");
    });
  });

  it("prefers h1 over breadcrumb when both are present", async () => {
    render(<TestComponentNoBreadcrumbs appName="My App" />);
    await waitFor(() => {
      expect(document.title).toBe("Only Heading · My App");
    });
  });

  it("restores the title found before mount when unmounting", async () => {
    document.title = "My App";

    const { unmount } = render(<TestPage appName="My App" heading="Page Heading" />);
    await waitFor(() => {
      expect(document.title).toBe("Page Heading · My App");
    });

    unmount();

    expect(document.title).toBe("My App");
  });

  it("keeps the new page title when one page unmounts while another mounts", async () => {
    document.title = "My App";

    const { rerender } = render(<TestPage key="page-a" appName="My App" heading="Page A" />);
    await waitFor(() => {
      expect(document.title).toBe("Page A · My App");
    });

    rerender(<TestPage key="page-b" appName="My App" heading="Page B" />);

    expect(document.title).toBe("Page B · My App");
  });

  it("keeps the title found before mount when the app name changes", async () => {
    document.title = "My App";

    const { rerender, unmount } = render(<TestPage appName="My App" heading="Page Heading" />);
    await waitFor(() => {
      expect(document.title).toBe("Page Heading · My App");
    });

    rerender(<TestPage appName="Meine App" heading="Page Heading" />);
    await waitFor(() => {
      expect(document.title).toBe("Page Heading · Meine App");
    });

    unmount();

    expect(document.title).toBe("My App");
  });

  it("sets the title when the effect is re-run on the same instance", async () => {
    document.title = "My App";

    render(
      <StrictMode>
        <TestPage appName="My App" heading="Page Heading" />
      </StrictMode>,
    );

    await waitFor(() => {
      expect(document.title).toBe("Page Heading · My App");
    });
  });

  it("sets the title again when the same page is mounted a second time", async () => {
    document.title = "My App";

    const { unmount } = render(<TestPage appName="My App" heading="Page Heading" />);
    await waitFor(() => {
      expect(document.title).toBe("Page Heading · My App");
    });
    unmount();

    render(<TestPage appName="My App" heading="Page Heading" />);

    await waitFor(() => {
      expect(document.title).toBe("Page Heading · My App");
    });
  });
});
