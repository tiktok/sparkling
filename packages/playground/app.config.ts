import lynxConfig from './lynx.shared.config.js'

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
