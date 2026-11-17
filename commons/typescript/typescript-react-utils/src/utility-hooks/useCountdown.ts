import { useCountdown as useCountdownUpstream, useIsomorphicLayoutEffect } from "usehooks-ts";
import { isTestMode } from "../test-mode/isTestMode";

type UpstreamCountdownOptions = Parameters<typeof useCountdownUpstream>[0];

/** The countdown options of `usehooks-ts`, plus `initiallyRunning`. */
interface CountdownOptions extends UpstreamCountdownOptions {
  /**
   * Starts the countdown on mount. Ignored in test mode (e.g. for screenshot tests).
   * @default false
   */
  initiallyRunning?: boolean;
}

/** `useCountdown` of `usehooks-ts` with `initiallyRunning`, which upstream lacks. */
export function useCountdown({ initiallyRunning = false, ...upstreamOptions }: CountdownOptions): ReturnType<typeof useCountdownUpstream> {
  const countdown = useCountdownUpstream(upstreamOptions);
  const { startCountdown } = countdown[1];

  // A layout effect starts the countdown before the first paint.
  useIsomorphicLayoutEffect(() => {
    // Only the mount decides: a later change of initiallyRunning must not restart a stopped countdown.
    if (initiallyRunning && !isTestMode()) {
      startCountdown();
    }
  }, []);

  return countdown;
}
