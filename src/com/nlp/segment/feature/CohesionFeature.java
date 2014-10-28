package com.nlp.segment.feature;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.HashBiMap;
import com.nlp.segment.preprocess.BaselinePreprocessor;
import com.nlp.segment.preprocess.NorPreprocessor;
import com.nlp.segment.preprocess.Preprocessor;
import com.nlp.segment.token.NorToken;
import com.nlp.segment.token.Token;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 字符成词概率，根据训练语料来统计
 */
public class CohesionFeature {
    // 字符到Id的映射，Id到字符的映射，用Guava提供的HashBiMap
    private HashBiMap<Character, Integer> wordIdMap = HashBiMap.create();
    // 存储数据
    private int[][] charData;  // 分母信息
    private int[][] cohesionData;  // 分子信息
    // 初始Id
    private int currentID = 0;
    // 按空格分割一行
    private Splitter splitter = Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings().trimResults();

    public CohesionFeature(String inputFile) {
        File file = new File(inputFile);
        try {
            List<String> lines = FileUtils.readLines(file, "UTF-8");
            // 获取字表大小
            for (String line: lines) {
                // 把文本从全部转换成全角
                line = NorPreprocessor.sbs2dbs(line);
                String rawLine = CharMatcher.WHITESPACE.removeFrom(line);
                if (!rawLine.isEmpty())
                    updateWordIdMap(rawLine);
            }

            charData = new int[currentID][currentID];
            cohesionData = new int[currentID][currentID];

            // 统计成词概率的分子分母
            for (String line: lines) {
                line = NorPreprocessor.sbs2dbs(line);
                String rawLine = CharMatcher.WHITESPACE.removeFrom(line);
                // 是否为空行
                if (!rawLine.isEmpty()) {
                    buildCharBigram(rawLine);
                    for (String word : splitter.split(line)) {
                        buildCohesionCharBigram(word);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据生成的句子构建字符Bigram
     * @param line 添加一行
     */
    private void buildCharBigram(String line) {
        char[] chars = line.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            char currentChar = chars[i];
            char nextChar = chars[i + 1];
            charData[wordIdMap.get(currentChar)][wordIdMap.get(nextChar)] += 1;
        }
    }

    private void buildCohesionCharBigram(String word) {
        char[] chars = word.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            char currentChar = chars[i];
            char nextChar = chars[i + 1];
            cohesionData[wordIdMap.get(currentChar)][wordIdMap.get(nextChar)] += 1;
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

    private String discreteProb(double prob) {
        if (prob < 0.2)
            return "S";
        else if (prob <= 0.75)
            return "N";
        else
            return "NS";
    }

    public List<String> getFeature(List<NorToken> sentence) {
        ArrayList<String> features = new ArrayList<String>();
        features.add(discreteProb(0));  // 句子的第一个字符无法跟前一个字符成词
        for (int i = 1; i < sentence.size(); i++) {
            Character currentChar = sentence.get(i).getNorText().charAt(0);
            Character previousChar = sentence.get(i - 1).getNorText().charAt(0);
            if (wordIdMap.containsKey(currentChar) &&
                    wordIdMap.containsKey(previousChar)) {
                int denominator = charData[wordIdMap.get(previousChar)][wordIdMap.get(currentChar)];
                int numerator = cohesionData[wordIdMap.get(previousChar)][wordIdMap.get(currentChar)];
                if (denominator == 0)
                    features.add(discreteProb(0));
                else {
                    double prob = ((double) numerator) / denominator;
                    features.add(discreteProb(prob));
                }
            }
            else
                features.add(discreteProb(0));
        }
        return features;
    }

    public void test() {
        Preprocessor preprocessor = new BaselinePreprocessor();
        List<Token> tokens = preprocessor.process("中共中央总书记");
        List<NorToken> norTokens = new ArrayList<NorToken>();
        for (Token token: tokens) {
            norTokens.add((NorToken) token);
        }
        List<String> res = getFeature(norTokens);
        for (String f: res) {
            System.out.println(f);
        }
    }

    public static void main(String[] args) {
        CohesionFeature cohesionFeature = new CohesionFeature("corpus/train/2010/Training-Labelled.txt");
        cohesionFeature.test();
    }
}
