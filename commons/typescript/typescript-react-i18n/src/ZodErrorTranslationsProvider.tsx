/**
 * This will translate the zod error messages
 *
 * inspired by this library: https://github.com/aiji42/zod-i18n/tree/main
 */

/**
 * Imports
 */

import type { PropsWithChildren, ReactElement } from "react";
import { useCallback, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { z as zod } from "zod";
import { type $ZodErrorMap } from "zod/v4/core";

/**
 * Utility Functions
 */

function boundaryOptionsToString(exact: boolean | undefined, inclusive: boolean | undefined): string {
  return exact ? "exact" : inclusive ? "inclusive" : "not_inclusive";
}

/**
 * Globally set default Translations for Zod Errors
 */

type ZodErrorTranslationsProviderProps = PropsWithChildren<{}>;

export function ZodErrorTranslationsProvider(props: ZodErrorTranslationsProviderProps): ReactElement {
  const { t, i18n } = useTranslation();

  const zodErrorMap: $ZodErrorMap = useCallback(
    (issue) => {
      switch (issue.code) {
        case "invalid_type":
          if (issue.input === undefined) {
            return t("zod:errors.invalid_type.received_undefined");
          } else if (issue.input === null) {
            return t("zod:errors.invalid_type.received_null");
          } else {
            return t("zod:errors.invalid_type.default", {
              expected: t(`zod:errors.types.${issue.expected}`),
            });
          }

        case "unrecognized_keys":
          return t("zod:errors.unrecognized_keys", {
            keys: issue.keys.join(", "),
          });

        case "too_small":
          switch (issue.origin) {
            case "array":
              return t(`zod:errors.too_small.array.${boundaryOptionsToString(issue.exact, issue.inclusive)}`, {
                minimum: issue.minimum,
              });
            case "string":
              if (issue.minimum === 1) {
                return t("zod:errors.invalid_type.received_undefined");
              }

              return t(`zod:errors.too_small.string.${boundaryOptionsToString(issue.exact, issue.inclusive)}`, {
                minimum: issue.minimum,
              });
            case "number":
            case "int":
            case "bigint":
              return t(`zod:errors.too_small.number.${boundaryOptionsToString(issue.exact, issue.inclusive)}`, {
                minimum: issue.minimum,
              });
            case "set":
              return t(`zod:errors.too_small.set.${boundaryOptionsToString(issue.exact, issue.inclusive)}`);
            case "date":
              return t(`zod:errors.too_small.date.${boundaryOptionsToString(issue.exact, issue.inclusive)}`, {
                minimum: new Date(issue.minimum as number),
              });
            case "file":
              return t(`zod:errors.too_small.file.${boundaryOptionsToString(issue.exact, issue.inclusive)}`, {
                minimum: issue.minimum,
              });
            default:
              return t("zod:errors.too_small.default");
          }

        case "too_big":
          switch (issue.origin) {
            case "array":
              return t(`zod:errors.too_big.array.${boundaryOptionsToString(issue.exact, issue.inclusive)}`, {
                maximum: issue.maximum,
              });
            case "string":
              return t(`zod:errors.too_big.string.${boundaryOptionsToString(issue.exact, issue.inclusive)}`, {
                maximum: issue.maximum,
              });
            case "number":
            case "int":
            case "bigint":
              return t(`zod:errors.too_big.number.${boundaryOptionsToString(issue.exact, issue.inclusive)}`, {
                maximum: issue.maximum,
              });
            case "set":
              return t(`zod:errors.too_big.set.${boundaryOptionsToString(issue.exact, issue.inclusive)}`);
            case "date":
              return t(`zod:errors.too_big.date.${boundaryOptionsToString(issue.exact, issue.inclusive)}`, {
                maximum: new Date(issue.maximum as number),
              });
            case "file":
              return t(`zod:errors.too_big.file.${boundaryOptionsToString(issue.exact, issue.inclusive)}`, {
                maximum: issue.maximum,
              });
            default:
              return t("zod:errors.too_big.default");
          }

        case "invalid_format":
          switch (issue.format) {
            case "starts_with":
              return t("zod:errors.invalid_format.startsWith", { startsWith: issue.pattern });
            case "ends_with":
              return t("zod:errors.invalid_format.endsWith", { endsWith: issue.pattern });
            case "includes":
              return t("zod:errors.invalid_format.includes", { includes: issue.pattern });
            case "date":
              return t("zod:errors.invalid_format.date");
            case "time":
              return t("zod:errors.invalid_format.time");
            case "datetime":
              return t("zod:errors.invalid_format.datetime");
            case "lowercase":
              return t("zod:errors.invalid_format.lowercase");
            case "uppercase":
              return t("zod:errors.invalid_format.uppercase");
            case "regex":
              return t("zod:errors.invalid_format.regex");
            case "base64":
            case "base64url":
            case "cidrv4":
            case "cidrv6":
            case "credit_card":
            case "cuid":
            case "cuid2":
            case "duration":
            case "e164":
            case "email":
            case "emoji":
            case "guid":
            case "iban":
            case "ipv4":
            case "ipv6":
            case "json_string":
            case "jwt":
            case "ksuid":
            case "nanoid":
            case "ulid":
            case "url":
            case "uuid":
            case "xid":
              return t("zod:errors.invalid_format.other", {
                validations: t(`zod:errors.validations.${issue.format}`),
              });
            default:
              return undefined;
          }

        case "not_multiple_of":
          return t("zod:errors.not_multiple_of", {
            multipleOf: issue.divisor,
          });

        case "invalid_union":
          return t("zod:errors.invalid_union");

        case "invalid_element":
          return t(`zod:errors.invalid_element.${issue.origin}`);

        case "invalid_key":
          return t("zod:errors.invalid_key");

        case "invalid_value":
          return t("zod:errors.invalid_value");

        case "custom":
          return undefined;
        default:
          throw new Error(`Unexpected value in Switch Statement: issue.code`);
      }
    },
    [t],
  );

  zod.config({
    customError: zodErrorMap,
  });

  useEffect(() => {
    const handleLanguageChange = () => {
      zod.config({
        customError: zodErrorMap,
      });
    };

    i18n.on("languageChanged", handleLanguageChange);
    return () => {
      i18n.off("languageChanged", handleLanguageChange);
    };
  }, [i18n, zodErrorMap]);

  return <>{props.children}</>;
}
