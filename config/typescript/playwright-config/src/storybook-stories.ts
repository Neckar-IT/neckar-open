import fetch from "sync-fetch";

/** A story of the index `/index.json` of a Storybook. */
export interface StorybookStory {
  id: string;
  tags: string[];
}

interface StorybookIndexEntry {
  id: string;
  type: "story" | "docs";
  tags?: string[];
}

/**
 * Every story of the Storybook at `url`, read synchronously: Playwright declares the tests of a file before the first one runs.
 */
export function storybookStories(url: string): StorybookStory[] {
  const entries = fetch(`${url}/index.json`).json().entries as Record<string, StorybookIndexEntry>;

  return Object.values(entries)
    .filter((entry) => entry.type === "story")
    .map((entry) => ({ id: entry.id, tags: entry.tags ?? [] }));
}
