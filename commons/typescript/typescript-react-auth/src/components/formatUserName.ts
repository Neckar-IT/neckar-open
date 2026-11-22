/** First and last name joined by a space, or [fallback] when the person has neither, e.g. the email address. */
export function formatUserName(user: { firstName: string | null; lastName: string | null }, fallback: string): string {
  const name = [user.firstName, user.lastName].filter((part) => part !== null && part !== "").join(" ");
  return name === "" ? fallback : name;
}
