// FL-01: Topic Proposal List (tìm nhanh) + Create Topic Proposal (đếm ký tự, kiểm tra form, modal xác nhận).
// Server vẫn kiểm tra lại toàn bộ; phần này chỉ để báo lỗi ngay trên form.
(function () {
    'use strict';

    // ---------- Topic Proposal List: tìm nhanh trong các thẻ đang hiển thị ----------
    var search = document.querySelector('[data-tp-search]');
    if (search) {
        var cards = Array.prototype.slice.call(document.querySelectorAll('.tp-card'));
        var empty = document.querySelector('[data-tp-search-empty]');
        search.addEventListener('input', function () {
            var q = search.value.trim().toLowerCase();
            var shown = 0;
            cards.forEach(function (card) {
                var match = !q || (card.getAttribute('data-tp-search-text') || '').indexOf(q) !== -1;
                card.hidden = !match;
                if (match) shown++;
            });
            if (empty) empty.hidden = !(q && cards.length && shown === 0);
        });
    }

    // ---------- Create Topic Proposal ----------
    var form = document.querySelector('[data-tp-form]');
    if (!form) return;

    var fields = {
        title: { el: form.querySelector('#title'), message: 'Vui lòng nhập tiêu đề đề tài.' },
        departmentId: { el: form.querySelector('#departmentId'), message: 'Vui lòng chọn ban phụ trách.' },
        summary: { el: form.querySelector('#summary'), message: 'Vui lòng nhập tóm tắt nội dung.' }
    };
    var modal = document.querySelector('[data-tp-confirm]');
    var openButton = form.querySelector('[data-tp-open-confirm]');
    var submitButton = modal.querySelector('[data-tp-submit-confirm]');
    var cancelButton = modal.querySelector('[data-tp-cancel-confirm]');

    // Đếm ký tự cho tiêu đề / tóm tắt
    ['title', 'summary'].forEach(function (name) {
        var input = fields[name].el;
        var counter = form.querySelector('[data-tp-count="' + name + '"]');
        var update = function () { counter.textContent = input.value.length; };
        input.addEventListener('input', update);
        update();
    });

    function setError(name, message) {
        var wrap = form.querySelector('[data-tp-field="' + name + '"]');
        var error = wrap.querySelector('[data-tp-error="' + name + '"]');
        if (message) {
            if (!error) {
                error = document.createElement('p');
                error.className = 'tp-error';
                error.id = name + '-error';
                error.setAttribute('data-tp-error', name);
                wrap.appendChild(error);
            }
            error.textContent = message;
            wrap.classList.add('has-error');
            fields[name].el.setAttribute('aria-invalid', 'true');
        } else {
            if (error) error.remove();
            wrap.classList.remove('has-error');
            fields[name].el.removeAttribute('aria-invalid');
        }
    }

    function validate(name) {
        var value = fields[name].el.value;
        var ok = value != null && value.trim() !== '';
        setError(name, ok ? null : fields[name].message);
        return ok;
    }

    Object.keys(fields).forEach(function (name) {
        var el = fields[name].el;
        // Đã báo lỗi thì kiểm tra lại ngay khi người dùng sửa
        el.addEventListener(el.tagName === 'SELECT' ? 'change' : 'input', function () {
            if (el.closest('.tp-field').classList.contains('has-error')) validate(name);
        });
    });

    // Submit Topic Confirmation
    var lastFocus = null;
    function openConfirm() {
        var firstInvalid = null;
        Object.keys(fields).forEach(function (name) {
            if (!validate(name) && !firstInvalid) firstInvalid = fields[name].el;
        });
        if (firstInvalid) { firstInvalid.focus(); return; }

        var select = fields.departmentId.el;
        modal.querySelector('[data-tp-confirm-title]').textContent = fields.title.el.value.trim();
        modal.querySelector('[data-tp-confirm-department]').textContent = select.options[select.selectedIndex].text;
        lastFocus = document.activeElement;
        modal.hidden = false;
        document.body.classList.add('tp-modal-open');
        submitButton.focus();
    }

    // Hủy: chỉ đóng modal, dữ liệu trên form giữ nguyên
    function closeConfirm() {
        modal.hidden = true;
        document.body.classList.remove('tp-modal-open');
        if (lastFocus) lastFocus.focus();
    }

    openButton.addEventListener('click', openConfirm);
    cancelButton.addEventListener('click', closeConfirm);
    modal.addEventListener('click', function (e) { if (e.target === modal) closeConfirm(); });
    document.addEventListener('keydown', function (e) {
        if (modal.hidden) return;
        if (e.key === 'Escape') { closeConfirm(); return; }
        // Giữ Tab trong modal
        if (e.key === 'Tab') {
            if (e.shiftKey && document.activeElement === cancelButton) { e.preventDefault(); submitButton.focus(); }
            else if (!e.shiftKey && document.activeElement === submitButton) { e.preventDefault(); cancelButton.focus(); }
        }
    });
    submitButton.addEventListener('click', function () {
        submitButton.disabled = true; // tránh gửi trùng khi bấm nhiều lần
        cancelButton.disabled = true;
        form.submit();
    });

    // Enter trong ô tiêu đề cũng đi qua modal thay vì gửi thẳng
    form.addEventListener('submit', function (e) {
        if (!submitButton.disabled) { e.preventDefault(); openConfirm(); }
    });
})();
