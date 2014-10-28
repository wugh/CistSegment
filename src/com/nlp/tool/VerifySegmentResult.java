package com.nlp.tool;

import com.google.common.base.CharMatcher;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 校验原始文件盒切分结果
 * 要保证对应行去除空白之后完全长度相同，并且对应位置内容相同
 */
public class VerifySegmentResult {
    public static boolean verify(String testFilename, String resultFilename)
            throws IOException {
        List<String> testContent = FileUtils.readLines(new File(testFilename), "UTF-8");
        List<String> resultContent = FileUtils.readLines(new File(resultFilename), "UTF-8");
        boolean isMatch = true;
        for (int i = 0; i < testContent.size(); i++) {
            String testLine = testContent.get(i);
            String resultLine = resultContent.get(i);
            testLine = CharMatcher.WHITESPACE.removeFrom(testLine);
            resultLine = CharMatcher.WHITESPACE.removeFrom(resultLine);
            if (!testLine.equals(resultLine)) {
                System.err.println(testLine + "\t" + resultLine);
                isMatch = false;
            }
        }
        return isMatch;
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: VerifySegmentResult testFile resultFile");
            System.exit(1);
        }

        verify(args[0], args[1]);
    }
}
