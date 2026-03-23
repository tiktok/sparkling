import { useCallback, useEffect, useState } from '@lynx-js/react'

import './App.css'

import { SafeAreaView } from '../../components/SafeAreaView.js'
import { close } from 'sparkling-navigation';
import * as storage from 'sparkling-storage';

export function App(props: {
  onMounted?: () => void
}) {

  const [data, setData] = useState<string>('');

  useEffect(() => {
    console.info('Hello, ReactLynx')
    console.info('lynx.__globalProps', lynx.__globalProps)
    props.onMounted?.()
  }, [])

  const onClose = useCallback(() => {
    close();
  }, []);

  const getStorageItem = useCallback(() => {
    storage.getItem({
      key: 'key',
    }, (v: storage.GetItemResponse) => {
      setData(JSON.stringify(v.data));
      console.log('v', v);
    });
  }, []);

  const getGlobalPropsString = useCallback(() => {
    if (!lynx.__globalProps) {
      return ''
    }
    return Object.keys(lynx.__globalProps).map((key) => {
      const value = (lynx.__globalProps as Record<string, any>)[key];
      const displayValue = typeof value === 'object' && value !== null 
        ? JSON.stringify(value, null, 2) 
        : value
      return `${key}: ${displayValue}`
    }).join('\n')
  }, [])


  return (
    // Top inset is applied by native SPKNavigationBar; only pad bottom (e.g. home indicator).
    <SafeAreaView edges={['bottom']} style={{ flex: 1 }}>
      <view className='App'>
        <view className='Banner'>
          <text className='Title'>This is the second page</text>
        </view>
        <view className='Content'>
          <text className='Button' bindtap={onClose}>
            Close
          </text>
          <text className='Button' bindtap={getStorageItem}>
            Get StorageItem  {data}
          </text>
          <scroll-view scroll-orientation='vertical'>
            <text className='GlobalProps'>
              {
                'GlobalProps: \n \n' + getGlobalPropsString()
              }
            </text>
          </scroll-view>
        </view>
      </view>
    </SafeAreaView>
  )
}
