package com.nlp.tool;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.List;

/**
 * 由于加了一些处理之后，我们的文件输出是按行对应的
 * 所以需要转一个转换，把行的结果转换成每一列一个词的结果
 */
public class ConvertFormat {
    public static void convert(String inputFilename, String outputFilename)
            throws IOException {
        List<String> inputContent = FileUtils.readLines(new File(inputFilename), "UTF-8");
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(outputFilename), "UTF-8"));
        Splitter splitter = Splitter.on(CharMatcher.WHITESPACE).
                trimResults().omitEmptyStrings();
        for (String line: inputContent) {
            line = CharMatcher.WHITESPACE.trimFrom(line);
            if (line.isEmpty())
                System.err.println("Error: There is empty is origin file");
            for (String word: splitter.split(line)) {
                bw.write(word);
                bw.newLine();
            }
            bw.newLine();
        }
        bw.close();
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: ConvertFormat inputFilename outputFilename");
            System.exit(1);
        }

        convert(args[0], args[1]);
    }
}
