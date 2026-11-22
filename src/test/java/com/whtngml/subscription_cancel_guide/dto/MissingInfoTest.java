package com.whtngml.subscription_cancel_guide.dto;

import com.whtngml.subscription_cancel_guide.domain.CancelChannel;
import com.whtngml.subscription_cancel_guide.domain.Category;
import com.whtngml.subscription_cancel_guide.domain.Difficulty;
import com.whtngml.subscription_cancel_guide.domain.IconImage;
import com.whtngml.subscription_cancel_guide.domain.Subscription;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MissingInfoTest {

    private final Category music = new Category("음악", "music");

    @Test
    void allMissing() {
        Subscription melon = new Subscription(music, "멜론", "melon", null, null, null);
        assertEquals(List.of("단계", "아이콘", "해지하는 곳", "난이도", "예상 시간", "공식 링크", "환불"),
                MissingInfo.of(melon, 0));
    }

    @Test
    void nothingMissing() {
        Subscription melon = new Subscription(music, "멜론", "melon", null, "환불돼요", null);
        melon.updateDetails(EnumSet.of(CancelChannel.WEB), null, Difficulty.EASY, "https://x.com", 3);
        melon.changeIcon(new IconImage(new byte[]{1}, "image/png"));
        assertEquals(List.of(), MissingInfo.of(melon, 3));
    }
}
