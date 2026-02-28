import { defineConfig } from '@rspress/core';
import { pluginLlms } from '@rspress/plugin-llms';
import { createRequire } from 'node:module';
import path from 'node:path';

const require = createRequire(import.meta.url);
const rootPkg = require('../../package.json');
const SPARKLING_VERSION: string = rootPkg.version;

const githubSocialIcon = {
  svg: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" aria-hidden="true">
  <title>GitHub</title>
  <path fill="currentColor" d="M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12"/>
</svg>`,
};

// ---------------------------------------------------------------------------
// Navigation
// ---------------------------------------------------------------------------

const navEn = [
  { text: 'Guide', link: '/guide/get-started/create-new-app' },
  { text: 'APIs', link: '/apis/' },
];

const navZh = [
  { text: '指南', link: '/guide/get-started/create-new-app' },
  { text: 'APIs', link: '/apis/' },
];

// ---------------------------------------------------------------------------
// Sidebar – English
// ---------------------------------------------------------------------------

// Rspress 2.0 requires sidebar items to be { text, link } objects (not plain strings).
const sidebarEn = {
  '/guide/': [
    {
      text: 'Get Started',
      items: [
        { text: 'Create a New App', link: '/guide/get-started/create-new-app' },
        {
          text: 'Integrate into Existing App',
          link: '/guide/get-started/integrate-sparkling-into-existing-app',
        },
        {
          text: 'Create a Custom Method',
          link: '/guide/get-started/create-custom-method',
        },
      ],
    },
    { text: 'CLI', items: [{ text: 'CLI', link: '/guide/cli' }] },
  ],
  '/apis/': [
    { text: 'Overview', link: '/apis/' },
    { sectionHeaderText: 'Sparkling Lynx' },
    { text: 'GlobalProps', link: '/apis/global-props/Interface.GlobalProps' },
    { text: 'Scheme', link: '/apis/scheme' },
    { text: 'Navigation', link: '/apis/sparkling-methods/sparkling-navigation' },
    { text: 'Storage', link: '/apis/sparkling-methods/sparkling-storage' },
    { text: 'Media', link: '/apis/sparkling-methods/sparkling-media' },
    { dividerType: 'solid' },
    { sectionHeaderText: 'Sparkling SDK' },
    { text: 'Android', link: '/apis/sparkling-sdk-android' },
    { text: 'iOS', link: '/apis/sparkling-sdk-ios' },
    { dividerType: 'solid' },
    { sectionHeaderText: 'Sparkling Method SDK' },
    { text: 'Android', link: '/apis/sparkling-method-android' },
    { text: 'iOS', link: '/apis/sparkling-method-ios' },
    { text: 'TypeScript', link: '/apis/sparkling-method-ts' },
  ],
};

// ---------------------------------------------------------------------------
// Sidebar – Chinese
// ---------------------------------------------------------------------------

const sidebarZhBase = {
  '/guide/': [
    {
      text: '快速开始',
      items: [
        { text: '创建新应用', link: '/guide/get-started/create-new-app' },
        {
          text: '接入已有应用',
          link: '/guide/get-started/integrate-sparkling-into-existing-app',
        },
        { text: '创建自定义 Method', link: '/guide/get-started/create-custom-method' },
      ],
    },
    { text: 'CLI', items: [{ text: 'CLI', link: '/guide/cli' }] },
  ],
  '/apis/': [
    { text: '概览', link: '/apis/' },
    { sectionHeaderText: 'Sparkling Lynx' },
    { text: 'GlobalProps', link: '/apis/global-props/Interface.GlobalProps' },
    { text: 'Scheme', link: '/apis/scheme' },
    { text: 'Navigation', link: '/apis/sparkling-methods/sparkling-navigation' },
    { text: 'Storage', link: '/apis/sparkling-methods/sparkling-storage' },
    { text: 'Media', link: '/apis/sparkling-methods/sparkling-media' },
    { dividerType: 'solid' },
    { sectionHeaderText: 'Sparkling SDK' },
    { text: 'Android', link: '/apis/sparkling-sdk-android' },
    { text: 'iOS', link: '/apis/sparkling-sdk-ios' },
    { dividerType: 'solid' },
    { sectionHeaderText: 'Sparkling Method SDK' },
    { text: 'Android', link: '/apis/sparkling-method-android' },
    { text: 'iOS', link: '/apis/sparkling-method-ios' },
    { text: 'TypeScript', link: '/apis/sparkling-method-ts' },
  ],
};

// Rspress routes for non-default locales are prefixed (e.g. /zh/guide/...).
// Sidebar matching is based on path prefixes, so we register both variants.
const sidebarZh = {
  ...sidebarZhBase,
  '/zh/guide/': sidebarZhBase['/guide/'],
  '/zh/apis/': sidebarZhBase['/apis/'],
};

// ---------------------------------------------------------------------------
// Config
// ---------------------------------------------------------------------------

export default defineConfig({
  root: 'docs',
  base: '/sparkling/',
  lang: 'en',
  locales: [
    { lang: 'en', label: 'English' },
    { lang: 'zh', label: '中文' },
  ],
  markdown: {
    link: {
      checkDeadLinks: false,
    },
  },
  title: 'Sparkling',
  description:
    "Sparkling is TikTok\u2019s hybrid container spanning Android, iOS, and Lynx.",
  icon: '/sparkling/sparkling_logo_144.png',
  logo: {
    light: '/sparkling/sparkling_logo_144_light.png',
    dark: '/sparkling/sparkling_logo_144.png',
  },
  logoText: 'Sparkling',
  themeConfig: {
    darkMode: true,
    llmsUI: true,
    nav: navEn,
    sidebar: sidebarEn,
    search: true,
    socialLinks: [
      {
        icon: githubSocialIcon,
        mode: 'link',
        content: 'https://github.com/tiktok/Sparkling',
      },
    ],
    locales: [
      {
        lang: 'en',
        label: 'English',
        nav: navEn,
        sidebar: sidebarEn,
      },
      {
        lang: 'zh',
        label: '中文',
        nav: navZh,
        sidebar: sidebarZh,
      },
    ],
    footer: {
      message: '\u00A9 2026 TikTok',
    },
    localeRedirect: 'auto',
  },
  search: {
    mode: 'local',
    codeBlocks: true,
  },
  globalStyles: path.resolve('styles/index.css'),
  // Built-in plugins (container syntax, medium zoom, auto nav) are handled by Rspress core.
  plugins: [
    pluginLlms([
      {
        llmsTxt: { name: 'llms.txt' },
        llmsFullTxt: { name: 'llms-full.txt' },
        include: ({ page }) => page.lang === 'en',
      },
      {
        llmsTxt: { name: 'zh/llms.txt' },
        llmsFullTxt: { name: 'zh/llms-full.txt' },
        include: ({ page }) => page.lang === 'zh',
      },
    ]),
  ],
  builderConfig: {
    source: {
      define: {
        __SPARKLING_VERSION__: JSON.stringify(SPARKLING_VERSION),
      },
    },
    output: {
      minify: false,
    },
    server: {
      publicDir: {
        name: path.resolve(__dirname, 'public'),
      },
    },
    html: {
      favicon: path.resolve(__dirname, 'public/sparkling_logo_144.png'),
      tags: [
        {
          tag: 'script',
          // Seed localStorage BEFORE the Rspress inline theme-detection script
          // so that first-time visitors default to dark mode.
          // { head: true, append: false } places this before existing <head> tags.
          children:
            "if(!localStorage.getItem('rspress-theme-appearance')){localStorage.setItem('rspress-theme-appearance','dark');document.documentElement.classList.add('dark','rp-dark');document.documentElement.style.colorScheme='dark';}",
          head: true,
          append: false,
        },
      ],
    },
  },
});
