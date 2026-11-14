/**
 * Imports
 */

import { useMediaQuery } from "usehooks-ts";

/**
 * Possible devices
 */

export type DeviceType = "mobile" | "tablet" | "desktop";

/**
 * Configuration
 *
 * Breakpoints inspired by Tailwind CSS
 */

const MOBILE_BREAKPOINT = 640;
const TABLET_BREAKPOINT = 1280;

/**
 * Hook that determines the device type based on the screen width.
 */

export function useCurrentDevice() {
  const maxWidthOnMobile = MOBILE_BREAKPOINT - 1;
  const isMobile = useMediaQuery(`(max-width: ${maxWidthOnMobile.toString()}px)`);

  const maxWidthOnTablet = TABLET_BREAKPOINT - 1;
  const isTablet = useMediaQuery(`(min-width: ${MOBILE_BREAKPOINT.toString()}px) and (max-width: ${maxWidthOnTablet.toString()}px)`);

  const isDesktop = useMediaQuery(`(min-width: ${TABLET_BREAKPOINT.toString()}px)`);

  let currentDevice: DeviceType = "mobile";
  if (isTablet) {
    currentDevice = "tablet";
  } else if (isDesktop) {
    currentDevice = "desktop";
  }

  return { isMobile, isTablet, isDesktop, currentDevice };
}
