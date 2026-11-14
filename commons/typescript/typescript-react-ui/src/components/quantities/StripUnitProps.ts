export type StripUnitProps<T> = Omit<T, "unit" | "unitPosition" | "conversionFactor">;
