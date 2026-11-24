import { type LucideIcon, MenuIcon } from "lucide-react";
import { type PropsWithChildren, type ReactElement, type ReactNode, useRef } from "react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { SidebarToggleButton } from "../../navigation/sidebar-toggle-button";
import { Separator } from "../../primitives/separator";
import { useSidebar } from "../../primitives/sidebar";
import { UnstyledButton } from "../../primitives/unstyled-button";
import { useAutoPageTitle } from "../page/useAutoPageTitle";

interface AppMobileHeaderProps {
  /** Shown next to the menu toggle. */
  brand: ReactNode;
}

/**
 * The brand bar with the menu toggle of an app inside a `SidebarProvider`; renders on mobile only. [AppMain] renders it
 * itself; an app with its own page frame places it above its route outlet, so the menu stays reachable on every page.
 */
export function AppMobileHeader({ brand }: AppMobileHeaderProps): ReactElement | null {
  const { isMobile, toggleSidebar } = useSidebar();
  const { t } = useEmbeddedTranslation();

  if (!isMobile) {
    return null;
  }

  return (
    <header className="bg-sidebar flex w-full items-center gap-4 border-b px-4 py-2">
      <div className="flex flex-1 items-center gap-2">{brand}</div>

      <UnstyledButton aria-label={t("appMobileHeader.toggleMenu")} onClick={toggleSidebar}>
        <MenuIcon className="h-7 w-7" />
      </UnstyledButton>
    </header>
  );
}

interface AppDesktopHeaderProps {
  breadcrumbs?: ReactNode;
}

/**
 * The header of height `h-12` with sidebar toggle and breadcrumbs; renders from tablet width only. It sticks to the top
 * of the content column, so it is as wide as the column next to the sidebar.
 */
export function AppDesktopHeader({ breadcrumbs }: AppDesktopHeaderProps): ReactElement | null {
  const { isMobile } = useSidebar();

  if (isMobile) {
    return null;
  }

  return (
    <header className="bg-background sticky top-0 z-50 flex h-12 items-center gap-2 border-b">
      <div className="flex min-w-0 flex-1 items-center gap-2 px-8">
        <SidebarToggleButton />

        {breadcrumbs !== undefined && breadcrumbs !== null && (
          <>
            <Separator className="mr-2 h-4 data-vertical:self-center" orientation="vertical" />
            {breadcrumbs}
          </>
        )}
      </div>
    </header>
  );
}

/**
 * The column next to the sidebar inside a `SidebarProvider` that holds the page frame.
 *
 * `min-w-0` ends it at the viewport also with wide content, and the sticky [AppDesktopHeader] is as wide as the column.
 */
export function AppContentColumn({ children }: PropsWithChildren): ReactElement {
  return <div className="flex w-full min-w-0 flex-col">{children}</div>;
}

interface AppMainProps extends PropsWithChildren {
  /** Shown in the mobile header and appended to the document title. */
  appName: string;
  brandIcon: LucideIcon;
  breadcrumbs?: ReactNode;
}

/**
 * The content column of a page: [AppMobileHeader] with icon and app name as brand, [AppDesktopHeader], the page
 * content centered at `max-w-7xl`, and the document title derived by `useAutoPageTitle`.
 */
export function AppMain({ appName, brandIcon: BrandIcon, breadcrumbs, children }: AppMainProps): ReactElement {
  const containerRef = useRef<HTMLDivElement>(null);

  useAutoPageTitle(containerRef, { appName });

  const brand = (
    <>
      <BrandIcon className="h-7 w-7" />
      <p className="truncate text-2xl font-bold">{appName}</p>
    </>
  );

  return (
    <div ref={containerRef} className="flex min-h-screen flex-1 flex-col">
      <AppMobileHeader brand={brand} />
      <AppDesktopHeader breadcrumbs={breadcrumbs} />
      <main className="flex flex-1 flex-col px-8 py-4">
        <div className="mx-auto flex w-full max-w-7xl flex-1 flex-col">{children}</div>
      </main>
    </div>
  );
}
