import { useCallback, useEffect, useState } from '@lynx-js/react'
import { SafeAreaView } from './components/SafeAreaView.js'
import SwitchButton from './components/SwitchButton.js';

import * as media from 'sparkling-media';
import type { NavigateResponse, OpenResponse } from 'sparkling-navigation';
import * as router from 'sparkling-navigation';
import * as storage from 'sparkling-storage';
import './App.css';
import sparklingLogo from './assets/sparkling_icon.png';
import type { InputEvent } from './typing.js';

export function App(props: {
  onMounted?: () => void
}) {

  const [bundlePath, setBundlePath] = useState('second.lynx.bundle');
  const [switchStates, setSwitchStates] = useState<Record<string, boolean>>({ hide_nav_bar: false, hide_status_bar: false, trans_status_bar: false, show_nav_bar_in_trans_status_bar: false, hide_loading: false, hide_error: false });
  const [isListExpanded, setIsListExpanded] = useState(false);
  const [apiResponse, setApiResponse] = useState<string>('TikTok Sparkling');

  useEffect(() => {
    console.info('Hello, ReactLynx')
    props.onMounted?.()
  }, [])

  const handleInput = (event: any) => {
    'background only';
    const currentValue = event.detail.value.trim();
    setBundlePath(currentValue);
  };

  const handleSwitchChange = useCallback((key: string, checked: boolean) => {
    setSwitchStates(prev => ({ ...prev, [key]: checked }));
  }, []);

  const buildQueryParams = useCallback(() => {
    const params: Record<string, string> = {};
    Object.entries(switchStates).forEach(([key, value]) => {
      if (value) {
        params[key] = '1';
      }
    });
    return params;
  }, [switchStates]);

  const isHTTPURL = (path: string) => /^https?:\/\//i.test(path);

  const routerOpen = () => {
    if (isHTTPURL(bundlePath)) {
      // For HTTP URLs (e.g. dev server), pass a hybrid scheme and forward route params via `extra`.
      const encoded = encodeURIComponent(bundlePath);
      const params = buildQueryParams();
      const scheme = `hybrid://lynxview?url=${encoded}`;
      router.open(
        {
          scheme,
          options: {
            extra: params,
          },
        },
        (v: OpenResponse) => {
          console.log('v', v);
          setApiResponse(`Router Open: ${JSON.stringify(v)}`);
        }
      );
    } else {
      router.navigate(
        {
          path: bundlePath,
          options: {
            params: buildQueryParams(),
          },
        },
        (v: NavigateResponse) => {
          console.log('v', v);
          setApiResponse(`Router Navigate: ${JSON.stringify(v)}`);
        }
      );
    }
  };

  const openSchemeQueryItemsDemo = () => {
    const scheme = 'hybrid://lynxview?bundle=second.lynx.bundle&title=Scheme%20Parse%20Demo&from_scheme=1&shared_key=scheme';
    router.open(
      {
        scheme,
        options: {
          extra: {
            from_extra: '1',
            shared_key: 'extra',
            query_items_demo: '1',
          },
        },
      },
      (v: OpenResponse) => {
        console.log('Scheme queryItems demo opened:', v);
        setApiResponse(`Scheme QueryItems Demo: ${JSON.stringify(v)}`);
      },
    );
  };

  const setStorageItem = () => {
    storage.setItem({
      key: 'key',
      data: {
        name: 'Vagrant',
        producer: 'Feint',
        time: 2015
      },
    }, (v: storage.SetItemResponse) => {
      console.log('v', v);
      setApiResponse(`Set Storage: ${JSON.stringify(v)}`);
    });
  };

  const getStorageItem = () => {
    storage.getItem({
      key: 'key'
    }, (v: storage.GetItemResponse) => {
      console.log('v', v);
      setApiResponse(`Get Storage: ${JSON.stringify(v)}`);
    });
  };

  const openCardView = () => {
    router.navigate(
      {
        path: 'card-view.lynx.bundle',
        options: { params: { title: 'Card View Demo', screen_orientation: 'portrait' } },
      },
      (v: router.NavigateResponse) => {
        console.log('Card view opened:', v);
        setApiResponse(`Card View Opened: ${JSON.stringify(v)}`);
      }
    );
  };

  const openCardViewDemo = () => {
    router.open(
      { scheme: 'hybrid://native_route=card_demo' },
      (v: OpenResponse) => {
        console.log('Card view demo opened:', v);
        setApiResponse(`Card View Demo Opened: ${JSON.stringify(v)}`);
      },
    );
  };

  const openDebugToolSwitch = () => {
    router.open(
      { scheme: 'hybrid://native_route=debug_tool_switch' },
      (v: OpenResponse) => {
        console.log('Debug tool switch opened:', v);
        setApiResponse(`Debug Tool Switch: ${JSON.stringify(v)}`);
      },
    );
  };

  const openMediaTest = () => {
    router.navigate(
      {
        path: 'media-test.lynx.bundle',
        options: { params: { title: 'Media Test' } },
      },
      (v: router.NavigateResponse) => {
        console.log('Media test opened:', v);
        setApiResponse(`Media Test Opened: ${JSON.stringify(v)}`);
      }
    );
  };

  const openThemedColorDemo = () => {
    router.navigate(
      {
        path: 'second.lynx.bundle',
        options: {
          params: {
            title: 'Themed Color Demo',
            nav_bar_color: '#888888',
            nav_bar_color_light: '#FFFFFF',
            nav_bar_color_dark: '#1A1A2E',
            title_color: '#333333',
            title_color_light: '#000000',
            title_color_dark: '#E0E0E0',
            container_bg_color: '#F5F5F5',
            container_bg_color_light: '#FFFFFF',
            container_bg_color_dark: '#16213E',
          },
        },
      },
      (v: NavigateResponse) => {
        setApiResponse(`Themed Color: ${JSON.stringify(v)}`);
      }
    );
  };

  const openTransStatusBarNoNav = () => {
    router.navigate(
      {
        path: 'second.lynx.bundle',
        options: {
          params: {
            title: 'Trans Status Bar (No Nav)',
            trans_status_bar: '1',
            container_bg_color: '#0F3460',
          },
        },
      },
      (v: NavigateResponse) => {
        setApiResponse(`Trans StatusBar (no nav): ${JSON.stringify(v)}`);
      }
    );
  };

  const openTransStatusBarWithNav = () => {
    router.navigate(
      {
        path: 'second.lynx.bundle',
        options: {
          params: {
            title: 'Trans Status Bar (With Nav)',
            trans_status_bar: '1',
            show_nav_bar_in_trans_status_bar: '1',
            nav_bar_color: '#80000000',
            title_color: '#FFFFFF',
            container_bg_color: '#0F3460',
          },
        },
      },
      (v: NavigateResponse) => {
        setApiResponse(`Trans StatusBar (with nav): ${JSON.stringify(v)}`);
      }
    );
  };

  const chooseImage = () => {
    media.chooseMedia({
      mediaTypes: ['image'],
      sourceType: 'album',
      maxCount: 1,
    }, (v: media.ChooseMediaResponse) => {
      setApiResponse(`Choose Image: ${JSON.stringify(v)}`);
    });
  };

  const chooseVideo = () => {
    media.chooseMedia({
      mediaTypes: ['video'],
      sourceType: 'album',
      maxCount: 1,
    }, (v: media.ChooseMediaResponse) => {
      setApiResponse(`Choose Video: ${JSON.stringify(v)}`);
    });
  };

  const takePhoto = () => {
    media.chooseMedia({
      mediaTypes: ['image'],
      sourceType: 'camera',
      cameraType: 'back',
      maxCount: 1,
    }, (v: media.ChooseMediaResponse) => {
      setApiResponse(`Take Photo: ${JSON.stringify(v)}`);
    });
  };

  const listItems = [
    { id: 1, title: 'open', api: routerOpen},
    { id: 2, title: 'setStorage', api: setStorageItem},
    { id: 3, title: 'getStorage', api: getStorageItem },
    { id: 4, title: 'cardView', api: openCardView },
    { id: 5, title: 'cardViewDemo', api: openCardViewDemo },
    { id: 12, title: 'debugToolSwitch', api: openDebugToolSwitch },
    { id: 6, title: 'mediaTest', api: openMediaTest },
    { id: 7, title: 'themedColor', api: openThemedColorDemo },
    { id: 8, title: 'transStatusBar (no nav)', api: openTransStatusBarNoNav },
    { id: 14, title: 'transStatusBar (with nav)', api: openTransStatusBarWithNav },
    { id: 9, title: 'chooseImage', api: chooseImage },
    { id: 10, title: 'chooseVideo', api: chooseVideo },
    { id: 11, title: 'takePhoto', api: takePhoto },
    { id: 13, title: 'schemeQueryItemsDemo', api: openSchemeQueryItemsDemo },
  ];

  return (
    <SafeAreaView edges={['top', 'bottom']} style={{ flex: 1 }}>
      <view className='App'>
        <view className='Banner'>
          <view className='Logo' >
            <image src={sparklingLogo} className='Logo--lynx' />
          </view>
          <text className='Title'>TikTok Sparkling</text>
        </view>
        <view className='Content'>
          <view className='custom-list-container'>
            <scroll-view
              scroll-orientation='vertical'
              style={{ width: '100%', height: '240px' }}
            >
              {listItems.map((item) => (
                <view
                  key={item.id}
                  style={{ padding: '10px' }}
                >
                  <view
                    className='custom-button'
                    bindtap={() => item.api()}
                  >
                    <text style={{ color: '#ffffff' }}>{item.title}</text>
                  </view>
                </view>
              ))}
            </scroll-view>
          </view>
          <view className='input-card-url'>
            <text className='bold-text'>Bundle Path / URL</text>
            <input
              className="input-box"
              bindinput={handleInput}
              placeholder="second.lynx.bundle or http://ip:3000/main.lynx.bundle"
              value={bundlePath}
              text-color='#ffffff'
            />
          </view>
          <view className='expandable-list'>
            <view className='list-header' bindtap={() => setIsListExpanded(!isListExpanded)}>
              <text>Route Params {isListExpanded ? '▲' : '▼'}</text>
            </view>
            {isListExpanded && (
              <list
                style={{ width: '100%', height: '200px' }}
                list-type='single'
                span-count={1}
                scroll-orientation='vertical'
              >
                {Object.entries(switchStates).map(([key, value]) => (
                  <list-item
                    key={key}
                    item-key={key}
                    style={{ width: '200px', flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: '10px', alignSelf: 'flex-start', display: 'flex' }}
                  >
                    <text>{key}</text>
                    <SwitchButton
                      checked={value}
                      onChange={(e) => handleSwitchChange(key, e)}
                    />
                  </list-item>
                ))}
              </list>
            )}
          </view>
        </view>
      </view>
    </SafeAreaView>
  )
}
