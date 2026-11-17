// Code evaluated from a string; `script-src 'self'` must turn it into an EvalError.
// oxlint-disable-next-line typescript/no-implied-eval
new Function("return 1")();
