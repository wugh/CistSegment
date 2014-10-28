package com.nlp.segment.feature;

import com.nlp.segment.preprocess.BaselinePreprocessor;
import com.nlp.segment.preprocess.Preprocessor;
import com.nlp.segment.token.NorToken;
import com.nlp.segment.token.Token;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * 计算L_{begin}(C_0)和L_{end}(C_0)
 */
public class LexicalFeature {
    // 所有的词
    private HashSet<String> dictionary = new HashSet<String>();
    // 以某个字符为开头的字符串的最大长度
    private HashMap<Character, Integer> prefixMaxLength = new HashMap<Character, Integer>();
    // 以某个字符为结尾的字符串的最大长度
    private HashMap<Character, Integer> suffixMaxLength = new HashMap<Character, Integer>();

    public LexicalFeature(String dictFile, String charSet) throws IOException {

        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(dictFile), charSet));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    String[] items = line.split("\\s+");
                    String word = items[0];
                    // 加入字典
                    dictionary.add(word);

                    // 记录word的长度，key为word的第一个字符
                    Integer currentWordLength = word.length();
                    Character prefixKey = word.charAt(0);
                    Character suffixKey = word.charAt(currentWordLength - 1);
                    if (prefixMaxLength.containsKey(prefixKey)) {
                        if (currentWordLength > prefixMaxLength.get(prefixKey))
                            prefixMaxLength.put(prefixKey, currentWordLength);
                    }
                    else {
                        prefixMaxLength.put(prefixKey, currentWordLength);
                    }

                    if (suffixMaxLength.containsKey(suffixKey)) {
                        if (currentWordLength > suffixMaxLength.get(suffixKey))
                            suffixMaxLength.put(suffixKey, currentWordLength);
                    }
                    else {
                        suffixMaxLength.put(suffixKey, currentWordLength);
                    }
                }
            }
        }
        finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public LexicalFeature(String dictFile) throws IOException {
        this(dictFile, "UTF-8");
    }

    /**
     * 查询以当前字符为开头的最大词长度
     * @param character 开头字符
     * @return 最大词长
     */
    public int getCharPrefixMaxLength(Character character) {
        if (!prefixMaxLength.containsKey(character))
            return 0;
        else {
            int value = prefixMaxLength.get(character);
            if (value >= 6)
                return 6;
            else
                return value;
        }
    }

    /**
     * 查询以当前字符为结尾的最大词长度
     * @param character 结束字符
     * @return 最大词长
     */
    public int getCharSuffixMaxLength(Character character) {
        if (!suffixMaxLength.containsKey(character))
            return 0;
        else {
            int value = suffixMaxLength.get(character);
            if (value >= 6)
                return 6;
            else
                return value;
        }
    }

    public List<String[]> getFeature(List<NorToken> sentence) {
        ArrayList<int[]> features = new ArrayList<int[]>();
        for (int i = 0; i < sentence.size(); i++) {
            int[] tempArray = {0, 0};
            features.add(tempArray);
        }

        for (int i = 0; i < sentence.size(); i++) {
            NorToken token = sentence.get(i);
            // 得到当前字符
            Character prefixChar = token.getNorText().charAt(0);
            Integer maxLength = prefixMaxLength.get(prefixChar);
            if (maxLength == null || maxLength < 2) {
                // 字典里面没有以prifixChar开头的词
                continue;
            }

            int maxEndIndex = Math.min(sentence.size(), i + maxLength);
            for (int endIndex = maxEndIndex; endIndex >= i + 2; endIndex--) {
                StringBuilder subStrBuilder = new StringBuilder();
                int j = i;
                for (; j < endIndex; j++) {
                    // 提取从i到endIndex的字符串
                    subStrBuilder.append(sentence.get(j).getNorText());
                }
                String subStr = subStrBuilder.toString();
                if (dictionary.contains(subStr)) {
                    // 修改特征值
                    for (j = i; j < endIndex; j++) {
                        // 词的开头字符
                        if (j == i) {
                            // 以当前字符开头的词最大长度
                            if (subStr.length() > features.get(j)[0])
                                features.get(j)[0] = subStr.length();
                        }
                        if (j == endIndex - 1) {
                            // 以某个字符为结尾的词的最大长度
                            if (subStr.length() > features.get(j)[1])
                                features.get(j)[1] = subStr.length();
                        }
                    }
                }
            }
        }

        ArrayList<String[]> strFeatures = new ArrayList<String[]>();
        for (int[] f: features) {
            String[] newFea = new String[f.length];
            for (int i = 0; i < f.length; i++) {
                newFea[i] = String.valueOf(f[i]);
            }
            strFeatures.add(newFea);
        }
        return strFeatures;
    }

    public void test() {
        Preprocessor preprocessor = new BaselinePreprocessor();
        List<Token> tokens = preprocessor.process("中共中央总书记");
        List<NorToken> norTokens = new ArrayList<NorToken>();
        for (Token token: tokens) {
            norTokens.add((NorToken) token);
        }
        List<String[]> res = getFeature(norTokens);
        for (String[] f: res) {
            for (String val: f) {
                System.out.print(val + "  ");
            }
            System.out.println();
        }
    }


    public static void main(String[] args) throws IOException {
        LexicalFeature lexicalFeature = new LexicalFeature("corpus/webdict/webdict_with_freq.txt");
        lexicalFeature.test();
    }
}
