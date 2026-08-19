package edu.northeastern.mellow.data.util;

import java.util.Random;

public class UsernameGenerator {

    private static final String[] ADJECTIVES = {
            "calm", "bright", "gentle", "cozy", "swift", "sunny", "happy", "fuzzy",
            "bouncy", "silly", "sleepy", "snappy", "jolly", "zesty", "mellow",
            "fluffy", "cheeky", "peppy", "quirky", "breezy"
    };

    private static final String[] FRUITS = {
            "mango", "kiwi", "peach", "plum", "lemon", "fig", "pear", "melon",
            "guava", "papaya", "lychee", "grape", "cherry", "lime", "berry",
            "apricot", "coconut", "dragonfruit", "starfruit", "jackfruit"
    };

    private UsernameGenerator() {}

    /** Generates a username like "happykiwi42" — lowercase, no spaces. */
    public static String generate() {
        Random random = new Random();
        String adj   = ADJECTIVES[random.nextInt(ADJECTIVES.length)];
        String fruit = FRUITS[random.nextInt(FRUITS.length)];
        int num      = random.nextInt(900) + 100; // 100–999
        return adj + fruit + num;
    }
}
