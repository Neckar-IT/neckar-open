/**
 * Imports
 */

import { storybookServerUrl } from "@neckar-hub/playwright-config";
import { waitForStorybookStoryReady } from "@neckar-hub/typescript-react-utils/storybook";
import { expect, type Page, test } from "@playwright/test";

/**
 * Configuration
 */

const url = storybookServerUrl(); // the Storybook this run serves — port from the package's Storybook slot

/**
 * Width an item got and width its text needs.
 */
interface ItemLayout {
  width: number;
  textWidth: number;
}

/**
 * Widths of the rendered trail in a container of fixed width. jsdom has no layout, so only a browser measures them.
 */
interface TrailLayout {
  containerWidth: number;
  /** Distance from the container's left edge to the right edge of the last item. */
  trailWidth: number;
  items: ItemLayout[];
}

/**
 * Opens the story and measures the trail inside its `breadcrumbs-container`.
 */
async function measureTrail(page: Page, storyId: string): Promise<TrailLayout> {
  await page.setViewportSize({ width: 1600, height: 400 });
  await page.goto(`${url}/iframe.html?id=${storyId}&viewMode=story`);
  await waitForStorybookStoryReady(page);

  return page.getByTestId("breadcrumbs-container").evaluate((container) => {
    const list = container.querySelector("[data-slot=breadcrumb-list]");
    if (list === null) {
      throw new Error("breadcrumb list missing");
    }
    const containerBox = container.getBoundingClientRect();
    const itemElements = [...list.querySelectorAll("[data-slot=breadcrumb-item]")];
    const lastItem = itemElements.at(-1);
    if (lastItem === undefined) {
      throw new Error("breadcrumb without items");
    }
    const items = itemElements.map((item) => {
      // A range spans the laid-out text also where a nested span clips it; `scrollWidth` would stop at the clip.
      const text = document.createRange();
      text.selectNodeContents(item);
      return { width: item.getBoundingClientRect().width, textWidth: text.getBoundingClientRect().width };
    });
    return {
      containerWidth: containerBox.width,
      trailWidth: lastItem.getBoundingClientRect().right - containerBox.left,
      items,
    };
  });
}

/**
 * The item at `index`; the trail of the stories has four.
 */
function itemAt(trail: TrailLayout, index: number): ItemLayout {
  const item = trail.items[index];
  if (item === undefined) {
    throw new Error(`trail has no item ${index}`);
  }
  return item;
}

/** Rounding of subpixel layout. */
const tolerance = 1;

test("a trail wider than its container keeps the short items and shares the rest equally between the long ones", async ({ page }) => {
  const trail = await measureTrail(page, "navigation-breadcrumbs--trail-at-640");
  const dashboard = itemAt(trail, 0);
  const customers = itemAt(trail, 1);
  const company = itemAt(trail, 2);
  const project = itemAt(trail, 3);

  expect(trail.trailWidth).toBeLessThanOrEqual(trail.containerWidth + tolerance);
  expect(dashboard.width).toBeGreaterThanOrEqual(dashboard.textWidth - tolerance);
  expect(customers.width).toBeGreaterThanOrEqual(customers.textWidth - tolerance);
  expect(company.width).toBeLessThan(company.textWidth - tolerance);
  expect(project.width).toBeLessThan(project.textWidth - tolerance);
  expect(Math.abs(company.width - project.width)).toBeLessThanOrEqual(tolerance);
});

test("a trail that fits its container shows every item in full", async ({ page }) => {
  const trail = await measureTrail(page, "navigation-breadcrumbs--trail-at-1280");

  trail.items.forEach((item) => expect(item.width).toBeGreaterThanOrEqual(item.textWidth - tolerance));
});

test("the header next to an open sidebar ends at the viewport and keeps the trail inside", async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 400 });
  await page.goto(`${url}/iframe.html?id=layout-appmain--with-sidebar&viewMode=story`);
  await waitForStorybookStoryReady(page);

  const headerRight = await page.getByRole("banner").evaluate((header) => header.getBoundingClientRect().right);
  const trailRight = await page.getByRole("navigation", { name: "breadcrumb" }).evaluate((nav) => {
    const lastItem = [...nav.querySelectorAll("[data-slot=breadcrumb-item]")].at(-1);
    if (lastItem === undefined) {
      throw new Error("breadcrumb without items");
    }
    return lastItem.getBoundingClientRect().right;
  });

  expect(headerRight).toBeLessThanOrEqual(1280 + tolerance);
  expect(trailRight).toBeLessThanOrEqual(headerRight + tolerance);
});
