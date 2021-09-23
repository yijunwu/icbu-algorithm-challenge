package cn.wuyijun;

import java.io.IOException;

/**
 * Created by tanke.wyj on 2017/6/15.
 */
public interface IRFQAnalyse {
    void doJob(String rfqContentFilePath, String wordsFilePath, String resultCSVFilePath) throws IOException;
}
