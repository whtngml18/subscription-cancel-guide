package com.whtngml.subscription_cancel_guide.domain;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChannelSetConverterTest {

    private final ChannelSetConverter converter = new ChannelSetConverter();

    @Test
    void emptyIsNull() {
        assertNull(converter.convertToDatabaseColumn(EnumSet.noneOf(CancelChannel.class)));
        assertNull(converter.convertToDatabaseColumn(null));
        assertEquals(EnumSet.noneOf(CancelChannel.class), converter.convertToEntityAttribute(null));
        assertEquals(EnumSet.noneOf(CancelChannel.class), converter.convertToEntityAttribute(""));
    }

    @Test
    void savesInEnumOrder() {
        assertEquals("APP,WEB", converter.convertToDatabaseColumn(
                new LinkedHashSet<>(List.of(CancelChannel.WEB, CancelChannel.APP))));
        assertEquals(EnumSet.of(CancelChannel.APP, CancelChannel.PHONE), converter.convertToEntityAttribute("PHONE,APP"));
    }

    @Test
    void ignoresUnknownNames() {
        assertEquals(EnumSet.of(CancelChannel.WEB), converter.convertToEntityAttribute("WEB,FAX"));
    }
}
