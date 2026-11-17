import { ArchiveFilter } from "@neckar-hub/typescript-utils/openapi-types";
import type { DataSliceParams, FilterParams } from "./DataSliceController";
import { parseInstant, parseInteger, parseLocalDate, parseNumber } from "./urlCodecs";

/**
 * How one request parameter reaches the URL and comes back, and the value it has while nothing sets it. The URL
 * controller and the state controller of a table are built from the same codecs.
 */
export interface ParamCodec<Value> {
  readonly defaultValue: Value;

  /** Reads the value from the URL; an absent or unreadable parameter yields the default value. */
  fromUrl(urlValue: string | null | undefined): Value;

  /** The URL value; `null` removes the parameter, which the default value always does. */
  toUrl(value: Value): string | null;
}

/**
 * One codec per filter parameter of the generated `Params`. A parameter without a codec, a codec for a parameter
 * `Params` lacks, and a codec of another value type each break the compilation.
 */
export type ParamDefinition<Params extends DataSliceParams<string>> = {
  readonly [Key in keyof FilterParams<Params>]-?: ParamCodec<FilterParams<Params>[Key]>;
};

/** The definition of a table whose request has no filter parameter. */
export const noParams = {} as const;

/** A text parameter; an empty text is no value. */
export function stringParam(): ParamCodec<string | undefined> {
  return {
    defaultValue: undefined,
    fromUrl: emptyToUndefined,
    toUrl: (value) => emptyToUndefined(value) ?? null,
  };
}

/** A whole number parameter, e.g. a count or a year. */
export function integerParam(): ParamCodec<number | undefined> {
  return numberParam(parseInteger);
}

/** A decimal number parameter, e.g. a power in kWp. */
export function floatParam(): ParamCodec<number | undefined> {
  return numberParam(parseNumber);
}

function numberParam(parse: (urlValue: string | null | undefined) => number | undefined): ParamCodec<number | undefined> {
  return {
    defaultValue: undefined,
    fromUrl: parse,
    toUrl: (value) => value?.toString() ?? null,
  };
}

/** A parameter that filters only while true: the URL carries `true`, `false` is no value. */
export function booleanParam(): ParamCodec<boolean | undefined> {
  return {
    defaultValue: undefined,
    fromUrl: (urlValue) => (urlValue === "true" ? true : undefined),
    toUrl: (value) => (value === true ? "true" : null),
  };
}

/**
 * One value of an enum parameter.
 *
 * @param values - The generated const object of the enum, e.g. `PublicationStatus`
 * @param options.defaultValue - The value while the URL names none; the URL omits it
 */
export function enumParam<Value extends string>(
  values: Readonly<Record<string, Value>>,
  options: { defaultValue?: Value } = {},
): ParamCodec<Value | undefined> {
  const allowed: readonly Value[] = Object.values(values);
  const { defaultValue } = options;
  return {
    defaultValue,
    fromUrl: (urlValue) => allowed.find((value) => value === urlValue) ?? defaultValue,
    toUrl: (value) => (value === undefined || value === defaultValue ? null : value),
  };
}

/** The archive state of an entity: the request always carries it, the URL omits the default `OnlyActive`. */
export function archiveParam(): ParamCodec<ArchiveFilter | undefined> {
  return enumParam(ArchiveFilter, { defaultValue: ArchiveFilter.OnlyActive });
}

/** Any number of values of an enum parameter that takes a list; the URL joins them with commas. */
export function enumSetParam<Value extends string>(values: Readonly<Record<string, Value>>): ParamCodec<Value[] | undefined> {
  const allowed: readonly Value[] = Object.values(values);
  return {
    defaultValue: undefined,
    fromUrl: (urlValue) => {
      const selected = allowed.filter((value) => (urlValue ?? "").split(",").includes(value));
      return selected.length === 0 ? undefined : selected;
    },
    toUrl: (value) => (value === undefined || value.length === 0 ? null : value.join(",")),
  };
}

/** A calendar day `yyyy-MM-dd`, for a `LocalDate` column. */
export function localDateParam(): ParamCodec<string | undefined> {
  return validatedStringParam((urlValue) => parseLocalDate(urlValue) !== undefined);
}

/** An ISO-8601 instant, for an `Instant` column. */
export function instantParam(): ParamCodec<string | undefined> {
  return validatedStringParam((urlValue) => parseInstant(urlValue) !== undefined);
}

function validatedStringParam(isValid: (urlValue: string) => boolean): ParamCodec<string | undefined> {
  return {
    defaultValue: undefined,
    fromUrl: (urlValue) => {
      const value = emptyToUndefined(urlValue);
      return value !== undefined && isValid(value) ? value : undefined;
    },
    toUrl: (value) => emptyToUndefined(value) ?? null,
  };
}

function emptyToUndefined(value: string | null | undefined): string | undefined {
  return value === null || value === undefined || value === "" ? undefined : value;
}
