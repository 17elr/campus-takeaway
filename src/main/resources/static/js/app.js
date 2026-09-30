document.querySelectorAll('[data-filter-zone]').forEach(zone => {
    const search = zone.querySelector('[data-search-input]');
    const status = zone.querySelector('[data-status-select]');
    const buttons = zone.querySelectorAll('[data-category]');
    let category = '';
    buttons.forEach(button => button.setAttribute('aria-pressed', String(button.classList.contains('active'))));
    const noResults = zone.querySelector('.no-results');
    if (noResults) noResults.setAttribute('role', 'status');
    const filter = () => {
        const query = (search?.value || '').trim().toLowerCase();
        let visible = 0;
        zone.querySelectorAll('[data-filter-item]').forEach(item => {
            const matches = item.textContent.toLowerCase().includes(query) && (!status?.value || item.dataset.status === status.value) && (!category || item.dataset.category === category);
            item.hidden = !matches; if (matches) visible++;
        });
        const empty = zone.querySelector('.no-results'); if (empty) empty.hidden = visible > 0 || zone.querySelectorAll('[data-filter-item]').length === 0;
    };
    search?.addEventListener('input', filter); status?.addEventListener('change', filter);
    buttons.forEach(button => button.addEventListener('click', () => { category = button.dataset.category; buttons.forEach(item => { item.classList.toggle('active', item === button); item.setAttribute('aria-pressed', String(item === button)); }); filter(); }));
});
document.querySelectorAll('form[data-confirm]').forEach(form => form.addEventListener('submit', event => { if (!window.confirm(form.dataset.confirm)) event.preventDefault(); }));
document.querySelectorAll('[data-demo-account]').forEach(button => button.addEventListener('click', () => {
    document.querySelector('#username').value = button.dataset.demoAccount;
    document.querySelector('#password').value = button.dataset.demoAccount + '123';
    document.querySelector('#username').focus();
}));
