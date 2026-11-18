package com.whtngml.subscription_cancel_guide.dto;

// 서비스 하나의 피드백 개수 (관리자 가이드 목록에 표시)
public record FeedbackCount(Long subscriptionId, long helpful, long notHelpful) {
}
