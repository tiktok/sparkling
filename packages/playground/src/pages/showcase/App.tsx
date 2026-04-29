import { useState } from '@lynx-js/react'
import { navigate } from '../../lib/navigation.js'
import { ThemeProvider, useTheme } from '../../lib/theme.js'
import { SafeAreaView } from '../../components/SafeAreaView.js'

import './App.css'

interface DemoItem {
  title: string
  description: string
  bundle: string
  icon: string
}

interface Category {
  name: string
  icon: string
  color: string
  items: DemoItem[]
}

const CATEGORIES: Category[] = [
  {
    name: 'Scheme',
    icon: '\u{1F517}',
    color: '#25f4ee',
    items: [
      {
        title: 'Interactive Builder',
        description: 'Build scheme URLs with live preview',
        bundle: 'scheme-builder.lynx.bundle',
        icon: '\u{1F527}',
      },
      {
        title: 'Container Style Presets',
        description: 'Pre-configured scheme examples',
        bundle: 'scheme-presets.lynx.bundle',
        icon: '\u{1F3A8}',
      },
    ],
  },
  {
    name: 'Navigation',
    icon: '\u{1F9ED}',
    color: '#fe2c3a',
    items: [
      {
        title: 'navigate() Basics',
        description: 'Open pages with params and custom data',
        bundle: 'nav-basic.lynx.bundle',
        icon: '\u{27A1}',
      },
      {
        title: 'Chain Navigation',
        description: 'Multi-level page stack A \u2192 B \u2192 C',
        bundle: 'nav-chain.lynx.bundle',
        icon: '\u{26D3}',
      },
    ],
  },
  {
    name: 'GlobalProps',
    icon: '\u{1F4F1}',
    color: '#009995',
    items: [
      {
        title: 'Device & OS Info',
        description: 'OS, device, locale, SDK version, accessibility',
        bundle: 'gp-device.lynx.bundle',
        icon: '\u{1F4BB}',
      },
      {
        title: 'Screen & Safe Area',
        description: 'Screen dimensions, safe area, status bar',
        bundle: 'gp-screen.lynx.bundle',
        icon: '\u{1F4D0}',
      },
    ],
  },
  {
    name: 'Storage',
    icon: '\u{1F4BE}',
    color: '#ff9500',
    items: [
      {
        title: 'setItem / getItem',
        description: 'Persistent key-value storage with TTL',
        bundle: 'storage-demo.lynx.bundle',
        icon: '\u{1F5C4}',
      },
    ],
  },
  {
    name: 'Media',
    icon: '\u{1F3AC}',
    color: '#af52de',
    items: [
      {
        title: 'chooseMedia',
        description: 'Pick images and videos from album or camera',
        bundle: 'media-choose.lynx.bundle',
        icon: '\u{1F4F7}',
      },
      {
        title: 'Upload',
        description: 'Upload files and images to server',
        bundle: 'media-upload.lynx.bundle',
        icon: '\u{2B06}',
      },
      {
        title: 'Download & Save',
        description: 'Download files and save data URLs',
        bundle: 'media-download.lynx.bundle',
        icon: '\u{2B07}',
      },
    ],
  },
]

function ShowcaseContent() {
  const { resolved } = useTheme()
  const isDark = resolved === 'dark'
  const [searchQuery, setSearchQuery] = useState('')
  const queryItems = (lynx.__globalProps as any)?.queryItems || {}
  const scrollTarget = queryItems.category ? `category-${queryItems.category}` : ''

  const dk = (base: string) => `${base} ${isDark ? `${base}--dark` : `${base}--light`}`

  const handleSearch = (event: { detail: { value: string } }) => {
    'background only'
    setSearchQuery(event.detail.value.trim().toLowerCase())
  }

  const handleItemTap = (bundle: string, title: string) => {
    'background only'
    navigate({
      path: bundle,
      options: {
        params: {
          title,
          hide_nav_bar: 0,
          container_bg_color: '#000000',
          nav_bar_color: '#000000',
          title_color: '#FFFFFF',
          loading_bg_color: '#000000',
          force_theme_style: 'dark',
        },
      },
    }, () => {})
  }

  const filteredCategories = searchQuery.length > 0
    ? CATEGORIES.map((cat) => ({
        ...cat,
        items: cat.items.filter(
          (item) =>
            item.title.toLowerCase().includes(searchQuery) ||
            item.description.toLowerCase().includes(searchQuery) ||
            cat.name.toLowerCase().includes(searchQuery)
        ),
      })).filter((cat) => cat.items.length > 0)
    : CATEGORIES

  const totalResults = filteredCategories.reduce((sum, cat) => sum + cat.items.length, 0)

  return (
    <view className={dk('showcase-root')}>
      {/* Search Bar */}
      <view className="showcase-search-wrap">
        <view className={dk('showcase-search-bar')}>
          <text className={dk('showcase-search-icon')}>{'\u{1F50D}'}</text>
          <input
            className={`showcase-search-input ${isDark ? 'showcase-search-input--dark' : 'showcase-search-input--light'}`}
            bindinput={handleSearch}
            placeholder="Search demos..."
            text-color={isDark ? '#ffffff' : '#000000'}
          />
        </view>
        {searchQuery.length > 0 ? (
          <text className={dk('showcase-search-count')}>
            {totalResults} {totalResults === 1 ? 'result' : 'results'}
          </text>
        ) : null}
      </view>

      {/* Content */}
      <scroll-view className="showcase-scroll" scroll-orientation="vertical" scroll-into-view={scrollTarget}>
        <view className="showcase-list">
          {filteredCategories.length === 0 ? (
            <view className="showcase-empty">
              <text className="showcase-empty-icon">{'\u{1F50E}'}</text>
              <text className={dk('showcase-empty-text')}>
                No demos matching "{searchQuery}"
              </text>
            </view>
          ) : null}
          {filteredCategories.map((category) => (
            <view key={category.name} id={`category-${category.name}`} className="category-section">
              {/* Category Header */}
              <view className="category-header">
                <view className="category-icon-circle" style={{ backgroundColor: `${category.color}18` }}>
                  <text className="category-icon">{category.icon}</text>
                </view>
                <text
                  className={dk('category-name')}
                  style={{ color: category.color }}
                >
                  {category.name}
                </text>
                <view className="category-count-badge" style={{ backgroundColor: `${category.color}20` }}>
                  <text className="category-count-text" style={{ color: category.color }}>
                    {category.items.length}
                  </text>
                </view>
              </view>

              {/* Demo Items */}
              <view className={dk('category-card')}>
                {category.items.map((item, index) => (
                  <view key={item.title}>
                    <view
                      className="menu-item"
                      bindtap={() => handleItemTap(item.bundle, item.title)}
                    >
                      <view
                        className="menu-item-icon"
                        style={{ backgroundColor: `${category.color}15` }}
                      >
                        <text className="menu-item-icon-text">{item.icon}</text>
                      </view>
                      <view className="menu-item-content">
                        <text className={dk('menu-item-title')}>
                          {item.title}
                        </text>
                        <text className={dk('menu-item-desc')}>
                          {item.description}
                        </text>
                      </view>
                      <text className={dk('menu-item-arrow')}>
                        {'\u203A'}
                      </text>
                    </view>
                    {index < category.items.length - 1 ? (
                      <view className={dk('menu-item-divider')} />
                    ) : null}
                  </view>
                ))}
              </view>
            </view>
          ))}

          {/* Bottom spacer */}
          <view className="showcase-bottom-spacer" />
        </view>
      </scroll-view>
    </view>
  )
}

export function App() {
  return (
    <ThemeProvider>
      <SafeAreaView edges={['bottom']} style={{ flex: 1 }}>
        <ShowcaseContent />
      </SafeAreaView>
    </ThemeProvider>
  )
}
