// @ts-nocheck
import { useEffect } from 'react';
import { useLang, withBase, NoSSR } from '@rspress/core/runtime';
import {
  HomeLayout as BasicHomeLayout,
  PackageManagerTabs,
  SvgWrapper as OriginalSvgWrapper,
} from '@rspress/core/theme-original';
import VideoBackground from './VideoBackground';

declare const __SPARKLING_VERSION__: string;

function HomeLayout() {
  const lang = useLang();
  const isZh = lang === 'zh';

  // Inject the dynamic version into the hero badge
  useEffect(() => {
    const brand = document.querySelector('.rp-home-hero__title-brand');
    if (brand) {
      (brand as HTMLElement).innerHTML = `
        <a
          href="https://github.com/tiktok/sparkling/releases"
          target="_blank"
          rel="noreferrer"
        >
          Public Beta · v${__SPARKLING_VERSION__}
        </a>
      `;
    }
  }, []);

  return (
    <BasicHomeLayout
      beforeHero={<VideoBackground />}
      afterHeroActions={
        <div className="spk-home-install">
          <NoSSR>
            <PackageManagerTabs command={{
              npm: "npm create sparkling-app@latest my-app",
              yarn: "yarn create sparkling-app@latest my-app",
              pnpm: "pnpm create sparkling-app@latest my-app",
              bun: "bun create sparkling-app@latest my-app",
            }} />
          </NoSSR>
        </div>
      }
    />
  );
}

/**
 * Override SvgWrapper to apply the base path to icon URLs.
 * Rspress's built-in SvgWrapper renders <img src={icon}> for file paths
 * but doesn't prepend the configured `base`, causing 404s.
 */
function SvgWrapper({ icon, ...rest }) {
  if (typeof icon === 'string' && icon.startsWith('/') && !icon.trim().startsWith('<')) {
    return <OriginalSvgWrapper icon={withBase(icon)} {...rest} />;
  }
  return <OriginalSvgWrapper icon={icon} {...rest} />;
}

export * from '@rspress/core/theme-original';
export { HomeLayout, SvgWrapper };
