# `typescript-react-error-handling` - Fehler- und Verbindungsanzeige für React-Apps

```bash
./gradlew :internal:open:commons:typescript:typescript-react-error-handling:storybook
```

Der Job `🚀 Deploy affected` deployt die Stories jedes `main`-Commits, der `internal/open/commons/typescript/typescript-react-error-handling/` oder eine `workspace:*`-Dependency aus `internal/open/commons/typescript/typescript-react-error-handling/package.json` ändert, nach [https://error-handling.showcase.neckar.it](https://error-handling.showcase.neckar.it).

| Entry Point                                                        | Inhalt                                                                                                                               |
| ------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------ |
| `@neckar-hub/typescript-react-error-handling/global-error-handler` | `GlobalErrorHandler`, `RouterErrorElement`, `ErrorHandler`: [src/global-error-handler/README.md](src/global-error-handler/README.md) |
| `@neckar-hub/typescript-react-error-handling/app-connectivity`     | `AppConnectivityProvider`, `ServiceAvailability`, `useDeviceIsOnline`, `useServiceAvailability`                                      |
| `@neckar-hub/typescript-react-error-handling/tailwind-sources.css` | Tailwind Source Manifest für das Stylesheet des Consumers                                                                            |

Die Reaktion auf jeden HTTP-Status beschreibt `docs/react/http-status-handling.html` im Monorepo.

`internal/open/commons/typescript/typescript-react-error-handling/src/localization/de.json` und `internal/open/commons/typescript/typescript-react-error-handling/src/localization/en.json` definieren die Texte im i18n-Namespace `error-handling`. Eine App überschreibt einen Text mit demselben Key in ihrem eigenen Bundle. Ausgenommen ist `missingCapabilityErrorHandler.message`: `<Trans>` liest diesen Key ausschließlich im Namespace `error-handling`.
