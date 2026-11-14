import { Children, Fragment } from "react";
import { BreadcrumbItem, BreadcrumbList, Breadcrumb as BreadcrumbRoot, BreadcrumbSeparator } from "../../primitives/breadcrumb";
import { truncateTextContentClass } from "../../text/text-overflow";

interface BreadcrumbsProps extends React.PropsWithChildren {}

/**
 * Breadcrumb navigation trail. It is a truncation zone of its own: an item that is a `data-text-content` renderer ends with an
 * ellipsis at the item's maximum width, also where the app installs no zone on the body.
 */
export function Breadcrumbs({ children }: BreadcrumbsProps): React.ReactElement {
  const items = Children.toArray(children);

  return (
    <BreadcrumbRoot>
      <BreadcrumbList className={truncateTextContentClass.join(" ")}>
        {items.map((child, index) => (
          <Fragment key={index}>
            {index > 0 ? <BreadcrumbSeparator /> : undefined}
            <BreadcrumbItem className="last:text-foreground flex max-w-40 overflow-hidden">{child}</BreadcrumbItem>
          </Fragment>
        ))}
      </BreadcrumbList>
    </BreadcrumbRoot>
  );
}
