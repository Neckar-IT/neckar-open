/**
 * Imports
 */

import { execFileSync } from "node:child_process";
import { readFileSync, statSync } from "node:fs";
import { createServer } from "node:http";
import type { AddressInfo } from "node:net";
import { extname, join, resolve, sep } from "node:path";
import { type ConsoleMessage, expect, type Page } from "@playwright/test";
import { storiesOfIndex } from "./storybook-stories";

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
 * Opens the first story of `index.json` of the Storybook build in `directory` under `staticSitePolicy()` and returns the page
 * errors and policy violations until the story rendered and the network was idle; throws with them when none renders within 15 s or on an error display.
 */
export async function openStorybookBuildUnderStaticSitePolicy(page: Page, directory: string): Promise<string[]> {
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

  const preview = page.frameLocator("#storybook-preview-iframe");
  const renderedStory = preview.locator("#storybook-root > *").first();
  // A story that throws, or an index without stories, swaps the root for Storybook's own display.
  const showsError = async (): Promise<boolean> => (await preview.locator("body.sb-show-errordisplay, body.sb-show-nopreview").count()) > 0;
  const noStoryMessage = (): string => `Storybook rendered no story from ${directory}; the browser reported: ${JSON.stringify(problems)}`;

  // The start page opens the first index entry, with autodocs a docs page that renders outside `#storybook-root`.
  const firstStory = storiesOfIndex(JSON.parse(readFileSync(join(directory, "index.json"), "utf8")))[0];

  try {
    if (firstStory === undefined) {
      throw new Error(`${directory}/index.json lists no story`);
    }
    await page.goto(`${server.url}/?path=/story/${firstStory.id}`);
    await expect(renderedStory).toBeAttached({ timeout: 15_000 }).catch((error: unknown) => {
      throw new Error(noStoryMessage(), { cause: error });
    });
    // Catches violations of the story's requests still in flight; one reported after an idle network is missed.
    await page.waitForLoadState("networkidle");

    // A story may crash after its first commit.
    if ((await renderedStory.count()) === 0 || (await showsError())) {
      throw new Error(noStoryMessage());
    }
  } finally {
    page.off("pageerror", onPageError);
    page.off("console", onConsole);
    await server.close();
  }

  return problems;
}
