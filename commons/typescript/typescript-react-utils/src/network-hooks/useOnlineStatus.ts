/**
 * useOnlineStatus Hook
 *
 * This hook can be used to track the online status of the client as well as the server.
 * It takes in an Array of functions with corresponding offline messages and returns a status string:
 * 'online' | 'offline' | 'server-not-available'
 *
 * By default it checks the online status of the server every 30 seconds, but this interval can be adjusted via the
 * options object.
 */

/**
 * Imports
 */

import { useEffect, useState } from "react";

/**
 * Type Declarations
 */

interface Options {
  interval: number;
}

export interface HeartbeatRequest {
  request: () => Promise<unknown>;
  message: string;
}

type Status = "online" | "offline" | "server-not-available";

/**
 * useOnlineStatus Hook
 */

export function useOnlineStatus(
  requests: HeartbeatRequest | HeartbeatRequest[],
  options: Options = { interval: 30 },
): { status: Status; timeUntilRetry: number } {
  const [browserIsOnline, setBrowserIsOnline] = useState<boolean>(true);
  const [serverIsOnline, setServerIsOnline] = useState<boolean>(true);
  const [timeUntilRetry, setTimeUntilRetry] = useState<number>(0);

  // check for internet connection
  useEffect(() => {
    const updateConnection = () => {
      if (window.navigator.onLine) {
        setBrowserIsOnline(true);
      } else {
        setBrowserIsOnline(false);
      }
    };

    updateConnection();

    window.addEventListener("online", updateConnection);
    window.addEventListener("offline", updateConnection);

    return () => {
      window.removeEventListener("online", updateConnection);
      window.removeEventListener("offline", updateConnection);
    };
  }, []);

  // check for heartbeats
  useEffect(() => {
    const checkHeartbeats = async () => {
      const requestsArray = Array.isArray(requests) ? requests : [requests];

      if (browserIsOnline && timeUntilRetry <= 0) {
        await Promise.allSettled(requestsArray.map(({ request }) => request())).then((results) => {
          const firstFailedRequestIndex = results.findIndex((result) => result.status === "rejected");

          if (firstFailedRequestIndex !== -1) {
            setServerIsOnline(false);
          } else {
            setServerIsOnline(true);
          }

          setTimeUntilRetry(options.interval);

          for (const [index, { message }] of requestsArray.entries()) {
            if (results[index]?.status === "rejected") {
              console.error(message);
            }
          }
          return undefined;
        });
      }
    };

    void checkHeartbeats();
  }, [requests, options, browserIsOnline, timeUntilRetry]);

  useEffect(() => {
    const intervalId = window.setInterval(() => {
      setTimeUntilRetry((prev) => prev - 1);
    }, 1000);

    return () => {
      window.clearInterval(intervalId);
    };
  }, []);

  let status: Status;
  if (!browserIsOnline) status = "offline";
  else if (!serverIsOnline) status = "server-not-available";
  else status = "online";

  return { status, timeUntilRetry };
}
