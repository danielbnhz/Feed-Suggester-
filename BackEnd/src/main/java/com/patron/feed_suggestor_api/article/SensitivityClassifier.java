package com.patron.feed_suggestor_api.article;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

final class SensitivityClassifier {

    private record Term(int weight, Pattern pattern) {}

    private static final List<Term> TERMS = new ArrayList<>();

    private static void add(int weight, String... regexes) {
        for (String r : regexes) {
            TERMS.add(new Term(weight, Pattern.compile("(?i)\\b(?:" + r + ")\\b")));
        }
    }

    static {

        add(3,
                "rap(?:e|es|ed|ing)|rapists?",
                "sexual(?:ly)? (?:assault|abuse|violence)\\w*",
                "molest\\w*",
                "child (?:abuse|porn\\w*|exploitation)|pedophil\\w*|paedophil\\w*",
                "sex(?:ual)? traffick\\w*",
                "porn\\w*",
                "nsfw|xxx|onlyfans|sexually explicit|sex tape|erotic\\w*",
                "massacre[sd]?|genocide|ethnic cleansing",
                "behead\\w*|decapitat\\w*|dismember\\w*|mutilat\\w*",
                "tortur\\w*",
                "murder\\w*|homicide\\w*|manslaughter",
                "serial killers?|mass shootings?|school shootings?|mass (?:murder|killing)s?",
                "suicid\\w*|self[- ]harm",
                "shot dead|stabbed to death|beaten to death|bodies found|body found|human remains|corpses?");

        // 2 = medium: two together, or one in the title, filters
        add(2,
                "nudes?|naked|topless",
                "kill(?:ed|er|ers|ing|ings)|slain|slaying",
                "stabb(?:ed|ing)|stabbings?",
                "gunman|gunmen|gunfire|shootout",
                "bombings?|bombed|airstrikes?|air strikes?|missile strikes?|shelling|shelled",
                "hostages?|kidnap\\w*|abduct\\w*",
                "assault(?:ed|s)?|abus(?:e|ed|es|ing)|abusers?",
                "war crimes?|atrocit(?:y|ies)|executions?|executed|lynch\\w*",
                "death toll|fatalit(?:y|ies)|fatally");

        // 1 = weak context words
        add(1,
                "shooting|shootings|shot|wounded|bloody|bloodshed|gore|gory",
                "violence|violent|victims?|attack(?:ed|er|ers|s)?",
                "dead|deaths?|dying|assassinat\\w*",
                "sex|sexual\\w*|explicit");
    }

    private SensitivityClassifier() {}

    static int score(String title, String summary) {
        int total = 0;
        for (Term t : TERMS) {
            if (title != null && t.pattern().matcher(title).find()) total += t.weight() * 2;
            if (summary != null && t.pattern().matcher(summary).find()) total += t.weight();
        }
        return total;
    }
}