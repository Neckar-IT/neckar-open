/** Kotlin's `trim()` strips U+001C–U+001F as well; JavaScript's does not, and the form is never stricter. */
export function trimAsKotlin(value: string): string {
  const isBlank = (character: string) => /\s/u.test(character) || (character >= "\u001c" && character <= "\u001f");
  let start = 0;
  let end = value.length;
  while (start < end && isBlank(value.charAt(start))) {
    start += 1;
  }
  while (end > start && isBlank(value.charAt(end - 1))) {
    end -= 1;
  }
  return value.slice(start, end);
}
