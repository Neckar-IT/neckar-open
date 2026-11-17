/**
 * Function to capitalize only the first character of a string
 */

export function capitalizeFirstCharacter(str: string): string {
  if (!str) return str; // handle empty string
  return `${str.charAt(0).toUpperCase()}${str.slice(1)}`;
}
