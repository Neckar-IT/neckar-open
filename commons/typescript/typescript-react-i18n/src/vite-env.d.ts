/**
 * Minimal type declaration for import.meta.env.
 * Provided by the bundler (Vite) at build time.
 * This package uses vitest but not vite directly, so we declare the types inline.
 */
interface ImportMetaEnv {
  readonly DEV: boolean;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
