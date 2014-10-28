package com.nlp.segment;

import com.google.common.base.CharMatcher;
import com.nlp.segment.postprocess.SpecialCharProcess;
import com.nlp.segment.token.Token;

import java.io.*;
import java.util.List;

/**
 * 输出的文件格式符合SIGHAN评测的要求
 */
public class FormatSegmenter extends WordSegmenter{

    public static final Character SEPARATOR = '≮';
    public static final Character END_CHAR = '.';

    public FormatSegmenter(String propertiesFilename) throws IOException {
        super(propertiesFilename);
    }

    @Override
    public void segmentFile(File inFile, File outFile) throws IOException {
        String line = "";
        BufferedReader br = new BufferedReader(new FileReader(inFile));
        PrintWriter pw = new PrintWriter(outFile);
        while ((line = br.readLine()) != null) {
            // 去掉首尾的空白，并且把文中所有的空白替换成英文的空格
            line = CharMatcher.WHITESPACE.trimAndCollapseFrom(line, ' ');
            // 需要把行号读出来
            // 00014≮主线设计车速为80公里／小时，双向八车道，.
            int lineNumberPosition = line.indexOf(SEPARATOR);
            // 这里得到行号是: 00014
            String lineNumberStr = line.substring(0, lineNumberPosition);
            // 需要切分的行的内容是: 主线设计车速为80公里／小时，双向八车道，
            String text = line.substring(lineNumberPosition + 1, line.length() - 1);

            List<Token> tokens = this.segment(text);
            pw.print(lineNumberStr + " ");
            pw.print(SEPARATOR + " ");
            StringBuilder sb = new StringBuilder();
            for (Token token : tokens) {
                sb.append(token.toString());
                sb.append(" ");
            }
            String segmentLine = SpecialCharProcess.process(sb.toString());
            pw.print(segmentLine);
            pw.print(END_CHAR + " ");
            pw.println();
        }
        pw.close();
        br.close();
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Usage: ChineseSegmentor <config_file> <test_directory> <result_directory>");
            System.exit(1);
        }
        FormatSegmenter formatSegmenter = new FormatSegmenter(args[0]);
        formatSegmenter.segmentDirectory(new File(args[1]), new File(args[2]));
    }
}
