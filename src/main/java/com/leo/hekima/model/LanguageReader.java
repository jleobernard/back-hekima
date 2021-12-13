package com.leo.hekima.model;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

import java.util.function.Function;

@ReadingConverter
public class LanguageReader implements Converter<Integer, Language> {

    @Override
    public Language convert(Integer source) {
        if(source == null) {
            return null;
        }
        return Language.values()[source];
    }
}