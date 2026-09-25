# iOS JSB Migration Compatibility Notes

The behavior baseline is commit `c4ce8d25c5ea277e13752d68ff1f2a66f5704240` of the public `tiktok/sparkling` repository. It includes `packages/methods/*/ios` and the Playground implementations of `RouterServiceImpl` and `StorageServiceImpl`. The baseline predates the runtime replacement experiment.

## Migration scope

Preserve the public implementations' parameters, control flow, and callback behavior. Adapt only their registration, models, and invocation interfaces to SPKMethod. Options that did not work in the baseline are outside this migration; existing issues are listed separately. Global methods continue to use `@SPKGlobalMethod`.

## Behavior preserved

| Methods | Compatibility work |
| --- | --- |
| `router.open` / `router.close` | The existing invocation hook places a weak reference to the calling `LynxView` in the parameter model's context before invocation. Navigation continues to use the original `SPKRouter`. The three `replace` sequences and the close-before-callback order are preserved. The migration's window-based lookup of the current page was removed. When caller context is missing, no container is inferred. |
| `storage.setItem` / `getItem` / `removeItem` | Continue using the Playground's `com.SPK.custom.userdefault` suite and the original key directly. Restore direct writes and the original `AnyCodableValue` filtering of returned values. No new validation, TTL, or business partitioning is added. Existing checks in the JavaScript package remain. |
| `media.chooseMedia` | Restore `SPKDefaultMediaPicker` (`UIImagePickerController`), photo-library and camera permission flows, prompts, cancellation, image compression, file creation, and result fields. Remove the added `PHPicker` and multiple-selection implementation. Restore the original 20 JSON mappings. Keep `compressImage`, `needBase64Data`, and `saveToPhotoAlbum` as properties without adding mappings; `isMultiSelect` remains a JavaScript-side option only. |
| `media.saveDataURL` | Restore the original Base64 parsing, file naming and writing, photo-library authorization, and callback flow. Remove the file-name validation added during migration. |
| `media.downloadFile` / `uploadFile` / `uploadImage` | Remove the added `formDataBody` handling and the outer branch that automatically treats non-2xx HTTP responses as failures. Restore the original parameter mappings, method-specific response fields, extensionless file naming, and download-to-album flow. Only network transport uses a `URLSession` adapter. |

The container entry point again uses the main thread by default and the current thread when `threadType=CURRENT_THREAD` is specified. The shared runtime adds no business dispatch flow. Sparkling SDK adapts container context through `SPKMethodCallRouter.hooksProvider` / `willInvoke`; the internal TikTok project is not changed.

## Limits of equivalence

1. The former media sources referenced networking APIs that are unavailable in the public baseline. The current `URLSession` adapter cannot be compared with that missing implementation for request encoding, timeouts, error codes, or common-parameter injection. `needCommonParams` remains a parameter but does not fabricate common parameters. The former outer code did not branch on HTTP status; whether its network backend converted non-2xx responses into errors is unknown.
2. The former media sources used `spk_stringByStrippingSandboxPath` and `spk_stringFromProcessFile`, neither of which has an implementation in the public baseline. The current code continues to return readable absolute paths and retains the existing file-URL parsing for uploads. It does not assume an undocumented path format.
3. The former service lookup used the old framework's `DIProvider`. This experiment connects the Playground's existing navigation and storage implementations directly to SPKMethod. It does not restore the old service-replacement API.
4. Mantle now handles SPKMethod models. Generic parameter errors, null serialization, and status codes follow the new framework. Preserving business flows does not establish equivalence for every input and output at the framework boundary.

## Existing issue retained

In the public baseline, `SPKResponder.isTopViewController(viewController:)` ignores its argument and compares its own `topViewController`. An overlaid page may therefore still cause the top page of the navigation stack to close. This migration fixes the missing caller context but does not change `SPKRouter` or `SPKResponder`. Regression tests check context identity and compare the result with a direct call to the original `SPKRouter`; they do not report the old routing behavior as fixed.

## Verification

`SparklingGoTests/JSBParameterCompatibilityTests.swift` covers caller context when another page overlays it, equivalence with the old routing behavior, weak context references, missing context, the original picker type and unmapped fields, parameter defaults, compression, stored data, Base64 handling, response fields, and requests through the `URLSession` adapter.

Run the SparklingGo scheme with `-only-testing:SparklingGoTests/JSBParameterCompatibilityTests`. Network tests use a local loopback server only. Camera and permission prompts still require manual verification on a physical device.
