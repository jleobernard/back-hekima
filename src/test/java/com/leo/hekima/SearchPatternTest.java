package com.leo.hekima;

import com.leo.hekima.subs.SearchPattern;
import com.leo.hekima.subs.Sentence;
import com.leo.hekima.subs.SentenceElement;
import com.leo.hekima.subs.SubsService;
import com.leo.hekima.to.SubsSearchRequest;
import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SearchPatternTest {
    private final Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);
    @Test
    public void testToNormalSentence() {
        final SubsSearchRequest request = fromString("저+는+ +학생+입니다");
        final List<SentenceElement> sentence = SearchPattern.toSentences(request, komoran).get(0).elements();
        assertEquals(5, sentence.size());
        for (SentenceElement sentenceElement : sentence) {
            final var values = sentenceElement.value();
            assertTrue(values.isPresent());
        }
        int i = 0;
        assertEquals("저", sentence.get(i++).value().get());
        assertEquals("는", sentence.get(i++).value().get());
        assertEquals("학생", sentence.get(i++).value().get());
        assertEquals("이", sentence.get(i++).value().get());
        assertEquals("ㅂ니다", sentence.get(i++).value().get());
    }
    @Test
    public void testWithNounPlaceHolder() {
        final SubsSearchRequest request = fromString(":N+는+ +학생+입니다");
        final List<SentenceElement> sentence = SearchPattern.toSentences(request, komoran).get(0).elements();
        assertEquals(5, sentence.size());
        final SentenceElement placeholder = sentence.get(0);
        assertTrue(placeholder.value().isEmpty());
        assertTrue(placeholder.type().isPresent());
        assertEquals("N", placeholder.type().get());
        for (int i1 = 1; i1 < sentence.size(); i1++) {
            final var sentenceElement = sentence.get(i1);
            final var values = sentenceElement.value();
            assertTrue(values.isPresent());
        }
        int i = 1;
        assertEquals("는", sentence.get(i++).value().get());
        assertEquals("학생", sentence.get(i++).value().get());
        assertEquals("이", sentence.get(i++).value().get());
        assertEquals("ㅂ니다", sentence.get(i++).value().get());
    }
    @Test
    public void testWithVerbPlaceHolderAndAlternative() {
        final SubsSearchRequest request = fromString("저+는+ +학생+:V+ㄹ/을+까요");
        final List<Sentence> sentences = SearchPattern.toSentences(request, komoran);
        assertEquals(2, sentences.size());
        for (Sentence sentence : sentences) {
            List<SentenceElement> elements = sentence.elements();
            assertEquals(5, elements.size());
            assertEquals("저", elements.get(0).value().get());
            assertEquals("는", elements.get(1).value().get());
            assertEquals("학생", elements.get(2).value().get());
            assertEquals("V", elements.get(3).type().get());
            assertTrue(elements.get(3).value().isEmpty());
        }
        assertEquals("ㄹ까요", sentences.get(0).elements().get(4).value().get());
        assertEquals("을까요", sentences.get(1).elements().get(4).value().get());
    }
    @Test
    public void testWithVerbPlaceHolderAndAlternatives() {
        final SubsSearchRequest request = fromString(":N+는/은+ +학생+:V+ㄹ/을+까요");
        final List<Sentence> sentences = SearchPattern.toSentences(request, komoran);
        assertEquals(4, sentences.size());
        for (Sentence sentence : sentences) {
            List<SentenceElement> elements = sentence.elements();
            assertEquals(5, elements.size());
            assertEquals("N", elements.get(0).type().get());
            assertTrue(elements.get(0).value().isEmpty());
            assertEquals("학생", elements.get(2).value().get());
            assertEquals("V", elements.get(3).type().get());
            assertTrue(elements.get(3).value().isEmpty());
        }
        assertEquals("는", sentences.get(0).elements().get(1).value().get());
        assertEquals("는", sentences.get(1).elements().get(1).value().get());
        assertEquals("은", sentences.get(2).elements().get(1).value().get());
        assertEquals("은", sentences.get(3).elements().get(1).value().get());
        assertEquals("ㄹ까요", sentences.get(0).elements().get(4).value().get());
        assertEquals("을까요", sentences.get(1).elements().get(4).value().get());
        assertEquals("ㄹ까요", sentences.get(2).elements().get(4).value().get());
        assertEquals("을까요", sentences.get(3).elements().get(4).value().get());
    }

    public static SubsSearchRequest fromString(final String q) {
        return new SubsSearchRequest(q, false,
            SubsService.parseQueryElements(q),
            0f, 0f, false);
    }

}
