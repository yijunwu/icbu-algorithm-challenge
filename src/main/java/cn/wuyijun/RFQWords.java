package cn.wuyijun;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 算法思想：用最简单直接的方法实现（步骤见代码），确保正确性，易于理解。
 * 使用Stream API和函数式编程增强代码可读性，使用parallel stream进行多线程并行处理，代码简单。
 */
public class RFQWords implements IRFQAnalyse {
    public static Pattern PATTERN_COMMA = Pattern.compile(",");
    public static Pattern PATTERN_SPACE = Pattern.compile(" ");
    public static Pattern PATTERN_DELIMITER = Pattern.compile("[^a-zA-Z0-9 ]");

    @Override
    public void doJob(String rfqFilePath, String dicFilePath, String resultCSVFilePath) {
        try {
            //构建词典hash map
            String dictContent = new String(Files.readAllBytes(Paths.get(dicFilePath)));
            Set<String> phrases = Arrays.stream(PATTERN_COMMA.split(dictContent)).parallel()
                    .map(String::trim)
                    .filter(a -> !a.isEmpty())
                    .collect(Collectors.toCollection(HashSet::new));

            //统计词典中词组最多包含几个单词
            int maxWordLen = phrases.parallelStream().map(s -> PATTERN_SPACE.split(s).length)
                    .reduce(Integer::max).get();
            System.gc();

            //读取RFQ文件，分割成句子
            String rfqContent = new String(Files.readAllBytes(Paths.get(rfqFilePath)));
            String[] sentences = PATTERN_DELIMITER.split(rfqContent);

            //对RFQ文件中的每个句子，统计词典中词组出现的次数
            Map<String, AtomicInteger> resultMap = new ConcurrentHashMap<>();
            Arrays.stream(sentences).parallel()
                    .map(s -> s.trim().toLowerCase())
                    .filter(s -> !s.isEmpty())
                    .map(sentence -> Arrays.stream(PATTERN_SPACE.split(sentence)).filter(s -> !s.isEmpty())
                            .collect(Collectors.toList()))
                    .forEach(sentence -> IntStream.range(0, sentence.size()).forEach(i -> {
                        StringBuilder builder = new StringBuilder();
                        for (int j = 0; j < maxWordLen && i + j < sentence.size(); j++) {
                            builder.append(sentence.get(i + j));
                            String string = builder.toString();
                            builder.append(" ");

                            if (phrases.contains(string)) {
                                resultMap.computeIfAbsent(string, key -> new AtomicInteger(0)).incrementAndGet();
                            }
                        }
                    }));

            //写结果
            StringBuilder resultBuilder = new StringBuilder();
            resultMap.forEach((key, value) ->
                    resultBuilder.append(key).append(",").append(value).append(System.lineSeparator())
            );
            Files.writeString(Paths.get(resultCSVFilePath), resultBuilder.toString());
        } catch (Exception e) {
            throw new RuntimeException("Exception encountered: " + e.getMessage(), e);
        }
    }

    public static void main(String[] args) throws IOException {
        IRFQAnalyse rfqAnalyse = new RFQWords();
        String dicFilePath = "D:\\Work\\Dictionary_100M.txt";
        String rfqFilePath = "D:\\Work\\RFQInput_100M.txt";
        String outputFilePath = "D:\\Work\\RFQOutput.txt";

        rfqAnalyse.doJob(rfqFilePath, dicFilePath, outputFilePath);
    }
}