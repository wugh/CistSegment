package com.nlp.tool;

import com.google.common.base.CharMatcher;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 按照一定的比例切分出训练集合和测试结合
 */
public class RandomSplit {
    /**
     * 对原始文件读入后，按一定比例分配训练集合和测试集合
     * @param filename 输入的文件
     * @param ratio 训练集合所占的比例
     * @param trainFilename 输出的训练文件名字
     * @param testFilename 输出的测试文件名字
     */
    public static void splitFile(String filename, double ratio, String trainFilename, String testFilename) throws IOException {
        if (ratio <= 0.0 || ratio >= 1.0 )
            ratio = 0.8;

        List<String> content = new ArrayList<String>();
        LineIterator it = FileUtils.lineIterator(new File(filename), "UTF-8");
        while (it.hasNext()) {
            String line = it.nextLine();
            line = CharMatcher.WHITESPACE.trimFrom(line);
            if (!line.isEmpty()) {
                content.add(line);
            }
        }

        Collections.shuffle(content);
        int length = content.size();
        int train_num = (int) (length * ratio);
        BufferedWriter trainBw = new BufferedWriter(new FileWriter(new File(trainFilename)));
        BufferedWriter testBw = new BufferedWriter(new FileWriter(new File(testFilename)));
        try {
            for (int index = 0; index < length; index++) {
                if (index < train_num) {
                    trainBw.write(content.get(index));
                    trainBw.newLine();
                }
                else {
                    testBw.write(content.get(index));
                    testBw.newLine();
                }
            }
        } finally {
            trainBw.close();
            testBw.close();
        }
    }

    public static void main(String[] args) throws IOException {
        splitFile("/home/guohuawu/Sighan/Corpus/pku/2000/all-data.utf8.txt",
                0.8,
                "./corpus/train/pku_2000_train",
                "./corpus/train/pku_2000_test");
    }
}
