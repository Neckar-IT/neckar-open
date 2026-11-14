/**
 * Component that renders it's children after a delay. This is useful for preventing flickering of loading indicators
 * for example.
 */

/**
 * Imports
 */

import { isTestMode } from "@neckar-hub/typescript-react-utils/is-test-mode";
import type { PropsWithChildren, ReactNode } from "react";
import { useEffect, useState } from "react";

/**
 * Prop Types
 */

type Props = PropsWithChildren<{
  delay?: number | undefined;
}>;

/**
 * Component
 *
 * @prop delay - The delay in milliseconds before the loading indicator is shown. Default is 300ms.
 */

export function Delayed({ delay = 300, children }: Props): ReactNode {
  // A screenshot of a story that waits on this timer shows the children or does not, depending on
  // which of the two won the race — so in test mode there is no waiting.
  const [isShown, setIsShown] = useState(isTestMode());

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setIsShown(true);
    }, delay);

    return () => {
      window.clearTimeout(timer);
    };
  }, [delay]);

  return isShown ? children : null;
}
