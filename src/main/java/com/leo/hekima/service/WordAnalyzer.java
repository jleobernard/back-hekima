package com.leo.hekima.service;

import com.google.common.collect.Sets;
import com.leo.hekima.model.Language;
import com.leo.hekima.model.Word;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.lucene.analysis.en.EnglishAnalyzer.ENGLISH_STOP_WORDS_SET;
import static org.apache.lucene.analysis.fr.FrenchAnalyzer.DEFAULT_ARTICLES;

@Service
public class WordAnalyzer {

    public static List<String> FRENCH_STOP_WORDS = newArrayList("au","aux","avec","ce","ces","dans","de","des","du","elle","en","et","eux","il","je","la","le","leur","lui","ma","mais","me","même","mes","moi","mon","ne","nos","notre","nous","on","ou","par","pas","pour","qu","que","qui","sa","se","ses","son","sur","ta","te","tes","toi","ton","tu","un","une","vos","votre","vous",
            "c","d","j","l","à","m","n","s","t","y","été","étée","étées","étés","étant","suis","es","est","sommes","êtes","sont","serai","seras","sera","serons","serez","seront","serais","serait","serions","seriez","seraient","étais","était","étions","étiez","étaient","fus","fut","fûmes","fûtes","furent","sois","soit","soyons","soyez","soient","fusse","fusses","fût","fussions","fussiez","fussent","ayant","eu","eue","eues","eus","ai","as","avons","avez","ont","aurai","auras","aura","aurons","aurez","auront","aurais","aurait","aurions","auriez","auraient","avais","avait","avions","aviez","avaient","eut","eûmes","eûtes","eurent","aie","aies","ait","ayons","ayez","aient","eusse","eusses","eût","eussions","eussiez","eussent","ceci","cela","celà","cet","cette","ici","ils","les","leurs","quel","quels","quelle","quelles","sans","soi");
    public static List<String> ENGLISH_STOP_WORDS = newArrayList("a", "an", "and", "are", "as", "at", "be", "but", "by",
            "for", "if", "in", "into", "is", "it",
            "no", "not", "of", "on", "or", "such",
            "that", "the", "their", "then", "there", "these",
            "they", "this", "to", "was", "will", "with");

    /**
     *
     * @param text Test to analyze
     * @return List of uniq words to be analyzed in the provided text
     */
    public Set<Word> getIndexableWords(final String text) {
        if(text == null) {
            return Collections.emptySet();
        }
        final String lowered = text.toLowerCase(Locale.FRENCH).trim();
        final Set<String> words = removeMarkupAndNonWords(lowered);
        removeUselessWords(words);
        return words.stream().map(w -> new Word(w, Language.FRENCH)).collect(Collectors.toSet());
    }

    private static void removeUselessWords(Set<String> words) {
        DEFAULT_ARTICLES.forEach(words::remove);
        ENGLISH_STOP_WORDS_SET.forEach(words::remove);
        FRENCH_STOP_WORDS.forEach(words::remove);
        ENGLISH_STOP_WORDS.forEach(words::remove);
    }

    private static Set<String> removeMarkupAndNonWords(String text) {
        final String sanitized = text.replaceAll("<.*?>", "")
            .replaceAll("[#@&\"'()\\[\\]?{}§_$€£%*+/\\\\=<>:.;,!]", " ");
        final String[] splitted = sanitized.split("\\s+");
        final Set<String> bagOfWords = Sets.newHashSet(splitted);
        bagOfWords.remove("");
        return bagOfWords;
    }
}
