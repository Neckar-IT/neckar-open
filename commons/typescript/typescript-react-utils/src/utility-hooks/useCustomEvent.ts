/**
 * Imports
 */

import { useEffect } from "react";

/**
 * Hook to add a listener for a custom event, and calls the callback function when the event is triggered
 */

export function useCustomEvent<T>(type: string, handler: (event: CustomEvent<T>) => void): void {
  useEffect(() => {
    const handleEvent = (event: Event) => {
      if (event.type === type) {
        const customEvent = event as CustomEvent<T>;
        handler(customEvent);
      }
    };

    window.document.addEventListener(type, handleEvent);

    return () => {
      window.document.removeEventListener(type, handleEvent);
    };
  }, [type, handler]);
}
