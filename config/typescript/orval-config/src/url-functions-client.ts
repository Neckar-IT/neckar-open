/**
 * Imports
 */

import { type ClientBuilder, type GeneratorVerbOptions, GetterPropType, makeRouteSafe, type OutputClientFunc, Verbs } from "@orval/core";

/**
 * Client
 */

/**
 * The `axios-functions` client plus `<operationName>Url` for every binary GET operation:
 * a PDF opens as a navigation in a new tab and needs its URL as a value.
 */
export const urlFunctionsClient: OutputClientFunc = (clients) => {
  const base = clients["axios-functions"];

  const client: ClientBuilder = async (verbOptions, options, outputClient, output) => {
    const generated = await base.client(verbOptions, options, outputClient, output);
    if (verbOptions.response.isBlob === false || verbOptions.verb !== Verbs.GET) return generated;

    const route = options.context.output.urlEncodeParameters ? makeRouteSafe(options.route, skippedParameters(verbOptions), parameterNames(verbOptions)) : options.route;

    return {
      ...generated,
      implementation: `${generated.implementation}\n${urlFunction(verbOptions, route)}`,
    };
  };

  return { ...base, client };
};

/**
 * URL Function
 */

/**
 * Throws for a binary operation with query parameters: a URL from the route alone misses them.
 */
function urlFunction(verbOptions: GeneratorVerbOptions, route: string): string {
  if (verbOptions.queryParams !== undefined) {
    throw new Error(`Operation "${verbOptions.operationName}" has a binary response and query parameters; urlFunctionsClient writes URL functions for path parameters only`);
  }

  const parameters = verbOptions.props
    .filter((prop) => prop.type === GetterPropType.PARAM || prop.type === GetterPropType.NAMED_PATH_PARAMS)
    .map((prop) => prop.implementation)
    .join(", ");

  return `export const ${verbOptions.operationName}Url = (${parameters}): string => \`${route}\`;\n`;
}

function skippedParameters(verbOptions: GeneratorVerbOptions): Set<string> {
  return new Set(verbOptions.params.filter((param) => param.allowReserved === true).map((param) => param.name));
}

function parameterNames(verbOptions: GeneratorVerbOptions): Set<string> {
  return new Set(verbOptions.params.map((param) => param.name));
}
