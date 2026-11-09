// Màn "Lịch sử duyệt" của Trưởng ban (templates/department_head/review-history.html).
// Việc của file này: LỌC các dòng trong bảng ngay trên trình duyệt, không gửi gì lên server.
//   - Nút "Tất cả / Đã duyệt / Yêu cầu sửa"  (data-rh-decision)
//   - Ô chọn "Chuyên mục"                     (id="rh-category")
//   - Ô tìm theo tít bài, đề tài, mã bài      (id="rh-search")
// Mỗi dòng <tr class="rh-row"> có sẵn data-decision, data-category, data-search do Thymeleaf in ra.
(() => {
  'use strict';
  const page = document.querySelector('.rh-page');
  if (!page) return;   // không phải trang Lịch sử duyệt thì không làm gì

  const rows = Array.from(page.querySelectorAll('.rh-row'));
  const decisionButtons = Array.from(page.querySelectorAll('[data-rh-decision]'));
  const categorySelect = page.querySelector('#rh-category');
  const searchInput = page.querySelector('#rh-search');
  const visibleCount = page.querySelector('#rh-visible-count');   // số "bản ghi" trên đầu bảng
  const noMatchBox = page.querySelector('#rh-no-match');          // khung "Không có lượt duyệt nào khớp bộ lọc"

  // Bộ lọc đang chọn. Lúc mở trang: xem tất cả.
  let selectedDecision = 'all';

  // Bỏ dấu tiếng Việt + chữ thường, để gõ "bao cao" vẫn tìm được "Báo cáo"
  const normalize = text => (text || '').normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'd').toLowerCase().trim();

  // Xét lại từng dòng: khớp cả 3 điều kiện thì hiện, không thì ẩn
  function applyFilters() {
    const category = categorySelect ? categorySelect.value : 'all';
    const keyword = normalize(searchInput ? searchInput.value : '');
    let shown = 0;

    rows.forEach(row => {
      const matchDecision = selectedDecision === 'all' || row.dataset.decision === selectedDecision;
      const matchCategory = category === 'all' || row.dataset.category === category;
      const matchKeyword = keyword === '' || normalize(row.dataset.search).includes(keyword);
      const isVisible = matchDecision && matchCategory && matchKeyword;
      row.hidden = !isVisible;
      if (isVisible) shown++;
    });

    if (visibleCount) visibleCount.textContent = shown;
    if (noMatchBox) noMatchBox.hidden = shown !== 0;
  }

  // Bấm một nút quyết định: đánh dấu nút đó là đang chọn rồi lọc lại
  decisionButtons.forEach(button => {
    button.addEventListener('click', () => {
      selectedDecision = button.dataset.rhDecision;
      decisionButtons.forEach(other => other.classList.toggle('is-active', other === button));
      applyFilters();
    });
  });

  if (categorySelect) categorySelect.addEventListener('change', applyFilters);
  if (searchInput) searchInput.addEventListener('input', applyFilters);
})();
