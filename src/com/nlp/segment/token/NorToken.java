package com.nlp.segment.token;

/**
 * 正规化的Token，同时包含原始信息和正规化信息
 */
public class NorToken extends Token{
    public static enum Attr {
        ALPHA,
        NUMBER,
        PUNCT,
        CHWORD,
        WHITESPACE,
        UNKNOW,
    }

    private String text;
    private Attr attr;

    public NorToken(String text, String context, int beginIndex, int endIndex, Attr attr) {
        super(context, beginIndex, endIndex);
        this.text = text;
        this.attr = attr;
    }

    public NorToken(String context, int beginIndex, int endIndex) {
        super(context, beginIndex, endIndex);
    }
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Attr getAttr() {
        return attr;
    }

    public void setAttr(Attr attr) {
        this.attr = attr;
    }

    public int getLength() {
        return text.length();
    }

    /**
     * 获取正规化的Token内容
     * @return Token的正规化内容
     */
    public String getNorText() {
        return text;
    }

    /**
     * 获取原始的Token内容
     * @return Token的原始内容
     */
    public String getOriText() {
        return super.toString();
    }
}
