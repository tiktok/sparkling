// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import fs from 'fs'
import path from 'path'
import { defineConfig } from '@lynx-js/rspeedy'
import lynxSharedConfig from './lynx.shared.config.js'

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
  ...lynxSharedConfig,
  server: {
    port: 5969,
  },
  plugins: [
    ...(lynxSharedConfig.plugins ?? []),
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
