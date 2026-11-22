import { describe, expect, it } from "vitest";
import { keycloakServerUrl } from "./keycloakServerUrl";

describe("keycloakServerUrl", () => {
  it("names the local Keycloak for a host of the local stack", () => {
    expect(keycloakServerUrl("ideas1.localdev")).toBe("https://auth.localdev");
  });

  it.each(["ideas.neckar.it", "ideas-dev.neckar.it", "localdev.example.com"])("names the production Keycloak for %s", (hostname) => {
    expect(keycloakServerUrl(hostname)).toBe("https://auth.neckar.it");
  });
});
