(() => {
  'use strict';

  const STORAGE_KEY = 'stayledger.invoices.v1';
  const form = document.getElementById('invoice-form');
  const propertyForm = document.getElementById('property-form');
  const invoiceDialog = document.getElementById('invoice-dialog');
  const previewDialog = document.getElementById('preview-dialog');
  const chargeList = document.getElementById('charge-list');
  const toast = document.getElementById('toast');
  let toastTimer;
  let invoices = loadInvoices();
  let property = loadProperty();

  function loadInvoices() {
    try {
      const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
      return Array.isArray(saved) ? saved : [];
    } catch (error) {
      console.warn('Saved billing records could not be read.', error);
      return [];
    }
  }

  function saveInvoices() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(invoices));
      return true;
    } catch (error) {
      showToast('Could not save this invoice in this browser. Check available storage.');
      return false;
    }
  }

  function loadProperty() {
    try {
      return JSON.parse(localStorage.getItem('stayledger.property.v1') || '{"propertyName":"","propertyAddress":"","propertyPhone":"","propertyEmail":""}');
    } catch (error) {
      return { propertyName: '', propertyAddress: '', propertyPhone: '', propertyEmail: '' };
    }
  }

  function saveProperty(event) {
    event.preventDefault();
    if (!propertyForm.reportValidity()) return;
    const nextProperty = Object.fromEntries(new FormData(propertyForm).entries());
    Object.keys(nextProperty).forEach(key => { nextProperty[key] = nextProperty[key].trim(); });
    try {
      localStorage.setItem('stayledger.property.v1', JSON.stringify(nextProperty));
      property = nextProperty;
      showToast('Hotel details saved. They will appear on new invoice previews.');
    } catch (error) {
      showToast('Could not save hotel details in this browser.');
    }
  }

  function localISODate(date = new Date()) {
    const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
    return local.toISOString().slice(0, 10);
  }

  function nextDate(dateString) {
    const date = new Date(`${dateString}T12:00:00`);
    date.setDate(date.getDate() + 1);
    return localISODate(date);
  }

  function formatMoney(amount) {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', currencyDisplay: 'code' }).format(Number(amount) || 0);
  }

  function formatDate(dateString) {
    if (!dateString) return '—';
    const date = new Date(`${dateString}T12:00:00`);
    return new Intl.DateTimeFormat('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }).format(date);
  }

  function escapeHTML(value) {
    return String(value ?? '').replace(/[&<>"']/g, character => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[character]);
  }

  function getStatus(invoice) {
    if (invoice.balance <= 0.009) return 'Paid';
    if (invoice.amountPaid > 0) return 'Partially paid';
    return 'Unpaid';
  }

  function statusMarkup(status) {
    const className = status === 'Paid' ? 'paid' : status === 'Unpaid' ? 'unpaid' : 'partial';
    return `<span class="badge ${className}">${escapeHTML(status)}</span>`;
  }

  function renderInvoiceRow(invoice) {
    return `<tr>
      <td><span class="invoice-number">${escapeHTML(invoice.number)}</span></td>
      <td class="guest-cell"><strong>${escapeHTML(invoice.guestName)}</strong><small>${escapeHTML(invoice.roomType)}${invoice.roomNumber ? ` · Room ${escapeHTML(invoice.roomNumber)}` : ''}</small></td>
      <td class="stay-cell"><strong>${formatDate(invoice.checkIn)} – ${formatDate(invoice.checkOut)}</strong><small>${invoice.nights} ${invoice.nights === 1 ? 'night' : 'nights'}</small></td>
      <td>${formatDate(invoice.issuedDate)}</td>
      <td class="amount-cell">${formatMoney(invoice.total)}</td>
      <td>${statusMarkup(getStatus(invoice))}</td>
      <td><button class="row-action" type="button" data-invoice="${escapeHTML(invoice.id)}">View</button></td>
    </tr>`;
  }

  function emptyRows(message) {
    return `<tr class="empty-row"><td colspan="7"><span class="empty-mark">▤</span><strong>No invoices found</strong><span>${escapeHTML(message)}</span></td></tr>`;
  }

  function renderInvoices() {
    const recentRows = document.getElementById('recent-rows');
    const allRows = document.getElementById('all-rows');
    const sorted = [...invoices].sort((a, b) => b.createdAt.localeCompare(a.createdAt));
    recentRows.innerHTML = sorted.length ? sorted.slice(0, 5).map(renderInvoiceRow).join('') : emptyRows('Create your first guest bill to see it here.');

    const query = document.getElementById('invoice-search').value.trim().toLowerCase();
    const status = document.getElementById('status-filter').value;
    const filtered = sorted.filter(invoice => {
      const matchesQuery = !query || `${invoice.number} ${invoice.guestName} ${invoice.phone} ${invoice.email} ${invoice.roomNumber}`.toLowerCase().includes(query);
      return matchesQuery && (status === 'all' || getStatus(invoice) === status);
    });
    allRows.innerHTML = filtered.length ? filtered.map(renderInvoiceRow).join('') : emptyRows(sorted.length ? 'Try another search or status filter.' : 'Create a guest bill to start your billing records.');

    const today = localISODate();
    const revenue = sorted.filter(invoice => invoice.issuedDate === today).reduce((sum, invoice) => sum + invoice.amountPaid, 0);
    const outstanding = sorted.reduce((sum, invoice) => sum + invoice.balance, 0);
    document.getElementById('stat-revenue').textContent = formatMoney(revenue);
    document.getElementById('stat-outstanding').textContent = formatMoney(outstanding);
    document.getElementById('stat-count').textContent = String(sorted.length);
  }

  function showToast(message) {
    toast.textContent = message;
    toast.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => toast.classList.remove('show'), 3200);
  }

  function setDefaultDates() {
    const today = localISODate();
    form.elements.checkIn.value = today;
    form.elements.checkOut.value = nextDate(today);
  }

  function addChargeRow(description = '', amount = '') {
    const row = document.createElement('div');
    row.className = 'charge-row';
    row.innerHTML = '<input type="text" maxlength="80" aria-label="Charge description" placeholder="e.g. Breakfast">' +
      '<input type="number" min="0" step="0.01" aria-label="Charge amount in rupees" placeholder="Amount">' +
      '<button type="button" class="remove-charge" aria-label="Remove charge">×</button>';
    row.querySelector('input[type="text"]').value = description;
    row.querySelector('input[type="number"]').value = amount;
    chargeList.append(row);
  }

  function openInvoiceForm() {
    form.reset();
    chargeList.replaceChildren();
    document.getElementById('form-error').classList.add('hidden');
    setDefaultDates();
    invoiceDialog.showModal();
    form.elements.guestName.focus();
  }

  function calculateInvoice(data) {
    const nights = Math.round((new Date(`${data.checkOut}T12:00:00`) - new Date(`${data.checkIn}T12:00:00`)) / 86400000);
    const roomSubtotal = nights * data.nightlyRate;
    const extrasSubtotal = data.charges.reduce((sum, charge) => sum + charge.amount, 0);
    const subtotal = roomSubtotal + extrasSubtotal;
    const discount = Math.min(subtotal, data.discount);
    const taxableAmount = Math.max(0, subtotal - discount);
    const tax = taxableAmount * data.taxRate / 100;
    const total = Math.round((taxableAmount + tax + Number.EPSILON) * 100) / 100;
    return { nights, roomSubtotal, extrasSubtotal, subtotal, discount, taxableAmount, tax, total };
  }

  function collectFormData() {
    const data = Object.fromEntries(new FormData(form).entries());
    data.guestName = data.guestName.trim();
    data.phone = data.phone.trim();
    data.email = data.email.trim();
    data.roomNumber = data.roomNumber.trim();
    data.guestCount = Math.max(1, Number(data.guestCount) || 1);
    data.nightlyRate = Number(data.nightlyRate) || 0;
    data.discount = Number(data.discount) || 0;
    data.taxRate = Number(data.taxRate) || 0;
    data.amountPaid = Number(data.amountPaid) || 0;
    data.charges = [...chargeList.querySelectorAll('.charge-row')].map(row => ({
      description: row.querySelector('input[type="text"]').value.trim(),
      amount: Number(row.querySelector('input[type="number"]').value) || 0
    })).filter(charge => charge.description && charge.amount > 0);
    return data;
  }

  function showFormError(message) {
    const box = document.getElementById('form-error');
    box.textContent = message;
    box.classList.remove('hidden');
  }

  function saveInvoice(event) {
    event.preventDefault();
    const data = collectFormData();
    if (!form.reportValidity()) return;
    if (!data.checkIn || !data.checkOut || data.checkOut <= data.checkIn) {
      showFormError('Check-out must be after the check-in date.');
      return;
    }
    if (data.charges.length !== chargeList.querySelectorAll('.charge-row').length) {
      showFormError('Each additional charge needs a description and an amount greater than zero.');
      return;
    }

    const totals = calculateInvoice(data);
    if (data.discount > totals.subtotal) {
      showFormError('Discount cannot be greater than the bill subtotal.');
      return;
    }
    if (data.amountPaid > totals.total) {
      showFormError('Amount paid cannot be greater than the invoice total.');
      return;
    }

    const sequence = Number(localStorage.getItem('stayledger.invoiceSequence') || 0) + 1;
    const invoice = {
      ...data,
      ...totals,
      property: { ...property },
      id: crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(36).slice(2)}`,
      number: `ST-${new Date().getFullYear()}-${String(sequence).padStart(4, '0')}`,
      issuedDate: localISODate(),
      createdAt: new Date().toISOString(),
      balance: Math.max(0, Math.round((totals.total - data.amountPaid + Number.EPSILON) * 100) / 100)
    };

    invoices.push(invoice);
    try {
      localStorage.setItem('stayledger.invoiceSequence', String(sequence));
    } catch (error) {
      // The invoice collection save below reports storage errors.
    }
    if (!saveInvoices()) {
      invoices.pop();
      return;
    }
    invoiceDialog.close();
    renderInvoices();
    showToast(`${invoice.number} saved for ${invoice.guestName}.`);
    openPreview(invoice);
  }

  function invoicePreview(invoice) {
    const chargeRows = [
      `<tr><td>Room · ${escapeHTML(invoice.roomType)}${invoice.roomNumber ? ` · Room ${escapeHTML(invoice.roomNumber)}` : ''}</td><td>${invoice.nights} × ${formatMoney(invoice.nightlyRate)}</td><td>${formatMoney(invoice.roomSubtotal)}</td></tr>`,
      ...invoice.charges.map(charge => `<tr><td>${escapeHTML(charge.description)}</td><td>1 × ${formatMoney(charge.amount)}</td><td>${formatMoney(charge.amount)}</td></tr>`)
    ].join('');
    const guestDetails = [invoice.phone, invoice.email, `${invoice.guestCount} ${invoice.guestCount === 1 ? 'guest' : 'guests'}`].filter(Boolean).map(escapeHTML).join(' · ');
    const invoiceProperty = invoice.property || property;
    const propertyName = invoiceProperty.propertyName || 'Hotel';
    const propertyContact = [invoiceProperty.propertyAddress, invoiceProperty.propertyPhone, invoiceProperty.propertyEmail].filter(Boolean).map(escapeHTML).join(' · ');
    return `<header class="print-head"><div><div class="print-brand">${escapeHTML(propertyName)}</div><div class="print-muted">${propertyContact || 'Hotel guest invoice'}</div></div><div class="print-meta"><strong>${escapeHTML(invoice.number)}</strong>Issued ${formatDate(invoice.issuedDate)}</div></header>
      <section class="print-section"><h3>Bill to</h3><div class="print-guest">${escapeHTML(invoice.guestName)}</div><div class="print-muted">${guestDetails || 'Guest details not provided'}</div></section>
      <section class="print-section"><h3>Stay details</h3><div>${formatDate(invoice.checkIn)} – ${formatDate(invoice.checkOut)} · ${invoice.nights} ${invoice.nights === 1 ? 'night' : 'nights'}</div><div class="print-muted">${escapeHTML(invoice.roomType)}${invoice.roomNumber ? ` · Room ${escapeHTML(invoice.roomNumber)}` : ''}</div></section>
      <section class="print-section"><h3>Itemized charges</h3><table class="print-lines"><thead><tr><th>DESCRIPTION</th><th>QTY / RATE</th><th>AMOUNT</th></tr></thead><tbody>${chargeRows}</tbody></table>
       <div class="print-totals"><div class="print-total-row"><span>Subtotal</span><span>${formatMoney(invoice.subtotal)}</span></div>${invoice.discount ? `<div class="print-total-row"><span>Discount</span><span>−${formatMoney(invoice.discount)}</span></div>` : ''}${invoice.tax ? `<div class="print-total-row"><span>Tax (${invoice.taxRate}%)</span><span>${formatMoney(invoice.tax)}</span></div>` : ''}<div class="print-total-row grand"><span>Total</span><span>${formatMoney(invoice.total)}</span></div><div class="print-total-row"><span>Paid · ${escapeHTML(invoice.paymentMethod)}</span><span>${formatMoney(invoice.amountPaid)}</span></div><div class="print-total-row"><strong>Balance due</strong><strong>${formatMoney(invoice.balance)}</strong></div></div>
      </section><p class="print-footer">Thank you for staying with us.</p>`;
  }

  function openPreview(invoice) {
    document.getElementById('print-invoice-content').innerHTML = invoicePreview(invoice);
    previewDialog.showModal();
  }

  function switchView(view) {
    const showInvoices = view === 'invoices';
    const showSettings = view === 'settings';
    document.getElementById('dashboard-view').classList.toggle('hidden', showInvoices || showSettings);
    document.getElementById('invoices-view').classList.toggle('hidden', !showInvoices);
    document.getElementById('settings-view').classList.toggle('hidden', !showSettings);
    document.getElementById('page-crumb').textContent = showInvoices ? 'Invoices' : showSettings ? 'Hotel details' : 'Overview';
    document.querySelectorAll('.nav-item').forEach(link => link.classList.toggle('active', link.dataset.view === view));
  }

  document.getElementById('today-label').textContent = new Intl.DateTimeFormat('en-IN', { weekday: 'short', day: 'numeric', month: 'short' }).format(new Date());
  for (const [key, value] of Object.entries(property)) {
    if (propertyForm.elements[key]) propertyForm.elements[key].value = value || '';
  }
  document.getElementById('new-invoice-top').addEventListener('click', openInvoiceForm);
  document.getElementById('new-invoice-list').addEventListener('click', openInvoiceForm);
  document.getElementById('add-charge').addEventListener('click', () => addChargeRow());
  document.getElementById('cancel-invoice').addEventListener('click', () => invoiceDialog.close());
  document.getElementById('close-preview').addEventListener('click', () => previewDialog.close());
  document.getElementById('print-invoice').addEventListener('click', () => window.print());
  document.getElementById('invoice-search').addEventListener('input', renderInvoices);
  document.getElementById('status-filter').addEventListener('change', renderInvoices);
  form.addEventListener('submit', saveInvoice);
  propertyForm.addEventListener('submit', saveProperty);

  document.addEventListener('click', event => {
    const navLink = event.target.closest('[data-view]');
    if (navLink) {
      event.preventDefault();
      switchView(navLink.dataset.view);
      return;
    }
    const removeButton = event.target.closest('.remove-charge');
    if (removeButton) {
      removeButton.closest('.charge-row').remove();
      return;
    }
    const invoiceButton = event.target.closest('[data-invoice]');
    if (invoiceButton) {
      const invoice = invoices.find(item => item.id === invoiceButton.dataset.invoice);
      if (invoice) openPreview(invoice);
    }
  });

  form.elements.checkIn.addEventListener('change', () => {
    if (form.elements.checkIn.value && (!form.elements.checkOut.value || form.elements.checkOut.value <= form.elements.checkIn.value)) {
      form.elements.checkOut.value = nextDate(form.elements.checkIn.value);
    }
  });

  renderInvoices();
})();
