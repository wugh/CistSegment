package com.nlp.tool;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * 删除北大语料里面的标注，把语料弄成只有分词信息
 */
public class RemovePkuLabel {
    static public void filterFile(String inputFilename, String outputFilename) throws IOException {
        LineIterator it = FileUtils.lineIterator(new File(inputFilename), "UTF-8");
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFilename)));
        Splitter tokenSplitter = Splitter.on(CharMatcher.WHITESPACE).trimResults().omitEmptyStrings();
        Joiner joiner = Joiner.on("  ");

        while (it.hasNext()) {
            ArrayList<String> resultFields = new ArrayList<String>();
            boolean skipTimestamp = true;
            for (String field : tokenSplitter.split(it.next())) {
                if (skipTimestamp) {
                    skipTimestamp = false;
                    continue;
                }
                field = field.replaceAll("/\\w+", ""); // 去掉词性标注
                field = field.replaceAll("\\[|]\\w+", "");  // 去掉合并词组标注
                field = field.replaceAll("\\{\\w+}", ""); // 去掉读音标注
                resultFields.add(field);
            }
            if (!resultFields.isEmpty()) {
                bw.write(joiner.join(resultFields));
                bw.newLine();
            }
        }

        it.close();
        bw.close();
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: RemovePkuLabel inputFilename outputFilename");
        }
        else
            filterFile(args[0], args[1]);
    }
}
