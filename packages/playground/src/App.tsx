import { useCallback, useEffect, useState } from '@lynx-js/react';
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
  const [switchStates, setSwitchStates] = useState<Record<string, boolean>>({ hide_nav_bar: false, hide_status_bar: false, trans_status_bar: false, hide_loading: false, hide_error: false });
  const [isListExpanded, setIsListExpanded] = useState(false);
  const [apiResponse, setApiResponse] = useState<string>('TikTok Sparkling');

  useEffect(() => {
    console.info('Hello, ReactLynx')
    props.onMounted?.()
  }, [])

  const handleInput = (event: InputEvent) => {
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
      // For HTTP URLs (e.g. dev server), use router.open() with a hybrid scheme
      const encoded = encodeURIComponent(bundlePath);
      const params = buildQueryParams();
      const extra = Object.entries(params).map(([k, v]) => `&${k}=${v}`).join('');
      const scheme = `hybrid://lynxview?url=${encoded}${extra}`;
      router.open(
        { scheme },
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
    { id: 5, title: 'mediaTest', api: openMediaTest },
    { id: 6, title: 'chooseImage', api: chooseImage },
    { id: 7, title: 'chooseVideo', api: chooseVideo },
    { id: 8, title: 'takePhoto', api: takePhoto },
  ];

  return (
    <view>
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
              text-color='#000000'
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
    </view>
  )
}
