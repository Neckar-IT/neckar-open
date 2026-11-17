/**
 * Rejects with [timeoutMessage] when [promise] has not settled within [timeoutMs], so a hung
 * dependency surfaces as a regular error instead of an indefinite pending state.
 *
 * The timer is cleared on every path, so a promise that settles first leaves nothing behind.
 */
export async function withTimeout<T>(promise: Promise<T>, timeoutMs: number, timeoutMessage: string): Promise<T> {
  let timeoutHandle: ReturnType<typeof setTimeout> | undefined = undefined;

  const timeout = new Promise<never>((_resolve, reject) => {
    timeoutHandle = setTimeout(() => reject(new Error(timeoutMessage)), timeoutMs);
  });

  try {
    return await Promise.race([promise, timeout]);
  } finally {
    clearTimeout(timeoutHandle);
  }
}
