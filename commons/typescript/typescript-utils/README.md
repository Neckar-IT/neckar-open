# typescript-utils

This package contains utilities for typescript:

- **address** — `Address` value class, `emptyDraftAddress` and `toConfirmedAddress` for the Draft/Confirmed lifecycle.
- **array** — Array helpers (e.g. duplicate index detection).
- **collections** — `Sequence` abstraction with lazy operations and step processing.
- **converters** — String and file converters: Base64 ↔ `File`, `slugify`, `capitalizeFirstCharacter`.
- **date** — Parsing and conversion between `DateTime` and `LocalDate`, plus the local-midnight period boundaries `startOfDay`, `startOfNextDay`, `startOfNextWeek` (ISO week) and `startOfNextMonth`.
- **feature-flags** — Runtime feature-flag store with URL and `localStorage` persistence, plus remote API fetch.
- **filter-utilities** — Search-term filtering across arbitrary object arrays.
- **generators** — ID generators: UUID (`generateUuid`) and NanoID (`generateNanoId`).
- **lang** — General language helpers: `isDefined` / `isNotDefined`, `null` ↔ `undefined` conversion, string utilities.
- **openapi-types** — TypeScript types and runtime constants generated from the Kotlin REST APIs via OpenAPI + Orval.
- **patchable** — `Patchable<T>` and `PatchableRequired<T>` discriminated unions (`Update` / `Keep` / `Delete`) for partial-update REST payloads.
- **promise-utils** — Promise type guards (`isPromiseLike`).
- **rest** — Helpers for the project's `RestResponse` type: `RestResponse` / `RestResponseGood` / `RestResponseBad` interfaces, `restResponseGoodType` constant, `dataIfSuccess`.
- **type-casts** — Runtime type-cast helpers (e.g. `castToObjectProps`).
- **type-guards** — Runtime type guards: `isGoodResponse`, `isBadResponse`, `isPaginationInfo`, `isExpectedApplicationError`, `isUnexpectedApplicationError`, `assertExactKeysFromType`.
- **types** — Plain TypeScript type aliases and helper types: `Uuid`, `MakeRequired`, `RequireAtLeastOne`, `DataState`, `FormMode`, `Primitive`.
- **zod-utils** — Zod schema helpers: enums from const objects, schema unwrapping, null handling, field extraction, `entityReferenceIdSchema`.

## OpenAPI-generated types

The `openapi-types` subpath re-exports types and runtime constants generated from the Kotlin REST APIs.

The OpenAPI files are produced by the following test classes:

- REST-Model: `RestModelOpenapiGeneratorTest`
- Auth: `AuthOpenapiGeneratorTest`
- Tags: `TagOpenapiGeneratorTest`
- Customer: `CustomerOpenapiGeneratorTest`

The files are merged into a single OpenAPI document with `@redocly/cli join` (see the `openapi:merge` script in `package.json`) and converted into TypeScript with Orval (`orval.config.ts`).

## How to add more utilities?

- If you want to add another file in one of the existing folder, just add the exports to the `index.ts` file inside the folder.
- When you add another folder, you also need to add an `index.ts` file in this folder that exports your new utilities. Those new utilities
  will be exported under the folder name from this package, i.e. you can import them like so: `import { ... } from "@neckar-hub/typescript-utils/<FOLDER_NAME>"
