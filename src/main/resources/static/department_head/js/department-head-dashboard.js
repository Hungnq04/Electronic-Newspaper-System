(() => {
  'use strict';
  const root = document.querySelector('.dh-dashboard');
  if (!root || root.dataset.dhInitialized === 'true') return;
  root.dataset.dhInitialized = 'true';

  const list = root.querySelector('#dh-draft-list');
  if (!list) return;
  const cards = Array.from(list.querySelectorAll('.dh-draft-card'));
  const search = root.querySelector('#dh-draft-search');
  const globalSearch = root.querySelector('#dh-global-search');
  const category = root.querySelector('#dh-category-filter');
  const type = root.querySelector('#dh-type-filter');
  const emptyState = root.querySelector('#dh-empty-state');
  // Số "bản ghi" trên đầu bảng: cập nhật theo số dòng đang hiện sau khi lọc
  const visibleCount = root.querySelector('#dh-visible-count');
  const priorityButtons = Array.from(root.querySelectorAll('[data-dh-priority]'));
  const tagButtons = Array.from(root.querySelectorAll('[data-dh-tag]'));
  const sortButtons = Array.from(root.querySelectorAll('[data-dh-sort]'));
  const tabButtons = Array.from(root.querySelectorAll('[data-dh-tab]'));
  const state = { tab: 'article', priority: 'all', tag: 'all', sort: 'deadline' };

  // If the page is rendered by Thymeleaf, hide static fallback cards when real entries exist.
  const renderedCards = cards.filter(card => !card.classList.contains('dh-draft-fallback'));
  if (renderedCards.length > 0) cards.filter(card => card.classList.contains('dh-draft-fallback')).forEach(card => card.hidden = true);

  const normalize = value => (value || '').toString().normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
  const update = () => {
    // Hai ô tìm (trên topbar và trong bộ lọc) luôn cùng nội dung, nên chỉ cần lấy một ô
    const query = normalize(search?.value || globalSearch?.value);
    let visible = 0;
    cards.forEach(card => {
      const haystack = normalize((card.dataset.title || '') + ' ' + card.textContent);
      const tags = normalize(card.dataset.tags).split(/\s+/).filter(Boolean);
      const matches = (!card.dataset.kind || card.dataset.kind === state.tab)
        && (state.priority === 'all' || normalize(card.dataset.priority) === state.priority)
        && (state.tag === 'all' || tags.includes(state.tag))
        && (!category || category.value === 'all' || card.dataset.category === category.value)
        && (!type || type.value === 'all' || card.dataset.type === type.value)
        && (!query || haystack.includes(query));
      card.hidden = !matches;
      if (matches) visible++;
    });
    if (emptyState) emptyState.hidden = visible !== 0;
    if (visibleCount) visibleCount.textContent = visible;
  };

  const setActive = (items, active, attr) => items.forEach(item => {
    const isActive = item === active;
    item.classList.toggle('is-active', isActive);
    if (attr) item.setAttribute(attr, isActive ? 'true' : 'false');
  });

  tabButtons.forEach(button => button.addEventListener('click', () => {
    state.tab = button.dataset.dhTab;
    setActive(tabButtons, button, 'aria-selected');
    // Topic entries use the same card schema; when none are loaded the empty state is shown.
    update();
  }));
  priorityButtons.forEach(button => button.addEventListener('click', () => {
    state.priority = button.dataset.dhPriority;
    state.tag = 'all';
    setActive(priorityButtons, button);
    tagButtons.forEach(tagButton => tagButton.classList.remove('is-active'));
    update();
  }));
  tagButtons.forEach(button => button.addEventListener('click', () => {
    state.tag = button.dataset.dhTag;
    state.priority = 'all';
    priorityButtons.forEach(priorityButton => priorityButton.classList.toggle('is-active', priorityButton.dataset.dhPriority === 'all'));
    setActive(tagButtons, button);
    update();
  }));
  [search, globalSearch].filter(Boolean).forEach(input => input.addEventListener('input', () => {
    // Gõ ở ô nào thì chép sang ô còn lại (kể cả khi xóa trắng), giống màn /reporter/tasks
    const other = input === search ? globalSearch : search;
    if (other) other.value = input.value;
    update();
  }));
  [category, type].filter(Boolean).forEach(select => select.addEventListener('change', update));

  sortButtons.forEach(button => button.addEventListener('click', () => {
    state.sort = button.dataset.dhSort;
    setActive(sortButtons, button);
    const visibleCards = cards.filter(card => !card.classList.contains('dh-draft-fallback'));
    const rank = card => Number(state.sort === 'newest' ? card.dataset.createdOrder : state.sort === 'oldest' ? -(Number(card.dataset.createdOrder) || 0) : card.dataset.deadlineOrder) || 999999;
    // Các dòng nằm trong <tbody id="dh-draft-list">: gắn lại theo thứ tự mới là bảng tự sắp xếp
    visibleCards.sort((a, b) => rank(a) - rank(b)).forEach(card => list.appendChild(card));
    update();
  }));

  const dispatch = (action, entryId = null, element = null) => {
    root.dispatchEvent(new CustomEvent('dh:action', {
      bubbles: true,
      detail: { action, entryId, element }
    }));
  };
  root.addEventListener('click', event => {
    const target = event.target.closest('[data-dh-action]');
    if (!target) return;
    const action = target.dataset.dhAction;
    if (action === 'clear-filters') {
      if (search) search.value = '';
      if (globalSearch) globalSearch.value = '';
      if (category) category.value = 'all';
      if (type) type.value = 'all';
      state.priority = 'all'; state.tag = 'all'; state.tab = 'article';
      priorityButtons.forEach(button => button.classList.toggle('is-active', button.dataset.dhPriority === 'all'));
      tagButtons.forEach(button => button.classList.remove('is-active'));
      setActive(tabButtons, tabButtons.find(button => button.dataset.dhTab === 'article'), 'aria-selected');
      update();
      return;
    }
    if (target.tagName === 'A') event.preventDefault();
    dispatch(action, target.dataset.entryId || null, target);
  });

  // Integration hook: connect these UI actions to your existing controller/API in the host layout.
  root.addEventListener('dh:action', event => {
    // Intentionally no fake approval/revision request is sent here.
    // Your application can listen for this bubbling event and call its real endpoint.
  });
  update();
})();
