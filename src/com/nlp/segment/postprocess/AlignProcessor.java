package com.nlp.segment.postprocess;

import com.nlp.segment.token.Token;

import java.util.List;

/**
 * 后处理接口
 */
public interface AlignProcessor {
    public List<Token> process(String originStr, List<Token> tokens);
}
