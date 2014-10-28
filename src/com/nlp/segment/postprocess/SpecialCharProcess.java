package com.nlp.segment.postprocess;

import com.google.common.base.CharMatcher;

import java.util.regex.Pattern;

/**
 * 处理语料里面的特殊符号，
 * 主要是括号
 */
public class SpecialCharProcess {
    static Pattern specialCharPattern = Pattern.compile("\\s*([（）｛｝\\(\\)\\{\\}\\[\\]])\\s*");

    public static String process(String origin) {
        return CharMatcher.WHITESPACE.trimLeadingFrom(
                specialCharPattern.matcher(origin).replaceAll(" $1 "));
    }
}
