package com.nlp.tool;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.HashBiMap;
import com.nlp.segment.constant.CommonValue;
import com.nlp.segment.preprocess.NorPreprocessor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 把语料转换成Treba所需要的格式
 */
public class CorpusForTreba {
    // 标点符号正则
    protected Pattern pattern = Pattern.compile("(,+|;+|\\?+|!+|，+|。+|！+|？+|；+)");
    // 按空格分割一行
    private Splitter splitter = Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings().trimResults();
    // 字符到Id的映射，Id到字符的映射，用Guava提供的HashBiMap
    private HashBiMap<Character, Integer> wordIdMap = HashBiMap.create();
    // 初始Id
    private int currentID = 0;

    public void convert(String inputFilename, String outputFilename) throws IOException {
        File file = new File(inputFilename);
        List<String> sentens = new ArrayList<String>();

        List<String> lines = FileUtils.readLines(file, "UTF-8");
        for (String line: lines) {
            // 把文本从全部转换成全角
            line = NorPreprocessor.sbs2dbs(line);
            for (String smallLine: splitter.split(line)) {
                if (!smallLine.isEmpty()) {
                    updateWordIdMap(smallLine);
                    sentens.addAll(simpleLineToSentences(smallLine));
                }
            }
        }

        // 把句子输出，每个字换成ID
        outputSentens(sentens, outputFilename);
    }

    private void outputSentens(List<String> sentens, String outputFilename) throws IOException {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(outputFilename)));
        try {
            for (String senten: sentens) {
                char[] chars = senten.toCharArray();
                for (int i = 0; i < chars.length; i++) {
                    bw.write(wordIdMap.get(chars[i]) + " ");
                }
                bw.newLine();
            }
        }
        finally {
            bw.close();
        }
    }

    /**
     * 根据输入的字符串更新wordIdMap
     * @param line
     */
    private void updateWordIdMap(String line) {
        char[] chars = line.toCharArray();
        for (char ch: chars) {
            if (!wordIdMap.containsKey(ch))
                wordIdMap.put(ch, currentID++);
        }
    }

    /**
     * 把一行转换成多个句子按标点符号切分，句子开头结尾不添加其他东西
     * @param line
     * @return
     */
    private List<String> simpleLineToSentences(String line) {
        List<String> sentens = new ArrayList<String>();
        if (!line.isEmpty()) {
            int prev_start = 0;
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                String temp = line.substring(prev_start, matcher.end());
                sentens.add(temp);
                prev_start = matcher.end();
            }
            if (prev_start != line.length()) {
                String temp = line.substring(prev_start);
                sentens.add(temp);
            }
        }
        return sentens;
    }

    public static void main(String[] args) throws IOException {
        CorpusForTreba corpusForTreba = new CorpusForTreba();
        corpusForTreba.convert(CommonValue.unsupervisedFilename, "2010-treba.txt");
    }
}
