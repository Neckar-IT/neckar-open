/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { convertBase64ToFile } from "./convertBase64ToFile";

/**
 * Mock Image
 */

export const mockBase64Image =
  "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACgAAAAoCAYAAACM/rhtAAAACXBIWXMAAAsTAAALEwEAmpwYAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAD2SURBVHgB7ZfRDYIwEIYPfOEBCCPgBMYNGIENjJvoBrgBmziDE+AGNDySkHpnDmN8INSjCdH7ktK0veN+rkcoAIqiKC/S1FpqsCAhrBwVKEUFSlGBUlSgFBUoZfUCAxfjpQ4CXRfMjvtbW0xPTm0YYI9Dw9PUn/setu/rmOsjzt9HG5ob18EnWWZz3OqWz341jrMJ2wxtKrZto8jm4BsM1HDAysGnJp8ksVfwCQYoWVwzlblPOJPPrMexLcABpxrE6impx/o6GROYuX5se+F7HMAB17d4R5fNBm7gSBhCTT0KLMAX0p+ib/z1UydFBUpRgYqi/DsPcnlf4HXVqi0AAAAASUVORK5CYII=";

/**
 * Tests
 */

describe("convertBase64ToFile", () => {
  it("should convert a base64 string to a file", () => {
    const file = convertBase64ToFile(mockBase64Image, "image");

    expect(file.type).toBe("image/png");
    expect(file.name).toBe("image.png");
    expect(file.size).toBe(353);
  });
});
