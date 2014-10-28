package com.nlp.segment;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.base.CharMatcher;
import com.nlp.segment.preprocess.BaselinePreprocessor;
import com.nlp.segment.feature.LexicalFeature;
import com.nlp.segment.preprocess.Preprocessor;
import com.nlp.segment.token.NorToken;
import com.nlp.segment.token.Token;
import com.nlp.unsupervised.AccessorVariety;
import com.nlp.segment.feature.EntropyFeature;
import com.nlp.segment.feature.WordToVectorFeature;
import com.nlp.unsupervised.FengAccessorVariety;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class FeatureOptions {
    @Parameter(names = "-type", description = "Add Char Type Feature", arity = 1)
    boolean useCharType = true;

    @Parameter(names = "-word2vec", description = "Word2vec classes filename", arity = 1)
    String word2vecFile;

    @Parameter(names = "-entropy", description = "Count forward and backward " +
            "entropy from which raw filename", arity = 1)
    String entropyFile;

    @Parameter(names = "-lexical", description = "Count lexical feature from which dict file", arity = 1)
    String lexicalFile;

    @Parameter(names = "-av", description = "Count Accessor Variety feature from which raw file", arity = 1)
    String accessorVarietyFile;

    @Parameter(names = "-fengav", description = "Count Feng Accessor Variety feature from which raw file", arity = 1)
    String fengAccessorVarietyFile;

    @Parameter(names = "-h", help = true, description = "Show help information")
    boolean help;

    @Parameter(names = "-i", description = "Input filename", required = true)
    String inputFilename;

    @Parameter(names = "-o", description = "Output filename", required = true)
    String outputFilename;
}


/**
 * 从原始语料生成CRF训练文件
 */
public class ProduceTrainData {
    // 是否采用逗号且分句子效果差不多，用逗号可以保证计算AV值时候的一致性
    protected Pattern punct = Pattern.compile("^(,+|\\?+|!+|;+|，+|。+|！+|？+|；+)$");
    // 是否使用字符类型特征
    private boolean useCharType;
    // word2vec特征
    private WordToVectorFeature wordToVectorFeature;
    // 前后向熵特征
    private EntropyFeature entropyFeature;
    // Lexical特征
    private LexicalFeature lexicalFeature;
    // 下面这两个无监督计算比较慢，吃内存，因为用到了后缀数组来加速计算
    public static final int MAXIUM_N_GRAM = 5;
    // AV特征
    private AccessorVariety accessorVariety;
    // Feng AV特征
    private FengAccessorVariety fengAccessorVariety;

    public ProduceTrainData(FeatureOptions options) throws IOException {
        // 是否使用字符类型特征
        useCharType = options.useCharType;

        // 使用word2vec特征
        if (options.word2vecFile != null)
            wordToVectorFeature = new WordToVectorFeature(options.word2vecFile);

        // 使用前后熵特征
        if (options.entropyFile != null)
            entropyFeature = new EntropyFeature(options.entropyFile);

        // Lexical特征
        if (options.lexicalFile != null)
            lexicalFeature = new LexicalFeature(options.lexicalFile);

        // AV特征
        if (options.accessorVarietyFile != null)
            accessorVariety = new AccessorVariety(options.accessorVarietyFile);

        // Feng AV特征
        if (options.fengAccessorVarietyFile != null)
            fengAccessorVariety = new FengAccessorVariety(options.fengAccessorVarietyFile);

        // 输入文件
        process(options.inputFilename, options.outputFilename, "UTF-8", "UTF-8");
    }

    public void process(String inputFilename, String outputFilename,
                        String inputCharset, String outputCharset) throws IOException {
        Preprocessor preprocessor = new BaselinePreprocessor();
        BufferedReader br = null;
        PrintWriter pw = null;

        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFilename), inputCharset));
            pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFilename), outputCharset));
            String line;
            int wordCount = 0, lineCount = 0;
            Matcher matcher;
            while ((line = br.readLine()) != null) {
                line = CharMatcher.WHITESPACE.trimAndCollapseFrom(CharMatcher.is('\uFEFF').trimFrom(line), ' ');
                StringTokenizer tokenizer = new StringTokenizer(line);
                if (!tokenizer.hasMoreTokens()) {
                    continue;
                }
                boolean hasToken = false;

                List<NorToken> senten = new ArrayList<NorToken>();
                List<String> tags = new ArrayList<String>();
                List<ArrayList<String>> sentenFeature = new ArrayList<ArrayList<String>>();

                while (tokenizer.hasMoreTokens()) {
                    String field = tokenizer.nextToken();
                    if (!hasToken) {
                        hasToken = true;
                    }
                    List<Token> tokens = preprocessor.process(field);
                    String tag, lastTag = null;
                    for (int i = 0; i < tokens.size(); ++i) {
                        NorToken token = (NorToken) tokens.get(i);
                        if (tokens.size() == 1) {
                            tag = "S";
                        } else if (i == 0) {
                            tag = "B";
                        } else if (i + 1 == tokens.size()) {
                            tag = "E";
                        } else if ("B".equals(lastTag)) {
                            tag = "B2";
                        } else if ("B2".equals(lastTag)) {
                            tag = "B3";
                        } else {
                            tag = "M";
                        }

                        // 往句子里面一个Token
                        senten.add(token);
                        // 这个Token对应的tag(B,B2,B3,M,E,S)
                        tags.add(tag);

                        // 一个Token的特征
                        ArrayList<String> tokenFeature = new ArrayList<String>();
                        tokenFeature.add(token.getNorText());  // 字符特征
                        // 是否需要添加字符类型特征
                        if (useCharType)
                            tokenFeature.add(BaselinePreprocessor.getTypeStr(token.getAttr()));
                        // 是否需要添加word2vec特征
                        if (wordToVectorFeature != null)
                            tokenFeature.add(wordToVectorFeature.queryWordToVecType(token.getNorText()));
                        // 是否添加熵特征
                        if (entropyFeature != null) {
                            tokenFeature.add(String.valueOf(entropyFeature.
                                    getForwardEntropy(token.getNorText().charAt(0))));
                            tokenFeature.add(String.valueOf(entropyFeature.
                                    getBackwardEntropy(token.getNorText().charAt(0))));
                        }
                        sentenFeature.add(tokenFeature);  // 加入这个Token的各种特征

                        lastTag = tag;
                    }
                    wordCount++;
                    matcher = punct.matcher(field);
                    if (matcher.matches()) {
                        sentenFeature = getSentenLevelFeature(sentenFeature, senten);
                        for (int senIndex = 0; senIndex < senten.size(); senIndex++) {
                            for (String tempFeature: sentenFeature.get(senIndex)) {
                                pw.print(tempFeature + "\t");
                            }
                            pw.println(tags.get(senIndex));
                        }
                        pw.println();
                        // 清空句子，特征和对应的tag
                        senten.clear();
                        sentenFeature.clear();
                        tags.clear();

                        hasToken = false;
                    }
                }
                if (hasToken) {
                    sentenFeature = getSentenLevelFeature(sentenFeature, senten);
                    for (int senIndex = 0; senIndex < senten.size(); senIndex++) {
                        for (String tempFeature: sentenFeature.get(senIndex)) {
                            pw.print(tempFeature + "\t");
                        }
                        pw.println(tags.get(senIndex));
                    }
                    pw.println();
                }
                lineCount++;
                if (lineCount % 1000 == 0) {
                    System.err.println("Record: " + lineCount);
                }
            }
            System.err.println("done!");
            System.err.println("#line " + lineCount + "; #word " + wordCount);
        } finally {
            if (br != null) {
                br.close();
            }
            if (pw != null) {
                pw.close();
            }
        }
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
    private List<ArrayList<String>> getSentenLevelFeature(
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
            for (int n = 1; n <= MAXIUM_N_GRAM; n++) {
                int[] ngramFeature = accessorVariety.sentenNgramFeature(n, senten);
                for (int i = 0; i < ngramFeature.length; i++) {
                    sentenFeature.get(i).add(String.valueOf(ngramFeature[i]));
                }
            }
        }
        // 是否需要Feng AV特征
        if (fengAccessorVariety != null) {
            // ngram从[1~5]
            for (int n = 1; n <= MAXIUM_N_GRAM; n++) {
                int[] ngramFeature = fengAccessorVariety.sentenNgramFeature(n, senten);
                for (int i = 0; i < ngramFeature.length; i++) {
                    sentenFeature.get(i).add(String.valueOf(ngramFeature[i]));
                }
            }
        }

        return sentenFeature;
    }

    public static void main(String[] args) throws IOException {
        FeatureOptions options = new FeatureOptions();

        try {
            JCommander jCommander = new JCommander(options, args);
            if (!options.help) {
                new ProduceTrainData(options);
            }
            else {
                jCommander.usage();
            }
        }
        catch (ParameterException exception) {
            String[] tempArgs = {"-h"};
            new JCommander(options, tempArgs).usage();
            System.exit(1);
        }
    }
}
