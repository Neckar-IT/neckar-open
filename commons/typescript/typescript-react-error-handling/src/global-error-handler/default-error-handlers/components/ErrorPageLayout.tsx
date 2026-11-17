/**
 * Prop Types
 */

interface ErrorPageLayoutProps {
  children: React.ReactNode;
}

interface ErrorPageViewportProps {
  children: React.ReactNode;
}

/**
 * Error Page Layout
 *
 * Stacks the sections of an error page and caps their width for all of them at once. Without the
 * cap a long response body or stack trace widens the column and pushes the other sections to the
 * viewport edge.
 */

export function ErrorPageLayout(props: ErrorPageLayoutProps) {
  return (
    <ErrorPageViewport>
      <div className="flex w-full max-w-screen-md flex-col items-center gap-4">{props.children}</div>
    </ErrorPageViewport>
  );
}

/**
 * Error Page Viewport
 *
 * Centers an error page in the window and keeps its content off the window edges. For a page whose
 * content is a single stacked column, [ErrorPageLayout] adds that column.
 */

export function ErrorPageViewport(props: ErrorPageViewportProps) {
  return <div className="flex min-h-screen items-center justify-center px-6 py-12">{props.children}</div>;
}
