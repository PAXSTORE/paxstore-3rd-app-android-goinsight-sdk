/*
 * ******************************************************************************
 * COPYRIGHT
 *               PAX TECHNOLOGY, Inc. PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with PAX  Technology, Inc. and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *
 *      Copyright (C) 2017 PAX Technology, Inc. All rights reserved.
 * ******************************************************************************
 */

package com.pax.market.android.app.sdk.device.provider;

import java.util.Arrays;

public class Base64 {

    private Base64() {
    }

    public static Encoder getEncoder() {
        return Encoder.RFC4648;
    }

    public static class Encoder {
        static final Encoder RFC4648 = new Encoder();
        private static final char[] TO_BASE64 = {
                'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
                'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
                'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
                'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
        };

        @SuppressWarnings("deprecation")
        public String encodeToString(byte[] src) {
            byte[] encoded = encode(src);
            return new String(encoded, 0, 0, encoded.length);
        }

        public byte[] encode(byte[] src) {
            int len = outLength(src.length);
            byte[] dst = new byte[len];
            int ret = encode0(src, 0, src.length, dst);
            if (ret != dst.length) {
                return Arrays.copyOf(dst, ret);
            }
            return dst;
        }

        private int outLength(int srclen) {
            return 4 * ((srclen + 2) / 3);
        }

        private int encode0(byte[] src, int off, int end, byte[] dst) {
            int sp = off;
            int sl = off + ((end - off) / 3 * 3);
            int dp = 0;
            while (sp < sl) {
                int bits = (src[sp++] & 0xff) << 16 |
                        (src[sp++] & 0xff) << 8 |
                        (src[sp++] & 0xff);
                dst[dp++] = (byte) TO_BASE64[(bits >>> 18) & 0x3f];
                dst[dp++] = (byte) TO_BASE64[(bits >>> 12) & 0x3f];
                dst[dp++] = (byte) TO_BASE64[(bits >>> 6) & 0x3f];
                dst[dp++] = (byte) TO_BASE64[bits & 0x3f];
            }
            if (sp < end) {
                int b0 = src[sp++] & 0xff;
                dst[dp++] = (byte) TO_BASE64[b0 >> 2];
                if (sp == end) {
                    dst[dp++] = (byte) TO_BASE64[(b0 << 4) & 0x3f];
                    dst[dp++] = '=';
                    dst[dp++] = '=';
                } else {
                    int b1 = src[sp++] & 0xff;
                    dst[dp++] = (byte) TO_BASE64[(b0 << 4) & 0x3f | (b1 >> 4)];
                    dst[dp++] = (byte) TO_BASE64[(b1 << 2) & 0x3f];
                    dst[dp++] = '=';
                }
            }
            return dp;
        }
    }
}
