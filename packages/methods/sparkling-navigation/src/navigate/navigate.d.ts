// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import type { OpenOptions, OpenResponse } from '../open/open.d';

export interface NavigateRequest {
    path: string;
    options?: NavigateOptions;
    baseScheme?: string;
}

export type NavigateParamKey =
    | 'bundle'
    | 'title'
    | 'fallback_url'
    | 'title_color'
    | 'hide_nav_bar'
    | 'nav_bar_color'
    | 'screen_orientation'
    | 'hide_status_bar'
    | 'trans_status_bar'
    | 'show_nav_bar_in_trans_status_bar'
    | 'hide_loading'
    | 'loading_bg_color'
    | 'container_bg_color'
    | 'hide_error'
    | 'force_theme_style';

type ThemedColorKey =
    | 'title_color_light'
    | 'title_color_dark'
    | 'nav_bar_color_light'
    | 'nav_bar_color_dark'
    | 'loading_bg_color_light'
    | 'loading_bg_color_dark'
    | 'container_bg_color_light'
    | 'container_bg_color_dark';

export type NavigateParams = Partial<Record<NavigateParamKey | ThemedColorKey, string | number | boolean>>;

export type NavigateOptions = OpenOptions & {
    params?: NavigateParams;
};
export type NavigateResponse = OpenResponse;

declare function navigate(params: NavigateRequest, callback: (result: NavigateResponse) => void): void;
