/**
 * Composes the document title as "{pageTitle} · {appName}".
 * When the page title equals the app name, no suffix is added.
 */
export function composeDocumentTitle(pageTitle: string, appName: string): string {
  return pageTitle === appName ? appName : `${pageTitle} · ${appName}`;
}
