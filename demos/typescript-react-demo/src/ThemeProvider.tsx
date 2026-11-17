import type { PropsWithChildren, ReactNode } from "react";
import { createContext, useContext, useEffect, useState } from "react";

const ThemeContext = createContext("light");

type ChangingThemeProviderProps = PropsWithChildren<{}>;

/**
 * Flips the context value between 'light' and 'dark' every 3 seconds.
 *
 * The state lives in this component, so it re-renders on every flip — but `children` is an element created by the
 * caller and therefore skipped. That isolates the one thing the demo is about: only the consumers that read the
 * context re-render, not everything below the provider.
 */
export const ChangingThemeProvider = ({ children }: ChangingThemeProviderProps): ReactNode => {
  const [theme, setTheme] = useState<"light" | "dark">("light");
  useEffect(() => {
    const handle = window.setInterval(() => {
      setTheme((oldTheme) => (oldTheme === "light" ? "dark" : "light"));
    }, 3000);
    return () => {
      window.clearInterval(handle);
    };
  }, []);

  return (
    <ThemeContext.Provider value={theme}>
      <div className="flex flex-col gap-y-4 bg-cyan-300 p-4">{children}</div>
    </ThemeContext.Provider>
  );
};

/** Subscribes with `useContext` and therefore re-renders on every flip — a sibling that merely sits inside the provider does not. */
export const ThemeConsumer = () => {
  const theme = useContext(ThemeContext);
  return <div className="bg-cyan-500 p-4">Current theme: {theme}</div>;
};
