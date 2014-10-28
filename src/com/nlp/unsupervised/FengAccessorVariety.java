package com.nlp.unsupervised;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.nlp.segment.preprocess.NorPreprocessor;
import com.nlp.segment.token.NorToken;
import org.apache.commons.io.FileUtils;
import org.jsuffixarrays.SuffixArrays;
import org.jsuffixarrays.SuffixData;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用来计算每个字串的AV值
 * zhao et al. 提供的AV特征值使用方法
 */
public class FengAccessorVariety {
    // 存储字串的AV值
    private HashMap<String, Integer> subseqAV =
            new HashMap<String,Integer>();
    // 所有语料的内容
    protected String content;
    // 后缀数组
    protected SuffixData suffixData;
    // 标点符号正则
    protected Pattern pattern = Pattern.compile("(,+|;+|\\?+|!+|，+|。+|！+|？+|；+)");
    // 按空格分割一行
    private Splitter splitter = Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings().trimResults();

    // 按行存储文件的内容，不包含空行
    // 暂时还没用到，所以去掉
    // protected List<String> contentLines;

    /**
     * 返回按行存储的文件内容
     * @return 按行存储的文件
     */
    /*
    public List<String> getContentLines() {
        return contentLines;
    }
    */

    /**
     * 构造函数
     * @param inputFile 输入的文件
     */
    public FengAccessorVariety(String inputFile) throws IOException {
        File file = new File(inputFile);
        List<String> sentens = new ArrayList<String>();
//        this.contentLines = new ArrayList<String>();
        List<String> lines = FileUtils.readLines(file, "UTF-8");
        for (String line: lines) {
            // 把文本从全部转换成全角
            line = NorPreprocessor.sbs2dbs(line);
            for (String smallLine: splitter.split(line)) {
                if (!smallLine.isEmpty()) {
                    sentens.addAll(lineToSentences(smallLine));
//                        contentLines.addAll(simpleLineToSentences(smallLine));
                }
            }
        }
        Joiner joiner = Joiner.on("").skipNulls();
        this.content = joiner.join(sentens);
        this.suffixData = SuffixArrays.createWithLCP(this.content);
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

    /**
     * 把一行转换成多个句子按标点符号切分，句子开头结尾不添加其他东西
     * @param line
     * @return
     */
    private List<String> simpleLineToSentences(String line) {
        List<String> sentens = new ArrayList<String>();
        if (!line.isEmpty()) {
            int prev_start = 0;
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                String temp = line.substring(prev_start, matcher.end());
                sentens.add(temp);
                prev_start = matcher.end();
            }
            if (prev_start != line.length()) {
                String temp = line.substring(prev_start);
                sentens.add(temp);
            }
        }
        return sentens;
    }

    /**
     * Returns the number of suffixes strictly less than the <tt>query</tt> string.
     * We note that <tt>rank(select(i))</tt> equals <tt>i</tt> for each <tt>i</tt>
     * between 0 and <em>N</em>-1.
     * @param query the query string
     * @return the number of suffixes strictly less than <tt>query</tt>
     */
    public int rank(String query) {
        // 这里得到的suffixes的长度比原始字符串大1
        int [] suffixes = this.suffixData.getSuffixArray();
        int lo = 0, hi = suffixes.length - 2;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            int cmp = compare(query, suffixes[mid]);
            if (cmp < 0) hi = mid - 1;
            else if (cmp > 0) lo = mid + 1;
            else return mid;
        }
        return lo;
    }

    // compare query string to suffix
    private int compare(String query, int suffix) {
        int suffixLength = this.content.length() - suffix;
        int N = Math.min(query.length(), suffixLength);
        for (int i = 0; i < N; i++) {
            if (query.charAt(i) < this.content.charAt(suffix + i)) return -1;
            if (query.charAt(i) > this.content.charAt(suffix + i)) return +1;
        }
        return query.length() - suffixLength;
    }

    /**
     * 查找语料中特定字串的AV值
     * @param subseq 待查询的字串
     * @return AV值
     */
    public int querySubseqAV(String subseq) {
        if (this.subseqAV.containsKey(subseq))
            return this.subseqAV.get(subseq);
        else {
            // 如果这个subseq是第一次查询
            // 需要根据后缀数组的信息查找

            // 找到严格小于subseq的后缀的个数
            // >=rank的位置的后缀含有subseq字串
            int rank = this.rank(subseq);
            int subseqLength = subseq.length();
            HashSet<Character> leftSet = new HashSet<Character>();
            int leftVal = 0;
            HashSet<Character> rightSet = new HashSet<Character>();
            int rightVal = 0;
            for (int i = rank; i < this.content.length(); i++) {
                int index = this.suffixData.getSuffixArray()[i];
                // 选择后缀的前subseqLength个字符
                String suffixSubseq = this.content.substring(index, Math.min(
                        index + subseqLength, this.content.length()));
                if (suffixSubseq.equals(subseq)) {
                    // Feng提出的那个AV特征只要考虑子串左边的第一个字符
                    // 如果这个后缀的子串跟subseq相同，那么就统计左右的字信息
                    int j = index - 1;
                    if (j >= 0) {
                        char ch = this.content.charAt(j);
                        if (ch == 'S')
                            leftVal++;
                        else
                            leftSet.add(ch);
                    }
                    // 计算index右边的第一个字
                    j = index + subseqLength;
                    if (j < this.content.length()) {
                        char ch = this.content.charAt(j);
                        if (ch == 'E')
                            rightVal++;
                        else
                            rightSet.add(ch);
                    }
                }
                else {
                    // 如果这个后缀的子串和subseq不同，那么说明后面的之后的肯定也不同
                    // 需要终止循环
                    break;
                }
            }
            leftVal += leftSet.size();
            rightVal += rightSet.size();
            int minVal = Math.min(leftVal, rightVal);
            this.subseqAV.put(subseq, minVal);
            return minVal;
        }
    }

    /**
     * 计算给定字符串的n-gram特征，详细步骤参考论文
     * <p>Unsupervised Overlapping Feature Selection for
     * Conditional Random Fields Learning in Chinese Word Segmentation</p>
     * @param n ngram的n取值
     * @param str 待计算的句子
     * @return ngram特征
     */
    public int[] ngramFeature(int n, String str) {
        char[] chars = str.toCharArray();
        int[] features = new int[chars.length];
        for (int i = 0; i < chars.length - n + 1; i++) {
            int subseq_end = i + n;
            String subseq = concat(chars, i, subseq_end);
            int logAV = log2(this.querySubseqAV(subseq));
            for (int j = i; j < subseq_end; j++) {
                if (logAV > features[j])
                    features[j] = logAV;
            }
        }
        return features;
    }

    /**
     * 给定一个句子计算每个字符的AV特征值，特征值的计算还依赖于ngram长度
     * @param n 要使用n元gram
     * @param senten 由NorToken组成的句子
     * @return 句子里面每个字符的AV特征值
     */
    public int[] sentenNgramFeature(int n, List<NorToken> senten) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < senten.size(); i++) {
            sb.append(senten.get(i).getNorText());
        }
        String sentenStr = sb.toString().trim();
        return ngramFeature(n, sentenStr);
    }

    /**
     * log_2(x)的值，当x==0的时候直接返回0，当x不为0的时候求log_2(x)
     * @param x 输入的整数
     * @return log_2(x)的值
     */
    private int log2(int x)
    {
        if (x == 0)
            return 0;
        else
            return (int) (Math.log(x) / Math.log(2));
    }

    protected String concat(char[] chars, int start, int end) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++)
            sb.append(chars[i]);
        return sb.toString();
    }

    public static void main(String[] args) throws IOException {
        FengAccessorVariety avtool = new FengAccessorVariety("corpus/train/2010/2010-train-test");
        int[] features  = avtool.ngramFeature(5, "制定了中国跨世纪发展的行动纲领");
        for (int item: features) {
            System.out.println(item);
        }
    }
}
