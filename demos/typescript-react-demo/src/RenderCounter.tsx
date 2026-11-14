import { useRef } from "react";

/**
 * Shows how often React has invoked this component body.
 *
 * Mutating a ref during render is impure by React's rules, and deliberate here: it is the only way to observe
 * render calls that produce identical output. `<React.StrictMode>` invokes every component body twice per commit
 * in development, so the number shown is twice the number of committed renders — first paint already reads 2.
 */
export const RenderCounter = () => {
  const renderCounter = useRef(0);
  renderCounter.current++;
  return <div className="bg-emerald-200 p-4">Rendered: {renderCounter.current}</div>;
};
