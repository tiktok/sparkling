// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import pipe from 'sparkling-method';
import type { ChooseMediaRequest, ChooseMediaResponse, MediaType, SourceType, CameraType } from './chooseMedia.d';
import { validateParams, validationRules, isValidCallback, logInvalidCallback, mapPipeResponse } from '../utils/validation';

// Native SPKChooseMediaMethodParamModel expects int enum values, not strings.
const MEDIA_TYPE_MAP: Record<MediaType, number> = { image: 1, video: 2 };
const SOURCE_TYPE_MAP: Record<SourceType, number> = { album: 1, camera: 2 };
const CAMERA_TYPE_MAP: Record<CameraType, number> = { front: 1, back: 2 };

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
        mediaTypes: params.mediaTypes.map(t => MEDIA_TYPE_MAP[t] ?? 0),
        sourceType: SOURCE_TYPE_MAP[params.sourceType] ?? 1,
        maxCount: params.maxCount ?? 1,
        cameraType: params.cameraType ? CAMERA_TYPE_MAP[params.cameraType] ?? 2 : 0,
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
