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

(function () {
    const form = document.getElementById('submissionForm');
    if (!form) return;

    const screenshots = document.getElementById('screenshots');
    const selectedList = document.getElementById('selectedScreenshots');
    const hidden = document.getElementById('aiStepsJson');
    const previewButton = document.getElementById('previewSubmission');
    const sendButton = document.getElementById('sendSubmission');
    const previewStatus = document.getElementById('previewStatus');
    const previewResponse = document.getElementById('aiPreviewResponse');
    let pending = null;
    let objectUrls = [];

    function clearPreview() {
        if (pending) {
            pending.abort();
            pending = null;
        }
        if (previewButton) previewButton.disabled = false;
        previewResponse.replaceChildren();
        hidden.value = '';
        sendButton.textContent = '제출하기';
        previewStatus.textContent = '';
    }

    function removeFile(index) {
        const transfer = new DataTransfer();
        Array.from(screenshots.files).forEach((file, position) => {
            if (position !== index) transfer.items.add(file);
        });
        screenshots.files = transfer.files;
        screenshots.dispatchEvent(new Event('change', { bubbles: true }));
        const buttons = selectedList.querySelectorAll('button');
        (buttons[Math.min(index, buttons.length - 1)] || screenshots).focus();
    }

    function renderFiles() {
        objectUrls.forEach(url => URL.revokeObjectURL(url));
        objectUrls = [];
        selectedList.replaceChildren();
        const files = Array.from(screenshots.files);
        selectedList.hidden = files.length === 0;
        files.forEach((file, index) => {
            const row = document.createElement('li');
            const label = document.createElement('span');
            label.textContent = (index + 1) + '번';
            const image = document.createElement('img');
            const url = URL.createObjectURL(file);
            objectUrls.push(url);
            image.src = url;
            image.alt = '';
            const remove = document.createElement('button');
            remove.className = 'btn btn-secondary btn-sm';
            remove.type = 'button';
            remove.textContent = '빼기';
            remove.setAttribute('aria-label', (index + 1) + '번 캡처 빼기');
            remove.addEventListener('click', () => removeFile(index));
            row.append(label, image, remove);
            selectedList.append(row);
        });
    }

    form.addEventListener('input', event => {
        if (event.target.id === 'serviceName' || event.target.id === 'rawText') clearPreview();
    });
    screenshots.addEventListener('change', clearPreview);
    try {
        screenshots.files = new DataTransfer().files;
        screenshots.addEventListener('change', renderFiles);
    } catch {
        // 브라우저가 FileList 편집을 지원하지 않으면 기본 파일 입력을 쓴다.
    }

    if (!previewButton || !window.fetch || !window.AbortController) return;

    function updateFieldErrors(page) {
        for (const field of form.querySelectorAll('input[id], textarea[id]')) {
            const updated = page.getElementById(field.id);
            if (!updated) continue;
            const description = updated.getAttribute('aria-describedby');
            if (description) field.setAttribute('aria-describedby', description);
            else field.removeAttribute('aria-describedby');
            const invalid = updated.getAttribute('aria-invalid');
            if (invalid) field.setAttribute('aria-invalid', invalid);
            else field.removeAttribute('aria-invalid');
            document.getElementById(field.id + '-error')?.remove();
            const error = page.getElementById(field.id + '-error');
            if (error) field.after(error);
        }
    }

    form.addEventListener('submit', async event => {
        if (event.submitter !== previewButton) return;
        event.preventDefault();
        clearPreview();
        const request = new AbortController();
        pending = request;
        previewButton.disabled = true;
        previewStatus.textContent = 'AI가 해지 단계를 만들고 있어요.';
        const body = new FormData(form);
        const headers = { 'X-Submission-Preview': 'true' };
        const csrf = body.get('_csrf');
        if (typeof csrf === 'string') headers['X-CSRF-TOKEN'] = csrf;

        try {
            const response = await fetch(previewButton.formAction, {
                method: 'POST', body, headers, signal: request.signal
            });
            if (!response.ok) throw new Error('Preview request failed');
            const page = new DOMParser().parseFromString(await response.text(), 'text/html');
            if (pending !== request) return;
            const result = page.getElementById('aiPreviewResponse');
            if (!result) throw new Error('Preview response missing');
            updateFieldErrors(page);
            previewResponse.replaceChildren(...result.childNodes);
            hidden.value = page.getElementById('aiStepsJson')?.value || '';
            if (hidden.value) sendButton.textContent = '이대로 제출하기';
            previewStatus.textContent = '';
        } catch {
            if (pending === request) {
                previewStatus.textContent = 'AI 미리보기를 불러오지 못했어요. 미리보기 없이도 제보할 수 있어요.';
            }
        } finally {
            if (pending === request) {
                pending = null;
                previewButton.disabled = false;
            }
        }
    });
})();
