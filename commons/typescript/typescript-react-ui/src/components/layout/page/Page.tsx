import { type TextKey, useTranslation } from "@neckar-hub/typescript-react-i18n";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "cn";

const pageVariants = cva("mx-auto flex w-full flex-col gap-8", {
  variants: {
    width: {
      md: "max-w-xl",
      lg: "max-w-5xl",
      xl: "max-w-7xl",
      full: "",
    },
  },
  defaultVariants: {
    width: "xl",
  },
});

interface PageProps extends React.PropsWithChildren, VariantProps<typeof pageVariants> {
  className?: string;

  /**
   * Explicit HTML title override (i18n key or pre-translated string).
   * When provided, this title takes precedence over automatic derivation.
   * When omitted, the title is auto-derived by useAutoPageTitle in the layout.
   */
  htmlTitle?: TextKey;
}

/**
 * Page layout wrapper with max-width, document title, and stacked sections.
 */
export function Page({ className, children, htmlTitle, width }: PageProps): React.ReactElement {
  const { t } = useTranslation();

  return (
    <>
      {htmlTitle !== undefined && <span data-explicit-page-title={t(htmlTitle)} hidden />}
      <div className={cn(pageVariants({ width }), className)}>{children}</div>
    </>
  );
}
