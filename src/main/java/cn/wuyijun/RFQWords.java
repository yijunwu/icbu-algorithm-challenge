package cn.wuyijun;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;

/**
 * 算法思想：用最简单直接的方法实现（步骤见代码），确保正确性，易于理解。
 * 使用Stream API和函数式编程增强代码可读性，使用parallel stream进行多线程并行处理，代码简单。
 */
public class RFQWords implements IRFQAnalyse {

    @Override
    public void doJob(String rfqFilePath, String dicFilePath, String resultCSVFilePath) throws IOException {
        long startTime = System.currentTimeMillis();
        //读取RFQ文件，分割成句子
        AtomicReference<List<Object>> sentences = new AtomicReference<>(null);
        new Thread(() -> {
            try { String rfqContent = Files.readString(Paths.get(rfqFilePath));
                sentences.set(Collections.list(new StringTokenizer(rfqContent, ",")));
            } catch (Exception e) { throw new RuntimeException(e); }
        }).start();
        System.out.println("Time elapsed 1 " + (System.currentTimeMillis() - startTime));

        //构建词典hash map
        String dictContent = Files.readString(Paths.get(dicFilePath));
        System.out.println("Time elapsed 2 " + (System.currentTimeMillis() - startTime));
        Set<String> phrases = Collections.list(new StringTokenizer(dictContent, ","))
                .stream()
                .map(s -> ((String)s).trim())
                .collect(toCollection(() -> new HashSet<>(523_001)));
        System.out.println("Time elapsed 3 " + (System.currentTimeMillis() - startTime));

        //统计词典中词组最多包含几个单词
        int maxWordsLen = phrases.parallelStream()
                .map(s -> new StringTokenizer(s, " ").countTokens())
                .reduce(Integer::max).orElse(0);
        System.out.println("Time elapsed 4 " + (System.currentTimeMillis() - startTime));

        //对RFQ文件中的每个句子，统计词典中词组出现的次数
        Map<String, AtomicInteger> resultMap = new ConcurrentHashMap<>();
        while (sentences.get() == null) { Thread.yield(); }
        System.out.println("Time elapsed 5 " + (System.currentTimeMillis() - startTime));

        sentences.get().parallelStream()
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
        System.out.println("Time elapsed 6 " + (System.currentTimeMillis() - startTime));
        //写结果
        ArrayList<Map.Entry<String, AtomicInteger>> entries = new ArrayList<>(resultMap.entrySet());
        Stream<CharSequence> stream = IntStream.range(0, entries.size() * 4).mapToObj(i -> {
            switch (i % 4) {
                case 0:
                    return entries.get(i / 4).getKey();
                case 1:
                    return ",";
                case 2:
                    return entries.get(i / 4).getValue().toString();
                case 3:
                    return System.lineSeparator();
                default:
                    return "";
            }
        });
//        String resultStr = resultMap.entrySet().stream()
//                .map(e -> new StringBuilder(e.getKey()).append(",").append(e.getValue()).append(System.lineSeparator()))
//                .collect(joining());
        System.out.println("Time elapsed 7 " + (System.currentTimeMillis() - startTime));

        Files.write(Paths.get(resultCSVFilePath), stream::iterator);
        System.out.println("Time elapsed 8 " + (System.currentTimeMillis() - startTime));
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