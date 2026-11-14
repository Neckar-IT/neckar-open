/**
 * Ladle appends `appendToHead` after Vite expanded its placeholders, so the `${APP_IMAGE_DIGEST}` and `${DEPLOYMENT_STAGE}`
 * tokens stay raw for /docker-entrypoint.d/40-inject-serve-time-values.sh of the showcase container and `serveTimeInjection()`
 * in .ladle/vite.config.ts.
 *
 * The deployment stage carries its own weight here: components gate developer-only UI on it — the
 * form validation panel of `FormDebug`, the i18n pseudo-languages in the language switcher. The
 * container sets DEPLOYMENT_STAGE=Production, so the deployed showcase shows neither; `pnpm ladle`
 * and the Playwright screenshot runs are injected with Development and keep the developer view.
 *
 * Stating it explicitly is belt and braces — a `ladle build` bundle without any injection already
 * reads as Production — but it makes the showcase say what it is instead of relying on a default.
 */

/** @type {import('@ladle/react').UserConfig} */
export default {
  // An array: Ladle evaluates a function, its default included, with `new Function`, which `script-src 'self'` blocks; see `verifyLadleConfigs`.
  storyOrder: ["*"],
  appendToHead: `<meta name="imageDigest" content="\${APP_IMAGE_DIGEST}" />
  <meta name="runtime-context.executionEnvironment" content="\${EXECUTION_ENVIRONMENT}" />
  <meta name="runtime-context.deploymentStage" content="\${DEPLOYMENT_STAGE}" />
  <meta name="runtime-context.serviceHost" content="\${SERVICE_HOST}" />
  <style>
    [data-theme="dark"] {--ladle-bg-color-primary: #002133; --ladle-bg-color-secondary: #006896;}
  </style>`,
  defaultStory: "theme--preview",
  addons: {
    msw: {
      enabled: true,
    },
    rtl: {
      enabled: false,
    },
    width: {
      enabled: true,
      options: {
        base: 400,
        sm: 640,
        md: 768,
        lg: 1024,
        xl: 1280,
        "2xl": 1536,
      },
      defaultState: 0,
    },
  },
  /**
   * Load the vite.config.ts specifically for vite
   */
  viteConfig: `${process.cwd()}/.ladle/vite.config.ts`,
};
