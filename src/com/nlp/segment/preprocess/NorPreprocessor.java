package com.nlp.segment.preprocess;

import com.nlp.segment.token.NorToken;
import com.nlp.segment.token.NorToken.Attr;
import com.nlp.segment.token.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正规化预处理模块，全部字符都转成全角再处理
 * 1. 数字串设定属性为NUMBER
 * 2. 英文串设定属性为ALPHA
 * 3. 标点符号设定属性为PUNCT
 * 4. 中文串设定属性为CHWORD
 * 5. 空白串设定属性为WHITESPACE
 * 6. 其他字符属性设定为UNKNOW
 */
public class NorPreprocessor implements Preprocessor{
    // 匹配英文字符串
    private Pattern alphaPattern = Pattern.compile("^[Ａ-Ｚ＿ａ-ｚ]+[Ａ-Ｚ＿－ａ-ｚ０-９]*");
    // 匹配数字串
    private Pattern digitPattern = Pattern.compile("^([０-９○〇幺一二三四五六七八九十零两百千万亿]+"  +
            "[点．][０-９○〇幺一二三四五六七八九十零两百千万亿]+|" +
            "[０-９○〇幺一二三四五六七八九十零两百千万亿]+)");
    // 匹配简单的时间格式"12点32分"
    private Pattern timePattern = Pattern.compile("^[０-９○〇幺一二三四五六七八九十零两百千万亿]+"  +
            "[点．][０-９○〇幺一二三四五六七八九十零两百千万亿]+分");
    // 匹配简单的数字串，不包括浮点
    private Pattern simpleDigitPattern = Pattern.compile("^[０-９○〇幺一二三四五六七八九十零两百千万亿]+");

    /**
     *single byte string to double byte string
     * @param str 输入的字符串
     * @return 返回全角字符串
     */
    public static String sbs2dbs(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            sb.append(BaselinePreprocessor.sbc2dbc(str.charAt(i)));
        }
        return sb.toString();
    }

    Attr judgeCharType(char ch) {
        return BaselinePreprocessor.judgeCharType(ch);
    }

    @Override
    public List<Token> process(String text) {
        List<Token> postTokens = new ArrayList<Token>();
        String fullText = sbs2dbs(text);
        for (int index = 0; index < fullText.length();) {
            Attr charType = judgeCharType(fullText.charAt(index));
            if (charType == Attr.CHWORD || charType == Attr.UNKNOW || charType == Attr.PUNCT) {
                postTokens.add(new NorToken(String.valueOf(fullText.charAt(index)),
                        text, index, index + 1, charType));
                index++;
            } else if (charType == Attr.ALPHA) {
                Matcher matcher = alphaPattern.matcher(fullText.substring(index));
                if (matcher.find()) {
                    // 用<alpha>替换英文
                    postTokens.add(new NorToken("<alpha>",
                            text, index, index + matcher.end(), charType));
                    index += matcher.end();
                } else
                    System.err.println("error");
            } else if (charType == Attr.NUMBER) {
                Matcher matcher = timePattern.matcher(fullText.substring(index));
                if (matcher.find()) {
                    Matcher digitMatcher = simpleDigitPattern.matcher(fullText.substring(index));
                    if (digitMatcher.find()) {
                        // 用<digit>替换数字
                        postTokens.add(new NorToken("<digit>",
                                text, index, index + digitMatcher.end(), charType));
                        index += digitMatcher.end();
                    } else
                        System.err.println("error");
                } else {
                    Matcher digitMatcher = digitPattern.matcher(fullText.substring(index));
                    if (digitMatcher.find()) {
                        // 用<digit>替换数字
                        postTokens.add(new NorToken("<digit>",
                                text, index, index + digitMatcher.end(), charType));
                        index += digitMatcher.end();
                    } else
                        System.err.println("error");
                }
            }
        }
        return postTokens;
    }

    public static void main(String[] args) {
        Preprocessor preprocessor = new NorPreprocessor();
        List<Token> tokens = preprocessor.process("qinqin1220转发赢得2011新款12.1现在是12点20分");
        for (Token token: tokens) {
            NorToken norToken = (NorToken) token;
            System.out.println(norToken.getOriText() + "\t" + norToken.getNorText());
        }
    }
}
