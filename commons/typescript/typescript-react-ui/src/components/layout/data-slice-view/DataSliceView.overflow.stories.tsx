import type { Story, StoryDefault } from "@ladle/react";
import { WithMemoryRouter } from "@neckar-hub/typescript-react-utils/ladle";
import {
  type ResponsiveColumnDefinition,
  type ResponsiveTableConfig,
  ResponsiveTable,
} from "../../data-display/responsive-table/ResponsiveTable";
import { Badge } from "../../primitives/badge";
import { Button } from "../../primitives/button";
import { TruncateTextContent } from "../../text/text-overflow";
import { DataSliceToolbar } from "./DataSliceToolbar";
import { DataSliceView } from "./DataSliceView";

/**
 * Regression coverage for #1424: "Tabellen-Layout bricht bei schmalem Browser-Fenster".
 *
 * The original bug was reported on the author detail page, where the books table
 * (`DataSliceView` + `ResponsiveTable`) pushed the whole page wider than the viewport at
 * ~768px and clipped the publication badges at ~480px.
 *
 * The symptom is layout-context dependent: an isolated table never reproduces it. The
 * culprit is a `flex-1` main column WITHOUT `min-w-0` — the classic flex-overflow trap,
 * where a flex child grows to its content width and pushes its siblings (and the page)
 * past the viewport. This story recreates exactly that page shell (fixed sidebar +
 * `flex-1` main column constrained to the viewport via the Ladle width addon) so the bug
 * surfaces — or is proven fixed — at the reported breakpoints.
 *
 * How to use: open this story, then pick `md` (768px) and `base` (400px) in the Ladle
 * width selector. The outer frame draws a red dashed border at the viewport edge; if any
 * content crosses it (horizontal page scrollbar), the bug is present.
 */
export default {
  title: "Layout / DataSliceView / Overflow",
  decorators: [
    (Story) => (
      <WithMemoryRouter>
        <Story />
      </WithMemoryRouter>
    ),
  ],
  meta: {
    responsive: true,
  },
} satisfies StoryDefault;

interface Book {
  id: string;
  title: string;
  isbn: string;
  publicationStatus: "Published" | "InReview" | "Draft";
  publisher: string;
  publishedDate: string;
  pages: number;
}

const books: Book[] = [
  {
    id: "BOOK001",
    title: "The Comprehensive and Annotated History of the Forgotten Northern Kingdoms, Volume One",
    isbn: "978-3-16-148410-0",
    publicationStatus: "InReview",
    publisher: "Königlich-Vereinigte Verlagsgesellschaft der Nordatlantischen Provinzen mbH",
    publishedDate: "2024-11-03",
    pages: 642,
  },
  {
    id: "BOOK002",
    title: "Crónica Ilustrada de las Tierras Olvidadas y los Reinos Perdidos del Lejano Norte",
    isbn: "978-1-23-456789-7",
    publicationStatus: "Published",
    publisher: "Editorial de las Provincias Unidas del Sur, Sociedad Anónima",
    publishedDate: "2023-06-21",
    pages: 318,
  },
  {
    id: "BOOK003",
    title: "Short Title",
    isbn: "978-0-00-000000-2",
    publicationStatus: "Draft",
    publisher: "Acme Press",
    publishedDate: "2025-01-15",
    pages: 96,
  },
];

const publicationStatusLabels: Record<Book["publicationStatus"], string> = {
  Published: "Veröffentlicht",
  InReview: "In Prüfung",
  Draft: "Entwurf",
};

const titleColumn: ResponsiveColumnDefinition<Book> = {
  id: "title",
  header: "Titel",
  viewOnMobile: "primary-header",
  cell: (item) => (
    <span data-text-content>
      <span>{item.title}</span>
    </span>
  ),
};

const isbnColumn: ResponsiveColumnDefinition<Book> = {
  id: "isbn",
  header: "ISBN",
  cell: (item) => (
    <span data-text-content>
      <span>{item.isbn}</span>
    </span>
  ),
};

const publicationStatusColumn: ResponsiveColumnDefinition<Book> = {
  id: "publicationStatus",
  header: "Publikation",
  viewOnMobile: "secondary-header",
  cell: (item) => <Badge>{publicationStatusLabels[item.publicationStatus]}</Badge>,
};

const publisherColumn: ResponsiveColumnDefinition<Book> = {
  id: "publisher",
  header: "Verlag",
  cell: (item) => (
    <span data-text-content>
      <span>{item.publisher}</span>
    </span>
  ),
};

const publishedDateColumn: ResponsiveColumnDefinition<Book> = {
  id: "publishedDate",
  header: "Erschienen",
  cell: (item) => (
    <span data-text-content>
      <span>{item.publishedDate}</span>
    </span>
  ),
};

const pagesColumn: ResponsiveColumnDefinition<Book> = {
  id: "pages",
  header: "Seiten",
  sortingIconVariant: "numerical",
  cell: (item) => (
    <span data-text-content>
      <span>{item.pages}</span>
    </span>
  ),
};

/**
 * Page shell that mirrors the real app: a fixed-width sidebar next to the main column.
 * The main column is the failure surface — `flex-1` without `min-w-0` is the bug trigger.
 */
export const PageShellStory: Story = () => {
  const config: ResponsiveTableConfig<Book> = {
    rowLink: (item) => `/books/${item.id}`,
    columns: [titleColumn, isbnColumn, publicationStatusColumn, publisherColumn, publishedDateColumn, pagesColumn],
  };

  return (
    <TruncateTextContent>
      {/* Red dashed frame = viewport edge. It must NOT clip: anything crossing it (or a
          horizontal page scrollbar appearing) means the page overflows the viewport. */}
      <div className="w-full border-2 border-dashed border-red-500">
        <div className="flex">
          <aside className="bg-muted text-muted-foreground hidden w-56 shrink-0 p-4 text-sm sm:block">Sidebar</aside>
          <main className="flex-1 p-4">
            <h1 className="mb-4 text-lg font-semibold">Bücher des Autors</h1>
            <DataSliceView
              toolbar={
                <DataSliceToolbar
                  filters={
                    <div className="flex items-center gap-2">
                      <Badge>In Prüfung</Badge>
                      <span className="text-muted-foreground text-sm whitespace-nowrap">Aktiver Filter</span>
                    </div>
                  }
                  actions={<Button>Neues Buch</Button>}
                />
              }
              paginationInfo={{
                pageIndex: 0,
                pageSize: 10,
                pageCount: 1,
                filteredElementsCount: books.length,
                totalElementsCount: books.length,
              }}
              onPaginationParamsChange={() => console.log("pagination changed")}
            >
              <ResponsiveTable
                config={config}
                data={books}
                sortBy={undefined}
                sortOrder="Asc"
                onSortingParamsChange={() => console.log("sorting changed")}
              />
            </DataSliceView>
          </main>
        </div>
      </div>
    </TruncateTextContent>
  );
};
PageShellStory.storyName = "Page Shell (sidebar + main)";
