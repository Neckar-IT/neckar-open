/**
 * The classes that make a sidebar nav item look like a sidebar nav item.
 *
 * Every top-level sidebar nav item shares one definition, and the disabled state also covers sub-items: the items must be
 * indistinguishable in the sidebar, and two copies of a class stack would drift.
 */

/** Sizing and icon scale of the item's button row. */
export const SidebarNavMenuButtonClasses = "h-10 p-2 [&_svg]:size-5";

/** Keeps the icon aligned while the sidebar collapses to icon width. */
export const SidebarNavMenuIconClasses = "transition-[margin] group-data-[state=collapsed]:-ml-0.5";

/** Greys out a disabled item and keeps hovering from highlighting it. */
export const SidebarNavMenuDisabledClasses =
  "text-muted-foreground hover:text-muted-foreground hover:cursor-not-allowed hover:bg-transparent";
