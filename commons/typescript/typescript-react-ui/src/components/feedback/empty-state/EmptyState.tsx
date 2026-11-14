/**
 * Imports
 */

import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "cn";
import type { ReactElement } from "react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import type { Action } from "../../actions/Action";
import { ActionButton } from "../../actions/ActionButton";
import { Empty } from "../empty";

/**
 * Variants
 */

const emptyStateVariants = cva("flex w-full flex-col items-center justify-center gap-2", {
  variants: {
    size: {
      /** Fills the place of a list that is the content of the page, in the height of its loading skeleton. */
      page: "h-80",
      /** One of several sections of a page: as high as its content, so empty sections stack without a gap of their own. */
      section: "py-6",
    },
  },
  defaultVariants: {
    size: "page",
  },
});

const emptyIconVariants = cva("", {
  variants: {
    size: {
      page: "h-16 w-16",
      section: "h-10 w-10",
    },
  },
  defaultVariants: {
    size: "page",
  },
});

/**
 * Prop Types
 */

interface EmptyStateProps {
  /** Text below the icon. */
  label: string;
  /** Rendered as an `ActionButton` below the label, e.g. the action that creates the first record. Without it, no button shows. */
  action?: Action;
  /** `page` for the list a page shows, `section` for a list among other sections of a page, such as the books on the page of an author. */
  size?: VariantProps<typeof emptyStateVariants>["size"];
  className?: string;
}

/**
 * Icon, label and optional action button shown in place of a list that holds no record yet.
 */

export function EmptyState(props: EmptyStateProps): ReactElement {
  return (
    <div className={cn(emptyStateVariants({ size: props.size }), props.className)}>
      <Empty className={emptyIconVariants({ size: props.size })} />
      <span className="text-muted-foreground max-w-56 text-center text-sm">{props.label}</span>
      {props.action === undefined ? null : <ActionButton action={props.action} />}
    </div>
  );
}

/**
 * The text "There are no <entities> yet.", for the `label` of an `EmptyState` of an entity list.
 */

export function useNoEntitiesLabel(entityPluralLabel: string): string {
  const { t } = useEmbeddedTranslation();
  return t({ key: "emptyState.noEntities", options: { entities: entityPluralLabel } });
}
