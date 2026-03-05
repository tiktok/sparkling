// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import fs from 'fs'
import path from 'path'
import { defineConfig } from '@lynx-js/rspeedy'
import { pluginQRCode } from '@lynx-js/qrcode-rsbuild-plugin'
import { pluginReactLynx } from '@lynx-js/react-rsbuild-plugin'

function copyDir(src: string, dest: string, filter?: (name: string) => boolean) {
  if (!fs.existsSync(src)) {
    console.warn(`Source directory ${src} does not exist, skipping copy`)
    return
  }

  fs.mkdirSync(dest, { recursive: true })
  const entries = fs.readdirSync(src, { withFileTypes: true })

  for (const entry of entries) {
    if (filter && !filter(entry.name)) {
      continue
    }

    const srcPath = path.join(src, entry.name)
    const destPath = path.join(dest, entry.name)

    if (entry.isDirectory()) {
      copyDir(srcPath, destPath, filter)
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
    },
  },
  output: {
    filename: {
      bundle: '[name].lynx.bundle',
    },
  },
  environments: {
    web: {
      output: {
        assetPrefix: '/',
        distPath: {
          root: 'dist/web',
        },
      },
    },
    lynx: {
      output: {
        assetPrefix: 'asset:///',
      },
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

          // Skip the web subdirectory when copying to native asset dirs
          const nativeFilter = (name: string) => name !== 'web'

          console.log(`Copying ${sourceDir} to Android (${androidDest})...`)
          copyDir(sourceDir, androidDest, nativeFilter)

          console.log(`Copying ${sourceDir} to iOS (${iosDest})...`)
          copyDir(sourceDir, iosDest, nativeFilter)

          console.log('Assets copied successfully!')
        })
      },
    },
  ],
})
