// =====================================================================
// Màn thẩm định bài viết của Trưởng ban (department_head/workspacereview.html)
//
// File này làm 4 việc:
//   1. Hiện thông báo nhỏ (toast) sau mỗi thao tác
//   2. Hỏi lại trước khi xóa góp ý
//   5. So sánh hai phiên bản của bài
//   6. Đổi tab góp ý, mở / đóng hộp thoại "Duyệt" và "Yêu cầu sửa"
// (Phần 3, 4 cũ là tô vàng và bôi đen để góp ý: đã bỏ, Trưởng phòng chỉ còn góp ý chung)
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
// 5. So sánh phiên bản (chỉ có khi bài có từ 2 phiên bản trở lên)
// ---------------------------------------------------------------------
const compareCard = byId('compare-card');     // null nếu bài mới có 1 phiên bản
const baseSelect = byId('compare-base');      // ô chọn "So với v..."
let comparing = false;                        // true = đang xem màn so sánh

// Đọc chữ của một phiên bản từ khối ẩn #version-data.
// Kết quả dạng { "Tít": "...", "Tít phụ": "...", "Sapo": "...", "Đoạn 1": "...", ... }
function readVersion(label) {
  const box = document.querySelector('#version-data > [data-label="' + label + '"]');
  const result = {};
  if (box) {
    box.querySelectorAll('[data-key]').forEach(function (element) {
      result[element.dataset.key] = element.textContent.trim();
    });
  }
  return result;
}

// Đếm số "Đoạn n" có trong một phiên bản
function countParagraphs(version) {
  return Object.keys(version).filter(function (key) {
    return key.startsWith('Đoạn ');
  }).length;
}

// Tách câu thành từng từ và khoảng trắng: "Xin chào" -> ["Xin", " ", "chào"]
function tokenize(text) {
  return text.match(/\s+|[^\s]+/g) || [];
}

// So sánh 2 dãy từ, trả về danh sách thao tác:
//   ['eq', từ] = giữ nguyên, ['del', từ] = bị xóa, ['ins', từ] = thêm mới.
// Dùng thuật toán "dãy con chung dài nhất" (LCS): tìm nhiều từ giống nhau nhất giữa 2 bản,
// phần còn lại là chữ bị xóa (ở bản cũ) hoặc chữ thêm mới (ở bản mới).
function diffTokens(oldWords, newWords) {
  const oldCount = oldWords.length;   // số từ của bản cũ
  const newCount = newWords.length;   // số từ của bản mới

  // lcs[i][j] = số từ giống nhau nhiều nhất giữa oldWords (tính từ vị trí i) và newWords (tính từ vị trí j)
  const lcs = [];
  for (let i = 0; i <= oldCount; i++) {
    lcs.push(new Uint16Array(newCount + 1));          // mảng số nguyên, mặc định toàn số 0
  }
  for (let i = oldCount - 1; i >= 0; i--) {
    for (let j = newCount - 1; j >= 0; j--) {
      if (oldWords[i] === newWords[j]) {
        lcs[i][j] = lcs[i + 1][j + 1] + 1;
      } else {
        lcs[i][j] = Math.max(lcs[i + 1][j], lcs[i][j + 1]);
      }
    }
  }

  // Đi lại từ đầu 2 dãy để ghi ra từng thao tác
  const operations = [];
  let i = 0;
  let j = 0;
  while (i < oldCount && j < newCount) {
    if (oldWords[i] === newWords[j]) {
      operations.push(['eq', oldWords[i]]);
      i++;
      j++;
    } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
      operations.push(['del', oldWords[i]]);
      i++;
    } else {
      operations.push(['ins', newWords[j]]);
      j++;
    }
  }
  while (i < oldCount) {                              // còn dư từ ở bản cũ: đều là chữ bị xóa
    operations.push(['del', oldWords[i]]);
    i++;
  }
  while (j < newCount) {                              // còn dư từ ở bản mới: đều là chữ thêm mới
    operations.push(['ins', newWords[j]]);
    j++;
  }
  return operations;
}

// Nhãn nhỏ ở cột trái mỗi dòng so sánh: Không đổi / Thêm mới / Đã bỏ / Đã sửa
function changeTag(oldText, newText) {
  if (oldText === newText) return '<span class="ws-compare-tag">Không đổi</span>';
  if (!oldText) return '<span class="ws-compare-tag ws-compare-tag--added">Thêm mới</span>';
  if (!newText) return '<span class="ws-compare-tag ws-compare-tag--removed">Đã bỏ</span>';
  return '<span class="ws-compare-tag ws-compare-tag--changed">Đã sửa</span>';
}

// Dựng lại toàn bộ bảng so sánh giữa bản chọn ở ô "So với" và bản đang duyệt
function renderCompare() {
  const allVersions = document.querySelectorAll('#version-data > [data-label]');
  const currentLabel = allVersions[allVersions.length - 1].dataset.label;   // bản cuối cùng = bản đang duyệt
  const baseLabel = baseSelect.value;
  const oldVersion = readVersion(baseLabel);
  const newVersion = readVersion(currentLabel);
  const onlyChanged = byId('compare-only-changed').checked;
  byId('compare-head-old').textContent = 'v' + baseLabel + ' · bản trước';

  // Thứ tự các dòng: Tít, Tít phụ, Sapo, rồi Đoạn 1..n (lấy theo bản có nhiều đoạn hơn)
  const keys = ['Tít', 'Tít phụ', 'Sapo'];
  const paragraphCount = Math.max(countParagraphs(oldVersion), countParagraphs(newVersion));
  for (let k = 1; k <= paragraphCount; k++) {
    keys.push('Đoạn ' + k);
  }

  let changed = 0;
  let added = 0;
  let removed = 0;
  let html = '';
  keys.forEach(function (key) {
    const oldText = oldVersion[key] || '';
    const newText = newVersion[key] || '';
    if (!oldText && !newText) return;                    // cả 2 bản đều không có ô này
    const same = oldText === newText;
    if (!same) {
      if (!oldText) added++;
      else if (!newText) removed++;
      else changed++;
    }
    if (onlyChanged && same) return;                     // đang bật "Chỉ hiện đoạn thay đổi"

    // Chữ cột bản cũ (left) và cột bản đang duyệt (right): tô đỏ chữ bị xóa, tô xanh chữ thêm mới
    let left = '';
    let right = '';
    if (!oldText) {
      left = '<span class="ws-compare-missing">(Chưa có trong v' + escapeHtml(baseLabel) + ')</span>';
      right = '<span class="diff-ins">' + escapeHtml(newText) + '</span>';
    } else if (!newText) {
      left = '<span class="diff-del">' + escapeHtml(oldText) + '</span>';
      right = '<span class="ws-compare-missing">(Đã bỏ)</span>';
    } else {
      diffTokens(tokenize(oldText), tokenize(newText)).forEach(function (operation) {
        const type = operation[0];
        const word = escapeHtml(operation[1]);
        if (type === 'eq') {
          left += word;
          right += word;
        } else if (type === 'del') {
          left += '<span class="diff-del">' + word + '</span>';
        } else {
          right += '<span class="diff-ins">' + word + '</span>';
        }
      });
    }

    // Một dòng gồm 3 cột: tên ô + nhãn | chữ bản cũ | chữ bản đang duyệt
    // (ws-compare-mobile-label chỉ hiện trên điện thoại, khi 3 cột xếp chồng lên nhau)
    const textClass = same ? 'ws-compare-text is-same' : 'ws-compare-text';
    html += '<div class="ws-compare-row">'
      + '<div class="ws-compare-key"><b>' + escapeHtml(key) + '</b>' + changeTag(oldText, newText) + '</div>'
      + '<div class="' + textClass + '"><span class="ws-compare-mobile-label">v' + escapeHtml(baseLabel) + '</span>' + left + '</div>'
      + '<div class="' + textClass + '"><span class="ws-compare-mobile-label is-current">v' + escapeHtml(currentLabel) + '</span>' + right + '</div>'
      + '</div>';
  });

  byId('compare-rows').innerHTML = html || '<p class="ws-note">Không có đoạn nào thay đổi.</p>';
  byId('compare-summary').textContent =
    changed + ' phần đã sửa, ' + added + ' phần thêm mới, ' + removed + ' phần đã bỏ so với v' + baseLabel + '.';
}

// Chuyển qua lại giữa bài đang duyệt và màn so sánh
function toggleCompare() {
  comparing = !comparing;
  articleCard.hidden = comparing;            // đang so sánh thì ẩn bài viết...
  compareCard.hidden = !comparing;           // ...và hiện màn so sánh (và ngược lại)
  byId('btn-compare-label').textContent = comparing ? 'Quay lại bản đang duyệt' : 'So sánh phiên bản';
  if (comparing) renderCompare();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

if (compareCard) {
  byId('btn-compare').addEventListener('click', toggleCompare);
  byId('compare-only-changed').addEventListener('change', renderCompare);
  baseSelect.addEventListener('change', function () {
    if (comparing) renderCompare();          // đổi bản để so sánh thì dựng lại bảng
  });
}


// ---------------------------------------------------------------------
// 6. Tab góp ý (Vòng này / Vòng trước) và hộp thoại Duyệt / Yêu cầu sửa
// ---------------------------------------------------------------------

// name = 'now' (góp ý vòng này) hoặc 'prev' (góp ý vòng trước)
function showTab(name) {
  byId('tab-now').hidden = name !== 'now';
  const prevPanel = byId('tab-prev');          // null nếu bài mới có 1 phiên bản
  if (prevPanel) prevPanel.hidden = name !== 'prev';
  document.querySelectorAll('.tab-btn').forEach(function (button) {
    const active = button.dataset.tab === name;
    button.classList.toggle('is-active', active);
    button.setAttribute('aria-selected', active ? 'true' : 'false');
  });
}
document.querySelectorAll('.tab-btn').forEach(function (button) {
  button.addEventListener('click', function () {
    showTab(button.dataset.tab);
  });
});

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
