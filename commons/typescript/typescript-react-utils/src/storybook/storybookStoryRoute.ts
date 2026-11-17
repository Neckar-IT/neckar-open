/** The route a story renders under, from the story parameters `path`, `url` and `params`; `WithMemoryRouter` takes it as props. */
export interface StorybookStoryRoute {
  path?: string;
  url?: string;
  params?: Record<string, string | number>;
}

/** The route of the story parameters of a decorator's story context; throws for a parameter of another type. */
export function storybookStoryRoute(parameters: Record<string, unknown>): StorybookStoryRoute {
  return {
    path: optionalString(parameters, "path"),
    url: optionalString(parameters, "url"),
    params: optionalParams(parameters, "params"),
  };
}

function optionalString(parameters: Record<string, unknown>, name: string): string | undefined {
  const value = parameters[name];
  if (value === undefined || typeof value === "string") {
    return value;
  }

  throw new Error(`Story parameter \`${name}\` is a ${typeof value}, expected a string`);
}

function optionalParams(parameters: Record<string, unknown>, name: string): Record<string, string | number> | undefined {
  const value = parameters[name];
  if (value === undefined) {
    return undefined;
  }
  if (
    typeof value !== "object" ||
    value === null ||
    Array.isArray(value) ||
    Object.values(value).some((entry) => typeof entry !== "string" && typeof entry !== "number")
  ) {
    throw new Error(`Story parameter \`${name}\` is no object of strings and numbers`);
  }

  return value as Record<string, string | number>;
}
