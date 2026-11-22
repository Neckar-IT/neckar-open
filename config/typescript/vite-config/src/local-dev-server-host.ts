/** The loopback address of a process that receives no `VITE_HOST`. */
const LoopbackAddress = "127.0.0.1";

/** The values of `server.host` with which Vite listens on every interface. */
const EveryInterface: ReadonlySet<string> = new Set(["0.0.0.0", "::", "true"]);

/**
 * The value of `VITE_HOST` for `server.host`: the Gradle task `run` of a frontend and `tools/shell-lib/tmux.sh` set it to the Dev-IP,
 * which the local Traefik container reaches. A process without `VITE_HOST` receives `127.0.0.1`, which the local Traefik does not reach.
 */
export function localDevServerHost(): string {
  const viteHost = process.env.VITE_HOST;
  // An empty value counts as unset.
  if (viteHost === undefined || viteHost === "") {
    return LoopbackAddress;
  }
  if (EveryInterface.has(viteHost)) {
    throw new Error(`VITE_HOST is ${viteHost}, which listens on every interface; a dev server listens on a loopback address or the Dev-IP.`);
  }
  return viteHost;
}
