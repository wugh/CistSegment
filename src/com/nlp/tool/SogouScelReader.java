package com.nlp.tool;

/*  Copyright (c) 2010 Xiaoyun Zhu
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

/**
 * Sougou Pinyin IME SCEL File Reader
 *
 * <pre>
 * SCEL Format overview:
 *
 * General Information:
 * - Chinese characters and pinyin are all encoded with UTF-16LE.
 * - Numbers are using little endian byte order.
 *
 * SCEL hex analysis:
 * - 0x0           Pinyin List Offset
 * - 0x120         total number of words
 * - 0x<PY-Offset> total number of pinyin
 * - ...           List of pinyin as [index, byte length of pinyin, pinyin as string] triples
 * - ...           Dictionary
 * - ...           <additional garbage>
 *
 * Dictionary format:
 * - It can interpreted as a list of
 *   [alternatives of words,
 *       byte length of pinyin indexes, pinyin indexes,
 *       [byte length of word, word as string, length of skip bytes, skip bytes]
 *       ... (alternatives)
 *   ].
 *
 * </pre>
 *
 */
public class SogouScelReader {
    public static void readScel(String scelFile) throws IOException{
        // read scel into byte array
        final ByteArrayOutputStream dataOut = new ByteArrayOutputStream();

        RandomAccessFile file = new RandomAccessFile(scelFile, "r");
        final FileChannel fChannel = file.getChannel();
        fChannel.transferTo(0, fChannel.size(), Channels.newChannel(dataOut));

        // scel as bytes
        final ByteBuffer dataRawBytes = ByteBuffer.wrap(dataOut.toByteArray());
        dataRawBytes.order(ByteOrder.LITTLE_ENDIAN);

        final byte[] buf = new byte[1024];
        final String[] pyDict = new String[512];

        final int totalWords = dataRawBytes.getInt(0x120);

        // pinyin offset
        dataRawBytes.position(dataRawBytes.getInt());
        final int totalPinyin = dataRawBytes.getInt();
        for (int i = 0; i < totalPinyin; i++) {
            final int idx = dataRawBytes.getShort();
            final int len = dataRawBytes.getShort();
            dataRawBytes.get(buf, 0, len);
            pyDict[idx] = new String(buf, 0, len, "UTF-16LE");
        }

        // extract dictionary
        int counter = 0;
        for (int i = 0; i < totalWords; i++) {
            final StringBuilder py = new StringBuilder();
            final StringBuilder word = new StringBuilder();

            int alternatives = dataRawBytes.getShort();
            int pyLength = dataRawBytes.getShort() / 2;
            boolean first = true;
            while (pyLength-- > 0) {
                final int key = dataRawBytes.getShort();
                if (first) {
                    first = false;
                } else {
                    py.append('\'');
                }
                py.append(pyDict[key]);
            }
            first = true;
            while (alternatives-- > 0) {
                if (first) {
                    first = false;
                } else {
                    // 一个词有多种说法
                    // 振振有词, 振振有辞
                    word.append("\n");
                }
                final int wordlength = dataRawBytes.getShort();
                dataRawBytes.get(buf, 0, wordlength);
                word.append(new String(buf, 0, wordlength, "UTF-16LE"));
                // skip bytes
                dataRawBytes.get(buf, 0, dataRawBytes.getShort());
                counter++;
            }
//            System.out.println(word.toString() + "\t" + py.toString());
            System.out.println(word.toString());
        }
        System.err.println(scelFile + ": " + counter + " words");
    }

    public static void main(final String[] args) throws IOException {
        if (args.length < 1)
            System.out.println("Usage: SogouScelReader dict1.scel [dict2.scel] [dict3.scel] ... ");
        else {
            for (int i = 0; i < args.length; i++) {
                readScel(args[0]);
            }
        }
    }
}
