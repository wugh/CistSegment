package com.nlp.segment.feature;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.HashBiMap;
import com.nlp.segment.preprocess.NorPreprocessor;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 求一个字符的左右熵，参考文献
 * “A Multi-layer Chinese Word Segmentation System Optimized for Out-of-domain Tasks”
 */
public class EntropyFeature {
    // 标点符号正则
    protected Pattern pattern = Pattern.compile("(,+|;+|\\?+|!+|，+|。+|！+|？+|；+)");
    // 按空格分割一行
    private Splitter splitter = Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings().trimResults();
    // 字符到Id的映射，Id到字符的映射，用Guava提供的HashBiMap
    private HashBiMap<Character, Integer> wordIdMap = HashBiMap.create();
    // 初始Id
    private int currentID = 0;
    // 存储数据
    private int[][] data;
    // 前向熵
    private HashMap<Character, Double> forwardEntropy = new HashMap<Character, Double>();
    // 后向熵
    private HashMap<Character, Double> backwardEntropy = new HashMap<Character, Double>();

    public EntropyFeature(String inputFile) throws IOException {
        File file = new File(inputFile);
        List<String> sentens = new ArrayList<String>();
        wordIdMap.put('S', currentID++);
        List<String> lines = FileUtils.readLines(file, "UTF-8");
        for (String line: lines) {
            // 把文本从全部转换成全角
            line = NorPreprocessor.sbs2dbs(line);
            for (String smallLine: splitter.split(line)) {
                if (!smallLine.isEmpty()) {
                    updateWordIdMap(smallLine);
                    sentens.addAll(lineToSentences(smallLine));
                }
            }
        }
        wordIdMap.put('E', currentID++);
        buildCharBigram(sentens);
        initForwardBackwardEntropy();
    }


    /**
     * 根据生成的句子构建字符Bigram
     * @param sentens
     */
    private void buildCharBigram(List<String> sentens) {
        data = new int[currentID][currentID];
        for (String senten: sentens) {
            char[] chars = senten.toCharArray();
            for (int i = 0; i < chars.length - 1; i++) {
                char currentChar = chars[i];
                char nextChar = chars[i + 1];
                data[wordIdMap.get(currentChar)][wordIdMap.get(nextChar)] += 1;
            }
        }
    }


    /**
     * 计算前后向熵
     */
    private void initForwardBackwardEntropy() {
        double minVal = Double.MAX_VALUE;
        double maxVal = -Double.MAX_VALUE;

        // 计算前向熵
        for (Character key: wordIdMap.keySet()) {
            if (key != 'S' && key != 'E') {
                int Z = 0;
                for (int i = 1; i < currentID; i++) {
                    Z += data[wordIdMap.get(key)][i];
                }
                // 求熵
                double entropy = 0.0;
                for (int i = 1; i < currentID; i++) {
                    double num = data[wordIdMap.get(key)][i];
                    if (num == 0)
                        continue;
                    else
                        entropy += (num / Z) * Math.log(num / Z);
                }

                entropy = -entropy;

                if (entropy < minVal)
                    minVal = entropy;
                if (entropy > maxVal)
                    maxVal = entropy;

                // 熵的公式最后有一个负号
                forwardEntropy.put(key, entropy);
            }
        }
        System.out.println("--------------forward entropy range---------------");
        System.out.println(minVal);
        System.out.println(maxVal);
        System.out.println("--------------------------------------------------");

        minVal = Double.MAX_VALUE;
        maxVal = -Double.MAX_VALUE;
        // 计算后向熵
        for (Character key: wordIdMap.keySet()) {
            if (key != 'S' && key != 'E') {
                int Z = 0;
                for (int i = 0; i < currentID - 1; i++) {
                    Z += data[i][wordIdMap.get(key)];
                }
                // 求熵
                double entropy = 0.0;
                for (int i = 0; i < currentID - 1; i++) {
                    double num = data[i][wordIdMap.get(key)];
                    if (num == 0)
                        continue;
                    else
                        entropy += (num / Z) * Math.log(num / Z);
                }

                entropy = -entropy;

                if (entropy < minVal)
                    minVal = entropy;
                if (entropy > maxVal)
                    maxVal = entropy;

                // 熵的公式最后有一个负号
                backwardEntropy.put(key, entropy);
            }
        }

        System.out.println("-------------backward entropy range---------------");
        System.out.println(minVal);
        System.out.println(maxVal);
        System.out.println("--------------------------------------------------");
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
     * 把一行转换成多个句子按标点符号切分，并且在句子开头加S
     * 结束加E
     * @param line
     * @return
     */
    private List<String> lineToSentences(String line) {
        List<String> sentens = new ArrayList<String>();
        if (!line.isEmpty()) {
            int prev_start = 0;
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                String temp = line.substring(prev_start, matcher.end());
                temp = "S" + temp + "E";
                sentens.add(temp);
                prev_start = matcher.end();
            }
            if (prev_start != line.length()) {
                String temp = line.substring(prev_start);
                temp = "S" + temp + "E";
                sentens.add(temp);
            }
        }
        return sentens;
    }


    private int discretizeEntropy(double entropy) {
        if (entropy < 1.0)
            return 0;
        else if (entropy < 2.0)
            return 1;
        else if (entropy < 3.5)
            return 2;
        else if (entropy < 5.0)
            return 4;
        else if (entropy < 7.0)
            return 5;
        else
            return 6;
    }


    /**
     * 查询字符的前向熵特征
     * @param character 待查询字符
     * @return
     */
    public int getForwardEntropy(Character character) {
        if (forwardEntropy.containsKey(character)) {
            return discretizeEntropy(forwardEntropy.get(character));
        }
        else {
            System.err.println("No such key " + character);
            return 0;
        }
    }


    /**
     * 查询字符的后向熵特征
     * @param character 待查询字符
     * @return
     */
    public int getBackwardEntropy(Character character) {
        if (backwardEntropy.containsKey(character)) {
            return discretizeEntropy(backwardEntropy.get(character));
        }
        else {
            System.err.println("No such key " + character);
            return 0;
        }
    }

    public static void main(String[] args) throws IOException {
        EntropyFeature entropyFeature = new EntropyFeature("corpus/train/2010/2010-train-unlabel-test.txt");
    }
}
