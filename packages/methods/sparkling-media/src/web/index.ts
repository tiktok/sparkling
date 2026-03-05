// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import { registerWebMethod } from 'sparkling-method/web-registry';
import type { WebMethodHandler } from 'sparkling-method';

registerWebMethod('media.chooseMedia', (params, callback) => {
    const data = params.data as Record<string, unknown> | null;
    const mediaTypes = data?.mediaTypes as string[] | undefined;
    const sourceType = data?.sourceType as string | undefined;
    const maxCount = (data?.maxCount as number) ?? 1;

    // Build accept string from mediaTypes
    const acceptParts: string[] = [];
    if (mediaTypes?.includes('image')) acceptParts.push('image/*');
    if (mediaTypes?.includes('video')) acceptParts.push('video/*');
    const accept = acceptParts.length ? acceptParts.join(',') : 'image/*,video/*';

    const input = document.createElement('input');
    input.type = 'file';
    input.accept = accept;
    input.multiple = maxCount > 1;

    // Camera source: use capture attribute (mobile browsers only)
    if (sourceType === 'camera') {
        input.capture = (data?.cameraType as string) === 'front' ? 'user' : 'environment';
    }

    input.onchange = () => {
        const files = Array.from(input.files ?? []);
        if (!files.length) {
            callback({ code: 0, msg: 'No file selected' });
            return;
        }

        const results = files.slice(0, maxCount).map(file => ({
            tempFilePath: URL.createObjectURL(file),
            size: file.size,
            type: file.type,
            name: file.name,
        }));

        callback({ code: 1, msg: 'ok', data: results });
    };

    // Handle user cancelling the file picker
    input.addEventListener('cancel', () => {
        callback({ code: 0, msg: 'User cancelled' });
    });

    input.click();
});

registerWebMethod('media.downloadFile', (params, callback) => {
    const data = params.data as Record<string, unknown> | null;
    const url = data?.url as string | undefined;
    const extension = data?.extension as string | undefined;
    const headers = data?.header as Record<string, string> | undefined;

    if (!url) {
        callback({ code: 0, msg: 'url is required' });
        return;
    }

    fetch(url, { headers })
        .then(res => {
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            return res.blob();
        })
        .then(blob => {
            const objectUrl = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = objectUrl;
            a.download = `download.${extension || 'bin'}`;
            a.click();
            URL.revokeObjectURL(objectUrl);
            callback({ code: 1, msg: 'ok', data: { tempFilePath: objectUrl } });
        })
        .catch(e => {
            callback({ code: 0, msg: `Download failed: ${e}` });
        });
});

// Shared implementation for uploadFile and uploadImage (same API shape)
const uploadFileImpl: WebMethodHandler = (params, callback) => {
    const data = params.data as Record<string, unknown> | null;
    const url = data?.url as string | undefined;
    const filePath = data?.filePath as string | undefined;
    const headers = data?.header as Record<string, string> | undefined;
    const extraParams = data?.params as Record<string, unknown> | undefined;

    if (!url) {
        callback({ code: 0, msg: 'url is required' });
        return;
    }

    // filePath on web is a blob URL from chooseMedia
    const filePromise = filePath
        ? fetch(filePath).then(r => r.blob())
        : Promise.resolve(new Blob());

    filePromise
        .then(blob => {
            const formData = new FormData();
            formData.append('file', blob);
            if (extraParams && typeof extraParams === 'object') {
                for (const [k, v] of Object.entries(extraParams)) {
                    formData.append(k, String(v));
                }
            }
            return fetch(url, { method: 'POST', headers, body: formData });
        })
        .then(res => {
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            return res.json();
        })
        .then(json => callback({ code: 1, msg: 'ok', data: json }))
        .catch(e => callback({ code: 0, msg: `Upload failed: ${e}` }));
};

registerWebMethod('media.uploadFile', uploadFileImpl);
registerWebMethod('media.uploadImage', uploadFileImpl);

registerWebMethod('media.saveDataURL', (_params, callback) => {
    // No web API to save directly to device photo album
    callback({ code: 0, msg: 'media.saveDataURL is not supported on web' });
});
