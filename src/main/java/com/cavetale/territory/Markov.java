package com.cavetale.territory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public final class Markov {
    private final Map<String, List<String>> chains = new HashMap<>();
    private final int order;
    private int maxLength = 0;

    public static void main(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(args[1]));
        Markov markov = new Markov(Integer.parseInt(args[0]));
        markov.scan(br);
        for (int i = 0; i < 20; i += 1) {
            System.out.println(markov.generate());
        }
    }

    public Markov(final int order) {
        this.order = order;
    }

    public void scan(String name) {
        if (maxLength < name.length()) {
            maxLength = name.length();
        }
        for (int toIndex = 0; toIndex <= name.length(); toIndex += 1) {
            final int fromIndex = Math.max(0, toIndex - order);
            String key = name.substring(fromIndex, toIndex);
            String tail = name.substring(toIndex, Math.min(name.length(), toIndex + 1));
            List<String> conts = chains.get(key);
            if (conts == null) {
                conts = new ArrayList<>();
                chains.put(key, conts);
            }
            if (!conts.contains(tail)) {
                conts.add(tail);
            }
        }
    }

    public void scan(BufferedReader reader) {
        reader.lines()
            .map(String::trim)
            .filter(it -> !it.isEmpty() && !it.startsWith("#"))
            .forEach(this::scan);
    }

    public String generate(Random random) {
        StringBuilder result = new StringBuilder("");
        while (result.length() < maxLength) {
            final int fromIndex = Math.max(0, result.length() - order);
            String key = result.substring(fromIndex, result.length());
            List<String> conts = chains.get(key);
            if (conts == null) break;
            String tail = conts.get(random.nextInt(conts.size()));
            if (tail.isEmpty()) break;
            result.append(tail);
        }
        return result.toString();
    }

    public String generate() {
        return generate(ThreadLocalRandom.current());
    }
}
