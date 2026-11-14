import { type ParseKeys, useTranslation } from "@neckar-hub/typescript-react-i18n";
import { Page, PageSection, PageSectionHeading } from "../page";

export interface EntityOverviewPurePageProps extends React.PropsWithChildren {
  /**
   * Translation key of the primary heading; the auto-derived document title reads it.
   */
  title: ParseKeys;
}

/**
 * Page frame of an entity overview: the translated title as primary heading, the children (typically the table) below it.
 */
export function EntityOverviewPurePage({ title, children }: EntityOverviewPurePageProps): React.ReactElement {
  const { t } = useTranslation();

  return (
    <Page>
      <PageSection>
        <PageSectionHeading level="primary">{t(title)}</PageSectionHeading>
        {children}
      </PageSection>
    </Page>
  );
}
