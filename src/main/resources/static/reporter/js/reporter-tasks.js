// Trang nhiệm vụ phóng viên: lọc danh sách (tab, tìm kiếm, chuyên mục, vai trò, hạn), đổi Bảng / Kanban, đếm ngược hạn chót.
(function () {
    'use strict';

    const VIEW_KEY = 'rp-task-view';

    function initBoard() {
        const items = Array.from(document.querySelectorAll('[data-rp-item]'));
        if (!document.querySelector('[data-rp-panel]')) return;

        const state = { tab: 'all', query: '', category: '', role: '', bucket: '' };
        const tabs = document.querySelectorAll('[data-rp-tab]');
        const searches = document.querySelectorAll('[data-rp-search]');
        const selects = document.querySelectorAll('[data-rp-filter]');
        const visibleCount = document.querySelector('[data-rp-visible-count]');
        const empty = document.querySelector('[data-rp-empty]');

        function matchesTab(el) {
            switch (state.tab) {
                case 'urgent': return el.dataset.urgent === 'true';
                case 'open': return el.dataset.group === 'todo' || el.dataset.group === 'doing';
                case 'review': return el.dataset.group === 'review';
                case 'done': return el.dataset.group === 'done';
                default: return true;
            }
        }

        function matches(el) {
            return matchesTab(el)
                && (!state.query || (el.dataset.search || '').includes(state.query))
                && (!state.category || el.dataset.category === state.category)
                && (!state.role || el.dataset.role === state.role)
                && (!state.bucket || el.dataset.bucket === state.bucket);
        }

        function apply() {
            let rows = 0;
            items.forEach(el => {
                const show = matches(el);
                el.hidden = !show;
                if (show && el.tagName === 'TR') rows++;
            });
            if (visibleCount) visibleCount.textContent = rows;
            if (empty) empty.hidden = rows > 0;
            document.querySelectorAll('.rp-kanban-col').forEach(col => {
                const counter = col.querySelector('[data-rp-column-count]');
                if (counter) counter.textContent = col.querySelectorAll('[data-rp-item]:not([hidden])').length;
            });
        }

        tabs.forEach(tab => tab.addEventListener('click', () => {
            tabs.forEach(t => {
                t.classList.toggle('is-active', t === tab);
                t.setAttribute('aria-selected', t === tab ? 'true' : 'false');
            });
            state.tab = tab.dataset.rpTab;
            apply();
        }));

        // Ô tìm trên topbar và ô tìm trong bộ lọc dùng chung một từ khóa
        searches.forEach(input => input.addEventListener('input', () => {
            state.query = input.value.trim().toLowerCase();
            searches.forEach(other => { if (other !== input) other.value = input.value; });
            apply();
        }));

        selects.forEach(select => select.addEventListener('change', () => {
            state[select.dataset.rpFilter] = select.value;
            apply();
        }));

        const reset = document.querySelector('[data-rp-action="reset"]');
        if (reset) reset.addEventListener('click', () => {
            state.query = state.category = state.role = state.bucket = '';
            searches.forEach(input => { input.value = ''; });
            selects.forEach(select => { select.value = ''; });
            tabs[0] && tabs[0].click();
            apply();
        });

        const viewButtons = document.querySelectorAll('[data-rp-view]');
        function setView(view) {
            viewButtons.forEach(b => b.classList.toggle('is-active', b.dataset.rpView === view));
            document.querySelectorAll('[data-rp-panel]').forEach(p => { p.hidden = p.dataset.rpPanel !== view; });
            try { localStorage.setItem(VIEW_KEY, view); } catch (e) { /* trình duyệt chặn storage: bỏ qua */ }
        }
        viewButtons.forEach(b => b.addEventListener('click', () => setView(b.dataset.rpView)));
        let saved = null;
        try { saved = localStorage.getItem(VIEW_KEY); } catch (e) { /* bỏ qua */ }
        if (saved === 'kanban' || saved === 'table') setView(saved);

        apply();
    }

    function formatLeft(ms) {
        const late = ms < 0;
        const totalMinutes = Math.max(1, Math.floor(Math.abs(ms) / 60000));
        const days = Math.floor(totalMinutes / 1440);
        const hours = Math.floor((totalMinutes % 1440) / 60);
        const minutes = totalMinutes % 60;
        const text = days > 0 ? days + ' NGÀY' + (hours > 0 ? ' ' + hours + ' GIỜ' : '')
            : hours > 0 ? hours + ' GIỜ ' + minutes + ' PHÚT'
            : minutes + ' PHÚT';
        return (late ? 'QUÁ HẠN ' : 'CÒN ') + text;
    }

    function initCountdown() {
        const banner = document.querySelector('[data-rp-countdown]');
        if (!banner) return;
        const deadline = new Date(banner.dataset.rpCountdown);
        const label = banner.querySelector('[data-rp-countdown-text]');
        if (isNaN(deadline) || !label) return;
        function tick() {
            const left = deadline - Date.now();
            label.textContent = formatLeft(left);
            banner.classList.toggle('is-overdue', left < 0);
        }
        tick();
        setInterval(tick, 30000);
    }

    document.addEventListener('DOMContentLoaded', () => {
        initBoard();
        initCountdown();
    });
})();
