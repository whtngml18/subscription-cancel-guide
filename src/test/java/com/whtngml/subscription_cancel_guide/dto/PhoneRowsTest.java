package com.whtngml.subscription_cancel_guide.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhoneRowsTest {

    @Test
    void 메뉴_나누기는_빈줄과_공백과_CR을_뺀다() {
        assertEquals(List.of("프로필", "알림"), PhoneRows.splitMenus(" 프로필 \r\n\r\n알림\n"));
        assertEquals(List.of(), PhoneRows.splitMenus(null));
    }

    @Test
    void 위치별_강조_자리() {
        assertTrue(PhoneRows.of("top-left", "해지", null).get(0).target());
        assertTrue(PhoneRows.of("top-right", "해지", null).get(0).target());
        assertTrue(PhoneRows.of("middle", "해지", null).get(2).target());
        assertTrue(PhoneRows.of("bottom", "해지", null).get(4).target());
        assertTrue(PhoneRows.of(null, "해지", null).get(2).target());
    }

    @Test
    void 메뉴가_없으면_강조줄만_있고_나머지는_회색() {
        List<PhoneRows.PhoneRow> rows = PhoneRows.of("middle", "해지", null);
        assertEquals(5, rows.size());
        assertEquals(new PhoneRows.PhoneRow("해지", true), rows.get(2));
        assertNull(rows.get(0).label());
    }

    @Test
    void 메뉴는_강조자리를_건너뛰며_위부터_채운다() {
        List<PhoneRows.PhoneRow> rows = PhoneRows.of("middle", "해지", "프로필\n알림");
        assertEquals("프로필", rows.get(0).label());
        assertEquals("알림", rows.get(1).label());
        assertNull(rows.get(3).label());
    }

    @Test
    void 메뉴가_많으면_남는건_버린다() {
        List<PhoneRows.PhoneRow> rows = PhoneRows.of("top-left", "해지", "a\nb\nc\nd\ne\nf");
        assertEquals(List.of("해지", "a", "b", "c", "d"), rows.stream().map(PhoneRows.PhoneRow::label).toList());
    }

    @Test
    void 누를_메뉴와_같은_이름은_다시_넣지_않는다() {
        List<PhoneRows.PhoneRow> rows = PhoneRows.of("bottom", "해지", "해지\n알림");
        assertEquals(1, rows.stream().filter(r -> "해지".equals(r.label())).count());
        assertEquals("알림", rows.get(0).label());
    }
}
