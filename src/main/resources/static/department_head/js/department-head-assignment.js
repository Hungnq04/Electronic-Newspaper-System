// Trang "Giao việc / Quản lý nhóm": chỉ hỗ trợ thao tác trên giao diện.
// Mọi kiểm tra thật (đúng một Lead, phóng viên ACTIVE, không trùng vai trò, deadline tương lai...) vẫn làm ở backend.
document.addEventListener('DOMContentLoaded', function () {
    var page = document.querySelector('.dht-page');
    if (!page) return;

    // Không cho chọn deadline trong quá khứ (giờ của máy người dùng, backend kiểm tra lại)
    var now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    var minValue = now.toISOString().slice(0, 16);
    page.querySelectorAll('[data-dha-deadline]').forEach(function (input) { input.min = minValue; });

    // Form gỡ Supporting / đổi Lead: hỏi lại trước khi gửi
    page.querySelectorAll('form[data-dha-confirm]').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            if (!confirm(form.dataset.dhaConfirm)) event.preventDefault();
        });
    });

    // Form có danh sách chọn phóng viên (giao việc lần đầu, thêm Supporting)
    page.querySelectorAll('[data-dha-form]').forEach(setupCandidateForm);
});

function setupCandidateForm(form) {
    var hasLead = form.dataset.hasLead === 'true';
    var existingSupporters = parseInt(form.dataset.existingSupporters || '0', 10);
    var candidates = form.querySelectorAll('[data-dha-candidate]');
    var searchInput = form.querySelector('[data-dha-search]');
    var noMatch = form.querySelector('[data-dha-no-match]');
    // Các ô tóm tắt chỉ có ở form giao việc lần đầu, có thể không tồn tại
    var leadNameBox = form.querySelector('[data-dha-lead-name]');
    var summaryLead = form.querySelector('[data-dha-summary-lead]');
    var summarySupport = form.querySelector('[data-dha-summary-support]');
    var summaryTotal = form.querySelector('[data-dha-total]');
    var supportCount = form.querySelector('[data-dha-support-count]');

    function setText(element, text) {
        if (element) element.textContent = text;
    }

    function refresh() {
        var leadName = null;
        var picked = 0;

        candidates.forEach(function (box) {
            var radio = box.querySelector('[data-dha-lead-radio]');
            var check = box.querySelector('[data-dha-support-check]');
            var panel = box.querySelector('[data-dha-panel]');
            var isLead = radio !== null && radio.checked;

            // Lead không được đồng thời là Supporting (BR-24)
            if (isLead) {
                check.checked = false;
                leadName = box.dataset.name;
            }
            check.disabled = isLead;

            // Ô nhiệm vụ + deadline riêng chỉ hiện (và bắt buộc) khi người này được chọn làm Supporting
            var isSupporter = check.checked;
            panel.hidden = !isSupporter;
            panel.querySelectorAll('input, textarea').forEach(function (field) { field.required = isSupporter; });
            if (isSupporter) picked++;

            box.classList.toggle('is-picked', isSupporter);
            box.classList.toggle('is-lead', isLead);
        });

        if (!hasLead) {
            setText(leadNameBox, leadName || 'Chưa chọn – chọn ở mục bên dưới');
            setText(summaryLead, leadName || '(chưa chọn)');
        }
        var supporters = existingSupporters + picked;
        setText(supportCount, picked);
        setText(summarySupport, supporters);
        setText(summaryTotal, (hasLead || leadName ? 1 : 0) + supporters);
    }

    candidates.forEach(function (box) {
        box.querySelectorAll('input[type="radio"], input[type="checkbox"]').forEach(function (input) {
            input.addEventListener('change', refresh);
        });
    });

    // Lọc danh sách phóng viên theo tên / email / phòng ban
    if (searchInput) {
        searchInput.addEventListener('input', function () {
            var keyword = searchInput.value.trim().toLowerCase();
            var shown = 0;
            candidates.forEach(function (box) {
                var visible = keyword === '' || box.dataset.search.indexOf(keyword) !== -1;
                box.hidden = !visible;
                if (visible) shown++;
            });
            noMatch.hidden = shown > 0 || candidates.length === 0;
        });
    }

    form.addEventListener('submit', function (event) {
        // Đề tài chưa có Lead: phải chọn Lead
        if (!hasLead && !form.querySelector('[data-dha-lead-radio]:checked')) {
            event.preventDefault();
            alert('Vui lòng chọn đúng một phóng viên làm Lead.');
            return;
        }
        // Form "Thêm Supporting": phải tick ít nhất một người
        if (hasLead && !form.querySelector('[data-dha-lead-name]') && !form.querySelector('[data-dha-support-check]:checked')) {
            event.preventDefault();
            alert('Vui lòng chọn ít nhất một phóng viên để thêm vào nhóm.');
        }
    });

    refresh();
}
