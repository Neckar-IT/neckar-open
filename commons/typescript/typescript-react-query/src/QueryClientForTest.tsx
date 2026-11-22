import { QueryClientProvider } from "@tanstack/react-query";
import { useState, type ReactElement, type ReactNode } from "react";
import { createQueryClientWithoutGcTimer } from "./createQueryClientWithoutGcTimer";

/**
 * Provides a query client of its own to every mount, with the defaults of `createQueryClient` and no
 * garbage-collection timer. Serves as `wrapper` of `render` and `renderHook` and as JSX element.
 */
export function QueryClientForTest({ children }: { children: ReactNode }): ReactElement {
  const [queryClient] = useState(createQueryClientWithoutGcTimer);
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}
