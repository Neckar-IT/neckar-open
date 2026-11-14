import type { ReactNode } from "react";

/**
 * Something that can be rendered either directly as a React node or as a function
 * that receives a context and returns a React node.
 *
 * This enables callers to provide either a static piece of content or a dynamic
 * renderer that reacts to some contextual information.
 */
export type Renderable<TContext> = ReactNode | ((context: TContext) => ReactNode);

/**
 * Renders a {@link Renderable}. If it is a function, it is called with the given context;
 * otherwise, it is returned as-is.
 */
export function flexRender<TContext>(renderable: Renderable<TContext>, context: TContext): ReactNode {
  if (typeof renderable === "function") {
    return (renderable as (context: TContext) => ReactNode)(context);
  }
  return renderable;
}
