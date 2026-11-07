// Lọc danh sách "Đề tài đã duyệt" ngay trên trình duyệt (không gọi lại server).
// Mỗi dòng <tr data-dht-item> có sẵn data-state, data-category, data-priority, data-search do Thymeleaf in ra.
document.addEventListener('DOMContentLoaded', function () {
    var page = document.querySelector('.dht-page');
    if (!page) return;

    var rows = page.querySelectorAll('[data-dht-item]');
    var tabs = page.querySelectorAll('[data-dht-tab]');
    var searchInput = page.querySelector('[data-dht-search]');
    var categorySelect = page.querySelector('[data-dht-filter="category"]');
    var prioritySelect = page.querySelector('[data-dht-filter="priority"]');
    var resetButton = page.querySelector('[data-dht-action="reset"]');
    var visibleCount = page.querySelector('[data-dht-visible-count]');
    var emptyBox = page.querySelector('[data-dht-empty]');

    var currentTab = 'all';

    function applyFilters() {
        var keyword = searchInput.value.trim().toLowerCase();
        var category = categorySelect.value;
        var priority = prioritySelect.value;
        var shown = 0;

        rows.forEach(function (row) {
            var matchTab = currentTab === 'all' || row.dataset.state === currentTab;
            var matchSearch = keyword === '' || row.dataset.search.indexOf(keyword) !== -1;
            var matchCategory = category === '' || row.dataset.category === category;
            var matchPriority = priority === '' || row.dataset.priority === priority;
            var visible = matchTab && matchSearch && matchCategory && matchPriority;
            row.hidden = !visible;
            if (visible) shown++;
        });

        visibleCount.textContent = shown;
        emptyBox.hidden = shown > 0;
    }

    tabs.forEach(function (tab) {
        tab.addEventListener('click', function () {
            currentTab = tab.dataset.dhtTab;
            tabs.forEach(function (t) {
                var active = t === tab;
                t.classList.toggle('is-active', active);
                t.setAttribute('aria-selected', active ? 'true' : 'false');
            });
            applyFilters();
        });
    });

    searchInput.addEventListener('input', applyFilters);
    categorySelect.addEventListener('change', applyFilters);
    prioritySelect.addEventListener('change', applyFilters);

    resetButton.addEventListener('click', function () {
        searchInput.value = '';
        categorySelect.value = '';
        prioritySelect.value = '';
        tabs[0].click(); // về tab "Tất cả" và lọc lại
    });
});
