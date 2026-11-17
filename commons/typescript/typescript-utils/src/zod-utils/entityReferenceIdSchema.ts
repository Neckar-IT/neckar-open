import { z } from "zod";

/**
 * An entity id reference; `z.guid()` because seed ids such as `c2c2c2c2-0002-0002-0002-000000000003` carry zeroed
 * version nibbles that `z.uuid()` rejects. Optional at the call site via `.nullable()`.
 */
export const entityReferenceIdSchema = z.guid();
