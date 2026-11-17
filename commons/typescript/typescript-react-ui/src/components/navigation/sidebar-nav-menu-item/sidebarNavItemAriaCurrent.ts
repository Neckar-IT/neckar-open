/**
 * The `aria-current` of a sidebar nav item: `page` when the location matches its target, `true` when only an
 * `isActive` override marks it, for a section that spans several pages.
 */
export function sidebarNavItemAriaCurrent(locationIsActive: boolean, isActive: boolean): "page" | "true" | undefined {
  if (locationIsActive) {
    return "page";
  }
  return isActive ? "true" : undefined;
}
