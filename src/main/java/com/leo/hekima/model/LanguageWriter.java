package com.leo.hekima.model;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class LanguageWriter implements Converter<Language, Integer> {

    @Override
    public Integer convert(Language e) {
        if(e == null) {
            return null;
        }
        return e.ordinal();
    }
}