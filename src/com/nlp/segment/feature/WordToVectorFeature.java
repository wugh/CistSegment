package com.nlp.segment.feature;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * 通过对每个字的word2vec向量聚类产生的特征
 */
public class WordToVectorFeature {
    private HashMap<String, Integer> featureMap;

    public WordToVectorFeature(String inputFilename) throws IOException {
        LineIterator lineIterator = null;
        featureMap = new HashMap<String, Integer>();
        lineIterator = FileUtils.lineIterator(new File(inputFilename), "UTF-8");
        while (lineIterator.hasNext()) {
            String line = lineIterator.nextLine();
            String[] fields = line.split("\\s+");
            featureMap.put(fields[0], new Integer(fields[1]));
        }
        lineIterator.close();
    }

    /**
     * 查询一个字符经过word2vec聚类应该属于的类别
     * @param str 输入的字符串（长度为1）
     * @return 如果能找到该字符则返回类别，找不到就返回“N”
     */
    public String queryWordToVecType(String str) {
        if (featureMap.containsKey(str))
            return String.valueOf(featureMap.get(str));
        else
            return "N";
    }
}
