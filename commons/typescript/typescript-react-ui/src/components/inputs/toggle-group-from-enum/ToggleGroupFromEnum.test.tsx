import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ToggleGroupFromEnum } from "./ToggleGroupFromEnum";

/**
 * Test Setup
 *
 * Mirrors the collision `enumName` exists for: `None` means "no online monitoring" to a
 * `MonitoringSystem` and "without cable" to everything else, and the shared flat namespace can only
 * hold one of the two. The same lookup backs {@link SelectFromEnum}, whose options only render once
 * its popover is open — this component renders its items directly, so the assertion needs no
 * pointer interaction.
 */
const languageResources = {
  de: {
    translation: {
      enums: {
        None: "ohne Kabel",
        SunnyPortal: "SMA Sunny Portal",
        MonitoringSystem: {
          None: "Kein Online-Monitoring",
        },
      },
    },
  },
};

const MonitoringSystem = {
  SunnyPortal: "SunnyPortal",
  None: "None",
} as const;

function renderToggleGroup(enumName?: string): void {
  render(
    <I18nProvider languageResources={languageResources} options={{ lng: "de" }}>
      <ToggleGroupFromEnum enumObject={MonitoringSystem} enumName={enumName} />
    </I18nProvider>,
  );
}

describe("ToggleGroupFromEnum", () => {
  it("shows the shared label when no enum is named", () => {
    renderToggleGroup();

    expect(screen.getByText("ohne Kabel")).toBeInTheDocument();
  });

  it("shows the qualified label of the named enum instead of the shared one", () => {
    renderToggleGroup("MonitoringSystem");

    expect(screen.getByText("Kein Online-Monitoring")).toBeInTheDocument();
    expect(screen.queryByText("ohne Kabel")).not.toBeInTheDocument();
  });

  it("falls back to the shared label for a value the named enum does not qualify", () => {
    renderToggleGroup("MonitoringSystem");

    expect(screen.getByText("SMA Sunny Portal")).toBeInTheDocument();
  });
});
