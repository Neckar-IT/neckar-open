/**
 * Imports
 */

import { execFileSync } from "node:child_process";
import { readFileSync, statSync } from "node:fs";
import { createServer } from "node:http";
import type { AddressInfo } from "node:net";
import { extname, join, resolve, sep } from "node:path";
import { type ConsoleMessage, expect, type Locator, type Page } from "@playwright/test";

/**
 * Configuration
 */

const staticSiteDirectory = "internal/infrastructure/common/static-site";

/** The policy variant of `CSP_POLICY_SOURCE=header`, the base image's default. */
const headerTemplatePath = `${staticSiteDirectory}/templates/includes/content-security-policy-header.conf.template`;

const dockerfilePath = `${staticSiteDirectory}/Dockerfile`;

const contentTypes: Record<string, string> = {
  ".html": "text/html; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".mjs": "text/javascript; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".json": "application/json",
  ".svg": "image/svg+xml",
  ".png": "image/png",
  ".ico": "image/x-icon",
  ".woff2": "font/woff2",
};

interface StaticSiteServer {
  url: string;
  close: () => Promise<void>;
}

interface StaticSitePolicy {
  headerName: string;
  headerValue: string;
}

/**
 * The policy header the `static-site` base image sends with its `ENV` defaults: the add_header line of
 * the header template, rendered with the `CSP_*` values of its Dockerfile.
 */
function staticSitePolicy(): StaticSitePolicy {
  const repositoryRoot = execFileSync("git", ["rev-parse", "--show-toplevel"], { encoding: "utf8" }).trim();
  const dockerfile = readFileSync(join(repositoryRoot, dockerfilePath), "utf8");
  const template = readFileSync(join(repositoryRoot, headerTemplatePath), "utf8");

  const defaults = new Map([...dockerfile.matchAll(/(CSP_\w+)="([^"]*)"/g)].map(([, name, value]) => [name, value]));
  const [, headerName, headerValue] = /add_header\s+(\S+)\s+"([^"]*)"/.exec(template) ?? [];
  if (headerName === undefined || headerValue === undefined) {
    throw new Error(`no add_header in ${headerTemplatePath}`);
  }

  const render = (text: string): string =>
    text.replace(/\$\{(\w+)\}/g, (_, name: string) => {
      const value = defaults.get(name);
      if (value === undefined) {
        throw new Error(`${headerTemplatePath} names \${${name}}, which ${dockerfilePath} gives no default`);
      }
      return value;
    });

  return { headerName: render(headerName), headerValue: render(headerValue) };
}

/**
 * Serves the files below `directory` with the header of `staticSitePolicy()`; a path without a file
 * answers 404.
 */
async function serveUnderStaticSitePolicy(directory: string): Promise<StaticSiteServer> {
  const root = resolve(directory);
  if (statSync(root, { throwIfNoEntry: false })?.isDirectory() !== true) {
    throw new Error(`${root} is no directory — build the site first`);
  }

  const policy = staticSitePolicy();

  const server = createServer((request, response) => {
    const pathname = decodeURIComponent(new URL(request.url ?? "/", "http://localhost").pathname);
    const requested = resolve(root, `.${pathname}`);
    const file = pathname.endsWith("/") ? join(requested, "index.html") : requested;

    response.setHeader(policy.headerName, policy.headerValue);

    if (file.startsWith(root + sep) && statSync(file, { throwIfNoEntry: false })?.isFile() === true) {
      response.setHeader("Content-Type", contentTypes[extname(file)] ?? "application/octet-stream");
      response.end(readFileSync(file));
      return;
    }

    response.statusCode = 404;
    response.end();
  });

  await new Promise<void>((resolveListen, rejectListen) => {
    server.once("error", rejectListen);
    server.listen(0, "127.0.0.1", resolveListen);
  });
  const { port } = server.address() as AddressInfo;

  return {
    url: `http://127.0.0.1:${port}`,
    close: () => new Promise<void>((resolveClose, rejectClose) => server.close((error) => (error === undefined ? resolveClose() : rejectClose(error)))),
  };
}

/**
 * Where a story viewer renders its first story, and how it shows that no story rendered.
 */
interface StoryViewer {
  name: string;
  /** The first element of the rendered story. */
  renderedStory: (page: Page) => Locator;
  /** Resolves true when the viewer shows its error page instead of the story. */
  showsError: (page: Page) => Promise<boolean>;
}

/**
 * Opens the build of `viewer` in `directory`, relative to the working directory, under
 * `staticSitePolicy()` and returns the page errors and policy violations reported until the first
 * story rendered and the network was idle; throws with them when no story renders within 15 s or
 * the viewer shows its error page.
 */
async function openBuildUnderStaticSitePolicy(page: Page, directory: string, viewer: StoryViewer): Promise<string[]> {
  const server = await serveUnderStaticSitePolicy(directory);
  const problems: string[] = [];

  const onPageError = (error: Error): void => {
    problems.push(`${error.name}: ${error.message}`);
  };
  const onConsole = (message: ConsoleMessage): void => {
    if (/Content[ -]Security[ -]Policy/.test(message.text())) {
      problems.push(message.text());
    }
  };
  page.on("pageerror", onPageError);
  page.on("console", onConsole);

  const renderedStory = viewer.renderedStory(page);
  const noStoryMessage = (): string => `${viewer.name} rendered no story from ${directory}; the browser reported: ${JSON.stringify(problems)}`;

  try {
    await page.goto(server.url);
    await expect(renderedStory).toBeAttached({ timeout: 15_000 }).catch((error: unknown) => {
      throw new Error(noStoryMessage(), { cause: error });
    });
    // Catches violations of the story's requests still in flight; one reported after an idle network is missed.
    await page.waitForLoadState("networkidle");

    // A story may crash after its first commit.
    if ((await renderedStory.count()) === 0 || (await viewer.showsError(page))) {
      throw new Error(noStoryMessage());
    }
  } finally {
    page.off("pageerror", onPageError);
    page.off("console", onConsole);
    await server.close();
  }

  return problems;
}

/**
 * Opens the Ladle build in `directory` like `openBuildUnderStaticSitePolicy`.
 */
export async function openLadleBuildUnderStaticSitePolicy(page: Page, directory: string): Promise<string[]> {
  return openBuildUnderStaticSitePolicy(page, directory, {
    name: "Ladle",
    // The spinner and Ladle's error pages are the other children; a crashed story leaves `.ladle-main` empty.
    renderedStory: (ladlePage) => ladlePage.locator(".ladle-main > :not(.ladle-ring-wrapper, .ladle-error-content)").first(),
    // "Story not found" renders inside the global provider.
    showsError: async (ladlePage) => (await ladlePage.locator(".ladle-main .ladle-error-content").count()) > 0,
  });
}

/**
 * Opens the Storybook build in `directory` like `openBuildUnderStaticSitePolicy`; the manager page
 * loads the first story into its preview frame.
 */
export async function openStorybookBuildUnderStaticSitePolicy(page: Page, directory: string): Promise<string[]> {
  return openBuildUnderStaticSitePolicy(page, directory, {
    name: "Storybook",
    renderedStory: (storybookPage) => storybookPage.frameLocator("#storybook-preview-iframe").locator("#storybook-root > *").first(),
    // A story that throws, or an index without stories, swaps the root for Storybook's own display.
    showsError: async (storybookPage) =>
      (await storybookPage.frameLocator("#storybook-preview-iframe").locator("body.sb-show-errordisplay, body.sb-show-nopreview").count()) > 0,
  });
}
