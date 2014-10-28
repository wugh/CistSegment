package com.nlp.segment.postprocess;

import com.nlp.segment.token.Token;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 处理英文字符串中间被切开的问题
 */
public class ProcessEnglish extends ProcessUrl{
    // 匹配英文，英文直接可以用点连接，例如hello.fun()
//    static Pattern englishPattern = Pattern.compile("[\\w.．]+",
    // 规定英文的最后一个字符不能使“.”，因为这个可以当做一个句子的结束
    static Pattern englishPattern = Pattern.compile("[A-Z_a-zＡ-Ｚ＿ａ-ｚ]+[.．]\\s+[A-Z_a-zＡ-Ｚ＿ａ-ｚ]+|" +
            "[A-Z_a-z.．Ａ-Ｚ＿ａ-ｚ]*[A-Z_a-zＡ-Ｚ＿ａ-ｚ]");

    @Override
    public List<Token> process(String originStr, List<Token> tokens) {
        Matcher matcher = englishPattern.matcher(originStr);
        while (matcher.find()) {
            // 找到一个网址进行一次拼接
            concatTokens(originStr, matcher.start(), matcher.end(), matcher.group(), tokens);
        }
        return tokens;
    }

    public static void main(String[] args) {
        Matcher matcher = englishPattern.matcher("函数值Uij.");
        while (matcher.find())
            System.out.println(matcher.group());
    }
}
