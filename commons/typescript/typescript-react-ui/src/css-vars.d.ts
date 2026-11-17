/**
 * Augment `React.CSSProperties` to accept CSS custom properties (`--*`).
 *
 * Without this, `<div style={{ "--sidebar-width": "200px" }} />` requires a
 * `as React.CSSProperties` cast — a known limitation of React's types.
 * The augmentation is the structural fix and lets style objects with CSS
 * custom properties type-check directly.
 */
import "react";

declare module "react" {
  interface CSSProperties {
    [cssCustomProperty: `--${string}`]: string | number | undefined;
  }
}
