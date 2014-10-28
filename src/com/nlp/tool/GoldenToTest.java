package com.nlp.tool;

import com.google.common.base.CharMatcher;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从标准答案生成测试语料，主义抱持非中文字符的边界
 */
public class GoldenToTest {
    private static Pattern pattern = Pattern.compile(" *([\\p{InCJK_Unified_Ideographs}" +
            "\\p{InCJK_Symbols_and_Punctuation}\\p{InGENERAL_PUNCTUATION}" +
            "\\p{InCJK_SYMBOLS_AND_PUNCTUATION}" +
            "\\p{InCJK_Unified_Ideographs_Extension_A}" +
            "\\p{InLETTERLIKE_SYMBOLS}" +
            "\\uFF01-\\uFF0F\\uFF1A-\\uFF20\\uFF3B-\\uFF40\\uFF5B-\\uFF5E" +
            "\\u0020-\\u002F\\u003A-\\u0040\\u005B-\\u0060\\u007B-\\u007E]) *");
    // 数字英文交接应该被合并
    // 1~1.5 cm ==> 1~1.5cm
    // 13 d病毒 ==> 13d病毒
    // 有时候会把英文字母当作数字用l: 1, o: 0, O: 0
    private static Pattern engDigPattern = Pattern.compile("([0-9０-９]) +([a-zA-Zａ-ｚＡ-Ｚ])");

    public static String removeSpace(String oriLine) {
        oriLine = CharMatcher.WHITESPACE.trimAndCollapseFrom(oriLine, ' ');
        Matcher matcher = pattern.matcher(oriLine);
        oriLine = matcher.replaceAll("$1");
        matcher = engDigPattern.matcher(oriLine);
        return matcher.replaceAll("$1$2");
    }

    /**
     * 对文件夹下的所有文件，逐个进行转换
     * @param inDirectoryName 源目录
     * @param outDirectoryName 输出目录
     * @throws java.io.IOException
     */
    public static void convertDirectory(String inDirectoryName, String outDirectoryName) throws IOException {
        File inDirectory = new File(inDirectoryName);
        File outDirectory = new File(outDirectoryName);
        Iterator<File> fileIterator = FileUtils.iterateFiles(inDirectory, null, false);
        // 如果输出目录不存在则创建
        if (!outDirectory.exists())
            outDirectory.mkdir();

        while (fileIterator.hasNext()) {
            File inFile = fileIterator.next();
            System.out.println(inFile.getName());
            convertFile(inFile, new File(outDirectory, inFile.getName()));
        }
    }

    /**
     * 对一个文件进行转换
     * @param inFile 输入的文件
     * @param outFile 输出的文件
     */
    public static void convertFile(File inFile, File outFile) throws IOException {
        String line = "";
        BufferedReader br = new BufferedReader(new FileReader(inFile));
        PrintWriter pw = new PrintWriter(outFile);
        while ((line = br.readLine()) != null) {
            pw.println(removeSpace(line));
        }
        pw.close();
        br.close();
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: GoldenToTest inputDirectory outputDirectory");
        }
        else
            convertDirectory(args[0], args[1]);
    }

}
