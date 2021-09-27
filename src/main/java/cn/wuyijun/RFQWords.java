package cn.wuyijun;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;

/**
 * 算法思想：用最简单直接的方法实现（步骤见代码），确保正确性，易于理解。
 * 使用Stream API和函数式编程增强代码可读性，使用parallel stream进行多线程并行处理，代码简单。
 */
public class RFQWords implements IRFQAnalyse {

    @Override
    public void doJob(String rfqFilePath, String dicFilePath, String resultCSVFilePath) throws IOException {
        //读取RFQ文件，分割成句子
        AtomicReference<List<Object>> sentences = new AtomicReference<>(null);
        new Thread(() -> {
            try { CustomString rfqContent = new CustomString(Files.readString(Paths.get(rfqFilePath)));
                sentences.set(Collections.list(new CustomStringTokenizer(rfqContent, ",")));
            } catch (IOException e) { sentences.set(emptyList()); }
        }).start();

        //构建词典hash map
        CustomString dictContent = new CustomString(Files.readString(Paths.get(dicFilePath)));
        Set<CustomString> phrases = Collections.list(new CustomStringTokenizer(dictContent, ","))
                .stream().map(s -> ((CustomString)s).trim())
                .collect(toCollection(() -> new HashSet<>(523_001)));

        //统计词典中词组最多包含几个单词
        int maxWordsLen = phrases.parallelStream()
                .map(s -> new CustomStringTokenizer(s, " ").countTokens())
                .reduce(Integer::max).orElse(0);

        //对RFQ文件中的每个句子，统计词典中词组出现的次数
        Map<String, AtomicInteger> resultMap = new ConcurrentHashMap<>();
        while (sentences.get() == null) { Thread.yield(); }

        sentences.get().parallelStream()
                .map(s -> ((CustomString)s).trim().toLowerCase())
                .map(sentence -> Collections.list(new CustomStringTokenizer(sentence, " ")))
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
        String resultStr = resultMap.entrySet().stream()
                .map(e -> e.getKey() + "," + e.getValue())
                .collect(joining(System.lineSeparator()));
        Files.writeString(Paths.get(resultCSVFilePath), resultStr);
    }

    public static void main(String[] args) throws IOException {
        long beforeUsedMem=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
        IRFQAnalyse rfqAnalyse = new RFQWords();
        String dicFilePath = "D:\\Work\\Dictionary_100M.txt";
        String rfqFilePath = "D:\\Work\\RFQInput_100M.txt";
        String outputFilePath = "D:\\Work\\RFQOutput.txt";

        long start = System.currentTimeMillis();
        rfqAnalyse.doJob(rfqFilePath, dicFilePath, outputFilePath);
        System.out.println("Time elapsed: " + (System.currentTimeMillis() - start));

        long afterUsedMem=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
        long actualMemUsed=afterUsedMem-beforeUsedMem;

        System.out.println("Memory used: " + (actualMemUsed));
    }
}