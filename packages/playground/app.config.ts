import { defineConfig } from '@lynx-js/rspeedy'
import { pluginQRCode } from '@lynx-js/qrcode-rsbuild-plugin'
import { pluginReactLynx } from '@lynx-js/react-rsbuild-plugin'

const lynxConfig = defineConfig({
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
      schema(url: string): string {
        return `${url}?fullscreen=true`
      },
    }),
    pluginReactLynx(),
  ],
})

const config = {
  lynxConfig,
  appName: 'SparklingGo',
  devtool: true,
  platform: {
    android: {
      packageName: 'com.tiktok.sparkling.playground',
    },
    ios: {
      bundleIdentifier: 'com.sparkling.playground',
    },
  },
  paths: {
    androidAssets: 'android/app/src/main/assets',
    iosAssets: 'ios/LynxResources',
  },
  router: {
    main: { path: './lynxPages/main' },
    showcase: { path: './lynxPages/showcase' },
    'scheme-builder': { path: './lynxPages/scheme-builder' },
    'scheme-presets': { path: './lynxPages/scheme-presets' },
    'nav-basic': { path: './lynxPages/nav-basic' },
    'nav-chain': { path: './lynxPages/nav-chain' },
    'gp-device': { path: './lynxPages/gp-device' },
    'gp-screen': { path: './lynxPages/gp-screen' },
    'gp-container': { path: './lynxPages/gp-container' },
    'storage-demo': { path: './lynxPages/storage-demo' },
    'media-choose': { path: './lynxPages/media-choose' },
    'media-upload': { path: './lynxPages/media-upload' },
    'media-download': { path: './lynxPages/media-download' },
  },
}

export default config
