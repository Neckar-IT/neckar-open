# Typescript Unit Tests

Characterization tests for the TypeScript dependencies and language built-ins this monorepo relies on.
Nothing here tests our own code — every assertion pins down how something else actually behaves.

The point is the assertion, not the coverage. A test belongs here when the documentation left the
answer open and the real behaviour turned out to be surprising: `dayjs(null)` and `dayjs(undefined)`
producing entirely different dates, zod's `.partial()` stopping at the first level, two `Date` objects
for the same instant being unequal. Covered so far are dayjs, zod and `Date`.

**A failing test here is a changed dependency, not a regression in our code.** The library was upgraded
and its behaviour moved. Before adjusting the assertion to the new value, check what in the monorepo
relied on the old one — that call site is the actual thing that broke, and the test is what surfaced it.
