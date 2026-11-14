import type { ArticleConditionsTimelineEntry, ArticleConditionsTimelineTableEntry } from "@neckar-hub/typescript-utils/conditions-timeline";
import type {
  ArticleConditions,
  ArticleConditionsWithAccessory,
  ArticleConditionsWithInstallationTime,
  FullArticleConditions,
} from "@neckar-hub/typescript-utils/openapi-types";

/**
 * Conditions fixtures shared by the tests and stories of this folder: one priced as pure material
 * per piece (no unit — the per-piece default), then one per combination of the two optional parts
 * (accessory, installation time) priced by an explicit unit, plus one deliberately wide record.
 *
 * Each fixture is typed on the implementation whose parts it carries, so a component under test
 * gets exactly the shape its article type would hand it.
 */
export const conditionsWithAccessory: FullArticleConditions = {
  part: { inputPrice: 313045, sellingPrice: 438263 },
  unit: "Meter",
  accessory: { inputPrice: 15000, sellingPrice: 21000 },
  installationTime: "PT8H30M",
};

/**
 * An article of the widest type that happens to carry neither optional part — the case that shows
 * the difference between "this article type has no accessory" and "this article has none".
 */
export const materialOnlyConditions: FullArticleConditions = {
  part: { inputPrice: 12000, sellingPrice: 16800 },
  accessory: null,
  installationTime: null,
};

/** An article type that has nothing beyond the part — a book. */
export const bareConditions: ArticleConditions = {
  part: { inputPrice: 1200, sellingPrice: 1990 },
};

/** Accessory surcharge, no installation time. */
export const accessoryOnlyConditions: ArticleConditionsWithAccessory = {
  part: { inputPrice: 4990, sellingPrice: 6990 },
  unit: "m",
  accessory: { inputPrice: 900, sellingPrice: 1290 },
};

/** An article of an accessory-carrying type that is supplied without one. */
export const withoutAccessoryConditions: ArticleConditionsWithAccessory = {
  part: { inputPrice: 4990, sellingPrice: 6990 },
  unit: "m",
  accessory: null,
};

/** Installation time, no accessory. */
export const installationTimeOnlyConditions: ArticleConditionsWithInstallationTime = {
  part: { inputPrice: 8500, sellingPrice: 11900 },
  unit: "m",
  installationTime: "PT45M",
};

/** Long unit, five-digit price, both optional parts set — the widest line the summary has to carry. */
export const wideConditions: FullArticleConditions = {
  part: { inputPrice: 1289000, sellingPrice: 1804600 },
  unit: "Rolle à 100 m",
  accessory: { inputPrice: 45000, sellingPrice: 63000 },
  installationTime: "PT12H15M",
};

export const currentConditionsEntry: ArticleConditionsTimelineEntry<FullArticleConditions> = {
  conditions: materialOnlyConditions,
  validFrom: "2026-01-01T00:00:00Z",
};

export const futureConditionsEntry: ArticleConditionsTimelineEntry<FullArticleConditions> = {
  conditions: conditionsWithAccessory,
  validFrom: "2099-06-01T00:00:00Z",
};

/**
 * A row of the conditions table for {@link conditionsWithAccessory}, priced the way the server answers it: per Meter,
 * 8,5 h at 75,00 €/h.
 */
export const fullConditionsTableEntry: ArticleConditionsTimelineTableEntry<FullArticleConditions> = {
  conditions: conditionsWithAccessory,
  validFrom: "2026-01-01T00:00:00Z",
  priceSummary: {
    unit: "Meter",
    part: { inputPrice: 313045, sellingPrice: 438263, surcharge: 125218, surchargeRate: 0.4 },
    accessory: { inputPrice: 15000, sellingPrice: 21000, surcharge: 6000, surchargeRate: 0.4 },
    labour: { installationTime: "PT8H30M", hourlyRate: 7500, sellingPrice: 63750 },
    customerPrice: 523013,
  },
};

/** A row of an article that carries neither optional part, priced per piece. */
export const materialOnlyTableEntry: ArticleConditionsTimelineTableEntry<FullArticleConditions> = {
  conditions: materialOnlyConditions,
  validFrom: "2025-01-01T00:00:00Z",
  priceSummary: {
    unit: null,
    part: { inputPrice: 12000, sellingPrice: 16800, surcharge: 4800, surchargeRate: 0.4 },
    accessory: null,
    labour: null,
    customerPrice: 16800,
  },
};
