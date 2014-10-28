package com.nlp.segment.postprocess;

import com.google.common.base.CharMatcher;
import com.nlp.segment.token.NorToken;
import com.nlp.segment.token.Token;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解决URL切分错误，例如：
 * 大家 帮 我 看看 怎么样 ? http://t.cn/x4 abc (错误)
 * 大家 帮 我 看看 怎么样 ? http://t.cn/x4abc(正确)
 */
public class ProcessUrl implements AlignProcessor{
    // 匹配URL的正则表达式
    Pattern urlPattern = Pattern.compile("(https?|ftp|file)://" +
            "[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]", Pattern.CASE_INSENSITIVE);

    public List<Token> process(String originStr, List<Token> tokens) {
        Matcher matcher = urlPattern.matcher(originStr);
        while (matcher.find()) {
            // 找到一个网址进行一次拼接
            concatTokens(originStr, matcher.start(), matcher.end(), matcher.group(), tokens);
        }
        return tokens;
    }

    /**
     * 把已切分字符串的某些部分拼接起来形成一个网址
     * @param originStr 原始字符串
     * @param start 网址在原始字符串的开始位置
     * @param end 网址在原始字符串的结束位置
     * @param url 具体的网址内容
     * @param tokens 由Token组成的字符串
     * @return 拼接好的字符串
     */
    protected List<Token> concatTokens(String originStr, int start, int end, String url,
                                           List<Token> tokens) {
        // 这里有一个严重的问题，原始字符串里面如果有空白符会导致下标不对齐
        // 需要把下标的残差加回去
        int spaceCount = 0;
        for (char c: originStr.substring(0, start).toCharArray()) {
            if (CharMatcher.WHITESPACE.matches(c))
                spaceCount++;
        }
        int searchStart = start - spaceCount;
        // 匹配英文的时候中间可能还会有空格英文人名“P. Poll”
        for (char c: originStr.substring(start, end).toCharArray()) {
            if (CharMatcher.WHITESPACE.matches(c))
                spaceCount++;
        }
        int searchEnd = end - spaceCount;

        // 寻找开始的words下标
        int cumulativeIndex = 0;
        int startIndex = -1;
        for (int index = 0; index < tokens.size(); index++) {
            if (cumulativeIndex == searchStart) {
                startIndex = index;
                break;
            }
            cumulativeIndex += tokens.get(index).toString().length();
        }

        // 寻找结束的words下标
        int endIndex = -1;
        if (startIndex != -1) {
            for (int index = startIndex; index < tokens.size(); index++) {
                cumulativeIndex += tokens.get(index).toString().length();
                if (cumulativeIndex == searchEnd) {
                    endIndex = index;
                    break;
                }
            }
        }

        if (endIndex > startIndex) {
            // 如果网址被分到两个词里面，那么需要合并一下
            for (int index = endIndex; index >= startIndex; index--) {
                tokens.remove(index);
            }
            // 删除完之后在原来的位置插入完整的网址
            tokens.add(startIndex, new NorToken(url, originStr, start, end, NorToken.Attr.UNKNOW));
        }
        return tokens;
    }
}
