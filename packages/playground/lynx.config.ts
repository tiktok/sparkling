// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import fs from 'fs'
import path from 'path'
import { defineConfig } from '@lynx-js/rspeedy'
import { pluginQRCode } from '@lynx-js/qrcode-rsbuild-plugin'
import { pluginReactLynx } from '@lynx-js/react-rsbuild-plugin'

function copyDir(src: string, dest: string) {
  if (!fs.existsSync(src)) {
    console.warn(`Source directory ${src} does not exist, skipping copy`)
    return
  }

  fs.mkdirSync(dest, { recursive: true })
  const entries = fs.readdirSync(src, { withFileTypes: true })

  for (const entry of entries) {
    const srcPath = path.join(src, entry.name)
    const destPath = path.join(dest, entry.name)

    if (entry.isDirectory()) {
      copyDir(srcPath, destPath)
    } else {
      fs.copyFileSync(srcPath, destPath)
    }
  }
}

export default defineConfig({
  server: {
    port: 5969,
  },
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
        // We use `?fullscreen=true` to open the page in LynxExplorer in full screen mode
        return `${url}?fullscreen=true`
      },
    }),
    pluginReactLynx(),
    {
      name: 'copy-assets-plugin',
      setup(api) {
        api.onAfterBuild(() => {
          const sourceDir = 'dist'
          const androidDest = 'android/app/src/main/assets'
          const iosDest = 'ios/LynxResources'

          console.log(`Copying ${sourceDir} to Android (${androidDest})...`)
          copyDir(sourceDir, androidDest)

          console.log(`Copying ${sourceDir} to iOS (${iosDest})...`)
          copyDir(sourceDir, iosDest)

          console.log('Assets copied successfully!')
        })
      },
    },
  ],
})
