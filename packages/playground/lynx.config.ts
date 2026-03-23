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
  source: {
    entry: {
      main: './src/index.tsx', // Main app entry
      second: './src/pages/second/index.tsx', // Second page entry
      'card-view': './src/pages/card-view/index.tsx', // Card view demo entry
      'media-test': './src/pages/media-test/index.tsx', // Media test page entry
      'card-view-demo': './src/pages/card-view-demo/index.tsx', // Card view demo with native container
      'debug-tool-switch': './src/pages/debug-tool-switch/index.tsx', // Lynx debug switches (native panel on device)
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
