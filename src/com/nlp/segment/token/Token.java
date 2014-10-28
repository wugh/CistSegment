package com.nlp.segment.token;

/**
 * 这个Token保存了三个信息
 * 1. 原始句子context
 * 2. Token开始的下标
 * 3. Token结束的下标
 */
public class Token {
    private String context;
    private int beginIndex;
    private int endIndex;
    public Token(String context, int beginIndex, int endIndex) {
        this.context = context;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public int getBeginIndex() {
        return beginIndex;
    }

    public void setBeginIndex(int beginIndex) {
        this.beginIndex = beginIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }

    public int getLength() {
        return endIndex - beginIndex;
    }

    @Override
    public String toString() {
        return context.substring(beginIndex, endIndex);
    }

    @Override
    public int hashCode() {
        String text = toString();
        return text.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Token other = (Token) obj;
        return this.toString().equals(other.toString());
    }
}

