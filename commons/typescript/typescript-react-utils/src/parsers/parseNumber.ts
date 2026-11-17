/**
 * Function to parse a string to a number
 */

export function parseNumber(value: string, decimalSeparator: string, fractionDigits?: number): number | null {
  if (value === "") return null; // Return null if the input is empty or invalid

  // check if number is negative
  const isNegative = value.startsWith("-");

  // Remove all characters except numbers, dots, commas
  const cleanedValue = value.replace(/[^0-9.,]/g, "");

  // ✅ Return null if nothing valid remains
  if (cleanedValue === "") return null;

  let decimalSeparatorFound = false;
  const parsed: string[] = [];
  for (let i = value.length - 1; i > -1; i--) {
    const c = value.charAt(i);
    if (isDigit(c)) {
      parsed.push(c);
    } else {
      if (!decimalSeparatorFound && c === decimalSeparator) {
        decimalSeparatorFound = true;
        parsed.push(".");
      }
    }
  }

  const parsedNumber = Number.parseFloat(parsed.toReversed().join(""));

  const signedNumber = isNegative ? parsedNumber * -1 : parsedNumber;

  // round to the specified amount of fraction digits
  if (typeof fractionDigits === "number") {
    return Number(signedNumber.toFixed(fractionDigits));
  }

  return signedNumber;
}

function isDigit(char: string): boolean {
  return /^[0-9]$/.test(char);
}
