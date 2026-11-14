import type { ArticleConditions, ArticleConditionsPriceSummary, PriceWithProfit } from "../openapi-types";

/**
 * Read view for a component that renders whatever optional parts an article type happens to have.
 *
 * Every implementation is assignable to it, so one renderer serves them all. That tolerance is
 * deliberate and belongs *only* here: what a table may show is enforced one level up, where a
 * column asks for the generated shape declaring the part it renders, and what may be sent is
 * enforced by the endpoint's request type. A leaf renderer decides nothing — it displays what it
 * is handed.
 */
export type ArticleConditionsToDisplay = ArticleConditions & {
  accessory?: PriceWithProfit | null;
  /** ISO-8601 duration, e.g. `"PT8H30M"`. */
  installationTime?: string | null;
};

/**
 * Read-side view of a single entry of an article-conditions timeline, over the
 * {@link ArticleConditions} implementation the owning article type uses.
 *
 * The generated entry-view type is named after that implementation —
 * `ArticleConditionsTimelineEntryViewBareArticleConditions` for a book,
 * `…FullArticleConditions` for a cable. Components that render entries take this structural view
 * instead, narrowed to the shape they read: every generated instantiation is assignable to it.
 */
export interface ArticleConditionsTimelineEntry<Conditions extends ArticleConditions = ArticleConditions> {
  conditions: Conditions;
  /** ISO-8601 instant, e.g. `"2026-04-24T03:10:00Z"`. */
  validFrom: string;
}

/**
 * One row of the conditions table, with the price overview the server calculated. The structural view of the
 * generated `ArticleConditionsTimelineTableEntry…` instantiations, for the same reason as {@link ArticleConditionsTimelineEntry}.
 */
export interface ArticleConditionsTimelineTableEntry<
  Conditions extends ArticleConditions = ArticleConditions,
> extends ArticleConditionsTimelineEntry<Conditions> {
  priceSummary: ArticleConditionsPriceSummary;
}

/**
 * Read-side view of the server-computed summary of an article-conditions timeline — the entry in
 * effect and the next upcoming one.
 *
 * The structural view of the generated `ArticleConditionsTimelineSummary…` instantiations, for the
 * same reason as {@link ArticleConditionsTimelineEntry}.
 */
export interface ArticleConditionsTimelineSummary<Conditions extends ArticleConditions = ArticleConditions> {
  currentEntry: ArticleConditionsTimelineEntry<Conditions> | null;
  nextFutureEntry: ArticleConditionsTimelineEntry<Conditions> | null;
}
