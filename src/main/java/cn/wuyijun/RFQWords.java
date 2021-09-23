package cn.wuyijun;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;

/**
 * 算法思想：用最简单直接的方法实现（步骤见代码），确保正确性，易于理解。
 * 使用Stream API和函数式编程增强代码可读性，使用parallel stream进行多线程并行处理，代码简单。
 */
public class RFQWords implements IRFQAnalyse {

    @Override
    public void doJob(String rfqFilePath, String dicFilePath, String resultCSVFilePath) throws IOException {
        //构建词典hash map
        String dictContent = new String(Files.readAllBytes(Paths.get(dicFilePath)));
        Set<String> phrases = Collections.list(new StringTokenizer(dictContent, ","))
                .parallelStream()
                .map(s -> ((String)s).trim())
                .collect(toCollection(ConcurrentHashMap::newKeySet));

        //统计词典中词组最多包含几个单词
        int maxWordsLen = phrases.parallelStream()
                .map(s -> new StringTokenizer(s, " ").countTokens())
                .reduce(Integer::max).orElse(0);

        //读取RFQ文件，分割成句子
        String rfqContent = new String(Files.readAllBytes(Paths.get(rfqFilePath)));
        List<Object> sentences = Collections.list(new StringTokenizer(rfqContent, ","));

        //对RFQ文件中的每个句子，统计词典中词组出现的次数
        Map<String, AtomicInteger> resultMap = new ConcurrentHashMap<>();
        sentences.parallelStream()
                .map(s -> ((String)s).trim().toLowerCase())
                .map(sentence -> Collections.list(new StringTokenizer(sentence, " ")))
                .forEach(words -> IntStream.range(0, words.size()).forEach(start -> {
                    StringBuilder builder = new StringBuilder();
                    for (int pos = 0; pos < maxWordsLen && start + pos < words.size(); pos ++) {
                        builder.append(words.get(start + pos));
                        String part = builder.toString();
                        builder.append(" ");

                        if (phrases.contains(part)) {
                            resultMap.computeIfAbsent(part, key -> new AtomicInteger(0)).incrementAndGet();
                        }
                    }
                }));

        //写结果
        String resultStr = resultMap.entrySet().parallelStream()
                .map(e -> e.getKey() + "," + e.getValue())
                .collect(joining(System.lineSeparator()));
        Files.writeString(Paths.get(resultCSVFilePath), resultStr);
    }

    public static void main(String[] args) throws IOException {
        IRFQAnalyse rfqAnalyse = new RFQWords();
        String dicFilePath = "D:\\Work\\Dictionary_100M.txt";
        String rfqFilePath = "D:\\Work\\RFQInput_100M.txt";
        String outputFilePath = "D:\\Work\\RFQOutput.txt";

        long start = System.currentTimeMillis();
        rfqAnalyse.doJob(rfqFilePath, dicFilePath, outputFilePath);
        System.out.println("Time elapsed: " + (System.currentTimeMillis() - start));
    }
}