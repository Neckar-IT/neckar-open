/**
 * Tailwind classes that activate text truncation for all descendant
 * components marked with `data-text-content`.
 *
 * You need to mark components that render server data with `data-text-content` for them to be
 * truncated by default.
 *
 * How it works:
 * - `[data-text-content]` targets the inline-flex container (BookTitle, PublisherName, etc.)
 *   and constrains it to `max-w-full` so it respects parent width; `flex-nowrap` keeps a renderer that wraps its
 *   items elsewhere on one line here.
 * - `[data-text-content] > span` targets the text elements (HighlightText renders a `<span>`).
 *   Badges (EntityStatusBadge) render as `<div>` and are intentionally not truncated; a `[data-slot=badge]` span and a
 *   link holding one keep their width too. A date (`[data-slot=date]` of `FormattedDate`, `FormattedDateTime`) and the marker holding
 *   it never shrink: a date is short and of fixed width, and an ellipsis would destroy its meaning; its column makes room for it.
 *   This span/div distinction is the contract between text components and this truncation zone.
 * - `[data-text-content] > a` and `[data-text-content] > a > span` extend the contract through
 *   a single `<a>` wrapper between the marker and the text span, as inserted by entity links
 *   (`BookLink`, `AuthorLink`, `PublisherLink`, plain `Link`). The `<a>` itself is constrained
 *   (`max-w-full`, `min-w-0`) so an inner `<span data-text-content>` (or a plain text span) has
 *   a bounded containing block to clip against. Wrappers other than `<a>`, or chains deeper than
 *   one wrapper element, remain out of scope (#1734).
 *
 * Why `overflow-clip` + `overflow-clip-margin` instead of Tailwind's `truncate`:
 * `truncate` expands to `overflow: hidden; text-overflow: ellipsis; white-space: nowrap;`, which
 * clips strictly at the content-box edge. Italic glyphs have a right-ward overhang that extends
 * beyond their geometric box, so the last character gets visibly cut off (the bug fixed in #1575).
 * `overflow: clip` plus `overflow-clip-margin: 0.25em` extends the visual clip region past the
 * padding-box edge, giving italic text room to render fully while `text-overflow: ellipsis` still
 * triggers on real overflow at the content-box edge.
 *
 * Exposed as a tuple of individual class tokens so consumers that need them as
 * tokens (e.g. `installTruncateTextContentOnBody` for `body.classList.add`) can
 * use them directly. The React components join them on application.
 */
export const truncateTextContentClass: readonly string[] = [
  "[&_[data-text-content]]:max-w-full",
  "[&_[data-text-content]]:min-w-0",
  "[&_[data-text-content]]:overflow-clip",
  "[&_[data-text-content]]:text-ellipsis",
  "[&_[data-text-content]]:whitespace-nowrap",
  "[&_[data-text-content]]:inline-flex",
  "[&_[data-text-content]]:flex-nowrap",
  "[&_[data-text-content]]:[overflow-clip-margin:0.25em]",
  "[&_[data-text-content]>span]:overflow-clip",
  "[&_[data-text-content]>span]:text-ellipsis",
  "[&_[data-text-content]>span]:whitespace-nowrap",
  "[&_[data-text-content]>span]:[overflow-clip-margin:0.25em]",
  "[&_[data-text-content]>span]:min-w-0",
  "[&_[data-text-content]>a]:max-w-full",
  "[&_[data-text-content]>a]:min-w-0",
  "[&_[data-text-content]>a>span]:overflow-clip",
  "[&_[data-text-content]>a>span]:text-ellipsis",
  "[&_[data-text-content]>a>span]:whitespace-nowrap",
  "[&_[data-text-content]>a>span]:[overflow-clip-margin:0.25em]",
  "[&_[data-text-content]>a>span]:min-w-0",
  "[&_[data-text-content]_[data-slot=badge]]:shrink-0",
  "[&_[data-text-content]:has(>[data-slot=date])]:!shrink-0",
  "[&_[data-text-content]:has(>[data-slot=date])]:!min-w-fit",
  "[&_[data-text-content]>[data-slot=date]]:!shrink-0",
  "[&_[data-text-content]>[data-slot=date]]:!min-w-fit",
  "[&_[data-text-content]>a:has(>[data-slot=badge])]:shrink-0",
];

/**
 * Tailwind classes that reverse text truncation for all descendant
 * components marked with `data-text-content`.
 *
 * Use inside a `TruncateTextContent` zone to restore full-length text
 * for specific sections (e.g. a single table column that should wrap).
 *
 * The reverse contract mirrors the truncation contract: it overrides the styles on
 * `[data-text-content]`, `[data-text-content] > span`, `[data-text-content] > a`, and
 * `[data-text-content] > a > span`, so a single `<a>` wrapper between the marker and the
 * inner text span is also re-expanded.
 *
 * We need to mark them all as `!important` to be sure to override the truncation
 * styles from the parent `TruncateTextContent` component.
 *
 * A multiline text (`HighlightText` with `multiline`, marked `data-multiline-text`) keeps its line breaks here: the attribute
 * selector outranks `[data-text-content] > span`. `truncateTextContentClass` has no such rule, so the same text stays on one line there.
 *
 * Same array shape as `truncateTextContentClass`; the React component joins on application.
 */
export const doNotTruncateTextContentClass: readonly string[] = [
  "[&_[data-text-content]]:!max-w-none",
  "[&_[data-text-content]]:!overflow-visible",
  "[&_[data-text-content]]:!text-clip",
  "[&_[data-text-content]]:!whitespace-normal",
  "[&_[data-text-content]]:!flex-wrap",
  "[&_[data-text-content]]:!min-w-auto",
  "[&_[data-text-content]>span]:!overflow-visible",
  "[&_[data-text-content]>span]:!text-clip",
  "[&_[data-text-content]>span]:!whitespace-normal",
  "[&_[data-text-content]>span[data-multiline-text]]:!whitespace-pre-wrap",
  "[&_[data-text-content]>span]:!min-w-auto",
  "[&_[data-text-content]>a]:!max-w-none",
  "[&_[data-text-content]>a]:!min-w-auto",
  "[&_[data-text-content]>a>span]:!overflow-visible",
  "[&_[data-text-content]>a>span]:!text-clip",
  "[&_[data-text-content]>a>span]:!whitespace-normal",
  "[&_[data-text-content]>a>span]:!min-w-auto",
];

/**
 * Activates text truncation with ellipsis for all descendant components
 * that are marked with `data-text-content`.
 *
 * We want to truncate text marked with `data-text-content` by default, so we wrap the
 * entire app with this component.
 */
export function TruncateTextContent({ children }: React.PropsWithChildren): React.ReactElement {
  return <div className={truncateTextContentClass.join(" ")}>{children}</div>;
}

/**
 * Attaches `truncateTextContentClass` to `document.body`, activating default
 * text truncation for every descendant marked with `data-text-content`.
 *
 * Call once at app startup from `main.tsx` (alongside `bootstrapObservability`) to
 * avoid an extra wrapper `<div>` between `<body>` and the app root.
 *
 * Idempotent: `DOMTokenList.add` ignores tokens that are already present, so
 * repeated calls produce no duplicate classes on `<body>`.
 */
export function installTruncateTextContentOnBody(): void {
  document.body.classList.add(...truncateTextContentClass);
}

/**
 * Reverses text truncation for all descendant components marked with
 * `data-text-content`.
 *
 * Wrap specific sections of the app (e.g. a details list) with this
 * component to disable truncation of data-text-content within that section.
 */
export function DoNotTruncateTextContent({ children }: React.PropsWithChildren): React.ReactElement {
  return <div className={doNotTruncateTextContentClass.join(" ")}>{children}</div>;
}
