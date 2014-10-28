package com.nlp.segment.preprocess;

import com.google.common.base.CharMatcher;
import com.nlp.segment.token.NorToken;
import com.nlp.segment.token.NorToken.Attr;
import com.nlp.segment.token.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * baseline的预处理，不对连续的英文、数字进行合并
 * 只是标注出字符类型
 */
public class BaselinePreprocessor implements Preprocessor{

    private static String chineseNums = "○〇幺一二三四五六七八九零十两百千万亿";

    public static String getTypeStr(Attr attr) {
        switch (attr) {
            case ALPHA:
                return "E";
            case NUMBER:
                return "N";
            case PUNCT:
                return "P";
            case CHWORD:
                return "C";
            case UNKNOW:
                return "O";
            default:
                return "O";
        }
    }

    public static Attr judgeCharType(char ch) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(ch);
        if (CharMatcher.WHITESPACE.matches(ch))
            return Attr.WHITESPACE;
        else if ((ch >= '\uFF10' && ch <= '\uFF19') ||
                (chineseNums.indexOf(ch) != -1))
            return Attr.NUMBER;
        else if (ub == Character.UnicodeBlock.GENERAL_PUNCTUATION ||
                ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION ||
                (ch >= '\uFF01' && ch <= '\uFF0E') ||
                (ch >= '\uFF1A' && ch <= '\uFF20') ||
                (ch >= '\uFF3B' && ch <= '\uFF40') ||
                (ch >= '\uFF5B' && ch <= '\uFF5E'))
            return Attr.PUNCT;
        else if ((ch >= '\uFF21' && ch <= '\uFF3A') ||
                (ch >= '\uFF41' && ch <= '\uFF5A'))
            return Attr.ALPHA;
        else if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
                ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
                ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A)
            return Attr.CHWORD;
        else
            return Attr.UNKNOW;
    }

    /**
     * 全角字符转半角
     * @param c
     * @return
     */
    public static char dbc2sbc(char c) {
        if (c < 128) {
            return c;
        }
        if (c == '\u3000') {
            return ' ';
        }
        if (c < '\uFF5F' && c > '\uFF00') {
            return (char) (c - 65248);
        } else {
            return 0;
        }
    }

    /**
     * 半角转全角
     * @param c 输入字符
     * @return 返回全角字符
     */
    public static char sbc2dbc(char c) {
        if (c < '\u0020' || c > '\u007e')
            return c;
        if (c == '\u0020')
            return c; // 空格没有替换成'\u3000'
        else {
            return (char) (c + '\uFEE0');
        }
    }

    @Override
    public List<Token> process(String text) {
        char och = 0; // 原始字符
        char dch = 0; // 全角字符
        Attr charType; // 字符类型
        List<Token> tokens = new ArrayList<Token>();
        for (int index = 0; index < text.length(); ++index) {
            och = text.charAt(index);
            dch = sbc2dbc(och);
            charType = judgeCharType(dch);
            tokens.add(new NorToken(String.valueOf(dch), text, index, index + 1, charType));
        }
        return tokens;
    }
}
