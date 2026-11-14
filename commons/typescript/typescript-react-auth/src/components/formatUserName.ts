/**
 * First and last name joined by a space, or `""` when the account carries neither — the caller picks
 * its own fallback, such as the email address.
 */
export function formatUserName(user: { firstName: string; lastName: string }): string {
  return [user.firstName, user.lastName].filter((part) => part !== "").join(" ");
}
