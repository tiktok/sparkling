// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import zlib from 'node:zlib';

/**
 * Enough PNG to resize an app icon.
 *
 * Deliberately not a dependency. `sharp` is a native binary an install has to
 * build or download, and a build tool that a developer runs once a day should
 * not carry one to scale a handful of images; `jimp` is a megabyte of JavaScript
 * for the same reason. Node already ships zlib, which is the only hard part of
 * a PNG, and box-filtered downscaling of an icon is a page of arithmetic.
 *
 * Supports 8-bit greyscale, RGB, palette, greyscale+alpha and RGBA, which is
 * every PNG an icon is ever exported as. Anything else - 16-bit samples, an
 * interlaced image - is refused by name rather than decoded wrongly.
 */

export interface Bitmap {
  width: number;
  height: number;
  /** RGBA, 4 bytes per pixel, row-major. */
  data: Buffer;
}

const SIGNATURE = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);

interface Chunk {
  type: string;
  data: Buffer;
}

function readChunks(file: Buffer): Chunk[] {
  const chunks: Chunk[] = [];
  let offset = SIGNATURE.length;
  while (offset + 8 <= file.length) {
    const length = file.readUInt32BE(offset);
    const type = file.toString('ascii', offset + 4, offset + 8);
    const start = offset + 8;
    chunks.push({ type, data: file.subarray(start, start + length) });
    offset = start + length + 4; // skip the CRC
    if (type === 'IEND') {
      break;
    }
  }
  return chunks;
}

function paethPredictor(a: number, b: number, c: number): number {
  const p = a + b - c;
  const pa = Math.abs(p - a);
  const pb = Math.abs(p - b);
  const pc = Math.abs(p - c);
  if (pa <= pb && pa <= pc) {
    return a;
  }
  return pb <= pc ? b : c;
}

/** Undo the per-scanline filters, in place, and return the raw samples. */
function unfilter(raw: Buffer, width: number, height: number, bytesPerPixel: number): Buffer {
  const stride = width * bytesPerPixel;
  const out = Buffer.alloc(stride * height);
  let position = 0;

  for (let y = 0; y < height; y += 1) {
    const filter = raw[position];
    position += 1;
    const line = raw.subarray(position, position + stride);
    position += stride;
    const target = out.subarray(y * stride, (y + 1) * stride);
    const previous = y > 0 ? out.subarray((y - 1) * stride, y * stride) : null;

    for (let x = 0; x < stride; x += 1) {
      const left = x >= bytesPerPixel ? target[x - bytesPerPixel] : 0;
      const up = previous ? previous[x] : 0;
      const upLeft = previous && x >= bytesPerPixel ? previous[x - bytesPerPixel] : 0;
      const value = line[x];

      switch (filter) {
        case 0: target[x] = value; break;
        case 1: target[x] = (value + left) & 0xff; break;
        case 2: target[x] = (value + up) & 0xff; break;
        case 3: target[x] = (value + ((left + up) >> 1)) & 0xff; break;
        case 4: target[x] = (value + paethPredictor(left, up, upLeft)) & 0xff; break;
        default: throw new Error(`Unsupported PNG scanline filter ${filter}`);
      }
    }
  }
  return out;
}

export function decodePng(file: Buffer): Bitmap {
  if (!file.subarray(0, SIGNATURE.length).equals(SIGNATURE)) {
    throw new Error('Not a PNG (bad signature)');
  }
  const chunks = readChunks(file);
  const header = chunks.find((chunk) => chunk.type === 'IHDR');
  if (!header) {
    throw new Error('PNG has no IHDR chunk');
  }

  const width = header.data.readUInt32BE(0);
  const height = header.data.readUInt32BE(4);
  const bitDepth = header.data.readUInt8(8);
  const colorType = header.data.readUInt8(9);
  const interlace = header.data.readUInt8(12);

  if (bitDepth !== 8) {
    throw new Error(`Unsupported PNG bit depth ${bitDepth}; 8 bits per sample is required`);
  }
  if (interlace !== 0) {
    throw new Error('Unsupported interlaced PNG; save it without Adam7 interlacing');
  }

  const samplesPerPixel = { 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 }[colorType as 0 | 2 | 3 | 4 | 6];
  if (!samplesPerPixel) {
    throw new Error(`Unsupported PNG colour type ${colorType}`);
  }

  const idat = Buffer.concat(chunks.filter((chunk) => chunk.type === 'IDAT').map((chunk) => chunk.data));
  const samples = unfilter(zlib.inflateSync(idat), width, height, samplesPerPixel);

  const palette = chunks.find((chunk) => chunk.type === 'PLTE')?.data;
  const transparency = chunks.find((chunk) => chunk.type === 'tRNS')?.data;

  const data = Buffer.alloc(width * height * 4);
  for (let i = 0; i < width * height; i += 1) {
    const source = i * samplesPerPixel;
    const target = i * 4;
    switch (colorType) {
      case 0:
        data[target] = data[target + 1] = data[target + 2] = samples[source];
        data[target + 3] = 255;
        break;
      case 2:
        data[target] = samples[source];
        data[target + 1] = samples[source + 1];
        data[target + 2] = samples[source + 2];
        data[target + 3] = 255;
        break;
      case 3: {
        if (!palette) {
          throw new Error('Palette PNG has no PLTE chunk');
        }
        const index = samples[source];
        data[target] = palette[index * 3];
        data[target + 1] = palette[index * 3 + 1];
        data[target + 2] = palette[index * 3 + 2];
        data[target + 3] = transparency && index < transparency.length ? transparency[index] : 255;
        break;
      }
      case 4:
        data[target] = data[target + 1] = data[target + 2] = samples[source];
        data[target + 3] = samples[source + 1];
        break;
      default:
        data[target] = samples[source];
        data[target + 1] = samples[source + 1];
        data[target + 2] = samples[source + 2];
        data[target + 3] = samples[source + 3];
        break;
    }
  }

  return { width, height, data };
}

export function encodePng(bitmap: Bitmap): Buffer {
  const stride = bitmap.width * 4;
  const raw = Buffer.alloc((stride + 1) * bitmap.height);
  for (let y = 0; y < bitmap.height; y += 1) {
    raw[y * (stride + 1)] = 0; // filter: none
    bitmap.data.copy(raw, y * (stride + 1) + 1, y * stride, (y + 1) * stride);
  }

  const chunk = (type: string, data: Buffer): Buffer => {
    const length = Buffer.alloc(4);
    length.writeUInt32BE(data.length, 0);
    const body = Buffer.concat([Buffer.from(type, 'ascii'), data]);
    const crc = Buffer.alloc(4);
    crc.writeUInt32BE(crc32(body), 0);
    return Buffer.concat([length, body, crc]);
  };

  const header = Buffer.alloc(13);
  header.writeUInt32BE(bitmap.width, 0);
  header.writeUInt32BE(bitmap.height, 4);
  header.writeUInt8(8, 8);   // bit depth
  header.writeUInt8(6, 9);   // colour type: RGBA
  header.writeUInt8(0, 10);  // compression
  header.writeUInt8(0, 11);  // filter
  header.writeUInt8(0, 12);  // interlace

  return Buffer.concat([
    SIGNATURE,
    chunk('IHDR', header),
    chunk('IDAT', zlib.deflateSync(raw, { level: 9 })),
    chunk('IEND', Buffer.alloc(0)),
  ]);
}

let crcTable: Uint32Array | null = null;

function crc32(buffer: Buffer): number {
  if (!crcTable) {
    crcTable = new Uint32Array(256);
    for (let n = 0; n < 256; n += 1) {
      let c = n;
      for (let k = 0; k < 8; k += 1) {
        c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
      }
      crcTable[n] = c >>> 0;
    }
  }
  let crc = 0xffffffff;
  for (let i = 0; i < buffer.length; i += 1) {
    crc = crcTable[(crc ^ buffer[i]) & 0xff] ^ (crc >>> 8);
  }
  return (crc ^ 0xffffffff) >>> 0;
}

/**
 * Resize by averaging over the source area each destination pixel covers.
 *
 * A box filter rather than nearest-neighbour because these are icons being
 * scaled down by large factors - 1024 to 48 - where dropping samples produces
 * visible aliasing on every diagonal. Alpha is premultiplied for the average
 * and divided back out, so a transparent edge does not drag colour in from
 * pixels nobody can see.
 */
export function resize(source: Bitmap, width: number, height: number): Bitmap {
  const data = Buffer.alloc(width * height * 4);
  const scaleX = source.width / width;
  const scaleY = source.height / height;

  for (let y = 0; y < height; y += 1) {
    const y0 = Math.floor(y * scaleY);
    const y1 = Math.max(y0 + 1, Math.ceil((y + 1) * scaleY));
    for (let x = 0; x < width; x += 1) {
      const x0 = Math.floor(x * scaleX);
      const x1 = Math.max(x0 + 1, Math.ceil((x + 1) * scaleX));

      let r = 0;
      let g = 0;
      let b = 0;
      let a = 0;
      let count = 0;

      for (let sy = y0; sy < Math.min(y1, source.height); sy += 1) {
        for (let sx = x0; sx < Math.min(x1, source.width); sx += 1) {
          const index = (sy * source.width + sx) * 4;
          const alpha = source.data[index + 3] / 255;
          r += source.data[index] * alpha;
          g += source.data[index + 1] * alpha;
          b += source.data[index + 2] * alpha;
          a += source.data[index + 3];
          count += 1;
        }
      }

      const target = (y * width + x) * 4;
      if (count === 0) {
        continue;
      }
      const alpha = a / count;
      const premultiplied = alpha / 255;
      data[target] = premultiplied > 0 ? Math.round(r / count / premultiplied) : 0;
      data[target + 1] = premultiplied > 0 ? Math.round(g / count / premultiplied) : 0;
      data[target + 2] = premultiplied > 0 ? Math.round(b / count / premultiplied) : 0;
      data[target + 3] = Math.round(alpha);
    }
  }

  return { width, height, data };
}

/** Composite the bitmap onto an opaque background, for formats that forbid alpha. */
export function flatten(bitmap: Bitmap, background: [number, number, number]): Bitmap {
  const data = Buffer.alloc(bitmap.data.length);
  for (let i = 0; i < bitmap.width * bitmap.height; i += 1) {
    const index = i * 4;
    const alpha = bitmap.data[index + 3] / 255;
    for (let channel = 0; channel < 3; channel += 1) {
      data[index + channel] = Math.round(
        bitmap.data[index + channel] * alpha + background[channel] * (1 - alpha),
      );
    }
    data[index + 3] = 255;
  }
  return { width: bitmap.width, height: bitmap.height, data };
}
