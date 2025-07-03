package ru.strategy48.ejudge;

import ru.strategy48.ejudge.util.CSVUtils;
import ru.strategy48.ejudge.util.JSONUtils;

import java.io.File;
import java.net.URI;

public class Test {
    public static void main(String[] args) throws Exception {
        JSONUtils.getDOMJudgeContest(URI.create("https://domjudge.nec.algocode.ru/api/contests/4/"), null, 123);
    }
}
