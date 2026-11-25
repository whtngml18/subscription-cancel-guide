// 제보 화면 글쓰기 도움: "예시 넣어 보기" 틀 채우기와 남은 글자 수
(function () {
    var text = document.getElementById('rawText');
    var button = document.getElementById('fillTemplate');
    var count = document.getElementById('rawTextCount');
    if (!text) {
        return;
    }

    var TEMPLATE = "앱 / 웹:\n"
        + "1. (어디쯤) '(버튼 이름)'을 눌러요.\n"
        + "2. (어디쯤) '(버튼 이름)'을 눌러요.\n"
        + "3. '(해지 버튼 이름)'을 누르고 확인해요.";

    function updateCount() {
        if (count) {
            count.textContent = text.value.length + ' / ' + text.maxLength;
        }
    }

    if (button) {
        button.hidden = false;
        button.addEventListener('click', function () {
            // 이미 쓴 글을 지우지 않게 비어 있을 때만 채운다
            if (text.value.trim() === '') {
                text.value = TEMPLATE;
                text.dispatchEvent(new Event('input'));
            }
            text.focus();
        });
    }

    text.addEventListener('input', updateCount);
    updateCount();
})();
