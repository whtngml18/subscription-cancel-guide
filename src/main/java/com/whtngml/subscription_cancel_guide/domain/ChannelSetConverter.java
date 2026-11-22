package com.whtngml.subscription_cancel_guide.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 해지하는 곳 집합을 DB 글 한 칸("APP,WEB")으로 저장한다.
 * 따로 표를 두지 않아 목록을 불러올 때 추가 조회가 생기지 않는다.
 * 저장 순서는 항상 enum 순서(APP → WEB → PHONE → OTHER), 빈 집합은 null.
 */
@Converter
public class ChannelSetConverter implements AttributeConverter<Set<CancelChannel>, String> {

    @Override
    public String convertToDatabaseColumn(Set<CancelChannel> channels) {
        if (channels == null || channels.isEmpty()) {
            return null;
        }
        return EnumSet.copyOf(channels).stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }

    @Override
    public Set<CancelChannel> convertToEntityAttribute(String text) {
        EnumSet<CancelChannel> channels = EnumSet.noneOf(CancelChannel.class);
        if (text == null || text.isBlank()) {
            return channels;
        }
        Arrays.stream(text.split(","))
                .map(String::strip)
                .forEach(name -> Arrays.stream(CancelChannel.values())
                        .filter(c -> c.name().equals(name))
                        .findFirst()
                        .ifPresent(channels::add));   // 모르는 이름은 버린다
        return channels;
    }
}
