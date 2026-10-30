// Màn thẩm định bài viết của Trưởng phòng (workspacereview.html).
// File được nạp ở cuối <body> nên mọi phần tử HTML đã có sẵn khi chạy.
const esc = s => s.replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
const articleCard = document.getElementById('article-card');

// ---------- Toast ----------
let toastTimer;
function toast(msg, isError) {
  const t = document.getElementById('toast');
  t.textContent = msg;
  t.classList.remove('hidden');
  t.classList.toggle('bg-error', !!isError);
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => t.classList.add('hidden'), 3500);
}
(() => {
  const t = document.getElementById('toast');
  if (t.dataset.message) toast(t.dataset.message, t.dataset.error === 'true');
})();

// ---------- Hỏi lại trước
//  khi gửi form có data-confirm (ví dụ: Xóa góp ý) ----------
document.querySelectorAll('form[data-confirm]').forEach(form => {
  form.addEventListener('submit', e => {
    if (!confirm(form.dataset.confirm)) e.preventDefault();
  });
});

// ---------- Vị trí ký tự trong một đoạn ----------
// Vị trí (đếm từ 0) của điểm (node, offset) tính từ đầu phần tử el = độ dài chữ đứng trước điểm đó
function offsetIn(el, node, offset) {
  const r = document.createRange();
  r.setStart(el, 0);
  r.setEnd(node, offset);
  return r.toString().length;
}
// Ngược lại: từ vị trí [start, end) trong el tạo ra Range để tô màu
function rangeFromOffsets(el, start, end) {
  const walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT);
  const range = document.createRange();
  let pos = 0, node, startSet = false;
  while ((node = walker.nextNode())) {
    const len = node.textContent.length;
    if (!startSet && start <= pos + len) { range.setStart(node, start - pos); startSet = true; }
    if (startSet && end <= pos + len) { range.setEnd(node, end - pos); return range; }
    pos += len;
  }
  return null;
}
function anchorElement(field, paragraph) {
  return articleCard.querySelector(`[data-field="${field}"][data-paragraph="${paragraph}"]`);
}

// ---------- Tô màu các góp ý đã lưu ----------
document.querySelectorAll('.comment-card[data-field]').forEach(card => {
  const el = anchorElement(card.dataset.field, card.dataset.paragraph);
  if (!el) return;
  const range = rangeFromOffsets(el, Number(card.dataset.start), Number(card.dataset.end));
  if (!range) return;
  const mark = document.createElement('mark');
  mark.dataset.cid = card.dataset.cid;
  mark.className = 'bg-tertiary-fixed text-on-tertiary-fixed font-semibold px-0.5 rounded cursor-pointer';
  try { range.surroundContents(mark); } catch (_) { /* hai góp ý chồng lên nhau: bỏ qua phần tô màu */ }
});

function flash(el) { el.classList.remove('flash'); void el.offsetWidth; el.classList.add('flash'); }

// Bấm "Xem vị trí" ở thẻ góp ý -> cuộn tới chữ được tô
document.getElementById('tab-now').addEventListener('click', e => {
  if (!e.target.closest('[data-goto]')) return;
  const card = e.target.closest('.comment-card');
  if (comparing) toggleCompare();
  const mark = document.querySelector(`mark[data-cid="${card.dataset.cid}"]`);
  if (mark) { mark.scrollIntoView({ behavior: 'smooth', block: 'center' }); flash(mark); }
});
// Bấm vào chữ được tô -> nhảy tới thẻ góp ý
articleCard.addEventListener('click', e => {
  const m = e.target.closest('mark[data-cid]');
  if (!m) return;
  showTab('now');
  const card = document.querySelector(`.comment-card[data-cid="${m.dataset.cid}"]`);
  if (card) { card.scrollIntoView({ behavior: 'smooth', block: 'nearest' }); flash(card); }
});

// ---------- Bôi đen -> khung góp ý ----------
const popover = document.getElementById('comment-popover');
if (popover) {
  const popText = document.getElementById('popover-text');
  const popSave = document.getElementById('popover-save');
  let pending = null;
  const hidePopover = () => { popover.classList.add('hidden'); pending = null; popText.value = ''; popSave.disabled = true; };
  window.hidePopover = hidePopover;
  const anchorOf = node => { const el = node.nodeType === 1 ? node : node.parentElement; return el ? el.closest('[data-field]') : null; };

  articleCard.addEventListener('mouseup', e => {
    if (popover.contains(e.target)) return;
    setTimeout(() => {
      const sel = window.getSelection();
      if (!sel.rangeCount || sel.isCollapsed) return;
      const range = sel.getRangeAt(0);
      const a1 = anchorOf(range.startContainer), a2 = anchorOf(range.endContainer);
      if (!sel.toString().trim() || !a1) return;
      if (a1 !== a2) { toast('Chỉ chọn trong một đoạn để góp ý.'); return; }
      pending = {
        field: a1.dataset.field,
        paragraph: a1.dataset.paragraph,
        start: offsetIn(a1, range.startContainer, range.startOffset),
        end: offsetIn(a1, range.endContainer, range.endOffset)
      };
      document.getElementById('popover-anchor').textContent = a1.dataset.anchor;
      document.getElementById('popover-quote').textContent = `“${sel.toString()}”`;
      const r = range.getBoundingClientRect(), c = articleCard.getBoundingClientRect();
      popover.classList.remove('hidden');
      popover.style.top = (r.bottom - c.top + 10) + 'px';
      popover.style.left = Math.max(16, Math.min(r.left - c.left, c.width - popover.offsetWidth - 16)) + 'px';
      popText.focus();
    }, 0);
  });
  popText.addEventListener('input', () => { popSave.disabled = !popText.value.trim(); });
  document.querySelectorAll('[data-chip]').forEach(b => b.addEventListener('click', () => {
    popText.value = (popText.value ? popText.value.trim() + ' ' : '') + b.textContent.trim() + '.';
    popSave.disabled = false;
    popText.focus();
  }));
  document.getElementById('popover-close').addEventListener('click', hidePopover);
  document.getElementById('popover-cancel').addEventListener('click', hidePopover);

  // Lưu góp ý: điền vào form ẩn rồi gửi lên server (server kiểm tra lại vị trí và tự lấy đoạn trích)
  popSave.addEventListener('click', () => {
    if (!pending) return;
    const form = document.getElementById('comment-form');
    form.anchorField.value = pending.field;
    form.anchorParagraph.value = pending.paragraph;
    form.anchorStart.value = pending.start;
    form.anchorEnd.value = pending.end;
    form.content.value = popText.value.trim();
    popSave.disabled = true;
    form.submit();
  });
}

// ---------- So sánh phiên bản ----------
// Đọc dữ liệu một version từ khối ẩn #version-data: { "Tít": "...", "Sapo": "...", "Đoạn 1": "..." }
function readVersion(label) {
  const box = document.querySelector(`#version-data > [data-label="${label}"]`);
  const out = {};
  if (box) box.querySelectorAll('[data-key]').forEach(el => { out[el.dataset.key] = el.textContent.trim(); });
  return out;
}
function tokenize(s) { return s.match(/\s+|[^\s]+/g) || []; }
// So sánh theo từng từ (thuật toán dãy con chung dài nhất - LCS)
function diffTokens(a, b) {
  const n = a.length, m = b.length;
  const dp = Array.from({ length: n + 1 }, () => new Uint16Array(m + 1));
  for (let i = n - 1; i >= 0; i--) for (let j = m - 1; j >= 0; j--)
    dp[i][j] = a[i] === b[j] ? dp[i + 1][j + 1] + 1 : Math.max(dp[i + 1][j], dp[i][j + 1]);
  const ops = []; let i = 0, j = 0;
  while (i < n && j < m) {
    if (a[i] === b[j]) { ops.push(['eq', a[i]]); i++; j++; }
    else if (dp[i + 1][j] >= dp[i][j + 1]) ops.push(['del', a[i++]]);
    else ops.push(['ins', b[j++]]);
  }
  while (i < n) ops.push(['del', a[i++]]);
  while (j < m) ops.push(['ins', b[j++]]);
  return ops;
}

const compareCard = document.getElementById('compare-card');
const baseSelect = document.getElementById('compare-base');
let comparing = false;

function renderCompare() {
  const versionsBox = document.querySelectorAll('#version-data > [data-label]');
  const currentLabel = versionsBox[versionsBox.length - 1].dataset.label;
  const baseLabel = baseSelect.value;
  const oldV = readVersion(baseLabel), newV = readVersion(currentLabel);
  const onlyChanged = document.getElementById('compare-only-changed').checked;
  document.getElementById('compare-head-old').textContent = `v${baseLabel} · bản trước`;

  // Thứ tự các ô: Tít, Tít phụ, Sapo, rồi Đoạn 1..n (lấy số đoạn nhiều hơn của hai bản)
  const countParagraphs = v => Object.keys(v).filter(k => k.startsWith('Đoạn ')).length;
  const keys = ['Tít', 'Tít phụ', 'Sapo'];
  for (let i = 1; i <= Math.max(countParagraphs(oldV), countParagraphs(newV)); i++) keys.push('Đoạn ' + i);

  let changed = 0, added = 0, removed = 0, html = '';
  keys.forEach(key => {
    const oldT = oldV[key] || '', newT = newV[key] || '';
    if (!oldT && !newT) return;
    const same = oldT === newT;
    if (!same) { if (!oldT) added++; else if (!newT) removed++; else changed++; }
    if (onlyChanged && same) return;
    let left = '', right = '';
    if (!oldT) {
      left = `<span class="text-secondary italic">(Chưa có trong v${baseLabel})</span>`;
      right = `<span class="diff-ins">${esc(newT)}</span>`;
    } else if (!newT) {
      left = `<span class="diff-del">${esc(oldT)}</span>`;
      right = '<span class="text-secondary italic">(Đã bỏ)</span>';
    } else {
      diffTokens(tokenize(oldT), tokenize(newT)).forEach(([t, v]) => {
        if (t === 'eq') { left += esc(v); right += esc(v); }
        else if (t === 'del') left += `<span class="diff-del">${esc(v)}</span>`;
        else right += `<span class="diff-ins">${esc(v)}</span>`;
      });
    }
    const tag = same ? '<span class="font-label-sm text-label-sm text-secondary">Không đổi</span>'
      : !oldT ? '<span class="font-label-sm text-label-sm font-bold text-success-badge">Thêm mới</span>'
      : !newT ? '<span class="font-label-sm text-label-sm font-bold text-error">Đã bỏ</span>'
      : '<span class="font-label-sm text-label-sm font-bold text-on-tertiary-container">Đã sửa</span>';
    html += `<div class="grid grid-cols-1 md:grid-cols-[6rem_1fr_1fr] gap-x-space-md gap-y-1 py-space-sm border-b border-surface-container-high last:border-0">
      <div class="flex md:flex-col gap-2 md:gap-1"><span class="font-label-md text-label-md font-bold">${esc(key)}</span>${tag}</div>
      <div class="font-body-md text-body-md leading-relaxed whitespace-pre-line ${same ? 'text-secondary' : ''}"><span class="md:hidden font-label-sm text-label-sm text-secondary block">v${baseLabel}</span>${left}</div>
      <div class="font-body-md text-body-md leading-relaxed whitespace-pre-line ${same ? 'text-secondary' : ''}"><span class="md:hidden font-label-sm text-label-sm text-primary block">v${currentLabel}</span>${right}</div>
    </div>`;
  });
  document.getElementById('compare-rows').innerHTML = html || '<p class="text-secondary font-body-sm text-body-sm">Không có đoạn nào thay đổi.</p>';
  document.getElementById('compare-summary').textContent =
    `${changed} phần đã sửa, ${added} phần thêm mới, ${removed} phần đã bỏ so với v${baseLabel}.`;
}

function toggleCompare() {
  comparing = !comparing;
  if (window.hidePopover) window.hidePopover();
  articleCard.classList.toggle('hidden', comparing);
  compareCard.classList.toggle('hidden', !comparing);
  document.getElementById('btn-compare-label').textContent = comparing ? 'Quay lại bản đang duyệt' : 'So sánh phiên bản';
  if (comparing) renderCompare();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}
if (compareCard) {
  document.getElementById('btn-compare').addEventListener('click', toggleCompare);
  document.getElementById('compare-only-changed').addEventListener('change', renderCompare);
  baseSelect.addEventListener('change', () => { if (comparing) renderCompare(); });
}

// ---------- Tab góp ý ----------
function showTab(name) {
  document.getElementById('tab-now').classList.toggle('hidden', name !== 'now');
  const prev = document.getElementById('tab-prev');
  if (prev) prev.classList.toggle('hidden', name !== 'prev');
  document.querySelectorAll('.tab-btn').forEach(b => {
    const on = b.dataset.tab === name;
    b.classList.toggle('bg-primary', on); b.classList.toggle('text-on-primary', on); b.classList.toggle('font-semibold', on);
    b.classList.toggle('text-on-surface-variant', !on);
  });
}
document.querySelectorAll('.tab-btn').forEach(b => b.addEventListener('click', () => showTab(b.dataset.tab)));

// ---------- Hộp thoại Duyệt / Yêu cầu sửa ----------
const modals = document.querySelectorAll('#modal-revise, #modal-approve');
const closeModals = () => modals.forEach(m => m.classList.add('hidden'));
document.querySelectorAll('[data-close]').forEach(b => b.addEventListener('click', closeModals));
modals.forEach(m => m.addEventListener('click', e => { if (e.target === m) closeModals(); }));
const btnRevise = document.getElementById('btn-revise');
if (btnRevise) {
  btnRevise.addEventListener('click', () => {
    document.getElementById('modal-revise').classList.remove('hidden');
    document.getElementById('revise-feedback').focus();
  });
  const feedback = document.getElementById('revise-feedback');
  feedback.addEventListener('input', () => { document.getElementById('revise-submit').disabled = !feedback.value.trim(); });
  document.getElementById('btn-approve').addEventListener('click', () => document.getElementById('modal-approve').classList.remove('hidden'));
}
document.addEventListener('keydown', e => {
  if (e.key === 'Escape') { if (window.hidePopover) window.hidePopover(); closeModals(); }
});
