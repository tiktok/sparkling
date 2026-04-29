/**
 * Re-export sparkling-navigation.
 *
 * The latest sparkling-navigation source (linked from ~/github/sparkling)
 * already handles:
 * - Dev mode: uses __webpack_public_path__ to load from dev server (HMR)
 * - All custom params passed through (no ALLOWED_SCHEME_PARAMS filter)
 * - Production: uses bundle= for local assets
 *
 * This wrapper exists so all app imports go through a single path.
 * If we need app-level overrides in the future, add them here.
 */
export { navigate, open, close } from 'sparkling-navigation'
export type {
  NavigateRequest,
  NavigateResponse,
  OpenRequest,
  OpenResponse,
} from 'sparkling-navigation'
