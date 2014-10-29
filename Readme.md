## 说明
本程序是我们参加[clp2014](http://www.cipsc.org.cn/clp2014/webpage/cn/home_cn.htm)
的中文切分任务的系统。

## 程序总体介绍

1. 生成训练语料
2. 训练分词模型
3. 利用模型进行切分

### 生成训练语料

1. 用`ProduceTrainData`产生CRF++训练文件
```
Usage: <main class> [options]
  Options:
    -av
       Count Accessor Variety feature from which raw file
    -fengav
       Count Feng Accessor Variety feature from which raw file
    -entropy
       Count forward and backward entropy from which raw filename
    -h
       Show help information
       Default: false
  * -i
       Input filename
    -lexical
       Count lexical feature from which dict file
  * -o
       Output filename
    -type
       Add Char Type Feature
       Default: true
    -word2vec
       Word2vec classes filename
```
* 程序使用的时候参数无顺序要求
* 各个特征的定义参考论文，忽略word2vec特征
* 默认情况下会使用字符类型特征
* 输入文件和输出文件的格式参考后面的例子

### 以clp2010年的数据为例说明如何使用
1. 程序使用的时候参数无顺序要求
1. 各个特征的定义参考论文，忽略word2vec特征
1. 默认情况下会使用字符类型特征
1. 输入文件和输出文件的格式参考后面的例子

### 以clp2010年的数据为例说明如何使用
语料在corpus.tar.gz压缩包里面，解压后就可以使用。
clp2010提供的训练语料在`./corpus/train/2010`里面
+ Training-Labelled.txt是官方提供的训练语料
+ Training-Unlabelled-A.txt是官方提供的A领域的无标语料
+ Training-Unlabelled-B.txt是官方提供的B领域的无标语料
+ 2010-train-unlabel-test.txt是我把官方提供的训练和测试语料合并之后去标注之后的语料

clp2010提供的测试语料总共有4个领域
+ `./corpus/test/2010Test`是测试语料
+ `./corpus/test/2010Golden`是标准答案

我们通过以下的命令就可以生成测试语料，抽取的特征包括
字符类型特征、条件熵特征、词典特征和AV特征
```
java -cp segment.jar com.nlp.segment.ProduceTrainData \
-i corpus/train/2010/Training-Labelled.txt \
-o corpus/crf/2010-chartype-entropy-lexical-av.txt \
-type true \
-entropy corpus/train/2010/2010-train-unlabel-test.txt \
-lexical corpus/webdict/webdict_with_freq.txt \
-av corpus/train/2010/2010-train-unlabel-test.txt
```
上面的命令会在`./corpus/crf/`目录下生成训练文件，
然后就可以用这个训练文件训练CRF模型，文件格式如下
```
~ head -n 6 2010-chartype-entropy-lexical-av.txt 
迈	C	2	4	2	0	5	3	0	0	0	B
向	C	5	4	0	2	9	0	0	0	0	E
充	C	2	4	2	0	6	5	0	2	2	B
满	C	4	4	0	2	7	0	0	0	0	E
希	C	1	4	2	0	5	7	3	0	0	B
望	C	4	2	0	2	7	4	0	0	0	E
```
文件的格式描述如下
1. 第1列是训练语料的一句话
2. 第2列是字符类型特征
3. 第3-4列是前、后向条件熵特征
4. 第5-6列分别是Lbegin(C)和Lend(c)
5. 第7-11列是AV特征
6. 最后一列是分词的tag set

### 训练分词模型
有了训练文件之后就可以写CRF++的特征模板进行模型训练，
模板文件参考`./corpus/crf/template`文件，用下面的命令训练模型
```
crf_learn -t -p 20 template 2010-chartype-entropy-lexical-av.txt 2010_chartype_entropy_av_model
```
这个`-t`选项的目的是生成文本格式的CRF++模型文件，我们的
JAVA程序只能读取文本格式CRF++模型文件，最后会得到2010_chartype_entropy_av_model.txt
的文本格式模型文件，通过gzip压缩后得到2010_chartype_entropy_av_model.txt.gz文件，
这样整个模型训练过程就算完成了。

### 调用模型进行切分
切分过程由`WordSegmenter`类完成，这个类会去读取一个配置文件，需要注意的是
需要确保配置文件中指定的特征数量和训练模型时用的特征数量一致
```
java -cp segment.jar com.nlp.segment.WordSegmenter config/2010_chartype_entropy_lexical_av.properties corpus/test/2010Test result
```
上述的命令会根据`config/2010_chartype_entropy_lexical_av.properties`文件的
配置来对`corpus/test/2010Test`里面的语料进行切分，最后输出到`result`文件夹。

## 致谢
[CRF++](https://code.google.com/p/crfpp/)
[nlpbamboo](https://code.google.com/p/nlpbamboo/)
[jsuffixarrays](https://github.com/carrotsearch/jsuffixarrays)
[commons-io](http://commons.apache.org/proper/commons-io/)
[guava-libraries](https://code.google.com/p/guava-libraries/)
[jcommander](http://jcommander.org/)
[nlpbamboo](https://code.google.com/p/nlpbamboo/)
