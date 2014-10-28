package com.nlp.segment;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.nlp.crf.CRFTagger;
import com.nlp.crf.Tagger;
import com.nlp.segment.postprocess.*;
import com.nlp.segment.preprocess.BaselinePreprocessor;
import com.nlp.segment.feature.LexicalFeature;
import com.nlp.segment.preprocess.Preprocessor;
import com.nlp.segment.token.NorToken;
import com.nlp.segment.token.Token;
import com.nlp.unsupervised.AccessorVariety;
import com.nlp.segment.feature.EntropyFeature;
import com.nlp.segment.feature.WordToVectorFeature;
import com.nlp.unsupervised.FengAccessorVariety;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.*;

/**
 * 利用训练好的CRF模型进行分词
 * 利用config文件来控制特征的选择
 */
public class WordSegmenter {
    private static final String SINGLE_TAG = "S";
    private static final String END_TAG = "E";

    private Preprocessor preprocessor;
    private Tagger tagger;
    // 是否使用字符类型特征(1列)
    private boolean useCharType;
    // word2vec特征(1列)
    private WordToVectorFeature wordToVectorFeature;
    // 前后向熵特征(2列)
    private EntropyFeature entropyFeature;
    // Lexical特征(2列)
    private LexicalFeature lexicalFeature;
    // 下面这两个无监督计算比较慢，吃内存，因为用到了后缀数组来加速计算
    private AccessorVariety accessorVariety;
    // Feng AV特征（原始的AV定义）
    private FengAccessorVariety fengAccessorVariety;
    // 是否输出调试信息
    private boolean debug;
    // 是否使用后处理
    private boolean usePostprocess;
    // 后处理
    AlignProcessor processUrl = null;
    AlignProcessor processEnglish = null;
    AlignProcessor processDigit = null;
    CombinePostProcessor combinePostProcessor = null;

    public WordSegmenter(String propertiesFilename, String charset) throws IOException {
        Properties properties = new Properties();
        Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(propertiesFilename), charset));
        properties.load(reader);
        String textModelFilename = properties.getProperty("text_model_filename");
        String textModelCharset = properties.getProperty("text_model_file_charset", "UTF-8");
        boolean textModelIsGZipped = Boolean.parseBoolean(properties.getProperty("text_model_is_gzipped", "true"));

        // 是否进入调试模式
        debug = Boolean.parseBoolean(properties.getProperty("debug", "false"));
        // 是否使用字符类型特征，默认使用
        useCharType = Boolean.parseBoolean(properties.getProperty("use_char_type_feature", "true"));
        // 是否使用word2vec特征
        if (properties.getProperty("use_word2vec_feature") != null)
            wordToVectorFeature = new WordToVectorFeature(properties.getProperty("use_word2vec_feature"));
        // 是否使用前后向熵特征
        if (properties.getProperty("use_entropy_feature") != null)
            entropyFeature = new EntropyFeature(properties.getProperty("use_entropy_feature"));
        // 是否使用Lexical特征
        if (properties.getProperty("use_lexical_feature") != null)
            lexicalFeature = new LexicalFeature(properties.getProperty("use_lexical_feature"));
        // 是否使用AV特征
        if (properties.getProperty("use_av_feature") != null)
            accessorVariety = new AccessorVariety(properties.getProperty("use_av_feature"));
        // 是否使用Feng AV
        if (properties.getProperty("use_fengav_feature") != null)
            fengAccessorVariety = new FengAccessorVariety(properties.getProperty("use_fengav_feature"));
        // 是否使用后处理
        usePostprocess = Boolean.parseBoolean(properties.getProperty("use_postprocess", "true"));
        // 包括以下几个部分
        if (usePostprocess) {
            // 对Token进行合并看看能不能在词典里面找到
            combinePostProcessor = new CombinePostProcessor(properties);
            processUrl = new ProcessUrl();
            processEnglish = new ProcessEnglish();
            processDigit = new ProcessDigit();
        }

        // 输出调试信息，告诉用户我们都使用了哪些特征
        if (debug)
            outputFeatureDebugInfo();

        File textModelFile = new File(textModelFilename);
        tagger = new CRFTagger(textModelFile, textModelIsGZipped, textModelCharset);
        preprocessor = new BaselinePreprocessor();
    }

    public WordSegmenter(String propertiesFilename) throws IOException {
        this(propertiesFilename, "UTF-8");
    }

    /**
     * 输出当前使用哪些特征的信息
     */
    private void outputFeatureDebugInfo() {
        if (useCharType)
            System.out.println("Using Char Type Feature");
        if (wordToVectorFeature != null)
            System.out.println("Using Word2vec Feature");
        if (entropyFeature != null)
            System.out.println("Using Forward and Backward Entropy Feature");
        if (lexicalFeature != null)
            System.out.println("Using Lexical Feature");
        if (accessorVariety != null)
            System.out.println("Using Accessor Variety N-gram Feature");
    }

    private List<NorToken> crfTag(String context, List<Token> preTokens, int offset, String[] tags) {
        List<NorToken> postTokens = new ArrayList<NorToken>();
        StringBuilder textSB = new StringBuilder();
        int beginIndex = -1;
        for (int index = 0; index < tags.length; index++) {
            String tag = tags[index];

            NorToken preToken = (NorToken) preTokens.get(offset + index);

            if (tag.equals("B")) {
                if (textSB.length() > 0) {
                    postTokens.add(new NorToken(textSB.toString(), context, beginIndex, beginIndex + textSB.length(), NorToken.Attr.UNKNOW));
                    textSB.setLength(0);
                }
                if (textSB.length() == 0) {
                    beginIndex = preToken.getBeginIndex();
                }
                textSB.append(preToken.getText());
            } else if (tag.equals("M") || tag.equals("B2") || tag.equals("B3")) {
                if (textSB.length() == 0) {
                    beginIndex = preToken.getBeginIndex();
                }
                textSB.append(preToken.getText());
            } else if (tag.equals("E")) {
                textSB.append(preToken.getText());
                NorToken postToken = new NorToken(textSB.toString(), context, beginIndex, beginIndex + textSB.length(), NorToken.Attr.UNKNOW);
                postTokens.add(postToken);
                textSB.setLength(0);
            } else if (tag.equals("S")) {
                if (textSB.length() > 0) {
                    postTokens.add(new NorToken(textSB.toString(), context, beginIndex, beginIndex + textSB.length(), NorToken.Attr.UNKNOW));
                    textSB.setLength(0);
                } else {
                    beginIndex = preToken.getBeginIndex();
                    postTokens.add(new NorToken(textSB.toString(), context, beginIndex, beginIndex + 1, NorToken.Attr.UNKNOW));
                }
            }
        }

        // 可能执行结束还有一个Token没有加进去
        if (textSB.length() > 0) {
            postTokens.add(new NorToken(textSB.toString(), context, beginIndex, beginIndex + textSB.length(), NorToken.Attr.UNKNOW));
            textSB.setLength(0);
        }

        return postTokens;
    }

    private List<NorToken> bambooCrfTag(String context, List<Token> preTokens, int offset, String[] tags) {
        List<NorToken> postTokens = new ArrayList<NorToken>();
        StringBuilder textSB = new StringBuilder();
        int beginIndex = -1;
        for (int index = 0; index < tags.length; index++) {
            String tag = tags[index];
            NorToken preToken = (NorToken) preTokens.get(offset + index);
            if (textSB.length() == 0) {
                beginIndex = preToken.getBeginIndex();
            }
            textSB.append(preToken.getText());

            if (tag.equals(END_TAG) || tag.equals(SINGLE_TAG)) {
                NorToken postToken = new NorToken(textSB.toString(), context, beginIndex,
                        beginIndex + textSB.length(), preToken.getAttr());
                postTokens.add(postToken);
                textSB.setLength(0);
            }
        }

        // 可能执行结束还有一个Token没有加进去
        if (textSB.length() > 0) {
            postTokens.add(new NorToken(textSB.toString(), context, beginIndex,
                    beginIndex + textSB.length(), NorToken.Attr.UNKNOW));
            textSB.setLength(0);
        }

        return postTokens;
    }

    public List<Token> segment(String text) {
        // 去掉首尾的空白
        text = CharMatcher.WHITESPACE.trimFrom(text);

        if (text == null || text.length() == 0) {
            return Collections.emptyList();
        }

        List<Token> preTokens = preprocessor.process(text);
        List<Token> tokens = new ArrayList<Token>();

        List<NorToken> senten = new ArrayList<NorToken>();
        List<ArrayList<String>> sentenFeature = new ArrayList<ArrayList<String>>();

        int offset = 0;
        int i = 0;
        for (; i < preTokens.size(); ++i) {
            NorToken token = (NorToken) preTokens.get(i);
            boolean append = token.getAttr() != NorToken.Attr.WHITESPACE;
            if (append) {
                senten.add(token);
                // 求这个Token的特征
                ArrayList<String> tokenFeature = new ArrayList<String>();
                tokenFeature.add(token.getNorText());
                if (useCharType)
                    tokenFeature.add(BaselinePreprocessor.getTypeStr(token.getAttr()));
                if (wordToVectorFeature != null)
                    tokenFeature.add(wordToVectorFeature.
                            queryWordToVecType(token.getNorText()));
                if (entropyFeature != null) {
                    tokenFeature.add(String.valueOf(entropyFeature.
                            getForwardEntropy(token.getNorText().charAt(0))));
                    tokenFeature.add(String.valueOf(entropyFeature.
                            getBackwardEntropy(token.getNorText().charAt(0))));
                }
                sentenFeature.add(tokenFeature);
            }
            char firstChar = token.getText().charAt(0);
            // 段句的时候根据空白还有，。；？！
            if (token.getAttr() == NorToken.Attr.WHITESPACE
                    || firstChar == '，' || firstChar == '！' || firstChar == '？'
                    || firstChar == '；' || firstChar == '。') {
                List<String[]> features = getSentenLevelFeature(sentenFeature, senten);
                String[] tags = tagger.tag(features);
                if (debug)
                    printFeaturesTags(features, tags);
                offset = i - features.size() + (append ? 1 : 0);
                tokens.addAll(bambooCrfTag(text, preTokens, offset, tags));
                // 清空数据
                senten.clear();
                sentenFeature.clear();
            }
        }
        if (!senten.isEmpty()) {
            List<String[]> features = getSentenLevelFeature(sentenFeature, senten);
            String[] tags = tagger.tag(features);
            if (debug)
                printFeaturesTags(features, tags);
            offset = i - features.size();
            tokens.addAll(bambooCrfTag(text, preTokens, offset, tags));
        }

        // 是否使用后处理
        if (usePostprocess) {
            List<Token> out = new ArrayList<Token>(tokens.size());
            combinePostProcessor.process(tokens, out);
            processUrl.process(text, out);
            processEnglish.process(text, out);
            processDigit.process(text, out);
            return out;
        } else {
            return tokens;
        }
    }

    private void printFeaturesTags(List<String[]> features, String[] tags) {
        Joiner joiner = Joiner.on("\t");
        for (int i = 0; i < features.size(); i++) {
            System.out.println(joiner.join(features.get(i)) + "\t" + tags[i]);
        }
        System.out.println();
    }

    /**
     * 计算句子级别的特征，有些特征要得到整个句子之后才可以计算
     * <ul>
     * <li>Lexical特征: L_{begin}(C_0)和L_{end}(C_0)</li>
     * <li>AV特征</li>
     * </ul>
     * @param sentenFeature 存储句子里面每个Token的特征
     * @param senten 整个句子包含的Token
     * @return 句子中每个Token的特征
     */
    private ArrayList<String[]> getSentenLevelFeature(
            List<ArrayList<String>> sentenFeature, List<NorToken> senten) {
        // 是否需要添加词典特征
        if (lexicalFeature != null) {
            List<String[]> lexicalFeatures = lexicalFeature.getFeature(senten);
            // lexical特征
            for (int senIndex = 0; senIndex < senten.size(); senIndex++) {
                String[] tempFeatures = lexicalFeatures.get(senIndex);
                // 这个特征有两列
                sentenFeature.get(senIndex).add(tempFeatures[0]);
                sentenFeature.get(senIndex).add(tempFeatures[1]);
            }
        }

        // 是否需要AV特征
        if (accessorVariety != null) {
            // ngram从[1~5]
            for (int n = 1; n <= ProduceTrainData.MAXIUM_N_GRAM; n++) {
                int[] ngramFeature = accessorVariety.sentenNgramFeature(n, senten);
                for (int i = 0; i < ngramFeature.length; i++) {
                    sentenFeature.get(i).add(String.valueOf(ngramFeature[i]));
                }
            }
        }
        // 是否需要Feng AV特征
        if (fengAccessorVariety != null) {
            // ngram从[1~5]
            for (int n = 1; n <= ProduceTrainData.MAXIUM_N_GRAM; n++) {
                int[] ngramFeature = fengAccessorVariety.sentenNgramFeature(n, senten);
                for (int i = 0; i < ngramFeature.length; i++) {
                    sentenFeature.get(i).add(String.valueOf(ngramFeature[i]));
                }
            }
        }

        // 把整个句子的特征转成CRF Tagger需要的格式
        ArrayList<String[]> features = new ArrayList<String[]>();
        for (int senIndex = 0; senIndex < senten.size(); senIndex++) {
            List<String> tokenFeature = sentenFeature.get(senIndex);
            features.add(tokenFeature.toArray(new String[tokenFeature.size()]));
        }
        return features;
    }

    /**
     * 读入整个文件进行切分
     * @param inFile 输入的文件
     * @param outFile 输出的文件
     */
    public void segmentFile(File inFile, File outFile) throws IOException {
        String line = "";
        BufferedReader br = new BufferedReader(new FileReader(inFile));
        PrintWriter pw = new PrintWriter(outFile);
        while ((line = br.readLine()) != null) {
            try {
                List<Token> tokens = this.segment(line);
                for (Token token : tokens) {
                    pw.print(token.toString() + " ");
                }
            }
            catch (StringIndexOutOfBoundsException exp) {
                System.out.println(line);
            }
            pw.println();
        }
        pw.close();
        br.close();
    }

    /**
     * 对文件夹下的所有文件，挨个进行切分，结果输出到另一个文件夹，文件名字一样
     * @param inDirectory 待切分的目录
     * @param outDirectory 存储结果的目录
     * @throws IOException
     */
    public void segmentDirectory(File inDirectory, File outDirectory) throws IOException {
        Iterator<File> fileIterator = FileUtils.iterateFiles(inDirectory, null, false);
        // 如果输出目录不存在则创建
        if (!outDirectory.exists())
            outDirectory.mkdir();

        while (fileIterator.hasNext()) {
            File inFile = fileIterator.next();
            System.out.println(inFile.getName());
            segmentFile(inFile, new File(outDirectory, inFile.getName()));
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Usage: ChineseSegmentor <config_file> <test_directory> <result_directory>");
            System.exit(1);
        }
        WordSegmenter baseChineseSegmentor = new WordSegmenter(args[0]);
        baseChineseSegmentor.segmentDirectory(new File(args[1]), new File(args[2]));
    }
}

