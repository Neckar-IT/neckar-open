import { useEffect } from "react";
import type { StorybookColorMode } from "./storybookPreviewParameters";

/** Sets the class `light` or `dark` on `<body>`, where the Tailwind `dark` variant of the stories reads it. */
export function useStorybookColorModeClass(colorMode: StorybookColorMode): void {
  useEffect(() => {
    window.document.body.classList.remove("light", "dark");
    window.document.body.classList.add(colorMode);
  }, [colorMode]);
}
