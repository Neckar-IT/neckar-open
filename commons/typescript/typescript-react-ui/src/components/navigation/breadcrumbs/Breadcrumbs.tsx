import { Children, Fragment } from "react";
import { BreadcrumbItem, BreadcrumbList, Breadcrumb as BreadcrumbRoot, BreadcrumbSeparator } from "../../primitives/breadcrumb";
import { truncateTextContentClass } from "../../text/text-overflow";

interface BreadcrumbsProps extends React.PropsWithChildren {}

/**
 * Breadcrumb navigation trail, at most as wide as the row it grows into. A trail too wide gives every item an equal share,
 * capped at its text width: a short item keeps its text, a long item ends with an ellipsis. An item keeps at least
 * min(text width, `w-12`); the separators keep their width.
 *
 * The trail is a truncation zone of its own: an item that is a `data-text-content` renderer ends with an ellipsis at the
 * item's width, also where the app installs no zone on the body.
 */
export function Breadcrumbs({ children }: BreadcrumbsProps): React.ReactElement {
  const items = Children.toArray(children);

  return (
    <BreadcrumbRoot className="min-w-0 grow">
      <BreadcrumbList className={truncateTextContentClass.join(" ")}>
        {items.map((child, index) => (
          <Fragment key={index}>
            {index > 0 ? <BreadcrumbSeparator /> : undefined}
            {/* Under `basis-0`, `w-12` only sets the automatic minimum size; `overflow-clip` keeps it, `overflow-hidden` would drop it.
                The clip cuts a child without `data-text-content`; its margin keeps an italic overhang, as in the truncation zone. */}
            <BreadcrumbItem className="last:text-foreground flex w-12 max-w-max grow basis-0 overflow-clip [overflow-clip-margin:0.25em]">
              {child}
            </BreadcrumbItem>
          </Fragment>
        ))}
      </BreadcrumbList>
    </BreadcrumbRoot>
  );
}
