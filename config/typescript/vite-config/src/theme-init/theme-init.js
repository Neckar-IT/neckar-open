// Applies what the ThemeProvider persisted, before the first paint: a classic script, because a
// module is deferred past the paint and an inline block is what `script-src 'self'` refuses to run.
// `theme-init-plugin.ts` emits this file as an asset and puts the tag into the head.
(function () {
  try {
    const theme = window.localStorage.getItem("nit.theme");
    const colorScheme = window.localStorage.getItem("nit.resolved-color-scheme");
    if (theme !== null) {
      document.documentElement.setAttribute("data-theme", theme);
    }
    if (colorScheme === "dark" || colorScheme === "light") {
      document.documentElement.classList.add(colorScheme);
      document.documentElement.style.colorScheme = colorScheme;
    }
  } catch (_ignore) {
    // localStorage is blocked in privacy mode, and the default theme is the answer there.
  }
})();
