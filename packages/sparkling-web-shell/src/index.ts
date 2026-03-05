// Import Lynx web runtime and elements
import '@lynx-js/web-core';
import '@lynx-js/web-core/index.css';
import '@lynx-js/web-elements/all';
import '@lynx-js/web-elements/index.css';

/**
 * Determine which bundle to load from the ?page= query parameter.
 * Default: "main" -> /main.lynx.bundle
 */
function getBundleUrl(): string {
  const params = new URLSearchParams(window.location.search);
  const page = params.get('page') || 'main';
  return `/${page}.lynx.bundle`;
}

/**
 * Render the Lynx view into the page.
 */
function render(): void {
  const bundleUrl = getBundleUrl();
  const container = document.getElementById('root');
  if (!container) {
    console.error('[sparkling-web-shell] #root element not found');
    return;
  }

  container.innerHTML = '';

  const lynxView = document.createElement('lynx-view');
  lynxView.setAttribute('url', bundleUrl);
  lynxView.style.width = '100vw';
  lynxView.style.height = '100vh';

  container.appendChild(lynxView);
}

// Initial render
render();

// Re-render when browser navigation changes the URL
window.addEventListener('popstate', render);
