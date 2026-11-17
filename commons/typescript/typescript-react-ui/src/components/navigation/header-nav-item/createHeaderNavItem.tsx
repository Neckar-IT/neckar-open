import type { LinkPropsPrimitive, NoParams } from "@neckar-hub/typescript-react-router";
import type { ReactElement } from "react";
import { createUseIsActive, type NavItemDeps } from "../createUseIsActive";

/** Class name, content and current-page marker belong to the item, so a caller cannot pass them. */
export type HeaderNavItemProps<AppRoutes extends Record<string, Record<string, string> | NoParams>> = LinkPropsPrimitive<AppRoutes> & {
  title: string;
  end?: boolean;
  className?: never;
  children?: never;
  "aria-current"?: never;
};

/**
 * Factory for typed header navigation links bound to a project's `AppRoutes`.
 *
 * Returns `HeaderNavItem`: the link of the current location, decided by `createUseIsActive`, carries `aria-current="page"`
 * and stands out. Callers pass a pre-translated `title`.
 */
export function createHeaderNavItem<AppRoutes extends Record<string, Record<string, string> | NoParams>>({
  Link,
  generateUrl,
}: NavItemDeps<AppRoutes>) {
  const useIsActive = createUseIsActive(generateUrl);

  function HeaderNavItem({ title, end = false, ...rest }: HeaderNavItemProps<AppRoutes>): ReactElement {
    const link = rest as LinkPropsPrimitive<AppRoutes>;
    const isActive = useIsActive(link, end);

    return (
      <Link
        {...link}
        aria-current={isActive ? "page" : undefined}
        className={isActive ? "text-foreground font-semibold" : "text-muted-foreground"}
      >
        {title}
      </Link>
    );
  }

  return HeaderNavItem;
}
