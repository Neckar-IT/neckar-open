/**
 * The hosts a Vite dev server answers besides `localhost`, as `server.allowedHosts` takes them.
 *
 * Vite admits `localhost` and `*.localhost` only. `.localdev` admits `localdev` and every subdomain of it, so the hosts of every worktree slot
 * (`patterns.localdev`, `patterns3.localdev`, `admin.patterns3.localdev`) pass without a list of them; Vite reads no wildcard such as `*.localdev`.
 */
export const localDevAllowedHosts: string[] = [".localdev"];
