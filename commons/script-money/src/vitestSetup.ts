// The Vitest stand-in for the declarations TypeScriptCode.fromClasspath appends to every script.
import * as scriptMoney from "./scriptMoney";

Object.assign(globalThis, scriptMoney);
