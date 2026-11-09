// Tìm thẻ có id="article-card" và lưu vào biến articleCard.
const articleCard = document.getElementById('article-card');

// Hàm anchorOf nhận một node và trả về thẻ cha gần nhất có thuộc tính data-field.
// Để biết được xem vùng được chọn thuộc phần nào sapo hay title hay subtitle..
function anchorOf(node) {
    const element = node.nodeType === Node.ELEMENT_NODE ? node : node.parentElement;
    return element ? element.closest('[data-field]') : null;
}
// Đếm số ký tự từ đầu phần tử tới một điểm
function offsetIn(element, node, offset) {
    const range = document.createRange();   // tạo một vùng chọn mới (trống)
    range.setStart(element, 0);             // điểm đầu: đầu thẻ <p>
    range.setEnd(node, offset);             // điểm cuối: chỗ cần đo
    return range.toString().length;         // lấy chữ trong vùng đó, đếm độ dài
}
// Tìm form có id "comment-popover" và các thành phần bên trong nó
const popover = document.getElementById('comment-popover');
const popoverText = document.getElementById('popover-text');
const popoverSave = document.getElementById('popover-save');
let pendingAnchor = null;      // nhớ vị trí vừa bôi đen
//tạo một biến để nhớ vị trí vừa bôi đen. Ban đầu là null (chưa có gì).
//Chữ “pending” nghĩa là “đang chờ”: vị trí này chờ người dùng gõ xong và bấm “Thêm góp ý” mới dùng tới.

// Hàm ẩn khung góp ý
function hidePopover() {
    popover.hidden = true; //ẩn khung góp ý
    pendingAnchor = null; //Xóa vị trí đang chờ
    popoverText.value = ''; //xóa chữ người dùng đã gõ dở trong ô.
    popoverSave.disabled = true; //làm mờ nút “Thêm góp ý” trở lại.
}


//người dùng nhả chuột trong vùng này, chạy đoạn code bên trong
//“Khi nhả chuột trong article-card, chạy hàm bên trong.” event là thông tin về cú nhả chuột đó (nhả ở đâu, trúng phần tử nào).
articleCard.addEventListener('mouseup', function (event) {
    // bấm trong khung góp ý thì bỏ qua
    if (popover.contains(event.target)) return;
    const selection = window.getSelection(); // Lấy đối tượng Selection hiện tại
    //nếu không có vùng chọn nào, hoặc vùng chọn đang rỗng, thì thoát khỏi hàm luôn
    if (selection.rangeCount === 0 || selection.isCollapsed) {
        return;
    }
    //Ví dụ bạn bôi đen chữ “ghe hàng lớn” thì range ghi nhận điểm bắt đầu trước chữ “ghe” và điểm kết thúc sau chữ “lớn”
    const range = selection.getRangeAt(0); // range cho biết vùng chọn bắt đầu ở đâu
    const startAnchor = anchorOf(range.startContainer); // Tìm thẻ cha gần nhất có thuộc tính data-field cho điểm bắt đầu ý là nguyên cả thẻ <p> chứa chữ vừa bôi đen
    const endAnchor = anchorOf(range.endContainer); // Tìm thẻ cha gần nhất có thuộc tính data-field cho điểm kết thúc ý là nguyên cả thẻ <p> chứa chữ vừa bôi đen
    if (!startAnchor || startAnchor !== endAnchor) {
        console.log('Chỉ chọn trong một đoạn');
        return;
    }

    // Lưu thông tin về vị trí vừa bôi đen vào biến pendingAnchor
    pendingAnchor = {
        field: startAnchor.dataset.field, // Lấy giá trị của thuộc tính data-field của thẻ <p> chứa chữ vừa bôi đen"
        paragraph: startAnchor.dataset.paragraph, // Lấy giá trị của thuộc tính data-paragraph của thẻ <p> chứa chữ vừa bôi đen
        start: offsetIn(startAnchor, range.startContainer, range.startOffset),//offsetIn(<p>, mẩu 2, 0) đo từ đầu <p> đến đầu mẩu 2, chữ nằm giữa là "Giá vàng ", dài 9
        end: offsetIn(startAnchor, range.endContainer, range.endOffset)//offsetIn(<p>, mẩu 2, 9) đo từ đầu <p> đến cuối “tăng mạnh”, chữ nằm giữa là "Giá vàng tăng mạnh", dài 18

    };

    popover.anchorField.value = pendingAnchor.field;
    popover.anchorParagraph.value = pendingAnchor.paragraph;
    popover.anchorStart.value = pendingAnchor.start;
    popover.anchorEnd.value = pendingAnchor.end;

    //Tìm thẻ <p id="popover-quote"> trong khung và ghi chữ vừa bôi đen vào đó
    //để người dùng thấy mình đang góp ý cho đoạn chữ nào. textContent là thuộc tính để gán chữ cho một thẻ.
    document.getElementById('popover-quote').textContent = selection.toString();

    // Đặt khung ngay dưới chỗ bôi đen (tọa độ tính theo article-card)
    const selectionBox = range.getBoundingClientRect();
    const cardBox = articleCard.getBoundingClientRect();
    popover.hidden = false;
    popover.style.top = (selectionBox.bottom - cardBox.top + 10) + 'px';
    popover.style.left = (selectionBox.left - cardBox.left) + 'px';
});
// Chỉ bật nút "Thêm góp ý" khi đã gõ chữ
popoverText.addEventListener('input', function () {
    popoverSave.disabled = !popoverText.value.trim();

});

document.getElementById('popover-cancel').addEventListener('click', hidePopover);

// Ngược với offsetIn: từ vị trí [start, end) tạo lại vùng chữ để tô màu
function rangeFromOffsets(element, start, end) {
    const walker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT);   // đi qua từng mẩu chữ
    const range = document.createRange();
    let position = 0;          // đã đi qua bao nhiêu ký tự
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
    return null;               // vị trí không còn đúng thì bỏ qua
}

// Với mỗi góp ý có vị trí: tìm đúng đoạn, khoanh đúng chữ, bọc trong <mark>
document.querySelectorAll('.comment-card[data-field]').forEach(function (card) {
    const element = articleCard.querySelector(
        '[data-field="' + card.dataset.field + '"][data-paragraph="' + (card.dataset.paragraph || '1') + '"]');
    if (!element) return;
    const range = rangeFromOffsets(element, Number(card.dataset.start), Number(card.dataset.end));
    if (!range) return;
    const mark = document.createElement('mark');   // thẻ <mark> có nền vàng sẵn
    mark.dataset.cid = card.dataset.cid;
    try {
        range.surroundContents(mark);
    } catch (error) {
        // Hai góp ý chồng lên nhau thì không bọc được: bỏ qua, góp ý vẫn hiện ở danh sách
    }
});
