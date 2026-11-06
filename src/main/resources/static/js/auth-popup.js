// FL-01 Iter3: điều khiển các pop-up trên trang /login.
// Chỉ lo phần giao diện; mọi kiểm tra thật đều ở server (AuthController / AuthService).
(function () {
    // ----- Chuyển pop-up (Login <-> Register, Login -> Forgot) không cần tải lại trang -----
    function openPopup(name) {
        document.querySelectorAll('[data-lp-popup]').forEach(function (section) {
            section.hidden = section.getAttribute('data-lp-popup') !== name;
        });
        // Thông báo / lỗi cũ thuộc pop-up trước, ẩn đi khi chuyển
        document.querySelectorAll('[data-lp-alert], .lp-field-error').forEach(function (el) { el.hidden = true; });
        document.querySelectorAll('.lp-invalid').forEach(function (el) { el.classList.remove('lp-invalid'); });
        focusFirstInput(name);
    }

    function focusFirstInput(name) {
        var section = document.querySelector('[data-lp-popup="' + name + '"]');
        var input = section && section.querySelector('input:not([type=hidden])');
        if (input) input.focus();
    }

    document.querySelectorAll('[data-lp-open]').forEach(function (link) {
        link.addEventListener('click', function (event) {
            var name = link.getAttribute('data-lp-open');
            // OTP / Reset luôn do server mở; các pop-up còn lại chuyển ngay trên trình duyệt
            if (name === 'login' || name === 'register' || name === 'forgot') {
                event.preventDefault();
                openPopup(name);
                history.replaceState(null, '', name === 'login' ? '/login' : '/login?popup=' + name);
            }
        });
    });

    // ----- Hiện / ẩn mật khẩu -----
    document.querySelectorAll('[data-lp-toggle]').forEach(function (toggle) {
        toggle.addEventListener('click', function () {
            var input = document.getElementById(toggle.getAttribute('data-lp-toggle'));
            var show = input.type === 'password';
            input.type = show ? 'text' : 'password';
            toggle.textContent = show ? 'Ẩn' : 'Hiện';
            toggle.setAttribute('aria-pressed', String(show));
        });
    });

    // ----- Khóa nút submit sau khi bấm để tránh gửi trùng -----
    document.querySelectorAll('[data-lp-form]').forEach(function (form) {
        form.addEventListener('submit', function () {
            var submit = form.querySelector('.lp-submit');
            if (submit) { submit.disabled = true; submit.textContent = 'Đang xử lý…'; }
        });
    });

    // ----- 6 ô OTP: mỗi ô 1 chữ số, tự nhảy ô, dán được cả mã 6 số -----
    var otpBox = document.querySelector('[data-lp-otp]');
    if (otpBox) {
        var cells = Array.prototype.slice.call(otpBox.querySelectorAll('input'));

        // Điền chuỗi số bắt đầu từ ô "start" (dùng khi dán hoặc gõ nhanh)
        function fillFrom(start, digits) {
            for (var i = 0; i < digits.length && start + i < cells.length; i++) {
                cells[start + i].value = digits[i];
            }
            var next = Math.min(start + digits.length, cells.length - 1);
            cells[next].focus();
        }

        cells.forEach(function (cell, index) {
            cell.addEventListener('input', function () {
                var digits = cell.value.replace(/\D/g, '');
                cell.value = '';
                if (digits) fillFrom(index, digits.split(''));
            });
            cell.addEventListener('keydown', function (event) {
                // Backspace ở ô trống thì lùi về ô trước
                if (event.key === 'Backspace' && !cell.value && index > 0) {
                    cells[index - 1].value = '';
                    cells[index - 1].focus();
                    event.preventDefault();
                }
                if (event.key === 'ArrowLeft' && index > 0) cells[index - 1].focus();
                if (event.key === 'ArrowRight' && index < cells.length - 1) cells[index + 1].focus();
            });
            cell.addEventListener('paste', function (event) {
                var text = (event.clipboardData || window.clipboardData).getData('text') || '';
                var digits = text.replace(/\D/g, '').slice(0, 6);
                if (digits) {
                    event.preventDefault();
                    fillFrom(digits.length === 6 ? 0 : index, digits.split(''));
                }
            });
        });
        if (!otpBox.closest('[data-lp-popup]').hidden) cells[0].focus();
    }

    // ----- Nút "Gửi lại mã": khóa 60 giây, đếm ngược -----
    var resend = document.querySelector('[data-lp-resend]');
    if (resend) {
        var wait = parseInt(resend.getAttribute('data-wait') || '0', 10);
        function tick() {
            if (wait > 0) {
                resend.disabled = true;
                resend.textContent = 'Gửi lại mã sau ' + wait + ' giây';
                wait--;
                setTimeout(tick, 1000);
            } else {
                resend.disabled = false;
                resend.textContent = 'Gửi lại mã';
            }
        }
        tick();
    }

    // Lần đầu mở trang: đặt con trỏ vào ô đầu của pop-up đang hiện (trừ OTP đã xử lý ở trên)
    var current = document.body.getAttribute('data-popup');
    if (current && current !== 'otp') focusFirstInput(current);
})();
