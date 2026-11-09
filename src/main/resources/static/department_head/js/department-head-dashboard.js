// =====================================================================
// Dashboard Trưởng phòng (department_head/dashboard.html)
//
// File này CHỈ lọc, tìm kiếm, sắp xếp các thẻ ĐÃ CÓ SẴN trên trang. Nó KHÔNG gửi gì về server.
// Thymeleaf đã in sẵn mọi thẻ (bài viết + đề tài); JS chỉ ẩn / hiện bằng thuộc tính hidden.
//
// HTML không "gọi" JS. JS đi TÌM HTML theo id / class / data-...:
//   .dh-draft-card            mỗi thẻ (một bài hoặc một đề tài)
//     data-kind               "article" hoặc "topic"  -> chỉ hiện dòng "article" (bài viết)
//     data-category           chuyên mục              -> dùng cho ô chọn chuyên mục
//     data-deadline-order     số thứ tự theo hạn chót (service đánh số sẵn)
//     data-created-order      số thứ tự theo thời điểm nộp (service đánh số sẵn)
//   [data-dh-sort]            nút "Gần hạn chót" / "Mới nộp" / "Đợi lâu"
// =====================================================================
(() => {   // hàm tự chạy ngay khi file được nạp; biến bên trong không lẫn với file JS khác
  'use strict';

  // Tìm khung dashboard. Không có (đang ở trang khác) hoặc đã chạy rồi thì thoát, tránh gắn sự kiện 2 lần.
  const root = document.querySelector('.dh-dashboard');
  if (!root || root.dataset.dhInitialized === 'true') return;
  root.dataset.dhInitialized = 'true';

  // ----- Tìm các phần tử trên trang -----
  const tableBody = root.querySelector('#dh-draft-list');              // Phần <tbody> của bảng danh sách chờ duyệt
  if (!tableBody) return;
  const cards = Array.from(tableBody.querySelectorAll('.dh-draft-card'));  // Mọi dòng trong bảng (mỗi dòng là một bài hoặc một đề tài)
  const searchInput = root.querySelector('#dh-draft-search');          // ô tìm trong bộ lọc
  const topbarSearchInput = root.querySelector('#dh-global-search');   // ô tìm trên thanh trên cùng
  const categorySelect = root.querySelector('#dh-category-filter');    // ô chọn chuyên mục
  const typeSelect = root.querySelector('#dh-type-filter');            // ô chọn loại (có thể không có)
  const emptyState = root.querySelector('#dh-empty-state');            // hộp "Không có bản thảo nào"
  // Số "bản ghi" trên đầu bảng: cập nhật theo số dòng đang hiện sau khi lọc
  const visibleCount = root.querySelector('#dh-visible-count');
  const sortButtons = Array.from(root.querySelectorAll('[data-dh-sort]'));

  // Cách sắp xếp đang chọn. Mặc định: sắp theo hạn chót.
  const filterState = { sort: 'deadline' };

  // Thẻ mẫu (class dh-draft-fallback) chỉ để xem trước khi mở file HTML trực tiếp.
  // Có thẻ thật do Thymeleaf in ra thì ẩn thẻ mẫu đi.
  const realCards = cards.filter(card => !card.classList.contains('dh-draft-fallback'));
  if (realCards.length > 0) {
    cards.filter(card => card.classList.contains('dh-draft-fallback')).forEach(card => card.hidden = true);
  }

  // Đổi chữ về dạng không dấu, chữ thường: "Nguyễn Văn Á" -> "nguyen van a".
  // Nhờ vậy gõ "nguyen" vẫn tìm ra "Nguyễn".
  const normalize = value => (value || '').toString().normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();

  // ----- Hàm chính: xét từng thẻ, hiện thẻ khớp MỌI điều kiện, ẩn thẻ còn lại -----
  const update = () => {
    // Hai ô tìm (trên topbar và trong bộ lọc) luôn cùng nội dung, nên chỉ cần lấy một ô
    const keyword = normalize(searchInput?.value || topbarSearchInput?.value);
    let visible = 0;
    cards.forEach(card => {
      const cardText = normalize((card.dataset.title || '') + ' ' + card.textContent);   // toàn bộ chữ của thẻ, để tìm
      const matches = (!card.dataset.kind || card.dataset.kind === 'article')                                        // chỉ hiện bài viết, ẩn đề tài
        && (!categorySelect || categorySelect.value === 'all' || card.dataset.category === categorySelect.value)     // đúng chuyên mục
        && (!typeSelect || typeSelect.value === 'all' || card.dataset.type === typeSelect.value)
        && (!keyword || cardText.includes(keyword));                                                                 // có chứa từ khóa
      card.hidden = !matches;          // không khớp thì ẩn
      if (matches) visible++;
    });
    if (emptyState) emptyState.hidden = visible !== 0;   // không còn thẻ nào thì hiện hộp "Không có bản thảo nào"
    if (visibleCount) visibleCount.textContent = visible;
  };

  // Làm sáng nút vừa bấm (class is-active), tắt sáng các nút còn lại cùng nhóm
  const setActive = (buttons, activeButton, attr) => buttons.forEach(button => {
    const isActive = button === activeButton;
    button.classList.toggle('is-active', isActive);
    if (attr) button.setAttribute(attr, isActive ? 'true' : 'false');
  });

  // ----- Gắn sự kiện cho các nút -----
  // Mỗi nút làm cùng một kiểu: cập nhật filterState -> đổi nút sáng -> gọi update()

  // Gõ vào ô tìm kiếm: lọc ngay khi gõ
  [searchInput, topbarSearchInput].filter(Boolean).forEach(input => input.addEventListener('input', () => {
    // Gõ ở ô nào thì chép sang ô còn lại (kể cả khi xóa trắng), giống màn /reporter/tasks
    const otherInput = input === searchInput ? topbarSearchInput : searchInput;
    if (otherInput) otherInput.value = input.value;
    update();
  }));
  // Đổi ô chọn chuyên mục / loại
  [categorySelect, typeSelect].filter(Boolean).forEach(select => select.addEventListener('change', update));

  // Nút sắp xếp: dùng số thứ tự service đã đánh sẵn (data-deadline-order, data-created-order)
  sortButtons.forEach(button => button.addEventListener('click', () => {
    filterState.sort = button.dataset.dhSort;
    setActive(sortButtons, button);
    // Thứ tự của một thẻ theo kiểu sắp xếp đang chọn. Số nhỏ đứng trước; không có số thì xếp cuối (999999).
    //   "newest" (Mới nộp)  : theo createdOrder
    //   "oldest" (Đợi lâu)  : theo createdOrder nhưng đảo dấu, nên nộp lâu nhất lên đầu
    //   còn lại (Gần hạn chót): theo deadlineOrder
    const orderOf = card => Number(filterState.sort === 'newest' ? card.dataset.createdOrder
        : filterState.sort === 'oldest' ? -(Number(card.dataset.createdOrder) || 0)
        : card.dataset.deadlineOrder) || 999999;
    // Gắn lại các thẻ vào bảng theo thứ tự mới là bảng tự sắp xếp
    realCards.slice().sort((a, b) => orderOf(a) - orderOf(b)).forEach(card => tableBody.appendChild(card));
    update();
  }));

  // ----- Các nút có data-dh-action -----
  root.addEventListener('click', event => {
    const target = event.target.closest('[data-dh-action]');
    if (!target) return;
    const action = target.dataset.dhAction;
    // Nút "Xóa bộ lọc": đưa mọi bộ lọc về mặc định
    if (action === 'clear-filters') {
      if (searchInput) searchInput.value = '';
      if (topbarSearchInput) topbarSearchInput.value = '';
      if (categorySelect) categorySelect.value = 'all';
      if (typeSelect) typeSelect.value = 'all';
      update();
      return;
    }
    // Các action khác (ví dụ nút "⋯", "Nhật ký duyệt", chuông thông báo) hiện CHƯA làm gì:
    // chỉ phát ra sự kiện "dh:action" để sau này nối với chức năng thật.
    if (target.tagName === 'A') event.preventDefault();
    root.dispatchEvent(new CustomEvent('dh:action', {
      bubbles: true,
      detail: { action, entryId: target.dataset.entryId || null, element: target }
    }));
  });

  // Chạy một lần lúc mở trang để áp bộ lọc mặc định (chỉ hiện bài viết)
  update();
})();
