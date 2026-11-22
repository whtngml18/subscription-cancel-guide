package com.whtngml.subscription_cancel_guide.dto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

// 휴대폰 그림의 메뉴 5줄을 정한다. 누를 줄은 position 자리에, 다른 메뉴는 위에서부터, 모자라면 회색 줄(label이 null).
public final class PhoneRows {

    public static final int ROW_COUNT = 5;

    public record PhoneRow(String label, boolean target) {
    }

    private PhoneRows() {
    }

    // 한 줄에 메뉴 하나. 앞뒤 공백(\r 포함)을 지우고 빈 줄은 뺀다.
    public static List<String> splitMenus(String otherMenus) {
        if (otherMenus == null) {
            return List.of();
        }
        return Arrays.stream(otherMenus.split("\n"))
                .map(String::strip)
                .filter(menu -> !menu.isEmpty())
                .toList();
    }

    public static List<PhoneRow> of(String position, String targetLabel, String otherMenus) {
        int targetIndex = targetIndex(position);
        String target = targetLabel == null ? null : targetLabel.strip();
        Iterator<String> menus = splitMenus(otherMenus).stream()
                .filter(menu -> !menu.equals(target))
                .iterator();

        List<PhoneRow> rows = new ArrayList<>();
        for (int i = 0; i < ROW_COUNT; i++) {
            if (i == targetIndex) {
                rows.add(new PhoneRow(target, true));
            } else {
                rows.add(new PhoneRow(menus.hasNext() ? menus.next() : null, false));
            }
        }
        return rows;
    }

    private static int targetIndex(String position) {
        if (position == null) {
            return 2;
        }
        return switch (position) {
            case "top-left", "top-right" -> 0;
            case "bottom" -> 4;
            default -> 2;
        };
    }
}
