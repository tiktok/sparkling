// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import { defineConfig } from '@lynx-js/rspeedy'
import { pluginQRCode } from '@lynx-js/qrcode-rsbuild-plugin'
import { pluginReactLynx } from '@lynx-js/react-rsbuild-plugin'

function toSparklingScheme(url: string) {
  const devUrl = new URL(url)
  devUrl.searchParams.set('fullscreen', 'true')
  return `hybrid://lynxview_page?url=${encodeURIComponent(devUrl.toString())}`
}

export default defineConfig({
  source: {
    entry: {
      main: './src/pages/main/index.tsx',
      showcase: './src/pages/showcase/index.tsx',
      'scheme-builder': './src/pages/scheme-builder/index.tsx',
      'scheme-presets': './src/pages/scheme-presets/index.tsx',
      'nav-basic': './src/pages/nav-basic/index.tsx',
      'nav-chain': './src/pages/nav-chain/index.tsx',
      'gp-device': './src/pages/gp-device/index.tsx',
      'gp-screen': './src/pages/gp-screen/index.tsx',
      'gp-container': './src/pages/gp-container/index.tsx',
      'storage-demo': './src/pages/storage-demo/index.tsx',
      'media-choose': './src/pages/media-choose/index.tsx',
      'media-upload': './src/pages/media-upload/index.tsx',
      'media-download': './src/pages/media-download/index.tsx',
    },
  },
  output: {
    assetPrefix: 'asset:///',
    filename: {
      bundle: '[name].lynx.bundle',
    },
  },
  plugins: [
    pluginQRCode({
      schema(url) {
        return toSparklingScheme(url)
      },
    }),
    pluginReactLynx(),
  ],
})
