# Folder-Structure

Every directory directly under `src/components` is a functional category (`inputs`, `filters`, `forms`, `data-display`,
`feedback`, `navigation`, `layout`, `overlays`, `actions`, `media`, `text`, `quantities`) or a domain export
(`entities`, `draft-confirmed`, `price-timeline`, `value-timeline`, `conditions-timeline`, `optimistic-locking`, `quote`,
`pages`). The table of categories is in the [README](../../README.md#structure).

## primitives

Wrapper layer over the shadcn/ui components of `@neckar-hub/typescript-react-ui-primitives`, plus `range-slider`,
`unstyled-button` and `visually-hidden`, which are built here on `radix-ui`. The category barrels re-export the wrappers.
