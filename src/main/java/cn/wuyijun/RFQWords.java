package cn.wuyijun;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.*;

/**
 * 算法思想：用最简单直接的方法实现（步骤见代码），确保正确性，易于理解。
 * 使用Stream API和函数式编程增强代码可读性，使用parallel stream进行多线程并行处理，代码简单。
 */
public class RFQWords implements IRFQAnalyse {
    byte commaAsByte = ",".getBytes()[0];
    byte spaceAsByte = " ".getBytes()[0];

    @Override
    public void doJob(String rfqFilePath, String dicFilePath, String resultCSVFilePath) throws IOException {
        //读取RFQ文件，分割成句子
        AtomicReference<List<List<String>>> sentences = new AtomicReference<>(null);

        new Thread(() -> {
            try { byte[] rfqContent = Files.readAllBytes(Paths.get(rfqFilePath));
                sentences.set(Collections.list(new ByteTwoLevelsTokenizer(rfqContent, commaAsByte, spaceAsByte))
                        .stream().map(p -> ((List<ByteBuffer>) p).stream().map(w -> US_ASCII.decode(w).toString().trim()).collect(toList()))
                        .collect(toList()));
                //sentences.set(Collections.list(new ByteTwoLevelsTokenizer(rfqContent, commaAsByte, spaceAsByte)));
            } catch (IOException e) { sentences.set(emptyList()); }
        }).start();

        while (sentences.get() == null) { Thread.yield(); }
        //构建词典hash map
        byte[] dictContent = Files.readAllBytes(Paths.get(dicFilePath));
        Set<List<String>> phrases = Collections.list(new ByteTwoLevelsTokenizer(dictContent, commaAsByte, spaceAsByte))
                .stream().map(p -> ((List<ByteBuffer>) p).stream().map(w -> US_ASCII.decode(w).toString().trim()).collect(toList()))
                .collect(toCollection(() -> new HashSet<>(523_001)));

        //统计词典中词组最多包含几个单词
        int maxWordsLen = phrases.parallelStream()
                .map(List::size)
                .reduce(Integer::max).orElse(0);

        //对RFQ文件中的每个句子，统计词典中词组出现的次数
        Map<List<String>, AtomicInteger> resultMap = new ConcurrentHashMap<>();
        while (sentences.get() == null) { Thread.yield(); }

        List<String> part = new ArrayList<>(maxWordsLen);
        for (int i = 0; i < sentences.get().size(); i ++) {
            List<String> words = sentences.get().get(i);
            if (i %1000 == 0) {
                System.out.println(i + ": " + System.currentTimeMillis());
            }

            IntStream.range(0, words.size()).forEach(start -> {
                for (int pos = 0; pos < maxWordsLen && start + pos < words.size(); pos++) {
                    part.add(words.get(start + pos));

                    if (phrases.contains(part)) {
                        resultMap.computeIfAbsent(part, key -> new AtomicInteger(0)).incrementAndGet();
                    }
                }
                part.clear();
            });
        }

        //写结果
        String resultStr = resultMap.entrySet().stream()
                .map(e -> e.getKey() + "," + e.getValue())
                .collect(joining(System.lineSeparator()));
        Files.writeString(Paths.get(resultCSVFilePath), resultStr);
    }

    public static void main(String[] args) throws IOException {
        long beforeUsedMem=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
        IRFQAnalyse rfqAnalyse = new RFQWords();
        String dicFilePath = "C:\\Work\\Dictionary_100M.txt";
        String rfqFilePath = "C:\\Work\\RFQInput_100M.txt";
        String outputFilePath = "C:\\Work\\RFQOutput.txt";

        long start = System.currentTimeMillis();
        rfqAnalyse.doJob(rfqFilePath, dicFilePath, outputFilePath);
        System.out.println("Time elapsed: " + (System.currentTimeMillis() - start));

        long afterUsedMem=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
        long actualMemUsed=afterUsedMem-beforeUsedMem;

        System.out.println("Memory used: " + (actualMemUsed));
    }
}