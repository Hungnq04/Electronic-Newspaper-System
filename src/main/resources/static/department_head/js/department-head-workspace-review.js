// =====================================================================
// Màn thẩm định bài viết của Trưởng ban (department_head/workspacereview.html)
//
// File này làm 3 việc:
//   1. Hiện thông báo nhỏ (toast) sau mỗi thao tác
//   2. Hỏi lại trước khi xóa góp ý
//   6. Mở / đóng hộp thoại "Duyệt" và "Yêu cầu sửa"
// (Đã bỏ: tô vàng + bôi đen để góp ý, so sánh phiên bản bằng JS, đổi tab góp ý)
//
// Cách ẩn / hiện một phần tử: element.hidden = true (ẩn) hoặc element.hidden = false (hiện).
// File được nạp ở cuối trang (layout/app.html) nên mọi phần tử HTML đã có sẵn khi chạy.
// =====================================================================

// Lấy phần tử theo id cho gọn: byId('toast') giống document.getElementById('toast')
function byId(id) {
  return document.getElementById(id);
}

// Đổi ký tự đặc biệt (& < > ") thành dạng an toàn trước khi chèn chữ vào HTML,
// để chữ trong bài không bị trình duyệt hiểu nhầm thành thẻ HTML
function escapeHtml(text) {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

// Khung chứa bài viết đang duyệt (tít, sapo, thân bài)
// Tìm thẻ có id="article-card" và lưu vào biến articleCard.
const articleCard = byId('article-card');


// ---------------------------------------------------------------------
// 1. Thông báo nhỏ (toast)
// ---------------------------------------------------------------------
let toastTimer = null;

// Hiện thông báo trong 3,5 giây. isError = true thì nền đỏ
function showToast(message, isError) {
  const toast = byId('toast');
  toast.textContent = message;
  toast.classList.toggle('is-error', isError === true);
  toast.hidden = false;
  clearTimeout(toastTimer);                  // nếu thông báo cũ chưa tắt thì hủy hẹn giờ cũ
  toastTimer = setTimeout(function () {
    toast.hidden = true;
  }, 3500);
}

// Controller gửi kèm thông báo (flash) sau mỗi thao tác: hiện ngay khi trang vừa mở
const toastBox = byId('toast');
if (toastBox.dataset.message) {
  showToast(toastBox.dataset.message, toastBox.dataset.error === 'true');
}


// ---------------------------------------------------------------------
// 2. Hỏi lại trước khi gửi form có data-confirm (ví dụ nút "Xóa" góp ý)
// ---------------------------------------------------------------------
document.querySelectorAll('form[data-confirm]').forEach(function (form) {
  form.addEventListener('submit', function (event) {
    // confirm() hiện hộp thoại OK / Cancel. Bấm Cancel thì chặn, không gửi form
    if (!confirm(form.dataset.confirm)) {
      event.preventDefault();
    }
  });
});


// ---------------------------------------------------------------------
// 6. Hộp thoại Duyệt / Yêu cầu sửa
// ---------------------------------------------------------------------

// Hộp thoại (modal): chỉ có khi mình đang được duyệt bài này
const modals = document.querySelectorAll('#modal-revise, #modal-approve');

function closeModals() {
  modals.forEach(function (modal) {
    modal.hidden = true;
  });
}

// Nút "Hủy" trong hộp thoại
document.querySelectorAll('[data-close]').forEach(function (button) {
  button.addEventListener('click', closeModals);
});
// Bấm ra vùng tối bên ngoài hộp thoại cũng đóng hộp thoại
modals.forEach(function (modal) {
  modal.addEventListener('click', function (event) {
    if (event.target === modal) closeModals();
  });
});

const btnRevise = byId('btn-revise');
if (btnRevise) {
  const reviseFeedback = byId('revise-feedback');

  // Mở hộp thoại "Yêu cầu sửa"
  btnRevise.addEventListener('click', function () {
    byId('modal-revise').hidden = false;
    reviseFeedback.focus();
  });

  // Nhận xét chung là bắt buộc: chỉ bật nút "Gửi trả bài" khi đã gõ chữ
  reviseFeedback.addEventListener('input', function () {
    byId('revise-submit').disabled = !reviseFeedback.value.trim();
  });

  // Mở hộp thoại "Duyệt"
  byId('btn-approve').addEventListener('click', function () {
    byId('modal-approve').hidden = false;
  });
}

// Phím Esc: đóng hộp thoại
document.addEventListener('keydown', function (event) {
  if (event.key === 'Escape') {
    closeModals();
  }
});
