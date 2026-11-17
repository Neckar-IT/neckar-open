/**
 * Demonstrates the TruncateTextContent pattern.
 *
 * Text components mark themselves with data-text-content.
 * Wrapping a section in <TruncateTextContent> activates truncation
 * with ellipsis for all marked descendants. Badges (rendered as <div>)
 * are not affected — only <span> children are truncated.
 *
 * For full-length text (detail pages), omit <TruncateTextContent>.
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { EllipsisIcon, PencilIcon, TrashIcon } from "lucide-react";
import { EntityDetailsList } from "../../data-display/entity-details-list";
import { entityPropertyRenderer } from "../../data-display/entity-property-renderer";
import { FormattedDateTime } from "../../data-display/formatted-date-time";
import { RatingStars } from "../../data-display/rating-stars";
import { BadgeBar } from "../../layout/badge-bar";
import { HighlightSource, HighlightText } from "../highlight-text";
import { DoNotTruncateTextContent } from "./TextOverflow";
import { TruncateTextContent } from "./TextOverflow";

const meta = {
  title: "Text / TruncateTextContent",
  // The entire app is wrapped in TruncateTextContent, so we simulate this here with a decorator.
  decorators: [
    (Story) => (
      <TruncateTextContent>
        <Story />
      </TruncateTextContent>
    ),
  ],
} satisfies Meta;

export default meta;

/**
 * Mock data
 */

interface Book {
  title: string;
  author: string;
  year: number | null;
  rating: number;
  status: "Confirmed" | "Draft";
}

// `as const` keeps the tuple shape, so indexed access on the fixed mock data yields a defined Book.
const books = [
  {
    title: "Harry Potter and the Extraordinarily Long Book Title That Someone Decided to Give This Particular Volume of the Series",
    author: "Johann Wolfgang von Goethe-Schiller-Lenz und weitere Autoren der Weimarer Klassik",
    year: 1997,
    rating: 4,
    status: "Confirmed",
  },
  {
    title: "Harry Potter",
    author: "J.K. Rowling",
    year: 1998,
    rating: 5,
    status: "Confirmed",
  },
  {
    title: "Die Leiden des jungen Werther — Eine Geschichte in Briefen, erstmals erschienen im Jahre 1774 in Leipzig",
    author: "Johann Wolfgang von Goethe",
    year: 1774,
    rating: 3,
    status: "Draft",
  },
  {
    title: "Faust. Eine Tragödie.",
    author: "Johann Wolfgang von Goethe",
    year: 1808,
    rating: 5,
    status: "Confirmed",
  },
  {
    title:
      "The Complete and Unabridged History of European Literature From the Ancient Greeks to the Present Day Including All Major and Minor Works",
    author: "Prof. Dr. Dr. h.c. mult. Alexander Friedrich Wilhelm von Humboldt-Universität",
    year: null,
    rating: 2,
    status: "Draft",
  },
] as const satisfies readonly Book[];

/**
 * Fake components — simulate the real BookTitle, AuthorDisplayName etc.
 */

function FakeBookTitle({ book }: { book: Book }): React.ReactElement {
  return (
    <span data-text-content className="inline-flex items-center gap-2">
      <span title={book.title} className="font-semibold">
        {book.title}
      </span>
      <div
        className={`inline-flex shrink-0 items-center gap-1 rounded-full border py-0.5 pr-2.5 pl-2 text-xs ${book.status === "Confirmed" ? "border-green-600/60 text-green-600" : "border-gray-400/60 text-gray-400"}`}
      >
        {book.status === "Confirmed" ? (
          <svg className="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2" />
            <path d="m9 14 2 2 4-4" />
            <rect x="9" y="1" width="6" height="4" rx="1" />
          </svg>
        ) : (
          <svg className="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2" />
            <path d="M14 9l-4 4" />
            <rect x="9" y="1" width="6" height="4" rx="1" />
          </svg>
        )}
      </div>
    </span>
  );
}

function FakeBookLink({ book }: { book: Book }): React.ReactElement {
  return (
    <a href="/books" className="hover:underline">
      <FakeBookTitle book={book} />
    </a>
  );
}

function FakeAuthorName({ name }: { name: string }): React.ReactElement {
  return (
    <span data-text-content>
      <span title={name}>{name}</span>
    </span>
  );
}

function FakeAuthorLink({ name }: { name: string }): React.ReactElement {
  return (
    <a href="/authors" className="hover:underline">
      <FakeAuthorName name={name} />
    </a>
  );
}

function RowActions(): React.ReactElement {
  return (
    <div className="flex justify-end">
      <button type="button" className="rounded p-1.5 hover:bg-gray-100">
        <EllipsisIcon className="size-4" />
      </button>
    </div>
  );
}

function MockPageActions(): React.ReactElement {
  return (
    <div className="flex shrink-0 items-center gap-2">
      <button type="button" className="inline-flex items-center gap-1.5 rounded border px-3 py-1.5 text-sm">
        <PencilIcon className="size-3.5" /> Bearbeiten
      </button>
      <button
        type="button"
        className="inline-flex items-center gap-1.5 rounded border border-red-300 bg-red-50 px-3 py-1.5 text-sm text-red-700"
      >
        <TrashIcon className="size-3.5" /> Löschen
      </button>
    </div>
  );
}

/**
 * By default truncation is active for all `data-text-content` elements
 */
export const Default: StoryObj<typeof meta> = {
  render: () => (
    <div className="rounded-md border">
      <table className="w-full border-collapse text-sm">
        <thead>
          <tr className="border-b bg-gray-50/50 text-left text-xs font-medium text-gray-500">
            <th className="w-[40%] px-4 py-3">Buchtitel</th>
            <th className="w-[25%] px-4 py-3">Autor</th>
            <th className="px-4 py-3">Jahr</th>
            <th className="px-4 py-3">Bewertung</th>
            <th className="px-4 py-3 text-right">Aktionen</th>
          </tr>
        </thead>
        <tbody>
          {books.map((book, i) => (
            <tr key={i} className="border-b last:border-b-0 hover:bg-gray-50/50">
              <td className="max-w-0 px-4 py-3">
                <FakeBookLink book={book} />
              </td>
              <td className="max-w-0 px-4 py-3">
                <FakeAuthorLink name={book.author} />
              </td>
              <td className="px-4 py-3 text-gray-500">{book.year ?? "—"}</td>
              <td className="px-4 py-3">
                <RatingStars rating={book.rating} />
              </td>
              <td className="px-4 py-3">
                <RowActions />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  ),
};

/**
 * Details page with details list where truncation is deactivated
 */
export const DetailPage: StoryObj<typeof meta> = {
  render: () => {
    const book = books[0];
    return (
      <div className="space-y-8">
        <section className="space-y-2">
          <header className="flex items-center gap-8 [&>:first-child]:min-w-0 [&>:not(:first-child)]:shrink-0">
            <div className="text-3xl font-bold">
              <FakeBookTitle book={book} />
            </div>
            <MockPageActions />
          </header>
        </section>

        <DoNotTruncateTextContent>
          <section className="w-3xl space-y-2">
            <h2 className="text-2xl font-semibold">Details</h2>
            <dl className="grid grid-cols-[auto_1fr] rounded-md border text-sm">
              <dt className="border-b px-4 py-3 font-medium text-gray-500">Buchtitel</dt>
              <dd className="border-b px-4 py-3">
                <FakeBookTitle book={book} />
              </dd>
              <dt className="border-b px-4 py-3 font-medium text-gray-500">Autor</dt>
              <dd className="border-b px-4 py-3">
                <FakeAuthorLink name={book.author} />
              </dd>
              <dt className="border-b px-4 py-3 font-medium text-gray-500">Jahr</dt>
              <dd className="border-b px-4 py-3">{book.year}</dd>
              <dt className="px-4 py-3 font-medium text-gray-500">Bewertung</dt>
              <dd className="px-4 py-3">
                <RatingStars rating={book.rating} />
              </dd>
            </dl>
          </section>
        </DoNotTruncateTextContent>

        <section className="space-y-2">
          <div className="text-2xl font-semibold">
            <FakeAuthorName name={book.author} />
          </div>
          <div className="rounded-md border">
            <table className="w-full border-collapse text-sm">
              <thead>
                <tr className="border-b bg-gray-50/50 text-left text-xs font-medium text-gray-500">
                  <th className="w-[50%] px-4 py-3">Buchtitel</th>
                  <th className="px-4 py-3">Jahr</th>
                  <th className="px-4 py-3">Bewertung</th>
                </tr>
              </thead>
              <tbody>
                {books.slice(0, 3).map((b, i) => (
                  <tr key={i} className="border-b last:border-b-0 hover:bg-gray-50/50">
                    <td className="max-w-0 px-4 py-3">
                      <FakeBookLink book={b} />
                    </td>
                    <td className="px-4 py-3 text-gray-500">{b.year ?? "—"}</td>
                    <td className="px-4 py-3">
                      <RatingStars rating={b.rating} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      </div>
    );
  },
};

/**
 * Select form fields — simulates BookSelectFormField, AuthorSelectFormField, PublisherSelectFormField.
 * The selected value is displayed in a button that can overflow.
 */
export const SelectFormFields: StoryObj<typeof meta> = {
  render: () => {
    const longBook = books[0];
    const shortBook = books[1];
    return (
      <div className="max-w-md space-y-6">
        <h3 className="text-lg font-semibold">Select-Formularfelder</h3>

        <div className="space-y-4">
          <div className="space-y-1">
            <span className="text-sm font-medium">Buch (langer Titel)</span>
            <div className="rounded-md border">
              <button type="button" className="flex h-[38px] w-full items-center justify-start px-3 text-sm font-normal">
                <FakeBookTitle book={longBook} />
              </button>
            </div>
          </div>

          <div className="space-y-1">
            <span className="text-sm font-medium">Buch (kurzer Titel)</span>
            <div className="rounded-md border">
              <button type="button" className="flex h-[38px] w-full items-center justify-start px-3 text-sm font-normal">
                <FakeBookTitle book={shortBook} />
              </button>
            </div>
          </div>

          <div className="space-y-1">
            <span className="text-sm font-medium">Autor (langer Name)</span>
            <div className="rounded-md border">
              <button type="button" className="flex h-[38px] w-full items-center justify-start px-3 text-sm font-normal">
                <FakeAuthorName name={longBook.author} />
              </button>
            </div>
          </div>

          <div className="space-y-1">
            <span className="text-sm font-medium">Platzhalter (kein Wert ausgewählt)</span>
            <div className="rounded-md border">
              <button
                type="button"
                className="text-muted-foreground flex h-[38px] w-full items-center justify-start px-3 text-sm font-normal"
              >
                Verlag auswählen...
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  },
};

/**
 * Italic text placeholders (e.g. NoValue "kein Wert") inside a truncate zone.
 * Regression guard for #1575: `overflow: hidden` clips the slanted overhang of the
 * final italic glyph at the content-box edge. The fix uses `overflow: clip` plus
 * `overflow-clip-margin` so italic text renders fully while ellipsis still triggers
 * on real overflow.
 */
export const ItalicOverhang: StoryObj<typeof meta> = {
  parameters: { name: "ItalicOverhang — Regression Guard" },
  render: () => {
    const fitsExactly = "Unbenanntes Buch";
    const overflows = "Sehr langer italienischer Platzhaltertext der definitiv nicht in die Spalte passt";
    return (
      <div className="space-y-6">
        <section className="space-y-2">
          <h3 className="text-lg font-semibold">Exact-fit width — final glyph must not be clipped</h3>
          <p className="text-muted-foreground text-sm">
            Column width fits the italic text exactly. With <code>overflow: hidden</code>, the right edge of the final character was cut
            off.
          </p>
          <div className="rounded-md border">
            <table className="w-full border-collapse text-sm">
              <thead>
                <tr className="border-b bg-gray-50/50 text-left text-xs font-medium text-gray-500">
                  <th className="w-[16ch] px-4 py-3">Titel (italic placeholder)</th>
                  <th className="px-4 py-3">Normaler Text</th>
                </tr>
              </thead>
              <tbody>
                <tr className="border-b last:border-b-0">
                  <td className="max-w-0 px-4 py-3">
                    <span data-text-content className="inline-flex items-center gap-2">
                      <span className="text-muted-foreground font-semibold italic" title={fitsExactly}>
                        {fitsExactly}
                      </span>
                    </span>
                  </td>
                  <td className="px-4 py-3 text-gray-500">Vergleichswert ohne italic</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section className="space-y-2">
          <h3 className="text-lg font-semibold">Real overflow — ellipsis still appears</h3>
          <p className="text-muted-foreground text-sm">
            Column is narrower than the italic text. <code>text-overflow: ellipsis</code> continues to work under{" "}
            <code>overflow: clip</code>.
          </p>
          <div className="rounded-md border">
            <table className="w-full border-collapse text-sm">
              <thead>
                <tr className="border-b bg-gray-50/50 text-left text-xs font-medium text-gray-500">
                  <th className="w-[20ch] px-4 py-3">Titel (italic, zu lang)</th>
                  <th className="px-4 py-3">Normaler Text</th>
                </tr>
              </thead>
              <tbody>
                <tr className="border-b last:border-b-0">
                  <td className="max-w-0 px-4 py-3">
                    <span data-text-content className="inline-flex items-center gap-2">
                      <span className="text-muted-foreground italic" title={overflows}>
                        {overflows}
                      </span>
                    </span>
                  </td>
                  <td className="px-4 py-3 text-gray-500">Vergleichswert ohne italic</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </div>
    );
  },
};

/**
 * PreserveTextContent: undo truncation for specific columns.
 * The title column is truncated, but the author column uses PreserveTextContent
 * to show the full name with natural wrapping.
 */
export const PreserveTextContentDemo: StoryObj<typeof meta> = {
  parameters: { name: "PreserveTextContent — Spalten-Opt-Out" },
  render: () => (
    <div className="rounded-md border">
      <table className="w-full border-collapse text-sm">
        <thead>
          <tr className="border-b bg-gray-50/50 text-left text-xs font-medium text-gray-500">
            <th className="w-[40%] px-4 py-3">Buchtitel (truncated)</th>
            <th className="w-[35%] px-4 py-3">Autor (preserved — wraps naturally)</th>
            <th className="px-4 py-3">Jahr</th>
            <th className="px-4 py-3">Bewertung</th>
          </tr>
        </thead>
        <tbody>
          {books.map((book, i) => (
            <tr key={i} className="border-b last:border-b-0 hover:bg-gray-50/50">
              <td className="max-w-0 px-4 py-3">
                <FakeBookLink book={book} />
              </td>
              <td className="max-w-0 px-4 py-3">
                <DoNotTruncateTextContent>
                  <FakeAuthorLink name={book.author} />
                </DoNotTruncateTextContent>
              </td>
              <td className="px-4 py-3 text-gray-500">{book.year ?? "—"}</td>
              <td className="px-4 py-3">
                <RatingStars rating={book.rating} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  ),
};

/**
 * Mock data and helper components for the AllScenarios overview story.
 */

const longText = "Johann Wolfgang von Goethe-Schiller-Lenz und weitere Autoren der Weimarer Klassik";
const exactFitText = "Unbenanntes Buch";
const shortText = "Goethe";
const longUrl = "https://api.example.com/v1/very/long/path?query=param&entityId=12345abcdef67890";
const longHash = "a1b2c3d4e5f6789012345678901234567890abcdef0123456789abcdef";

function HighlightSpan({ text, highlight }: { text: string; highlight: string }): React.ReactElement {
  const index = text.indexOf(highlight);
  if (index < 0) {
    return <span>{text}</span>;
  }
  return (
    <span>
      {text.slice(0, index)}
      <span className="rounded bg-yellow-200">{text.slice(index, index + highlight.length)}</span>
      {text.slice(index + highlight.length)}
    </span>
  );
}

function FakeStatusBadge(): React.ReactElement {
  return (
    <div className="inline-flex shrink-0 items-center rounded-full border border-green-600/60 px-2 py-0.5 text-xs text-green-600">OK</div>
  );
}

/**
 * Deliberately wrong: renders the badge as a `<span>` instead of a `<div>` to demonstrate
 * what breaks when the contract is violated. The selector `[data-text-content] > span`
 * applies truncation styles to the badge too.
 */
function WrongStatusBadgeAsSpan(): React.ReactElement {
  return (
    <span className="inline-flex shrink-0 items-center rounded-full border border-red-600/60 px-2 py-0.5 text-xs text-red-600">WRONG</span>
  );
}

function ScenarioBlock({
  number,
  title,
  description,
  expected,
  children,
}: {
  number: string;
  title: string;
  description: string;
  expected: string;
  children: React.ReactNode;
}): React.ReactElement {
  return (
    <section className="space-y-3">
      <header className="space-y-1">
        <h3 className="text-lg font-semibold">
          <span className="text-gray-400">{number} · </span>
          {title}
        </h3>
        <p className="text-sm text-gray-600">{description}</p>
        <p className="text-xs text-gray-500">
          <span className="font-medium">Expected:</span> {expected}
        </p>
      </header>
      {children}
    </section>
  );
}

function WidthMatrixHeader(): React.ReactElement {
  return (
    <div className="grid grid-cols-[80px_200px_400px_1fr] gap-4 font-mono text-xs text-gray-500">
      <div>80px</div>
      <div>200px</div>
      <div>400px</div>
      <div>unbounded</div>
    </div>
  );
}

function WidthMatrix({ children }: { children: () => React.ReactNode }): React.ReactElement {
  return (
    <div className="space-y-2">
      <WidthMatrixHeader />
      <div className="grid grid-cols-[80px_200px_400px_1fr] items-start gap-4">
        <div className="rounded border border-dashed border-gray-400 bg-gray-50/30 p-1">{children()}</div>
        <div className="rounded border border-dashed border-gray-400 bg-gray-50/30 p-1">{children()}</div>
        <div className="rounded border border-dashed border-gray-400 bg-gray-50/30 p-1">{children()}</div>
        <div className="rounded border border-dashed border-gray-400 bg-gray-50/30 p-1">{children()}</div>
      </div>
    </div>
  );
}

function SubLabel({ children }: React.PropsWithChildren): React.ReactElement {
  return <p className="text-xs font-semibold text-gray-700">{children}</p>;
}

/**
 * AllScenarios — comprehensive overview of every DOM constellation that the
 * `data-text-content` truncation contract is expected to handle.
 *
 * Each scenario renders the same content in four width cells (80px / 200px /
 * 400px / unbounded) so the behavior at narrow, exact-fit, wide-overflow, and
 * unconstrained widths is visible side-by-side. The story is the executable
 * reference for `internal/patterns/docs/typescript/truncate-text-content.md`.
 */
export const AllScenarios: StoryObj<typeof meta> = {
  parameters: { name: "AllScenarios — Truncation contract overview" },
  render: () => (
    <div className="space-y-6">
      <header className="space-y-1">
        <h2 className="text-2xl font-bold">Truncation Scenarios — Overview</h2>
        <p className="text-sm text-gray-600">
          Every scenario renders the same content at four container widths. Cells use{" "}
          <code className="rounded bg-gray-100 px-1">grid-template-columns: 80px 200px 400px 1fr</code> so each column is exactly as wide as
          its label. Dashed borders mark the truncation containers.
        </p>
      </header>

      <ScenarioBlock
        number="1"
        title="Pure inline-only"
        description="<span data-text-content> with a single inner <span>. The minimum HighlightText shape."
        expected="Ellipsis at 80px and 200px; full text at 400px and unbounded."
      >
        <WidthMatrix>{() => <FakeAuthorName name={longText} />}</WidthMatrix>
      </ScenarioBlock>

      <ScenarioBlock
        number="2"
        title="Inline + trailing badge"
        description="<span data-text-content> with <span> + <div> (BookLink/AuthorCountryLink shape). Badge is rendered as <div> and stays fully visible."
        expected="The text-span truncates; the badge is always fully rendered, regardless of width."
      >
        <WidthMatrix>
          {() => (
            <span data-text-content className="inline-flex items-center gap-2">
              <span>{longText}</span>
              <FakeStatusBadge />
            </span>
          )}
        </WidthMatrix>
      </ScenarioBlock>

      <ScenarioBlock
        number="3"
        title="Nested inline elements"
        description="<span data-text-content> with an inner <span> that contains a highlight <span> (HighlightText with <mark>-style markup), italic and mixed glyphs."
        expected="Highlighted segment renders inline; truncation cuts the line at the right edge with ellipsis when overflowing."
      >
        <div className="space-y-3">
          <SubLabel>Plain highlight markup</SubLabel>
          <WidthMatrix>
            {() => (
              <span data-text-content className="inline-flex items-center gap-2">
                <HighlightSpan text={longText} highlight="Goethe" />
              </span>
            )}
          </WidthMatrix>
          <SubLabel>Italic + highlight + mixed glyphs</SubLabel>
          <WidthMatrix>
            {() => (
              <span data-text-content className="inline-flex items-center gap-2">
                <span className="italic">
                  <HighlightSpan text={longText} highlight="Klassik" />
                </span>
              </span>
            )}
          </WidthMatrix>
        </div>
      </ScenarioBlock>

      <ScenarioBlock
        number="4"
        title="Block-element root with data-text-content"
        description="data-text-content on a <div> (real use case — card body, page heading wrapper). The selector targets [data-text-content] > span."
        expected="(a) block root + inner <span>: truncates. (b) block root + raw text: does not truncate (selector miss). (c) inline-flex root: truncates as the canonical shape."
      >
        <div className="space-y-3">
          <SubLabel>
            (a) <code className="rounded bg-gray-100 px-1">{`<div data-text-content><span>...</span></div>`}</code> — truncates
          </SubLabel>
          <WidthMatrix>
            {() => (
              <div data-text-content>
                <span>{longText}</span>
              </div>
            )}
          </WidthMatrix>
          <SubLabel>
            (b) <code className="rounded bg-gray-100 px-1">{`<div data-text-content>raw text</div>`}</code> — does not truncate (no inner{" "}
            <code className="rounded bg-gray-100 px-1">{`<span>`}</code>; selector miss)
          </SubLabel>
          <WidthMatrix>{() => <div data-text-content>{longText}</div>}</WidthMatrix>
          <SubLabel>
            (c) <code className="rounded bg-gray-100 px-1">{`<span data-text-content className="inline-flex">...</span>`}</code> — canonical
          </SubLabel>
          <WidthMatrix>
            {() => (
              <span data-text-content className="inline-flex items-center gap-2">
                <span>{longText}</span>
              </span>
            )}
          </WidthMatrix>
        </div>
      </ScenarioBlock>

      <ScenarioBlock
        number="5"
        title="Multi-line text in truncation zone"
        description="Text that would wrap across lines without whitespace-nowrap."
        expected="whitespace-nowrap forces single-line rendering; ellipsis appears on overflow. No wrapping in any cell."
      >
        <WidthMatrix>
          {() => (
            <span data-text-content className="inline-flex items-center gap-2">
              <span>Mehrere Wörter die ohne Truncation umbrechen würden weil der Text die Zeilenbreite überschreitet</span>
            </span>
          )}
        </WidthMatrix>
      </ScenarioBlock>

      <ScenarioBlock
        number="6"
        title="Flex parent"
        description="data-text-content as a flex item — the typical table/card layout. Browser default min-width: auto on flex items prevents shrinking below intrinsic content width."
        expected="Without min-w-0 on the flex item: container expands and ellipsis does not appear. With min-w-0: ellipsis works correctly."
      >
        <div className="space-y-3">
          <SubLabel>Without min-w-0 on the flex item — broken (container overflows)</SubLabel>
          <WidthMatrix>
            {() => (
              <div className="flex items-center gap-2 overflow-hidden">
                <span data-text-content className="inline-flex items-center gap-2">
                  <span>{longText}</span>
                </span>
                <FakeStatusBadge />
              </div>
            )}
          </WidthMatrix>
          <SubLabel>With min-w-0 on the flex item — correct (ellipsis appears)</SubLabel>
          <WidthMatrix>
            {() => (
              <div className="flex items-center gap-2 overflow-hidden">
                <span data-text-content className="inline-flex min-w-0 items-center gap-2">
                  <span>{longText}</span>
                </span>
                <FakeStatusBadge />
              </div>
            )}
          </WidthMatrix>
        </div>
      </ScenarioBlock>

      <ScenarioBlock
        number="7"
        title="Grid parent"
        description="data-text-content in a grid — the same min-width: auto trap as flex applies to grid items."
        expected="Without minmax(0, 1fr): grid item refuses to shrink and overflows. With minmax(0, 1fr): truncation works."
      >
        <div className="space-y-3">
          <SubLabel>
            Grid with <code className="rounded bg-gray-100 px-1">grid-cols-[1fr_auto]</code> — broken
          </SubLabel>
          <WidthMatrix>
            {() => (
              <div className="grid grid-cols-[1fr_auto] items-center gap-2">
                <span data-text-content className="inline-flex items-center gap-2">
                  <span>{longText}</span>
                </span>
                <FakeStatusBadge />
              </div>
            )}
          </WidthMatrix>
          <SubLabel>
            Grid with <code className="rounded bg-gray-100 px-1">grid-cols-[minmax(0,1fr)_auto]</code> — correct
          </SubLabel>
          <WidthMatrix>
            {() => (
              <div className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-2">
                <span data-text-content className="inline-flex items-center gap-2">
                  <span>{longText}</span>
                </span>
                <FakeStatusBadge />
              </div>
            )}
          </WidthMatrix>
        </div>
      </ScenarioBlock>

      <ScenarioBlock
        number="8"
        title="Nested truncation zones"
        description="TruncateTextContent inside TruncateTextContent (idempotent), and DoNotTruncateTextContent inside TruncateTextContent (inner subtree opts out)."
        expected="Nested TruncateTextContent has no extra effect. DoNotTruncateTextContent restores full-text rendering for its subtree."
      >
        <div className="space-y-3">
          <SubLabel>Nested TruncateTextContent — should match the outer behavior</SubLabel>
          <WidthMatrix>
            {() => (
              <TruncateTextContent>
                <FakeAuthorName name={longText} />
              </TruncateTextContent>
            )}
          </WidthMatrix>
          <SubLabel>DoNotTruncateTextContent inside TruncateTextContent — text wraps to multiple lines</SubLabel>
          <WidthMatrix>
            {() => (
              <DoNotTruncateTextContent>
                <FakeAuthorName name={longText} />
              </DoNotTruncateTextContent>
            )}
          </WidthMatrix>
        </div>
      </ScenarioBlock>

      <ScenarioBlock
        number="9"
        title="Italic glyph overhang"
        description="Italic glyphs have a right-ward overhang. overflow: hidden clips the slanted edge at the content-box; overflow: clip + overflow-clip-margin: 0.25em renders the overhang fully while ellipsis still triggers on real overflow."
        expected="Exact-fit text renders fully (no clipped italic edge). Overflowing text shows ellipsis."
      >
        <div className="space-y-3">
          <SubLabel>Exact fit — italic text must not be clipped at the right edge</SubLabel>
          <WidthMatrix>
            {() => (
              <span data-text-content className="inline-flex items-center gap-2">
                <span className="italic">{exactFitText}</span>
              </span>
            )}
          </WidthMatrix>
          <SubLabel>Real overflow — ellipsis appears under italic</SubLabel>
          <WidthMatrix>
            {() => (
              <span data-text-content className="inline-flex items-center gap-2">
                <span className="italic">{longText}</span>
              </span>
            )}
          </WidthMatrix>
        </div>
      </ScenarioBlock>

      <ScenarioBlock
        number="10"
        title="Sub-glyph container width"
        description="Container narrower than the minimum glyph width — the truncation must not break the surrounding layout."
        expected="At 80px the cell is wider than glyphs but narrower than most words; the layout stays intact and ellipsis appears."
      >
        <WidthMatrix>
          {() => (
            <span data-text-content className="inline-flex items-center gap-2">
              <span>{shortText}</span>
            </span>
          )}
        </WidthMatrix>
      </ScenarioBlock>

      <ScenarioBlock
        number="11"
        title="Long unbroken words"
        description="URLs, hashes, identifiers without whitespace — the natural break opportunities CSS uses to wrap text are absent."
        expected="Text is treated as a single word; ellipsis cuts at the right edge regardless of natural word boundaries."
      >
        <div className="space-y-3">
          <SubLabel>URL</SubLabel>
          <WidthMatrix>
            {() => (
              <span data-text-content className="inline-flex items-center gap-2">
                <span>{longUrl}</span>
              </span>
            )}
          </WidthMatrix>
          <SubLabel>Hash / identifier</SubLabel>
          <WidthMatrix>
            {() => (
              <span data-text-content className="inline-flex items-center gap-2">
                <span>{longHash}</span>
              </span>
            )}
          </WidthMatrix>
        </div>
      </ScenarioBlock>

      <ScenarioBlock
        number="12"
        title="Truncated next to non-truncated content & contract violation"
        description="The <span>/<div> contract: span children are truncated, div children are not. The contract-violation row demonstrates what breaks when a badge is rendered as <span> instead of <div>."
        expected="Correct row: <span> truncates, <div> badge stays fully visible. Wrong row: the <span> badge is also truncated by the same selector."
      >
        <div className="space-y-3">
          <SubLabel>
            Correct: badge as <code className="rounded bg-gray-100 px-1">{`<div>`}</code> — badge always full, span truncates
          </SubLabel>
          <WidthMatrix>
            {() => (
              <span data-text-content className="inline-flex items-center gap-2">
                <span>{longText}</span>
                <FakeStatusBadge />
              </span>
            )}
          </WidthMatrix>
          <SubLabel>
            Contract violation: badge as <code className="rounded bg-gray-100 px-1">{`<span>`}</code> — badge gets truncated too
          </SubLabel>
          <WidthMatrix>
            {() => (
              <span data-text-content className="inline-flex items-center gap-2">
                <span>{longText}</span>
                <WrongStatusBadgeAsSpan />
              </span>
            )}
          </WidthMatrix>
        </div>
      </ScenarioBlock>

      <ScenarioBlock
        number="13"
        title="Link wrapper between data-text-content markers"
        description="An <a> sits between an outer [data-text-content] marker and an inner [data-text-content] marker (as in BookLink/AuthorLink wrapped by an outer marker). The selector contract extends through one <a> wrapper so the outer marker also constrains the link, giving the inner marker a bounded containing block to clip against."
        expected="(a) Nested marker via FakeBookLink wrapped in outer [data-text-content]: ellipsis at 80px / 200px, badge stays full, full text at 400px and unbounded. (b) Same structure inside DoNotTruncateTextContent re-expands to natural wrapping."
      >
        <div className="space-y-3">
          <SubLabel>
            (a){" "}
            <code className="rounded bg-gray-100 px-1">{`<span data-text-content> > <a> > <span data-text-content> > <span> + <div>`}</code>{" "}
            via FakeBookLink — text truncates, badge full
          </SubLabel>
          <WidthMatrix>
            {() => (
              <span data-text-content className="inline-flex items-center gap-2">
                <FakeBookLink book={books[0]} />
              </span>
            )}
          </WidthMatrix>
          <SubLabel>
            (b) Nested case inside <code className="rounded bg-gray-100 px-1">DoNotTruncateTextContent</code> — reverse selector traverses
            the <code className="rounded bg-gray-100 px-1">{`<a>`}</code> wrapper, text expands fully
          </SubLabel>
          <WidthMatrix>
            {() => (
              <DoNotTruncateTextContent>
                <span data-text-content className="inline-flex items-center gap-2">
                  <FakeAuthorLink name={longText} />
                </span>
              </DoNotTruncateTextContent>
            )}
          </WidthMatrix>
        </div>
      </ScenarioBlock>

      <ScenarioBlock
        number="14"
        title="Text beside a badge inside a BadgeBar"
        description="A table cell lays out a commit link and a point in time in a BadgeBar, a flex row. Each renderer root is a [data-text-content] marker; FormattedDateTime renders its value as a <span>."
        expected="Up to 400px the link and the date share the width and each ends in an ellipsis, the badge stays full; unbounded everything shows in full."
      >
        <WidthMatrix>
          {() => (
            <BadgeBar>
              <span data-text-content>
                <span className="font-mono">{longHash}</span>
              </span>
              <span data-text-content className="text-muted-foreground text-sm">
                <FormattedDateTime date="2026-10-23T13:45:00Z" />
              </span>
              <FakeStatusBadge />
            </BadgeBar>
          )}
        </WidthMatrix>
      </ScenarioBlock>
    </div>
  ),
};

const FakeOrderNotes = entityPropertyRenderer("order")<{ notes: string }>({
  displayName: "FakeOrderNotes",
  render: ({ entity }) => <HighlightText text={entity.notes} multiline />,
});

const orderWithParagraphs = { notes: "Geschenkverpackung\nbitte bis Freitag\n\nLieferung an die Nachbarn" };

/**
 * A multiline text shows its breaks and search hits in a details list and stays on one line in a truncation zone.
 */
export const MultilineText: StoryObj<typeof meta> = {
  render: () => (
    <HighlightSource tokens="Freitag">
      <div className="flex flex-col gap-4">
        <div data-testid="truncation-zone" className="w-64">
          <FakeOrderNotes order={orderWithParagraphs} />
        </div>
        <div data-testid="details-zone">
          <EntityDetailsList rows={[{ label: "Notes", value: <FakeOrderNotes order={orderWithParagraphs} /> }]} />
        </div>
      </div>
    </HighlightSource>
  ),
};
