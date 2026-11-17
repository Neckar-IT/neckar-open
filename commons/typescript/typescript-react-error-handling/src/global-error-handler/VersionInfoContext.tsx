/**
 * Imports
 */

import type { AppImageInfo } from "@neckar-hub/typescript-utils/deploy-metadata";
import { createContext, useContext, type PropsWithChildren } from "react";

/**
 * Type Declarations
 */

export type VersionInfoData = AppImageInfo;

/**
 * Context
 */

const VersionInfoContext = createContext<VersionInfoData | null>(null);

/**
 * Provider
 */

export function VersionInfoProvider(props: PropsWithChildren<{ versionInfo: VersionInfoData }>) {
  return <VersionInfoContext.Provider value={props.versionInfo}>{props.children}</VersionInfoContext.Provider>;
}

/**
 * Hook
 */

export function useVersionInfo(): VersionInfoData {
  const context = useContext(VersionInfoContext);
  if (context === null) {
    throw new Error("useVersionInfo must be used within a VersionInfoProvider (wrap your app in <GlobalErrorHandler versionInfo={...}>)");
  }
  return context;
}
