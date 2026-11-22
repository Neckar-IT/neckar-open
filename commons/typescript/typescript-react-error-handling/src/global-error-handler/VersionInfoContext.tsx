import type { AppImageInfo } from "@neckar-hub/typescript-utils/deploy-metadata";
import { createContext, useContext, type PropsWithChildren } from "react";

export type VersionInfoData = AppImageInfo;

const VersionInfoContext = createContext<VersionInfoData | null>(null);

export function VersionInfoProvider(props: PropsWithChildren<{ versionInfo: VersionInfoData }>) {
  return <VersionInfoContext.Provider value={props.versionInfo}>{props.children}</VersionInfoContext.Provider>;
}

export function useVersionInfo(): VersionInfoData {
  const context = useContext(VersionInfoContext);
  if (context === null) {
    throw new Error("useVersionInfo must be used within a VersionInfoProvider (wrap your app in <GlobalErrorHandler versionInfo={...}>)");
  }
  return context;
}
