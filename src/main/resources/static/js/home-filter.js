// 홈 카테고리 탭: 누르면 카테고리 화면으로 가지 않고 이 화면의 목록을 바로 거른다.
// 자바스크립트가 없으면 탭은 그냥 /category/{slug} 링크로 동작한다.
(function () {
    var tabs = document.querySelectorAll('.tabs a[data-category]');
    var groups = document.querySelectorAll('.svc-group[data-category]');

    tabs.forEach(function (tab) {
        tab.addEventListener('click', function (event) {
            // Ctrl·Cmd·Shift 클릭이나 가운데 클릭(새 탭 열기)은 막지 않는다
            if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey || event.button !== 0) {
                return;
            }
            event.preventDefault();

            var slug = tab.getAttribute('data-category');
            groups.forEach(function (group) {
                group.hidden = slug !== '' && group.getAttribute('data-category') !== slug;
            });
            tabs.forEach(function (other) {
                if (other === tab) {
                    other.setAttribute('aria-current', 'page');
                } else {
                    other.removeAttribute('aria-current');
                }
            });
        });
    });
})();
