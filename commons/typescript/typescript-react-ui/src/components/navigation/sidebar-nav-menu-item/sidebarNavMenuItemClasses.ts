/**
 * The classes that make a sidebar nav item look like a sidebar nav item.
 *
 * Extracted so every kind of item shares one definition: the router-typed items from
 * `createSidebarNavMenuItem` and the external-link item in `SidebarNavExternalLinkItem` must be
 * indistinguishable in the sidebar, and two copies of the class stack would drift.
 */

/** Sizing and icon scale of the item's button row. */
export const SidebarNavMenuButtonClasses = "h-10 p-2 [&_svg]:size-5";

/** Keeps the icon aligned while the sidebar collapses to icon width. */
export const SidebarNavMenuIconClasses = "transition-[margin] group-data-[state=collapsed]:-ml-0.5";
