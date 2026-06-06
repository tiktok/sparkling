// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import pipe from 'sparkling-method';
import type { ChooseMediaRequest, ChooseMediaResponse } from './chooseMedia.d';
import { validateParams, validationRules, isValidCallback, logInvalidCallback, mapPipeResponse } from '../utils/validation';

/**
 * Choose media from album or camera
 * @param params - Choose media parameters
 * @param callback - Callback function with result
 */
export function chooseMedia(params: ChooseMediaRequest, callback: (result: ChooseMediaResponse) => void): void {
    const error = validateParams<ChooseMediaRequest, ChooseMediaResponse>(params, callback, [
        validationRules.paramsRequired(),
        validationRules.requiredArray('mediaTypes'),
        validationRules.enumValue('sourceType', ['album', 'camera']),
        validationRules.conditional(
            (p) => p.sourceType === 'camera',
            validationRules.custom(
                (p) => !!p.cameraType,
                'Invalid params: cameraType is required when sourceType is "camera"'
            )
        ),
    ]);
    if (error) return;

    if (!isValidCallback(callback)) {
        logInvalidCallback('chooseMedia');
        return;
    }

    const pipeParams = {
        mediaTypes: params.mediaTypes,
        sourceType: params.sourceType,
        maxCount: params.maxCount ?? 1,
        cameraType: params.cameraType ?? '',
        compressImage: params.compressImage ?? false,
        saveToPhotoAlbum: params.saveToPhotoAlbum ?? false,
        isNeedCut: params.isNeedCut ?? false,
        cropRatioWidth: params.cropRatioWidth ?? 0,
        cropRatioHeight: params.cropRatioHeight ?? 0,
        needBase64Data: params.needBase64Data ?? false,
        compressOption: params.compressOption ?? 0,
        compressWidth: params.compressWidth ?? 0,
        compressHeight: params.compressHeight ?? 0,
        permissionDenyAction: params.permissionDenyAction ?? 0,
        isMultiSelect: params.isMultiSelect ?? false,
        useNewCompressSolution: params.useNewCompressSolution ?? false,
        shouldKeepOriginalFormat: params.shouldKeepOriginalFormat,
        compressQuality: params.compressQuality ?? 100,
    };
    pipe.call('media.chooseMedia', pipeParams, (v: unknown) => {
        callback(mapPipeResponse<ChooseMediaResponse>(v));
    });
}
