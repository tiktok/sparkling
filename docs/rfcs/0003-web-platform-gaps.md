# RFC 0003 — Where the missing web APIs belong

Status: draft · Related: RFC 0001

## Summary

`Intl`, `TextEncoder`/`TextDecoder`, `WebSocket` and React's `Children` helpers
are all missing from a Lynx page, and every Sparkling app reimplements or works
around each of them. Three of the four do **not** belong in Sparkling. This RFC
says where each one goes and why, so the work is filed in the right place
instead of accumulating as four more Sparkling packages.

## The four

### `TextEncoder` / `TextDecoder` — belongs in Lynx, and is nearly there

Lynx already has the native half and a JS wrapper:

- `core/runtime/js/bindings/text_codec_helper.cc` implements the codec and
  exposes `globalThis.TextCodecHelper`.
- `js_libraries/lynx-core/src/modules/fetch/TextEncoder.ts` is a `TextEncoder`
  class built on it — but it lives inside the fetch module and is never
  installed as a global.

What is missing is the two lines that put `TextEncoder` and `TextDecoder` on
`globalThis`, plus `encodeInto`, which the existing class throws on.

**Action: a PR to `lynx-family/lynx`.** Sparkling should not ship a polyfill for
something the runtime already contains.

### `Children` — belongs in ReactLynx, and is three functions

`@lynx-js/react` does not re-export React's `Children` helpers, so every app
writes its own flatten when it needs to count children, interleave separators,
or stagger an entrance. The helpers are pure JavaScript over the element tree
and carry no renderer-specific behaviour beyond `isValidElement` and `Fragment`,
both of which ReactLynx already exports.

**Action: a PR to `lynx-family/lynx-stack`** adding `Children` to the
`@lynx-js/react` export surface. If it is declined, a ~30-line
`@sparklingjs/react-children` is an acceptable consolation, but it should be
asked for upstream first.

### `Intl` — nobody should ship all of it

Full ICU is tens of megabytes of data and is not a thing to put in a mobile
bundle. The realistic options, in the order they should be considered:

1. **Do nothing, document the gap.** Most apps need one or two formats -
   a duration, a relative time, a thousands separator - and hand-written
   formatting in the app's own locale files is smaller and faster than any
   polyfill.
2. **A recommended `@formatjs` preset.** `@formatjs/intl-numberformat` and
   `intl-datetimeformat` with per-locale data loaded on demand. Sparkling's
   contribution is a documented rspeedy configuration that splits the locale
   data out of the main bundle, not a package of its own.
3. **Native `Intl` through RFC 0001.** Both platforms have a complete ICU in the
   OS - `NSNumberFormatter`, `android.icu` - and a SparklingModule could expose
   formatting without shipping any data at all. This is the right long-term
   answer and it is blocked on the module system, so it is not the first move.

**Action: document (1), publish the preset for (2), revisit (3) after RFC 0001
stage 3.**

### `WebSocket` — belongs in Sparkling, on RFC 0001

This is the one that is genuinely Sparkling's. It cannot be polyfilled: there is
no socket primitive in the runtime to build on. It needs a native
implementation, and a module system that can carry binary frames and an event
stream — which is exactly what RFC 0001 stage 4 delivers.

Requirements, so it is not "a socket" but the API people expect: `readyState`
with the four constants, `binaryType` of `blob`|`arraybuffer` (with `blob`
rejected while there is no `Blob`), `open`/`message`/`error`/`close` events,
close codes and reasons, `bufferedAmount`, and sane behaviour when the app goes
to the background — the socket is closed and the page told, rather than silently
half-alive.

Implementation is a C++ client on the module system's binary path. The choice of
library, and what it costs in binary size on each platform, is the open
question.

**Action: a SparklingModule, after RFC 0001 stage 4. Not before.**

## Why this matters more than the individual APIs

The tempting move is to publish four Sparkling packages and call the gap closed.
That would leave Lynx with the same holes for everyone who is not using
Sparkling, and would leave Sparkling maintaining a polyfill for a codec its own
runtime already implements. Two of these are upstream bugs, one is a
documentation and configuration problem, and one is a real Sparkling feature.
Filing them that way is the whole point of this RFC.
