package com.nlp.tool;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import java.io.*;
import java.util.HashSet;

/**
 * 统计训练语料里面出现的词，输入的文本是最原始的北大语料（带标签，第一列是时间）
 */
public class WordCount {
    static public void count(String inputFilename, String outputFilename) throws IOException {
        LineIterator it = FileUtils.lineIterator(new File(inputFilename), "UTF-8");
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFilename)));
        Splitter tokenSplitter = Splitter.on(CharMatcher.WHITESPACE).trimResults().omitEmptyStrings();
        HashSet<String> words = new HashSet<String>();

        while (it.hasNext()) {
            boolean skipTimestamp = true;
            for (String field : tokenSplitter.split(it.next())) {
                if (skipTimestamp) {
                    skipTimestamp = false;
                    continue;
                }
                field = field.replaceAll("/\\w+", ""); // 去掉词性标注
                field = field.replaceAll("\\[|]\\w+", "");  // 去掉合并词组标注
                field = field.replaceAll("\\{\\w+}", ""); // 去掉读音标注
                words.add(field);
            }
        }

        for (String word: words) {
            bw.write(word);
            bw.newLine();
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: WordCount inputFilename outputFilename");
        }
        else
            count(args[0], args[1]);
    }
}
