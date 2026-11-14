import { type Story, type StoryDefault } from "@ladle/react";
import { useState } from "react";
import { Input } from "../../primitives/input";
import { HighlightSource } from "./HighlightContext";
import { HighlightText } from "./HighlightText";

export default {
  title: "Text / HighlightSource",
} satisfies StoryDefault;

interface Args {
  outerText: string;
  innerText: string;
  outsideText: string;
}

/**
 * Demonstrates the `HighlightSource` ↔ `HighlightText` pair: the source publishes the search
 * tokens, descendant `HighlightText` instances read them from context. Nested sources override
 * the outer value; `HighlightText` instances rendered outside any source receive no highlight.
 */
export const HighlightSourceStory: Story<Args> = ({ outerText, innerText, outsideText }) => {
  const [outerSearch, setOuterSearch] = useState("lorem");
  const [innerSearch, setInnerSearch] = useState("dolor");

  return (
    <div className="flex flex-col gap-6">
      <div className="flex gap-3">
        <Input
          className="w-60"
          value={outerSearch}
          placeholder="outer source tokens"
          onChange={(event) => {
            setOuterSearch(event.target.value);
          }}
        />
        <Input
          className="w-60"
          value={innerSearch}
          placeholder="inner source tokens"
          onChange={(event) => {
            setInnerSearch(event.target.value);
          }}
        />
      </div>

      <HighlightSource tokens={outerSearch}>
        <section className="rounded border border-amber-300 bg-amber-50 p-4">
          <h3 className="mb-2 text-sm font-semibold">Outer HighlightSource</h3>
          <HighlightText text={outerText} />

          <HighlightSource tokens={innerSearch}>
            <section className="mt-4 rounded border border-sky-300 bg-sky-50 p-4">
              <h3 className="mb-2 text-sm font-semibold">Inner HighlightSource (overrides outer)</h3>
              <HighlightText text={innerText} />
            </section>
          </HighlightSource>
        </section>
      </HighlightSource>

      <section className="rounded border border-zinc-300 p-4">
        <h3 className="mb-2 text-sm font-semibold">Outside any HighlightSource (no highlight)</h3>
        <HighlightText text={outsideText} />
      </section>
    </div>
  );
};
HighlightSourceStory.storyName = "Highlight Source";
HighlightSourceStory.args = {
  outerText: "Lorem ipsum dolor sit amet, consetetur sadipscing elitr.",
  innerText: "Lorem ipsum dolor sit amet — the inner source highlights 'dolor', not 'lorem'.",
  outsideText: "Lorem ipsum dolor sit amet — no source above, so nothing is highlighted.",
};
