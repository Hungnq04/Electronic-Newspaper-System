// =====================================================================
// Phần góp ý cho phóng viên trong trình soạn thảo (templates/reporter/article-feedback.html)
//
// File này làm 2 việc và KHÔNG chèn gì vào nội dung bài, nên không ảnh hưởng tự động lưu nháp:
//   1. "Tìm trong bài": tìm đoạn chữ được góp ý trong bản đang sửa rồi bôi đen (chọn) nó
//   2. Popup "Chi tiết phiên bản": tô vàng đúng chỗ Trưởng ban / Thư ký góp ý,
//      bấm qua lại giữa góp ý và chữ được tô vàng
//
// Việc mở / đóng popup do editor.js lo (nút có data-open="versionDetail-..."), file này không đụng tới.
// Bọc trong (() => { ... })() để tên hàm ở đây không trùng với file JS khác trên cùng trang.
// =====================================================================
(() => {

  // ---------------------------------------------------------------------
  // Hàm dùng chung
  // ---------------------------------------------------------------------

  // Từ vị trí ký tự [start, end) bên trong el, tạo "Range" (vùng chữ, giống vùng bôi đen bằng chuột).
  // Ký tự ở vị trí end không tính. Trả về null nếu vị trí không còn đúng.
  function rangeFromOffsets(el, start, end) {
    // TreeWalker đi lần lượt qua từng mẩu chữ (text node) bên trong el
    const walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT);
    const range = document.createRange();
    let position = 0;           // đã đi qua bao nhiêu ký tự
    let startFound = false;
    let node = walker.nextNode();
    while (node) {
      const length = node.textContent.length;
      if (!startFound && start <= position + length) {
        range.setStart(node, start - position);
        startFound = true;
      }
      if (startFound && end <= position + length) {
        range.setEnd(node, end - position);
        return range;
      }
      position += length;
      node = walker.nextNode();
    }
    return null;
  }

  // Đổi dấu cách đặc biệt (&nbsp;) thành dấu cách thường để so chữ cho đúng.
  // Đổi 1 ký tự thành 1 ký tự nên vị trí các ký tự khác không bị lệch.
  function normalizeSpaces(text) {
    return text.replace(/ /g, ' ');
  }

  // Nháy viền vàng để người dùng thấy chỗ vừa được cuộn tới
  function flash(el) {
    el.classList.remove('fb-flash');
    void el.offsetWidth;                         // bắt trình duyệt vẽ lại, để hiệu ứng chạy lại từ đầu
    el.classList.add('fb-flash');
  }


  // ---------------------------------------------------------------------
  // 1. "Tìm trong bài": bôi đen đoạn chữ được góp ý trong bản đang sửa
  // ---------------------------------------------------------------------

  // Ô nhập của tít, tít phụ, sapo trong trình soạn thảo (id trong articles/editor.html)
  const FIELD_INPUTS = { TITLE: 'title', SUBTITLE: 'subtitle', SAPO: 'sapo' };

  // card = thẻ góp ý (.fb-comment) có data-field (vị trí) và data-quote (đoạn chữ được góp ý)
  function findInEditor(card) {
    const quote = normalizeSpaces(card.dataset.quote || '');
    const notFound = card.querySelector('[data-fb-not-found]');
    notFound.hidden = true;
    if (!quote) return;

    const inputId = FIELD_INPUTS[card.dataset.field];
    let found = false;
    if (inputId) {
      // Tít, tít phụ, sapo là ô nhập chữ: dùng setSelectionRange để bôi đen trong ô
      const input = document.getElementById(inputId);
      const index = input ? normalizeSpaces(input.value).indexOf(quote) : -1;
      if (index >= 0) {
        input.focus({ preventScroll: true });
        input.setSelectionRange(index, index + quote.length);
        input.scrollIntoView({ behavior: 'smooth', block: 'center' });
        found = true;
      }
    } else {
      // Nội dung bài: tìm trong vùng soạn thảo #editor rồi đặt vùng chọn (giống bôi đen bằng chuột)
      const editor = document.getElementById('editor');
      const index = editor ? normalizeSpaces(editor.textContent).indexOf(quote) : -1;
      const range = index >= 0 ? rangeFromOffsets(editor, index, index + quote.length) : null;
      if (range) {
        editor.focus({ preventScroll: true });
        const selection = window.getSelection();
        selection.removeAllRanges();
        selection.addRange(range);
        range.startContainer.parentElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
        found = true;
      }
    }

    // Không tìm thấy: phóng viên đã sửa / xóa đoạn đó, báo ngay dưới thẻ góp ý
    if (!found) notFound.hidden = false;
  }

  document.querySelectorAll('.fb-comment [data-fb-find]').forEach(function (button) {
    button.addEventListener('click', function () {
      findInEditor(button.closest('.fb-comment'));
    });
  });


  // ---------------------------------------------------------------------
  // 2. Popup "Chi tiết phiên bản": tô vàng chữ được góp ý
  // ---------------------------------------------------------------------

  // Với mỗi popup phiên bản: bọc chữ được góp ý trong <mark class="fb-mark">
  document.querySelectorAll('.modal[id^="versionDetail-"]').forEach(function (modal) {
    const version = modal.querySelector('.fb-version');        // nội dung phiên bản (fragment versionBody)
    if (!version) return;
    modal.querySelectorAll('.fb-old-comment[data-field]').forEach(function (item) {
      // Tít / tít phụ / sapo chỉ có 1 đoạn: góp ý cũ có thể không lưu số đoạn (NULL) thì coi là đoạn 1
      const paragraph = item.dataset.paragraph || '1';
      const selector = '[data-field="' + item.dataset.field + '"][data-paragraph="' + paragraph + '"]';
      const el = version.querySelector(selector);
      if (!el) return;
      const range = rangeFromOffsets(el, Number(item.dataset.start), Number(item.dataset.end));
      if (!range) return;
      const mark = document.createElement('mark');
      mark.className = 'fb-mark';
      mark.dataset.commentId = item.dataset.commentId;        // nhớ chữ vàng này thuộc góp ý nào
      try {
        range.surroundContents(mark);
      } catch (error) {
        // Hai góp ý chồng lên nhau thì không bọc được: bỏ qua phần tô màu, góp ý vẫn hiện trong danh sách
      }
    });
  });

  // Cuộn tới chữ được tô vàng của một góp ý trong popup đang mở
  function showMark(commentId) {
    const mark = document.querySelector('.modal:not([hidden]) mark.fb-mark[data-comment-id="' + commentId + '"]');
    if (mark) {
      mark.scrollIntoView({ behavior: 'smooth', block: 'center' });
      flash(mark);
    }
  }

  // Trong popup: bấm góp ý -> cuộn tới chữ vàng; bấm chữ vàng -> cuộn tới góp ý
  document.addEventListener('click', function (event) {
    const item = event.target.closest('.fb-old-comment[data-field]');
    if (item) {
      showMark(item.dataset.commentId);
      return;
    }
    const mark = event.target.closest('mark.fb-mark');
    if (mark) {
      const modal = mark.closest('.modal');
      const comment = modal.querySelector('.fb-old-comment[data-comment-id="' + mark.dataset.commentId + '"]');
      if (comment) {
        comment.scrollIntoView({ behavior: 'smooth', block: 'center' });
        flash(comment);
      }
    }
  });

  // Nút "Xem vị trí ở bản v…" trên thẻ "Góp ý cần sửa":
  // editor.js mở popup trước (nhờ data-open) và cuộn popup về đầu, nên phải đợi một chút rồi mới cuộn tới chữ vàng
  document.querySelectorAll('.fb-comment [data-fb-show-old]').forEach(function (button) {
    button.addEventListener('click', function () {
      const commentId = button.closest('.fb-comment').dataset.commentId;
      setTimeout(function () {
        showMark(commentId);
      }, 50);
    });
  });
})();
