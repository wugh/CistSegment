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
 * 提取webdict的词特征，label包括O, B, Y, E, B+Y, E+Y
 */
public class WebdictFeature {

    // 字典树
//    private TrieTree dictionary = new TrieTree();
    // 存储webdict的字典信息，感觉用TrieTree树很慢，估计是我的程序原因
    private HashSet<String> dictionary = new HashSet<String>();

    // 以某个字符为开头的字符串的最大长度
    private HashMap<Character, Integer> prefixMaxLength = new HashMap<Character, Integer>();

    public WebdictFeature(String dictFile, String charSet) {

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
                    Character key = word.charAt(0);
                    if (prefixMaxLength.containsKey(key)) {
                        if (currentWordLength > prefixMaxLength.get(key))
                            prefixMaxLength.put(key, currentWordLength);
                    }
                    else {
                        prefixMaxLength.put(key, currentWordLength);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public WebdictFeature(String dictFile) {
        this(dictFile, "UTF-8");
    }

    public List<String[]> getFeature(List<NorToken> sentence) {
        ArrayList<String[]> features = new ArrayList<String[]>();
        for (NorToken token: sentence) {

            if ((token.getAttr() != NorToken.Attr.CHWORD) &&
                    (token.getAttr() != NorToken.Attr.NUMBER)) {
                String[] tempArray = {"O", "O"};
                features.add(tempArray);
            } else {
                String[] tempArray = {"N", "N"};
                features.add(tempArray);
            }
        }

        for (int i = 0; i < sentence.size();) {
            NorToken token = sentence.get(i);
            if (token.getAttr() != NorToken.Attr.CHWORD) {
                i++;
                continue;
            }

            // 当前字符是中文
            Character prefixChar = token.getNorText().charAt(0);
            Integer maxLength = prefixMaxLength.get(prefixChar);
            // 当前字符开头的词没有或者长度是1
            if (maxLength == null || maxLength < 2) {
                i++;
                continue;
            }

            // 有当前字符开头的词，并且长度是>=2
            boolean isMatch = false;
            int maxEndIndex = Math.min(sentence.size(), i + maxLength);
            for (int endIndex = maxEndIndex; endIndex >= i + 2; endIndex--) {
                StringBuilder subStrBuilder = new StringBuilder();
                int j = i;
                boolean otherCharInChineseSeq = false;
                for (; j < endIndex && !otherCharInChineseSeq; j++) {
                    // 我们允许数字和中文组成词
                    // 例如 第十五次
                    if ((sentence.get(j).getAttr() != NorToken.Attr.CHWORD) &&
                            sentence.get(j).getAttr() != NorToken.Attr.NUMBER)
                        otherCharInChineseSeq = true;
                    subStrBuilder.append(sentence.get(j).getNorText());
                }
                if (otherCharInChineseSeq)
                    continue;
                String subStr = subStrBuilder.toString();
                if (dictionary.contains(subStr)) {
                    // 修改特征值
                    for (j = i; j < endIndex; j++) {
                        // 词的开头字符
                        if (j == i) {
                            features.get(j)[1] = plusFeature(features.get(j)[1], "B");
                        }
                        else if (j == endIndex - 1) {
                            features.get(j)[0] = plusFeature(features.get(j)[0], "E");
                        }
                        else {
                            features.get(j)[0] = plusFeature(features.get(j)[0], "Y");
                            features.get(j)[1] = plusFeature(features.get(j)[1], "Y");
                        }
                    }
                    break;
                }
            }
            i++;
        }
        return features;
    }


    public String plusFeature(String oriFeature, String curFeature) {
        if (oriFeature.equals("N"))
            return curFeature;

        if (oriFeature.equals("Y")) {
            if (curFeature.equals("B"))
//                return "Y+B";
                return "T";
            if (curFeature.equals("E"))
//                return "Y+E";
                return "U";
        }

        if (oriFeature.equals("B") && curFeature.equals("Y"))
//            return "Y+B";
            return "T";

        if (oriFeature.equals("E") && curFeature.equals("Y"))
//            return "Y+E";
            return "U";

        return curFeature;
    }

    public void test() {
        Preprocessor preprocessor = new BaselinePreprocessor();
        List<Token> tokens = preprocessor.process("到本世纪末");
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

    public static void main(String[] args) {
        WebdictFeature webdictFeature = new WebdictFeature("corpus/webdict/webdict_with_freq.txt");
        System.out.println("start get feature");
        webdictFeature.test();
    }
}
