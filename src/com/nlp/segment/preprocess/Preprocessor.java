package com.nlp.segment.preprocess;

import com.nlp.segment.token.Token;

import java.util.List;

/**
 * 预处理方法需要提供的接口
 */
public interface Preprocessor {
    List<Token> process(String text);
}
