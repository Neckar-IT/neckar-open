import { cn } from "cn";
import { TextSkeleton } from "../../feedback/text-skeleton";
import { NoValue } from "../../text/no-value";

export interface EntityPropertyRendererBaseProps {
  className?: string;
}

type NoExtra = object;

interface EntityPropertyRendererConfig<TEntity, TExtra extends object> {
  /**
   * React DevTools display name for the produced component (e.g. "BookTitle").
   * Required so the rendered tree is self-describing in DevTools and tests.
   */
  displayName: string;
  /**
   * Optional class names merged onto the wrapping `<span data-text-content>`. Useful
   * for layout classes (e.g. `"inline-flex items-center gap-2"` on `*Display`
   * components). Caller's `className` is appended after this base.
   */
  baseClassName?: string;
  /**
   * Render the "entity is here" branch. Return `null`/`undefined`/`false`/`""` to
   * fall through to `<NoValue />`; return any non-nil ReactNode to be wrapped in
   * `<span data-text-content className={cn(baseClassName, callerClassName)}>`.
   *
   * The argument is always named `entity` regardless of the outer prop name — the
   * outer prop name is the caller-facing API; the inner render contract stays
   * uniform across every renderer.
   */
  render: (args: { entity: TEntity } & TExtra) => React.ReactNode;
}

type EntityProp<TPropName extends string, TEntity> = {
  [K in TPropName]: TEntity | null | undefined;
};

/**
 * Factory for **Entity Property Renderer** components. The returned component
 * encodes the project-wide four-state contract:
 *
 * | Input                                     | Rendered as                                    | Meaning                                       |
 * |-------------------------------------------|------------------------------------------------|-----------------------------------------------|
 * | entity prop `=== undefined`               | `<TextSkeleton />`                             | data not yet loaded                           |
 * | entity prop `=== null`                    | `<NoValue />`                                  | loaded, optional sub-entity explicitly absent |
 * | entity present, `render(...)` returns nil | `<NoValue />`                                  | derived/optional value missing on the entity  |
 * | entity present, `render(...)` returns JSX | `<span data-text-content>{...}</span>` wrapper | the value                                     |
 *
 * The wrapper carries `data-text-content` so the rendered value participates in the
 * `TruncateTextContent` CSS contract.
 *
 * The factory is curried: `entityPropertyRenderer(propName)<TEntity, TExtra?>(config)`.
 * - `propName` is the camelCased domain name of the entity (e.g. `"book"`, `"author"`,
 *   `"membershipPlan"`) under which the produced component exposes the entity at the
 *   call site: `<BookTitle book={...} />`. Currying keeps `propName` the only generic
 *   of the outer call so TS reliably infers it as a literal.
 * - `<TEntity, TExtra?>` are the inner generics: the entity type and an optional bag
 *   of extra props (e.g. `{ badgeVariant?: StatusBadgeVariant }` for `*Display`).
 * - Inside `render`, the argument is always `entity` regardless of the outer prop name.
 *
 * Caller convention for the entity value:
 * - **`undefined`** = "still loading". Produced naturally by `dataIfSuccess(...)`
 *   while a query is pending or has failed.
 * - **`null`** = "loaded but explicitly absent". Used for nullable sub-entities such
 *   as `book.secondAuthor` (a book may legitimately have no second author).
 * - **complete entity** = render. Pass the whole entity, never a partial.
 *
 * The factory is the single primitive for every Entity Renderer, including link and
 * composite variants. For `*Link` components, `render` returns a `<Link>` directly:
 * the outer span wrapper is harmless (`<span>` may contain `<a>`).
 *
 * The `TEntity` type parameter is an explicit union of all accepted DTO variants
 * (e.g. `BookPreview | BookTableEntry | BookDetails`), never a structural base type
 * like `BookLike`. See `entity-property-renderer.md` rule 11.
 *
 * @example Required text property
 * ```tsx
 * export const BookTitle = entityPropertyRenderer("book")<BookPreview | BookTableEntry | BookDetails>({
 *   displayName: "BookTitle",
 *   render: ({ entity }) => <HighlightText text={entity.title} />,
 * });
 *
 * // call site:
 * <BookTitle book={book} />
 * ```
 *
 * @example Nullable property — return `null` from `render` to render `<NoValue />`
 * ```tsx
 * export const BookIsbn = entityPropertyRenderer("book")<BookDetails>({
 *   displayName: "BookIsbn",
 *   render: ({ entity }) => (entity.isbn == null ? null : <HighlightText text={entity.isbn} />),
 * });
 * ```
 *
 * @example Composite with custom layout — `baseClassName` for the wrap, `TExtra` for extra props
 * ```tsx
 * export const BookDisplay = entityPropertyRenderer("book")<BookPreview | BookTableEntry | BookDetails, { badgeVariant?: StatusBadgeVariant }>({
 *   displayName: "BookDisplay",
 *   baseClassName: "inline-flex items-center gap-2",
 *   render: ({ entity, badgeVariant = "icon" }) => (
 *     <>
 *       <BookTitle book={entity} />
 *       <EntityStatusBadge entity={entity} variant={badgeVariant} />
 *     </>
 *   ),
 * });
 * ```
 *
 * @example Link variant — `render` returns its own `<Link>` root
 * ```tsx
 * export const BookYearOfPublicationLink = entityPropertyRenderer("book")<BookDetails>({
 *   displayName: "BookYearOfPublicationLink",
 *   render: ({ entity }) =>
 *     entity.yearOfPublication == null ? null : (
 *       <Link to="/books" searchParams={{ year: entity.yearOfPublication }}>{entity.yearOfPublication}</Link>
 *     ),
 * });
 * ```
 */
export function entityPropertyRenderer<const TPropName extends string>(
  propName: TPropName,
): <TEntity, TExtra extends object = NoExtra>(
  config: EntityPropertyRendererConfig<TEntity, TExtra>,
) => React.FC<EntityPropertyRendererBaseProps & EntityProp<TPropName, TEntity> & TExtra> {
  return <TEntity, TExtra extends object = NoExtra>(config: EntityPropertyRendererConfig<TEntity, TExtra>) => {
    type Props = EntityPropertyRendererBaseProps & EntityProp<TPropName, TEntity> & TExtra;

    function Content(props: { entity: TEntity; extra: TExtra; mergedClassName: string }): React.ReactElement {
      const content = config.render({ entity: props.entity, ...props.extra });
      if (content == null || content === false || content === "") {
        return <NoValue className={props.mergedClassName} />;
      }
      return (
        <span data-text-content className={props.mergedClassName}>
          {content}
        </span>
      );
    }
    Content.displayName = `${config.displayName}.Content`;

    function Renderer(props: Props): React.ReactElement {
      const { className, [propName]: entityValue, ...rest } = props as Props & { className?: string };
      const entity = entityValue as TEntity | null | undefined;
      const extra = rest as unknown as TExtra;
      const mergedClassName = cn(config.baseClassName, className);

      if (entity === undefined) {
        return <TextSkeleton className={mergedClassName} />;
      }
      if (entity === null) {
        return <NoValue className={mergedClassName} />;
      }

      return <Content entity={entity} extra={extra} mergedClassName={mergedClassName} />;
    }

    Renderer.displayName = config.displayName;
    return Renderer;
  };
}
