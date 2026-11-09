// Trang soạn bài: trình soạn thảo kiểu Word dựa trên contenteditable + execCommand, tự động lưu nháp qua AJAX.
(() => {
    const A = window.ARTICLE;
    const $ = (s, root = document) => root.querySelector(s);
    const $$ = (s, root = document) => [...root.querySelectorAll(s)];
    const editor = $('#editor');
    const titleEl = $('#title');
    const subtitleEl = $('#subtitle');
    const sapoEl = $('#sapo');
    const categoryEl = $('#category');
    const csrfToken = $('meta[name="_csrf"]')?.content;
    const csrfHeader = $('meta[name="_csrf_header"]')?.content;

    if (window.lucide) window.lucide.createIcons();

    /* ---------- Tiện ích chung ---------- */
    const escapeHtml = s => String(s ?? '').replace(/[&<>"']/g, c => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[c]));
    const formatTime = iso => {
        const d = iso ? new Date(iso) : new Date();
        const p = n => String(n).padStart(2, '0');
        return `${p(d.getHours())}:${p(d.getMinutes())} ${p(d.getDate())}/${p(d.getMonth() + 1)}`;
    };
    let toastTimer;
    function toast(message, isError = false) {
        const t = $('#toast');
        t.textContent = message;
        t.classList.toggle('error', isError);
        t.hidden = false;
        clearTimeout(toastTimer);
        toastTimer = setTimeout(() => { t.hidden = true; }, 3200);
    }
    function autosize(t) {
        if (!t) return;
        t.style.height = 'auto';
        t.style.height = t.scrollHeight + 'px';
    }

    /* ---------- Hộp thoại ---------- */
    // Chỉ mở một hộp thoại tại một thời điểm (vd. danh sách phiên bản <-> chi tiết phiên bản)
    function openModal(id) { const m = document.getElementById(id); if (!m) return; closeModals(); m.hidden = false; m.querySelector('.modal-box')?.scrollTo(0, 0); }
    function closeModals() { $$('.modal').forEach(m => { m.hidden = true; }); }
    document.addEventListener('click', e => {
        const opener = e.target.closest('[data-open]');
        if (opener) { openModal(opener.dataset.open); return; }
        if (e.target.closest('[data-close]') || e.target.classList.contains('modal')) closeModals();
    });
    document.addEventListener('keydown', e => { if (e.key === 'Escape') closeModals(); });

    /* ---------- Chỉ số bài viết ---------- */
    function updateStats() {
        const text = editor.innerText.replace(/ /g, ' ');
        const words = text.trim() ? text.trim().split(/\s+/).length : 0;
        const chars = text.replace(/\n/g, '').length;
        const paragraphs = $$('p', editor).filter(p => p.textContent.trim()).length;
        const headings = $$('h2, h3, h4', editor).length;
        const images = $$('img', editor).length;
        const videos = $$('video', editor).length;
        const quotes = $$('blockquote', editor).length;
        const tables = $$('table', editor).length;

        $('#wordCount').textContent = words.toLocaleString('vi-VN');
        $('#charCount').textContent = chars.toLocaleString('vi-VN');
        $('#readTime').textContent = `~${Math.max(1, Math.ceil(words / 220))} phút`;
        $('#lengthHint').textContent = words < 300 ? 'Tin ngắn' : words < 800 ? 'Bài tiêu chuẩn' : 'Chuyên luận sâu';
        $('#structure').textContent = `${paragraphs} đoạn · ${headings} tiêu đề phụ · ${images} ảnh · ${videos} video · ${quotes} trích dẫn · ${tables} bảng`;
        $('#paraCount').textContent = `${paragraphs} đoạn`;
        $('#sbWords').textContent = words.toLocaleString('vi-VN');
        $('#sbChars').textContent = chars.toLocaleString('vi-VN');
        $('#titleCount').textContent = titleEl.value.length;
        $('#sapoCount').textContent = sapoEl.value.length;
        editor.classList.toggle('is-empty', !text.trim() && images === 0 && videos === 0 && tables === 0);
    }

    /* ---------- Đếm ngược hạn nộp ---------- */
    const deadlineEl = $('#deadlineLeft');
    function updateDeadline() {
        if (!deadlineEl || !deadlineEl.dataset.deadline) return;
        const diff = new Date(deadlineEl.dataset.deadline) - new Date();
        const abs = Math.abs(diff);
        const d = Math.floor(abs / 86400000), h = Math.floor(abs % 86400000 / 3600000), m = Math.floor(abs % 3600000 / 60000);
        const span = (d ? `${d} ngày ` : '') + (d || h ? `${h} giờ ` : '') + `${m} phút`;
        deadlineEl.textContent = diff >= 0 ? `Còn ${span}` : `Quá hạn ${span}`;
        deadlineEl.classList.toggle('late', diff < 0 || diff < 3600000);
    }
    updateDeadline();
    setInterval(updateDeadline, 30000);

    /* ---------- Xem trước ---------- */
    $('#previewBtn').addEventListener('click', () => {
        const body = $('#previewBody');
        const hero = $('#heroImg');
        body.innerHTML = '';
        body.insertAdjacentHTML('beforeend',
            `<p class="pv-cat">${escapeHtml(categoryEl.value)}</p>` +
            `<h1>${escapeHtml(titleEl.value)}</h1>` +
            (subtitleEl.value.trim() ? `<p class="pv-sub">${escapeHtml(subtitleEl.value)}</p>` : '') +
            (sapoEl.value.trim() ? `<p class="pv-sapo">${escapeHtml(sapoEl.value)}</p>` : '') +
            (hero ? `<figure class="pv-hero"><img src="${escapeHtml(hero.getAttribute('src'))}" alt=""><figcaption>${escapeHtml($('#heroCaption')?.textContent)}</figcaption></figure>` : ''));
        const content = document.createElement('div');
        content.className = 'prose';
        content.innerHTML = editor.innerHTML;
        body.appendChild(content);
        openModal('previewModal');
    });

    [titleEl, sapoEl].forEach(autosize);
    updateStats();

    if (!A.editable) return;

    /* ================= Từ đây trở xuống chỉ chạy khi được sửa ================= */
    document.execCommand('defaultParagraphSeparator', false, 'p');
    if (!editor.innerHTML.trim()) editor.innerHTML = '<p><br></p>';

    // Giữ vùng chọn trong editor để các ô select / màu (làm mất focus) vẫn áp dụng đúng chỗ
    let savedRange = null;
    document.addEventListener('selectionchange', () => {
        const sel = window.getSelection();
        if (sel.rangeCount && editor.contains(sel.anchorNode)) {
            savedRange = sel.getRangeAt(0).cloneRange();
            refreshToolbar();
        }
    });
    function restoreSelection() {
        editor.focus();
        const sel = window.getSelection();
        if (savedRange) {
            sel.removeAllRanges();
            sel.addRange(savedRange);
        } else {
            const r = document.createRange();
            r.selectNodeContents(editor);
            r.collapse(false);
            sel.removeAllRanges();
            sel.addRange(r);
        }
    }
    function exec(cmd, value = null) {
        restoreSelection();
        document.execCommand('styleWithCSS', false, true);
        document.execCommand(cmd, false, value);
        changed();
        refreshToolbar();
    }

    /* ---------- Trạng thái thanh công cụ ---------- */
    const STATE_CMDS = ['bold', 'italic', 'underline', 'strikeThrough', 'superscript', 'subscript',
        'insertUnorderedList', 'insertOrderedList', 'justifyLeft', 'justifyCenter', 'justifyRight', 'justifyFull'];
    const blockSelect = $('#blockFormat'), fontSelect = $('#fontName'), sizeSelect = $('#fontSize'), lineSelect = $('#lineHeight');
    const hasOption = (select, value) => [...select.options].some(o => o.value === value);
    function refreshToolbar() {
        STATE_CMDS.forEach(cmd => {
            const btn = $(`.toolbar [data-cmd="${cmd}"]`);
            if (btn) btn.classList.toggle('active', document.queryCommandState(cmd));
        });
        const block = (document.queryCommandValue('formatBlock') || 'p').toLowerCase();
        blockSelect.value = hasOption(blockSelect, block) ? block : 'p';
        const node = window.getSelection().anchorNode;
        const el = node && (node.nodeType === 1 ? node : node.parentElement);
        if (!el || !editor.contains(el)) return;
        const style = getComputedStyle(el);
        const family = style.fontFamily.split(',')[0].replace(/["']/g, '').trim();
        fontSelect.value = hasOption(fontSelect, family) ? family : '';
        const size = String(Math.round(parseFloat(style.fontSize)));
        sizeSelect.value = hasOption(sizeSelect, size) ? size : '';
        lineSelect.value = '';
    }

    /* ---------- Các nút định dạng ---------- */
    $$('.toolbar [data-cmd]').forEach(btn => {
        btn.addEventListener('mousedown', e => e.preventDefault());
        btn.addEventListener('click', () => exec(btn.dataset.cmd));
    });
    $$('.toolbar button').forEach(btn => btn.addEventListener('mousedown', e => e.preventDefault()));

    blockSelect.addEventListener('change', () => exec('formatBlock', `<${blockSelect.value}>`));
    fontSelect.addEventListener('change', () => { if (fontSelect.value) exec('fontName', fontSelect.value); });
    sizeSelect.addEventListener('change', () => { if (sizeSelect.value) applyFontSize(sizeSelect.value); });
    lineSelect.addEventListener('change', () => {
        if (!lineSelect.value) return;
        selectedBlocks().forEach(b => { b.style.lineHeight = lineSelect.value; });
        changed();
    });

    // execCommand('fontSize') chỉ nhận 1-7 nên đánh dấu bằng size 7 rồi thay bằng span có cỡ chữ px thật
    function applyFontSize(px) {
        restoreSelection();
        if (window.getSelection().isCollapsed) { toast('Hãy bôi đen đoạn chữ cần đổi cỡ'); return; }
        document.execCommand('styleWithCSS', false, false);
        document.execCommand('fontSize', false, '7');
        const created = [];
        $$('font[size="7"]', editor).forEach(font => {
            const span = document.createElement('span');
            span.style.fontSize = px + 'px';
            while (font.firstChild) span.appendChild(font.firstChild);
            font.replaceWith(span);
            created.push(span);
        });
        $$('span[style*="xxx-large"]', editor).forEach(span => { span.style.fontSize = px + 'px'; created.push(span); });
        // Bỏ cỡ chữ cũ của các span con để cỡ mới có hiệu lực
        created.forEach(span => $$('[style*="font-size"]', span).forEach(child => { child.style.fontSize = ''; }));
        if (created.length) {
            const r = document.createRange();
            r.setStartBefore(created[0]);
            r.setEndAfter(created[created.length - 1]);
            const sel = window.getSelection();
            sel.removeAllRanges();
            sel.addRange(r);
            savedRange = r.cloneRange();
        }
        changed();
    }

    function selectedBlocks() {
        if (!savedRange) return [];
        const blocks = $$('p, h2, h3, h4, li, blockquote, pre, figcaption, div', editor);
        return blocks.filter(b => savedRange.intersectsNode(b) && !$('p, h2, h3, h4, li, pre', b));
    }

    // Màu chữ / màu nền
    $('#foreColor').addEventListener('change', e => {
        $('#foreSwatch').style.background = e.target.value;
        exec('foreColor', e.target.value);
    });
    $('#backColor').addEventListener('change', e => {
        $('#backSwatch').style.background = e.target.value;
        exec('hiliteColor', e.target.value);
    });

    // Liên kết
    function insertLink() {
        const url = prompt('Nhập đường dẫn liên kết:', 'https://');
        if (!url) return;
        if (!/^(https?:\/\/|mailto:|\/)/i.test(url.trim())) { toast('Đường dẫn phải bắt đầu bằng http(s)://', true); return; }
        restoreSelection();
        if (window.getSelection().isCollapsed) {
            exec('insertHTML', `<a href="${escapeHtml(url.trim())}" target="_blank" rel="noopener">${escapeHtml(url.trim())}</a>`);
        } else {
            exec('createLink', url.trim());
        }
    }
    $('#linkBtn').addEventListener('click', insertLink);

    // Ảnh
    function insertFigure(src, caption, sourceId) {
        closeModals();
        const idAttr = sourceId ? ` data-source-id="${escapeHtml(sourceId)}"` : '';
        exec('insertHTML', `<figure class="article-figure"><img src="${escapeHtml(src)}" alt="${escapeHtml(caption)}"${idAttr}>` +
            `<figcaption>${escapeHtml(caption || 'Chú thích ảnh')}</figcaption></figure><p><br></p>`);
    }
    function insertVideo(src, caption) {
        closeModals();
        exec('insertHTML', `<figure class="article-figure article-video"><video src="${escapeHtml(src)}" controls preload="metadata" playsinline></video>` +
            (caption ? `<figcaption>${escapeHtml(caption)}</figcaption>` : '') + '</figure><p><br></p>');
    }
    $('#insertImageGrid').addEventListener('click', e => {
        const btn = e.target.closest('.insert-image');
        if (btn) insertFigure(btn.dataset.src, btn.dataset.caption, btn.dataset.id);
    });

    /* ---------- Chèn ảnh / video từ tư liệu đề tài ---------- */
    // Bài đã lưu: gắn tư liệu vào article_source trước (để làm ảnh đại diện / hiện trong tư liệu bài) rồi chèn.
    // Bài mới chưa có id: chèn thẳng vào nội dung.
    async function insertTopicMedia(btn) {
        const { sourceId, src, caption } = btn.dataset;
        const isVideo = btn.dataset.video === 'true';
        if (!A.topicMediaUrl) {
            if (isVideo) insertVideo(src, caption); else insertFigure(src, caption);
            return;
        }
        btn.disabled = true;
        try {
            const res = await fetch(A.topicMediaUrl, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded', [csrfHeader]: csrfToken },
                body: new URLSearchParams({ sourceId })
            });
            const data = await res.json().catch(() => ({}));
            if (!res.ok || !data.success) { toast(data.message || 'Không chèn được tư liệu', true); return; }
            if (data.kind === 'VIDEO') {
                insertVideo(data.url, data.caption);
            } else {
                if (!$(`#insertImageGrid .insert-image[data-id="${data.articleSourceId}"]`)) addImageOption(data);
                insertFigure(data.url, data.caption || data.title, data.articleSourceId);
                if (data.featured) showHero(data.url, data.caption || data.title, data.title);
            }
            toast(data.message);
        } catch (ignored) {
            toast('Mất kết nối khi chèn tư liệu', true);
        } finally {
            btn.disabled = false;
        }
    }
    $('#topicMediaGrid')?.addEventListener('click', e => {
        const btn = e.target.closest('.insert-topic-media');
        if (btn) insertTopicMedia(btn);
    });

    /* ---------- Tải ảnh / video từ máy lên Cloudinary ---------- */
    const IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
    const VIDEO_TYPES = ['video/mp4', 'video/webm', 'video/quicktime'];
    const MB = 1024 * 1024;
    const mediaInput = $('#mediaFile'), uploadBtn = $('#uploadMediaBtn'), mediaDrop = $('#mediaDrop');
    const progress = $('#uploadProgress');
    let mediaFile = null;

    // Kiểm tra trước ở trình duyệt để khỏi gửi tệp lớn rồi mới bị từ chối; server vẫn kiểm tra lại
    function pickMedia(file) {
        mediaFile = null;
        uploadBtn.disabled = true;
        if (!file) return;
        const isImage = IMAGE_TYPES.includes(file.type), isVideo = VIDEO_TYPES.includes(file.type);
        if (!isImage && !isVideo) { toast('Chỉ hỗ trợ ảnh JPG, PNG, WebP, GIF và video MP4, WebM, MOV', true); return; }
        if (file.size > (isImage ? 10 : 100) * MB) { toast(isImage ? 'Ảnh tối đa 10MB' : 'Video tối đa 100MB', true); return; }
        mediaFile = file;
        uploadBtn.disabled = false;
        $('#mediaFileName').textContent = `${file.name} (${(file.size / MB).toFixed(1)} MB)`;
    }
    function addImageOption(data) {
        const attrs = `data-src="${escapeHtml(data.url)}" data-caption="${escapeHtml(data.caption || data.title)}" data-id="${data.articleSourceId}"`;
        const inner = `<img src="${escapeHtml(data.url)}" alt=""><span>${escapeHtml(data.title)}</span>`;
        $('#insertImageGrid').insertAdjacentHTML('beforeend', `<button type="button" class="image-option insert-image" ${attrs}>${inner}</button>`);
        $('#featuredGrid')?.insertAdjacentHTML('beforeend',
            `<button type="button" class="image-option choose-featured" ${attrs} data-title="${escapeHtml(data.title)}">${inner}</button>`);
        $('#noImageHint')?.remove();
        const change = $('#heroChangeBtn');
        if (change) change.hidden = false;
    }
    function uploadMedia() {
        if (!mediaFile || !A.mediaUrl) return;
        const form = new FormData();
        form.append('file', mediaFile);
        form.append('caption', $('#mediaCaption').value.trim());
        const xhr = new XMLHttpRequest();
        xhr.open('POST', A.mediaUrl);
        xhr.setRequestHeader(csrfHeader, csrfToken);
        xhr.upload.onprogress = e => { if (e.lengthComputable) $('span', progress).style.width = `${Math.round(e.loaded / e.total * 100)}%`; };
        const done = () => { uploadBtn.disabled = !mediaFile; progress.hidden = true; $('span', progress).style.width = '0'; };
        xhr.onload = () => {
            let data = {};
            try { data = JSON.parse(xhr.responseText); } catch (ignored) { /* lỗi không phải JSON */ }
            done();
            if (xhr.status !== 200 || !data.success) { toast(data.message || 'Tải tệp lên không thành công', true); return; }
            if (data.kind === 'VIDEO') {
                insertVideo(data.url, data.caption);
            } else {
                addImageOption(data);
                insertFigure(data.url, data.caption || data.title, data.articleSourceId);
                if (data.featured) showHero(data.url, data.caption || data.title, data.title);
            }
            mediaFile = null;
            mediaInput.value = '';
            $('#mediaCaption').value = '';
            $('#mediaFileName').textContent = 'Chọn hoặc kéo thả ảnh / video từ máy';
            uploadBtn.disabled = true;
            toast(data.message);
        };
        xhr.onerror = () => { done(); toast('Mất kết nối khi tải tệp lên', true); };
        uploadBtn.disabled = true;
        progress.hidden = false;
        xhr.send(form);
    }
    if (mediaInput) {
        mediaInput.addEventListener('change', () => pickMedia(mediaInput.files[0]));
        uploadBtn.addEventListener('click', uploadMedia);
        ['dragenter', 'dragover'].forEach(t => mediaDrop.addEventListener(t, e => { e.preventDefault(); mediaDrop.classList.add('over'); }));
        ['dragleave', 'drop'].forEach(t => mediaDrop.addEventListener(t, () => mediaDrop.classList.remove('over')));
        mediaDrop.addEventListener('drop', e => { e.preventDefault(); pickMedia(e.dataTransfer.files[0]); });
    }
    $('#insertUrlImage').addEventListener('click', () => {
        const url = $('#imageUrl').value.trim();
        if (!/^(https?:\/\/|\/)/i.test(url)) { toast('Đường dẫn ảnh không hợp lệ', true); return; }
        insertFigure(url, $('#imageCaption').value.trim());
        $('#imageUrl').value = '';
        $('#imageCaption').value = '';
    });

    // Bảng: lưới chọn số hàng x cột như Word
    const tableGrid = $('#tableGrid'), grid = $('.grid', tableGrid), tableSize = $('#tableSize');
    for (let r = 1; r <= 8; r++) for (let c = 1; c <= 8; c++) {
        const cell = document.createElement('span');
        cell.dataset.r = r;
        cell.dataset.c = c;
        grid.appendChild(cell);
    }
    grid.addEventListener('mouseover', e => {
        const { r, c } = e.target.dataset;
        if (!r) return;
        $$('span', grid).forEach(s => s.classList.toggle('on', +s.dataset.r <= +r && +s.dataset.c <= +c));
        tableSize.textContent = `${r} hàng × ${c} cột`;
    });
    grid.addEventListener('mousedown', e => e.preventDefault());
    grid.addEventListener('click', e => {
        const { r, c } = e.target.dataset;
        if (!r) return;
        const head = `<tr>${'<th>Tiêu đề</th>'.repeat(+c)}</tr>`;
        const rows = `<tr>${'<td><br></td>'.repeat(+c)}</tr>`.repeat(Math.max(0, +r - 1));
        tableGrid.hidden = true;
        exec('insertHTML', `<table><thead>${head}</thead><tbody>${rows}</tbody></table><p><br></p>`);
    });
    $('#tableBtn').addEventListener('click', () => { tableGrid.hidden = !tableGrid.hidden; });
    document.addEventListener('click', e => { if (!e.target.closest('.table-picker')) tableGrid.hidden = true; });

    /* ---------- Bàn phím ---------- */
    document.addEventListener('keydown', e => {
        const mod = e.ctrlKey || e.metaKey;
        if (mod && e.key.toLowerCase() === 's') {
            e.preventDefault();
            // Bài mới chưa có id: Ctrl+S tạo bản nháp bằng cách gửi form
            if (A.isNew) $('#articleForm').requestSubmit($('#createDraftBtn'));
            else saveDraft(true);
        }
    });
    editor.addEventListener('keydown', e => {
        const mod = e.ctrlKey || e.metaKey;
        if (mod && e.key.toLowerCase() === 'k') { e.preventDefault(); insertLink(); }
        if (e.key === 'Tab') {
            e.preventDefault();
            const inList = window.getSelection().anchorNode?.parentElement?.closest('li');
            if (inList) exec(e.shiftKey ? 'outdent' : 'indent');
            else if (!e.shiftKey) exec('insertHTML', '&emsp;');
        }
    });

    /* ---------- Dán nội dung: bỏ rác định dạng từ Word / web ---------- */
    const KEEP_ATTRS = ['href', 'src', 'alt', 'colspan', 'rowspan'];
    const KEEP_STYLES = ['font-weight', 'font-style', 'text-decoration', 'text-align'];
    function cleanPastedHtml(html) {
        const doc = new DOMParser().parseFromString(html, 'text/html');
        $$('script, style, meta, link, title, xml, iframe, object, embed, form, input, button', doc).forEach(n => n.remove());
        $$('*', doc.body).forEach(el => {
            const keptStyle = KEEP_STYLES.map(p => el.style.getPropertyValue(p) ? `${p}:${el.style.getPropertyValue(p)}` : '').filter(Boolean).join(';');
            [...el.attributes].forEach(a => { if (!KEEP_ATTRS.includes(a.name)) el.removeAttribute(a.name); });
            if (keptStyle) el.setAttribute('style', keptStyle);
            if (el.hasAttribute('href') && /^\s*javascript:/i.test(el.getAttribute('href'))) el.removeAttribute('href');
            if (/^(O:P|FONT)$/i.test(el.tagName)) el.replaceWith(...el.childNodes);
        });
        return doc.body.innerHTML.replace(/<!--[\s\S]*?-->/g, '');
    }
    editor.addEventListener('paste', e => {
        const data = e.clipboardData;
        if (!data) return;
        e.preventDefault();
        const html = data.getData('text/html');
        if (html) {
            exec('insertHTML', cleanPastedHtml(html));
        } else {
            const text = data.getData('text/plain');
            const paragraphs = text.split(/\r?\n\s*\r?\n/).map(p => `<p>${escapeHtml(p).replace(/\r?\n/g, '<br>')}</p>`);
            exec('insertHTML', paragraphs.length > 1 ? paragraphs.join('') : escapeHtml(text));
        }
    });

    /* ---------- Lưu nháp tự động ---------- */
    const saveState = $('#saveState'), saveBox = $('.save-state');
    let dirty = false, saving = false;
    function setSaveState(text, cls = '') {
        saveState.textContent = text;
        saveBox.classList.remove('dirty', 'error');
        if (cls) saveBox.classList.add(cls);
    }
    function changed() {
        dirty = true;
        setSaveState('Có thay đổi chưa lưu…', 'dirty');
        updateStats();
    }
    function contentHtml() {
        const clone = editor.cloneNode(true);
        $$('.selected', clone).forEach(n => n.classList.remove('selected'));
        const empty = !clone.textContent.trim() && !$('img, video, table', clone);
        return empty ? '' : clone.innerHTML;
    }
    function payload() {
        return { title: titleEl.value, subtitle: subtitleEl.value, sapo: sapoEl.value, category: categoryEl.value, content: contentHtml() };
    }
    async function saveDraft(manual) {
        if (saving) return;
        if (!titleEl.value.trim()) { if (manual) toast('Tiêu đề không được để trống', true); return; }
        saving = true;
        dirty = false;
        setSaveState('Đang lưu…');
        saveBtn.disabled = true;
        try {
            const res = await fetch(A.draftUrl, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', [csrfHeader]: csrfToken },
                body: JSON.stringify(payload())
            });
            const data = await res.json().catch(() => ({}));
            if (!res.ok || !data.success) throw new Error(data.message || 'Phiên đăng nhập có thể đã hết hạn.');
            setSaveState('Đã lưu lúc ' + formatTime(data.updatedAt));
            $('#updatedAt').textContent = formatTime(data.updatedAt);
            if (manual) toast('Đã lưu nháp');
        } catch (err) {
            dirty = true;
            setSaveState('Chưa lưu được', 'error');
            toast(err.message, true);
        } finally {
            saving = false;
            saveBtn.disabled = false;
        }
    }
    const saveBtn = $('#saveBtn');
    if (!A.isNew) {
        saveBtn.addEventListener('click', () => saveDraft(true));
        setInterval(() => { if (dirty) saveDraft(false); }, 30000);
    }

    editor.addEventListener('input', changed);
    [titleEl, subtitleEl, sapoEl, categoryEl].forEach(el => el.addEventListener('input', () => { autosize(el); changed(); }));
    titleEl.addEventListener('keydown', e => { if (e.key === 'Enter') { e.preventDefault(); sapoEl.focus(); } });
    window.addEventListener('beforeunload', e => { if (dirty) { e.preventDefault(); e.returnValue = ''; } });

    /* ---------- Khôi phục phiên bản: hỏi lại trước khi gửi (form có data-confirm) ---------- */
    document.querySelectorAll('form.restore-form[data-confirm]').forEach(form => {
        form.addEventListener('submit', e => {
            if (!confirm(form.dataset.confirm)) { e.preventDefault(); return; }
            dirty = false;   // nội dung sẽ được thay bằng bản khôi phục, không hỏi "rời trang?" nữa
        });
    });

    /* ---------- Nộp bài ---------- */
    $('#articleForm').addEventListener('submit', e => {
        const isSubmit = e.submitter?.value === 'submit' || (!A.isNew && !e.submitter);
        // Nộp lại (bài đã có phiên bản) dùng câu hỏi riêng; ô ghi chú "đã sửa gì" là bắt buộc (thuộc tính required)
        const isResubmit = e.submitter?.dataset.resubmit === 'true';
        const question = isResubmit
            ? 'Nộp lại bài cho Trưởng ban duyệt? Hệ thống sẽ tạo phiên bản mới và khóa chỉnh sửa cho tới khi bài được trả về.'
            : 'Nộp bản thảo cho Trưởng ban duyệt? Sau khi nộp, bài sẽ bị khóa chỉnh sửa.';
        if (isSubmit && !confirm(question)) { e.preventDefault(); return; }
        $('#contentInput').value = contentHtml();
        dirty = false;
    });

    /* ---------- Ảnh đại diện ---------- */
    function showHero(src, caption, title) {
        if (!$('#heroImg')) {
            const empty = $('#heroEmpty');
            empty.insertAdjacentHTML('afterend', '<figure class="hero" id="heroFigure"><img id="heroImg" alt="">' +
                '<figcaption><i data-lucide="info"></i><span>Chú thích: <span id="heroCaption"></span> · Nguồn: <strong id="heroSource"></strong></span></figcaption></figure>');
            empty.remove();
            if (window.lucide) window.lucide.createIcons();
        }
        $('#heroImg').src = src;
        $('#heroImg').alt = title || '';
        $('#heroCaption').textContent = caption || '';
        $('#heroSource').textContent = title || '';
        $$('.choose-featured').forEach(b => b.classList.toggle('selected', b.dataset.src === src));
    }
    $('#featuredGrid')?.addEventListener('click', async e => {
        const btn = e.target.closest('.choose-featured');
        if (!btn) return;
        const body = new URLSearchParams({ articleSourceId: btn.dataset.id });
        const res = await fetch(A.featuredUrl, { method: 'POST', headers: { [csrfHeader]: csrfToken }, body });
        const data = await res.json().catch(() => ({}));
        if (!res.ok || !data.success) { toast(data.message || 'Không đổi được ảnh đại diện', true); return; }
        showHero(btn.dataset.src, btn.dataset.caption, btn.dataset.title);
        closeModals();
        toast(data.message);
    });

    // Chọn cả khối ảnh khi bấm vào ảnh trong nội dung (để xóa / di chuyển dễ hơn)
    editor.addEventListener('click', e => {
        $$('figure.selected', editor).forEach(f => f.classList.remove('selected'));
        const fig = e.target.tagName === 'IMG' && e.target.closest('figure');
        if (fig) {
            fig.classList.add('selected');
            const r = document.createRange();
            r.selectNode(fig);
            const sel = window.getSelection();
            sel.removeAllRanges();
            sel.addRange(r);
        }
    });

    refreshToolbar();
})();
