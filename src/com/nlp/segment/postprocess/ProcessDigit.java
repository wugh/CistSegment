package com.nlp.segment.postprocess;

import com.nlp.segment.token.Token;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 处理连续数字的情况
 */
public class ProcessDigit extends ProcessUrl{
    // 匹配URL的正则表达式
//    static Pattern digitPattern = Pattern.compile("[-+－＋]?\\p{Nd}*[.．]?\\p{Nd}+",
    // 不考虑正负号，因为很有可能这个东西在数学公式里面出现例如1+2+3=6应该切成1 + 2 + 3 = 6
    static Pattern digitPattern = Pattern.compile("\\p{Nd}{1,3}([,，]\\p{Nd}{3})*|"
                    +"\\p{Nd}*[.．]?\\p{Nd}+");

    @Override
    public List<Token> process(String originStr, List<Token> tokens) {
        Matcher matcher = digitPattern.matcher(originStr);
        while (matcher.find()) {
            // 找到一个网址进行一次拼接
            concatTokens(originStr, matcher.start(), matcher.end(), matcher.group(), tokens);
        }
        return tokens;
    }
}
