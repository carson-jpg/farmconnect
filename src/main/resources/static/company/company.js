/* FarmConnect company portal. Plain JavaScript, no libraries. All text goes in through textContent, never innerHTML. */
(function () {
  'use strict';

  var TOKEN_KEY = 'fc_company_token', USER_KEY = 'fc_company_user';
  var state = { token: sessionStorage.getItem(TOKEN_KEY), user: null, company: null, counties: null, menuOpen: false, unread: 0 };
  try { state.user = JSON.parse(sessionStorage.getItem(USER_KEY) || 'null'); } catch (e) { state.user = null; }

  var CATEGORIES = { AGRI_SUPPLIER: 'Agricultural supplier (seeds, fertiliser, feeds, equipment)', PRODUCE_BUYER: 'Bulk produce buyer / processor', TRANSPORT: 'Transport and logistics', SERVICE_PROVIDER: 'Agricultural service provider' };
  var STATUS = { PENDING_VERIFICATION: 'Pending verification', UNDER_REVIEW: 'Under review', VERIFIED: 'Verified', REJECTED: 'Rejected', SUSPENDED: 'Suspended' };
  var STATUS_TONE = { PENDING_VERIFICATION: 'amber', UNDER_REVIEW: 'blue', VERIFIED: 'green', REJECTED: 'red', SUSPENDED: 'grey' };
  var DOC_TYPES = { REGISTRATION_CERT: 'Certificate of registration *', KRA_PIN_CERT: 'KRA PIN certificate *', TRADE_LICENCE: 'Trade / business licence', SECTOR_LICENCE: 'Sector licence (KEPHIS, PCPB, NTSA...)', REP_ID: 'Representative ID', OTHER: 'Other' };

  // ---------- icons ----------
  var ICONS = {
    home: ['M3 10.5 12 3l9 7.5V21a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1z'],
    box: ['M21 8l-9-5-9 5v8l9 5 9-5z', 'M3.3 7.5 12 12.5l8.7-5', 'M12 22V12.5'],
    clipboard: ['M9 4h6a1 1 0 0 1 1 1v1H8V5a1 1 0 0 1 1-1z', 'M8 6H6a1 1 0 0 0-1 1v13a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V7a1 1 0 0 0-1-1h-2', 'M9 12h6', 'M9 16h4'],
    wallet: ['M3 7a2 2 0 0 1 2-2h13v4', 'M3 7v11a2 2 0 0 0 2 2h15a1 1 0 0 0 1-1V10a1 1 0 0 0-1-1H5a2 2 0 0 1-2-2z', 'M16.5 14.5h.01'],
    file: ['M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z', 'M14 3v5h5', 'M9 13h6', 'M9 17h6'],
    send: ['M22 2 11 13', 'M22 2l-7 20-4-9-9-4z'],
    truck: ['M1 6h13v10H1z', 'M14 9h4l4 4v3h-8z', 'M6 19.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z', 'M18 19.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z'],
    wrench: ['M14.7 6.3a4 4 0 0 0-5.4 5.4L3 18l3 3 6.3-6.3a4 4 0 0 0 5.4-5.4l-2.6 2.6-2.4-.6-.6-2.4z'],
    users: ['M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2', 'M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z', 'M23 21v-2a4 4 0 0 0-3-3.9', 'M16 3.1a4 4 0 0 1 0 7.8'],
    bell: ['M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9z', 'M13.7 21a2 2 0 0 1-3.4 0'],
    chart: ['M3 3v18h18', 'M7 14l4-4 3 3 5-6'],
    user: ['M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2', 'M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z'],
    shield: ['M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z', 'M9 12l2 2 4-4'],
    logout: ['M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4', 'M16 17l5-5-5-5', 'M21 12H9'],
    menu: ['M3 12h18', 'M3 6h18', 'M3 18h18'],
    x: ['M18 6 6 18', 'M6 6l12 12'],
    plus: ['M12 5v14', 'M5 12h14'],
    download: ['M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4', 'M7 10l5 5 5-5', 'M12 15V3'],
    printer: ['M6 9V2h12v7', 'M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2', 'M6 14h12v8H6z'],
    check: ['M20 6 9 17l-5-5'],
    refresh: ['M23 4v6h-6', 'M1 20v-6h6', 'M3.5 9a9 9 0 0 1 14.9-3.4L23 10', 'M1 14l4.6 4.4A9 9 0 0 0 20.5 15'],
    lock: ['M5 11h14v10H5z', 'M8 11V7a4 4 0 0 1 8 0v4'],
    inbox: ['M22 12h-6l-2 3h-4l-2-3H2', 'M5.5 5.1 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.5-6.9A2 2 0 0 0 16.7 4H7.3a2 2 0 0 0-1.8 1.1z'],
    cart: ['M6 6h15l-2 9H8z', 'M6 6 5 3H2', 'M9 20a1 1 0 1 0 0-2 1 1 0 0 0 0 2z', 'M18 20a1 1 0 1 0 0-2 1 1 0 0 0 0 2z'],
    leaf: ['M11 20A7 7 0 0 1 4 13c0-6 5-9 16-9 0 11-3 16-9 16z', 'M4 20c2-6 5-9 9-12'],
    alert: ['M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z', 'M12 9v4', 'M12 17h.01']
  };
  function icon(name) {
    var ns = 'http://www.w3.org/2000/svg', svg = document.createElementNS(ns, 'svg');
    svg.setAttribute('viewBox', '0 0 24 24'); svg.setAttribute('class', 'ico'); svg.setAttribute('aria-hidden', 'true');
    (ICONS[name] || ICONS.box).forEach(function (d) { var p = document.createElementNS(ns, 'path'); p.setAttribute('d', d); svg.appendChild(p); });
    return svg;
  }

  // ---------- DOM helpers ----------
  function add(parent, child) {
    if (child == null || child === false) return;
    if (Array.isArray(child)) { child.forEach(function (c) { add(parent, c); }); return; }
    parent.appendChild(typeof child === 'object' ? child : document.createTextNode(String(child)));
  }
  function h(tag, attrs) {
    var el = document.createElement(tag);
    for (var k in (attrs || {})) {
      var v = attrs[k];
      if (v == null || v === false) continue;
      if (k === 'class') el.className = v;
      else if (k === 'text') el.textContent = v;
      else if (k === 'value') el.value = v;
      else if (k.indexOf('on') === 0) el.addEventListener(k.slice(2), v);
      else el.setAttribute(k, v === true ? '' : v);
    }
    for (var i = 2; i < arguments.length; i++) add(el, arguments[i]);
    return el;
  }
  function when(iso) { return iso ? new Date(iso).toLocaleString('en-KE', { dateStyle: 'medium', timeStyle: 'short' }) : '-'; }
  function dayText(d) { return d ? new Date(d + 'T00:00:00').toLocaleDateString('en-KE', { dateStyle: 'medium' }) : '-'; }
  function inDays(n) { var d = new Date(); d.setDate(d.getDate() + n); return d.toISOString().slice(0, 10); }
  function money(n) { return 'KSh ' + Number(n || 0).toLocaleString('en-KE', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  function moneyK(n) { return 'KSh ' + Number(n || 0).toLocaleString('en-KE', { maximumFractionDigits: 0 }); }
  function num(n) { return Number(n || 0).toLocaleString('en-KE', { maximumFractionDigits: 2 }); }
  function pill(text, tone) { return h('span', { class: 'pill ' + (tone || 'grey'), text: text }); }
  function field(label, input, wide, hint) { return h('div', { class: wide ? 'wide' : null }, h('label', { text: label }), input, hint ? h('div', { class: 'hint', text: hint }) : null); }
  function btn(label, opts) {
    opts = opts || {};
    var b = h('button', { class: 'btn' + (opts.kind ? ' ' + opts.kind : '') + (opts.small ? ' small' : ''), type: opts.type || 'button', onclick: opts.onclick, title: opts.title });
    if (opts.icon) b.appendChild(icon(opts.icon));
    b.appendChild(h('span', { text: label }));
    return b;
  }
  function busy(b, on) { b.classList.toggle('loading', !!on); b.disabled = !!on; }
  function toast(msg, bad) {
    var t = h('div', { class: 'toast' + (bad ? ' bad' : ''), role: 'status' }, icon(bad ? 'alert' : 'check'), h('span', { text: msg }));
    document.getElementById('toasts').appendChild(t);
    setTimeout(function () { t.remove(); }, bad ? 6500 : 3500);
  }
  function empty(title, text, ic) { return h('div', { class: 'empty' }, icon(ic || 'inbox'), h('b', { text: title }), text ? h('div', { text: text }) : null); }
  function skeleton(n) { var d = h('div'); for (var i = 0; i < (n || 3); i++) d.appendChild(h('div', { class: 'skeleton' })); return d; }
  function pageHead(title, sub, actions) { return h('div', { class: 'page-head' }, h('div', {}, h('h2', { text: title }), sub ? h('p', { text: sub }) : null), h('div', { class: 'left' }, actions || null)); }

  /** A table that turns into cards on phones: every cell gets its column name as data-label. */
  function tableOf(heads, trs) {
    var body = h('tbody', {}, trs);
    Array.prototype.forEach.call(body.querySelectorAll('tr'), function (tr) {
      Array.prototype.forEach.call(tr.children, function (td, i) { if (heads[i] && !td.hasAttribute('data-label')) td.setAttribute('data-label', heads[i]); });
    });
    return h('div', { class: 'table-wrap' }, h('table', {}, h('thead', {}, h('tr', {}, heads.map(function (t) { return h('th', { text: t }); }))), body));
  }

  // ---------- modal + dialogs ----------
  var modalStack = [];
  function openModal(content, opts) {
    opts = opts || {};
    var back = h('div', { class: 'modal-back' });
    var box = h('div', { class: 'modal' + (opts.small ? ' small' : ''), role: 'dialog', 'aria-modal': 'true' });
    var api = { close: function () { var i = modalStack.indexOf(api); if (i >= 0) modalStack.splice(i, 1); back.remove(); }, dismiss: function () { api.close(); if (opts.onDismiss) opts.onDismiss(); } };
    box.appendChild(h('button', { class: 'x', type: 'button', 'aria-label': 'Close', onclick: api.dismiss }, icon('x')));
    box.appendChild(content);
    back.appendChild(box);
    back.addEventListener('mousedown', function (ev) { if (ev.target === back) api.dismiss(); });
    document.body.appendChild(back);
    modalStack.push(api);
    api.el = box;
    return api;
  }
  document.addEventListener('keydown', function (ev) { if (ev.key === 'Escape' && modalStack.length) modalStack[modalStack.length - 1].dismiss(); });

  /** Confirm / prompt dialogs. Resolve true/false (confirm) or the text / null (prompt). */
  function dialog(o) {
    return new Promise(function (resolve) {
      var input = null, m;
      if (o.input) input = o.multiline ? h('textarea', { rows: 3, maxlength: o.max || 500, placeholder: o.placeholder || '' }) : h('input', { maxlength: o.max || 500, placeholder: o.placeholder || '', value: o.value || '' });
      var err = h('div', { class: 'error', role: 'alert' });
      var ok = btn(o.ok || 'OK', { kind: o.danger ? 'danger' : '', type: 'submit' });
      function finish(v) { m.close(); resolve(v); }
      var form = h('form', { onsubmit: function (ev) {
        ev.preventDefault();
        if (!o.input) { finish(true); return; }
        var v = input.value.trim();
        if (!o.optional && v.length < (o.min || 1)) { err.textContent = o.min > 1 ? 'Please enter at least ' + o.min + ' characters' : 'This is required'; return; }
        finish(v);
      } }, h('h2', { text: o.title }), o.message ? h('p', { class: 'dlg-msg', text: o.message }) : null,
        o.input ? [h('label', { text: o.label || '' }), input] : null, err,
        h('div', { class: 'actions' }, btn(o.cancel || 'Cancel', { kind: 'secondary', onclick: function () { finish(o.input ? null : false); } }), ok));
      m = openModal(form, { small: true, onDismiss: function () { resolve(o.input ? null : false); } });
      setTimeout(function () { (input || ok).focus(); }, 30);
    });
  }
  function confirmBox(title, message, okLabel, danger) { return dialog({ title: title, message: message, ok: okLabel || 'Confirm', danger: danger }); }
  function promptBox(title, label, opts) { opts = opts || {}; opts.title = title; opts.label = label; opts.input = true; return dialog(opts); }

  // ---------- server calls ----------
  function api(path, opts) {
    opts = opts || {};
    var headers = { 'Accept': 'application/json' };
    if (state.token) headers['Authorization'] = 'Bearer ' + state.token;
    var init = { method: opts.method || 'GET', headers: headers };
    if (opts.json !== undefined) { headers['Content-Type'] = 'application/json'; init.body = JSON.stringify(opts.json); }
    if (opts.form) init.body = opts.form;
    return fetch(path, init).then(function (r) {
      if (r.status === 401 && state.token) { logout(); throw new Error('Please log in again'); }
      if (!r.ok) {
        return r.text().then(function (t) {
          var m = 'Something went wrong (' + r.status + ')', raw = null;
          try { var j = JSON.parse(t); raw = j.message; if (j.message) m = j.message; } catch (e) { /* not JSON */ }
          var err = new Error(m); err.status = r.status; err.raw = raw; throw err;
        });
      }
      return r;
    });
  }
  function getJson(path) { return api(path).then(function (r) { return r.json(); }); }
  function post(path, json) { return api(path, { method: 'POST', json: json }).then(function (r) { return r.text().then(function (t) { return t ? JSON.parse(t) : null; }); }); }
  /** Downloads a file that needs the login token (CSV exports). */
  function download(path, filename) {
    return api(path).then(function (r) { return r.blob(); }).then(function (blob) {
      var url = URL.createObjectURL(blob), a = h('a', { href: url, download: filename });
      document.body.appendChild(a); a.click(); a.remove();
      setTimeout(function () { URL.revokeObjectURL(url); }, 30000);
    });
  }
  function logout() {
    sessionStorage.removeItem(TOKEN_KEY); sessionStorage.removeItem(USER_KEY);
    state.token = null; state.user = null; state.company = null; state.menuOpen = false; state.unread = 0;
    modalStack.slice().forEach(function (m) { m.close(); });
    render();
  }
  function signIn(u) {
    if (u.role !== 'COMPANY') throw new Error('This portal is for company accounts.');
    state.token = u.token; state.user = { name: u.name, email: u.email };
    sessionStorage.setItem(TOKEN_KEY, u.token); sessionStorage.setItem(USER_KEY, JSON.stringify(state.user));
    location.hash = '#overview';
  }

  // ---------- login + register ----------
  function authView() {
    var mode = 'login', box = h('div');
    var tabLogin = h('button', { type: 'button', text: 'Log in', onclick: function () { mode = 'login'; draw(); } });
    var tabReg = h('button', { type: 'button', text: 'Register a company', onclick: function () { mode = 'register'; draw(); } });
    var title = h('h2'), sub = h('p');
    function draw() {
      tabLogin.className = mode === 'login' ? 'on' : ''; tabReg.className = mode === 'register' ? 'on' : '';
      title.textContent = mode === 'login' ? 'Welcome back' : 'Create your company account';
      sub.textContent = mode === 'login' ? 'Log in to manage your company on FarmConnect.' : 'Step 1 of 2. Your business details come next.';
      box.textContent = '';
      box.appendChild(mode === 'login' ? loginForm() : registerForm());
    }
    draw();
    var perks = [['cart', 'Sell seeds, fertiliser, feed and equipment to farmers'], ['send', 'Post bulk produce requests and receive quotations'], ['truck', 'Offer transport, machinery hire and agronomy services'], ['wallet', 'Get paid to your M-Pesa, paybill or till']];
    return h('div', { class: 'auth' },
      h('div', { class: 'auth-hero' },
        h('div', { class: 'logo' }, icon('leaf'), 'FarmConnect'),
        h('div', {}, h('h1', { text: 'Grow your agribusiness across Kenya' }), h('p', { text: 'One platform connecting farmers, buyers, companies and county governments.' }),
          h('ul', {}, perks.map(function (p) { return h('li', {}, icon(p[0]), p[1]); }))),
        h('small', { text: 'Every company is verified by the FarmConnect super admin before it can trade.' })),
      h('div', { class: 'auth-panel' }, h('div', { class: 'auth-card' }, title, sub,
        h('div', { class: 'seg' }, tabLogin, tabReg), box)));
  }

  function loginForm() {
    var err = h('div', { class: 'error', role: 'alert' });
    var email = h('input', { type: 'email', autocomplete: 'username', required: true, placeholder: 'company@example.com' });
    var pass = h('input', { type: 'password', autocomplete: 'current-password', required: true });
    var b = btn('Log in', { type: 'submit' }); b.classList.add('block');
    return h('form', { onsubmit: function (ev) {
      ev.preventDefault(); err.textContent = ''; busy(b, true);
      api('/api/auth/login', { method: 'POST', json: { email: email.value.trim(), password: pass.value } })
        .then(function (r) { return r.json(); }).then(function (u) { signIn(u); return loadCompany(); }).then(render)
        .catch(function (e) { err.textContent = e.message; state.token = null; sessionStorage.removeItem(TOKEN_KEY); })
        .then(function () { busy(b, false); });
    } }, h('label', { text: 'Email' }), email, h('label', { text: 'Password' }), pass, h('div', { class: 'sp' }), b, err);
  }

  function registerForm() {
    var err = h('div', { class: 'error', role: 'alert' });
    var name = h('input', { required: true, autocomplete: 'name', placeholder: 'Your full name' });
    var email = h('input', { type: 'email', required: true, autocomplete: 'email' });
    var phone = h('input', { type: 'tel', autocomplete: 'tel', placeholder: '07XXXXXXXX' });
    var pass = h('input', { type: 'password', required: true, autocomplete: 'new-password', minlength: 6 });
    var confirm = h('input', { type: 'password', required: true, autocomplete: 'new-password' });
    var b = btn('Create account', { type: 'submit' }); b.classList.add('block');
    return h('form', { onsubmit: function (ev) {
      ev.preventDefault(); err.textContent = '';
      if (pass.value !== confirm.value) { err.textContent = 'Passwords do not match'; return; }
      busy(b, true);
      api('/api/auth/register', { method: 'POST', json: { name: name.value.trim(), email: email.value.trim(), phone: phone.value.trim(), password: pass.value, confirmPassword: confirm.value, role: 'COMPANY' } })
        .then(function (r) { return r.json(); }).then(function (u) { signIn(u); state.company = null; render(); })
        .catch(function (e) { err.textContent = e.message; })
        .then(function () { busy(b, false); });
    } },
      field('Your name (authorised representative)', name), field('Email', email), field('Phone', phone), field('Password', pass, false, 'At least 6 characters'),
      field('Confirm password', confirm), h('div', { class: 'sp' }), b, err);
  }

  // ---------- company data ----------
  function loadCompany() {
    return api('/api/company/me').then(function (r) { return r.json(); })
      .then(function (c) { state.company = c; })
      .catch(function (e) { if (e.status === 404 && e.raw === 'NO_COMPANY') { state.company = null; return; } throw e; });
  }
  function getCounties() {
    if (state.counties) return Promise.resolve(state.counties);
    return getJson('/api/locations').then(function (l) { state.counties = l.map(function (c) { return c.name; }); return state.counties; })
      .catch(function () { state.counties = []; return []; });
  }
  function countySelect(counties, value) {
    var s = h('select', {}, [h('option', { value: '', text: 'Choose county' })].concat(counties.map(function (n) { return h('option', { value: n, text: n }); })));
    s.value = value || '';
    return s;
  }
  function countyChecks(counties, picked) {
    var set = {}; (picked || []).forEach(function (n) { set[n] = true; });
    var grid = h('div', { class: 'check-grid' }, counties.map(function (n) {
      var cb = h('input', { type: 'checkbox', value: n }); cb.checked = !!set[n];
      return h('label', {}, cb, n);
    }));
    grid.values = function () { return Array.prototype.map.call(grid.querySelectorAll('input:checked'), function (i) { return i.value; }); };
    return grid;
  }

  /** The business details form: registers the company the first time, edits it afterwards. */
  function detailsForm(existing, counties, done) {
    var c = existing || {};
    var locked = !!existing && (c.status === 'VERIFIED' || c.status === 'UNDER_REVIEW');
    var err = h('div', { class: 'error', role: 'alert' });
    var name = h('input', { required: true, maxlength: 150, value: c.name || '', disabled: locked });
    var reg = h('input', { required: true, minlength: 3, maxlength: 40, value: c.businessRegNumber || '', disabled: locked });
    var kra = h('input', { required: true, maxlength: 11, placeholder: 'A123456789B', value: c.kraPin || '', disabled: locked });
    var cat = h('select', {}, Object.keys(CATEGORIES).map(function (k) { return h('option', { value: k, text: CATEGORIES[k] }); }));
    cat.value = c.category || 'AGRI_SUPPLIER';
    var buy = h('input', { type: 'checkbox' }), sell = h('input', { type: 'checkbox' });
    buy.checked = existing ? !!c.canBuy : cat.value === 'PRODUCE_BUYER'; sell.checked = existing ? !!c.canSell : cat.value !== 'PRODUCE_BUYER';
    var repName = h('input', { required: true, maxlength: 100, value: c.repName || (state.user ? state.user.name : '') });
    var repPhone = h('input', { required: true, type: 'tel', maxlength: 30, value: c.repPhone || '' });
    var repEmail = h('input', { type: 'email', maxlength: 150, value: c.repEmail || (state.user ? state.user.email : '') });
    var address = h('input', { required: true, maxlength: 250, value: c.address || '' });
    var county = countySelect(counties, c.county);
    var desc = h('textarea', { maxlength: 1000, rows: 3 }); desc.value = c.description || '';
    var grid = countyChecks(counties, c.operatingCounties);
    var b = btn(existing ? 'Save changes' : 'Register company', { type: 'submit' });
    return h('form', { onsubmit: function (ev) {
      ev.preventDefault(); err.textContent = '';
      if (!buy.checked && !sell.checked) { err.textContent = 'Choose whether the company buys, sells, or both'; return; }
      var body = { name: name.value.trim(), businessRegNumber: reg.value.trim(), kraPin: kra.value.trim().toUpperCase(), category: cat.value,
        canBuy: buy.checked, canSell: sell.checked, repName: repName.value.trim(), repPhone: repPhone.value.trim(), repEmail: repEmail.value.trim(),
        description: desc.value.trim(), address: address.value.trim(), county: county.value, operatingCounties: grid.values() };
      busy(b, true);
      api('/api/company/me', { method: existing ? 'PUT' : 'POST', json: body }).then(function (r) { return r.json(); })
        .then(function (res) { state.company = res; toast(existing ? 'Saved' : 'Company registered. Now upload your documents.'); done(res); })
        .catch(function (e) { err.textContent = e.message; })
        .then(function () { busy(b, false); });
    } },
      locked ? h('p', { class: 'hint', text: 'Name, registration number and KRA PIN are locked while the company is verified or under review.' }) : null,
      h('div', { class: 'form-grid' },
        field('Registered company name *', name), field('Business registration number *', reg), field('KRA PIN *', kra, false, 'Format: A123456789B'),
        field('Business category *', cat),
        h('div', {}, h('label', { text: 'What does the company do? *' }), h('label', { class: 'check' }, buy, 'Buys produce from farmers'), h('label', { class: 'check' }, sell, 'Sells products or services')),
        field('Authorised representative *', repName), field('Representative phone *', repPhone), field('Representative email', repEmail),
        field('Physical address *', address), field('County *', county),
        field('About the company', desc, true), field('Counties you operate in', grid, true)),
      h('div', { class: 'sp' }), b, err);
  }

  // ---------- charts: small SVG helpers, no libraries ----------
  var PALETTE = ['#2e7d32', '#f9a825', '#1e88e5', '#8e24aa', '#e53935', '#00897b', '#6d4c41'];
  var SVG_NS = 'http://www.w3.org/2000/svg';
  function svgEl(name, attrs, text) {
    var el = document.createElementNS(SVG_NS, name);
    for (var k in (attrs || {})) el.setAttribute(k, attrs[k]);
    if (text != null) el.textContent = text;
    return el;
  }
  function compact(n) {
    n = Number(n || 0);
    if (n >= 1e6) return (n / 1e6).toFixed(1).replace(/\.0$/, '') + 'M';
    if (n >= 1e3) return (n / 1e3).toFixed(n >= 1e4 ? 0 : 1).replace(/\.0$/, '') + 'k';
    return String(Math.round(n));
  }
  function monthBuckets(n) {
    var out = [], now = new Date();
    for (var i = n - 1; i >= 0; i--) {
      var d = new Date(now.getFullYear(), now.getMonth() - i, 1);
      out.push({ key: d.getFullYear() + '-' + (d.getMonth() + 1), label: d.toLocaleString('en-KE', { month: 'short' }) + " '" + String(d.getFullYear()).slice(2), value: 0 });
    }
    return out;
  }
  function addToMonth(buckets, iso, v) {
    if (!iso) return;
    var d = new Date(iso), key = d.getFullYear() + '-' + (d.getMonth() + 1);
    for (var i = 0; i < buckets.length; i++) if (buckets[i].key === key) { buckets[i].value += v; return; }
  }
  function noData() { return h('div', { class: 'chart-empty', text: 'No data yet' }); }

  /** Vertical bars: [{label, value}] */
  function barChart(data, fmt) {
    var max = 0; data.forEach(function (d) { if (d.value > max) max = d.value; });
    if (!max) return noData();
    var W = 520, H = 230, top = 22, bottom = 28, left = 6, right = 6, gap = 14;
    var bw = (W - left - right - gap * (data.length - 1)) / data.length, plot = H - top - bottom;
    var svg = svgEl('svg', { viewBox: '0 0 ' + W + ' ' + H, class: 'chart', role: 'img', 'aria-label': 'Bar chart' });
    svg.appendChild(svgEl('line', { x1: left, x2: W - right, y1: H - bottom, y2: H - bottom, class: 'axis' }));
    data.forEach(function (d, i) {
      var hgt = Math.max(d.value > 0 ? 3 : 0, (d.value / max) * plot), x = left + i * (bw + gap), y = H - bottom - hgt;
      svg.appendChild(svgEl('rect', { x: x, y: y, width: bw, height: hgt, rx: 5, fill: PALETTE[0], opacity: d.value > 0 ? 1 : 0.2 }));
      if (d.value > 0) svg.appendChild(svgEl('text', { x: x + bw / 2, y: y - 6, 'text-anchor': 'middle', class: 'val' }, (fmt || compact)(d.value)));
      svg.appendChild(svgEl('text', { x: x + bw / 2, y: H - 9, 'text-anchor': 'middle' }, d.label));
    });
    return svg;
  }

  /** Donut with its own legend: [{label, value}] */
  function donutChart(data, centreLabel) {
    var items = data.filter(function (d) { return d.value > 0; }), total = 0;
    items.forEach(function (d) { total += d.value; });
    if (!total) return noData();
    var svg = svgEl('svg', { viewBox: '0 0 380 200', class: 'chart', role: 'img', 'aria-label': 'Donut chart' });
    var R = 68, C = 2 * Math.PI * R, offset = 0;
    svg.appendChild(svgEl('circle', { cx: 100, cy: 100, r: R, fill: 'none', stroke: '#eef2ee', 'stroke-width': 26 }));
    items.forEach(function (d, i) {
      var len = (d.value / total) * C;
      svg.appendChild(svgEl('circle', { cx: 100, cy: 100, r: R, fill: 'none', stroke: PALETTE[i % PALETTE.length], 'stroke-width': 26,
        'stroke-dasharray': len + ' ' + (C - len), 'stroke-dashoffset': -offset, transform: 'rotate(-90 100 100)' }));
      offset += len;
    });
    svg.appendChild(svgEl('text', { x: 100, y: 106, 'text-anchor': 'middle', class: 'big' }, String(total)));
    svg.appendChild(svgEl('text', { x: 100, y: 124, 'text-anchor': 'middle' }, centreLabel || ''));
    items.slice(0, 7).forEach(function (d, i) {
      var y = 34 + i * 25;
      svg.appendChild(svgEl('rect', { x: 215, y: y - 10, width: 11, height: 11, rx: 3, fill: PALETTE[i % PALETTE.length] }));
      svg.appendChild(svgEl('text', { x: 233, y: y, 'font-size': 12 }, d.label));
      svg.appendChild(svgEl('text', { x: 372, y: y, 'text-anchor': 'end', class: 'val' }, d.value + ' · ' + Math.round(d.value * 100 / total) + '%'));
    });
    return svg;
  }

  /** Horizontal bars: [{label, value}] */
  function hBars(data, fmt) {
    var max = 0; data.forEach(function (d) { if (d.value > max) max = d.value; });
    if (!max) return noData();
    var W = 520, row = 34, H = data.length * row + 6, labelW = 150, valW = 56;
    var svg = svgEl('svg', { viewBox: '0 0 ' + W + ' ' + H, class: 'chart', role: 'img', 'aria-label': 'Bar chart' });
    data.forEach(function (d, i) {
      var y = 4 + i * row, w = Math.max(3, (d.value / max) * (W - labelW - valW));
      svg.appendChild(svgEl('text', { x: 0, y: y + 17 }, d.label.length > 22 ? d.label.slice(0, 21) + '…' : d.label));
      svg.appendChild(svgEl('rect', { x: labelW, y: y + 5, width: w, height: 16, rx: 5, fill: PALETTE[i % PALETTE.length] }));
      svg.appendChild(svgEl('text', { x: labelW + w + 8, y: y + 18, class: 'val' }, (fmt || compact)(d.value)));
    });
    return svg;
  }

  function chartCard(title, body) { return h('div', { class: 'card chart-card' }, h('h3', { text: title }), h('div', { class: 'chart-body' }, body)); }
  function countBy(list, keyFn, labels) {
    var m = {}, order = [];
    list.forEach(function (x) { var k = keyFn(x); if (!(k in m)) { m[k] = 0; order.push(k); } m[k]++; });
    return order.map(function (k) { return { label: (labels && labels[k]) || k, value: m[k] }; });
  }

  // ---------- overview ----------
  function docsHave(c) { var have = {}; (c.documents || []).forEach(function (d) { have[d.type] = true; }); return !!(have.REGISTRATION_CERT && have.KRA_PIN_CERT); }

  function verificationSteps(c) {
    var hasDocs = docsHave(c), s = c.status, verified = s === 'VERIFIED' || s === 'SUSPENDED';
    var steps = [
      { t: 'Registered', cls: 'done', mark: '✓' },
      { t: 'Documents', cls: hasDocs ? 'done' : 'now', mark: hasDocs ? '✓' : '2' },
      { t: 'Review', cls: verified ? 'done' : s === 'REJECTED' ? 'bad' : hasDocs ? 'now' : '', mark: verified ? '✓' : s === 'REJECTED' ? '!' : '3' },
      { t: 'Verified', cls: s === 'VERIFIED' ? 'done' : s === 'SUSPENDED' ? 'bad' : '', mark: s === 'VERIFIED' ? '✓' : s === 'SUSPENDED' ? '!' : '4' }];
    return h('div', { class: 'steps' }, steps.map(function (x) { return h('div', { class: 'step ' + x.cls }, h('div', { class: 'dot', text: x.mark }), x.t); }));
  }

  function statusMessage(c) {
    var s = c.status;
    if (s === 'PENDING_VERIFICATION') return docsHave(c) ? 'Your documents are in. The FarmConnect super admin will review your company soon.' : 'Upload your registration certificate and KRA PIN certificate so the super admin can review your company.';
    if (s === 'UNDER_REVIEW') return 'The super admin is reviewing your company right now.';
    if (s === 'VERIFIED') return 'Your company is verified and can trade on FarmConnect.';
    if (s === 'REJECTED') return 'Not verified. Reason: ' + (c.rejectionReason || '-') + '. Fix the details or documents, then save your profile to resubmit.';
    return 'Suspended. Reason: ' + (c.suspensionReason || '-') + '. Please contact FarmConnect support.';
  }

  function kpi(label, value, note, ic, tone) {
    return h('div', { class: 'kpi' }, h('div', { class: 'ico-wrap ' + (tone || '') }, icon(ic)), h('div', {}, h('div', { class: 'label', text: label }), h('div', { class: 'value', text: value }), note ? h('div', { class: 'note', text: note }) : null));
  }

  function overview(box) {
    var c = state.company, verified = c.verified;
    box.appendChild(skeleton(4));
    var jobs = [Promise.resolve(null), Promise.resolve(null), Promise.resolve(null), Promise.resolve([]), Promise.resolve([])];
    if (verified && c.canSell) {
      jobs[0] = getJson('/api/company/summary').catch(function () { return null; });
      jobs[3] = getJson('/api/company/supply-orders').catch(function () { return []; });
      jobs[4] = getJson('/api/company/service-requests').catch(function () { return []; });
    }
    if (verified && c.canBuy) { jobs[1] = getJson('/api/company/requests').catch(function () { return []; }); jobs[2] = getJson('/api/company/purchase-orders').catch(function () { return []; }); }
    Promise.all(jobs).then(function (d) {
      box.textContent = '';
      var s = d[0], reqs = d[1] || [], pos = d[2] || [], orderList = d[3] || [], srList = d[4] || [];
      var tone = c.status === 'VERIFIED' ? '' : (c.status === 'REJECTED' || c.status === 'SUSPENDED') ? ' bad' : ' wait';
      var first = (state.user && state.user.name ? state.user.name.split(' ')[0] : ''); first = first.charAt(0).toUpperCase() + first.slice(1);
      box.appendChild(h('div', { class: 'hero' + tone }, h('div', {}, h('h2', { text: (first ? 'Hello, ' + first : 'Welcome') }), h('p', { text: c.name + ' · ' + statusMessage(c) })),
        pill(STATUS[c.status] || c.status, STATUS_TONE[c.status])));
      if (c.status !== 'VERIFIED') box.appendChild(h('div', { class: 'card' }, h('h3', { text: 'Verification progress' }), verificationSteps(c)));

      if (s) box.appendChild(h('h3', { class: 'section-title', text: 'Selling' }));
      if (s) box.appendChild(h('div', { class: 'kpis' },
        kpi('New orders', String(s.newOrders), 'waiting for you', 'inbox', s.newOrders ? 'amber' : ''), kpi('In progress', String(s.inProgress), 'confirmed or dispatched', 'truck', 'blue'),
        kpi('Delivered', String(s.delivered), 'orders', 'check'), kpi('Active products', String(s.activeProducts), s.lowStock + ' low on stock', 'box', s.lowStock ? 'red' : ''),
        kpi('Sales delivered', moneyK(s.grossDelivered), 'before fees', 'chart'), kpi('FarmConnect fees', moneyK(s.feesDelivered), 'on delivered orders', 'wallet', 'purple'),
        kpi('You earn', moneyK(s.netDelivered), 'after fees', 'wallet'), kpi('Still to collect', moneyK(s.netUnpaid), 'delivered, not paid', 'alert', s.netUnpaid > 0 ? 'amber' : '')));
      if (s) {
        var live = orderList.filter(function (o) { return o.status !== 'CANCELLED' && o.status !== 'REJECTED'; });
        var sales = monthBuckets(6), cnt = monthBuckets(6), qtyByProduct = {};
        orderList.forEach(function (o) { if (o.status === 'DELIVERED') addToMonth(sales, o.deliveredAt || o.createdAt, Number(o.total)); });
        live.forEach(function (o) { addToMonth(cnt, o.createdAt, 1); o.items.forEach(function (i) { qtyByProduct[i.name] = (qtyByProduct[i.name] || 0) + i.quantity; }); });
        var top = Object.keys(qtyByProduct).map(function (k) { return { label: k, value: qtyByProduct[k] }; }).sort(function (a, b) { return b.value - a.value; }).slice(0, 5);
        box.appendChild(h('div', { class: 'charts' },
          chartCard('Sales per month (KSh)', barChart(sales)),
          chartCard('Orders per month', barChart(cnt)),
          chartCard('Orders by status', donutChart(countBy(orderList, function (o) { return o.status; }, ORDER_STATUS), 'orders')),
          chartCard('Top products (units sold)', hBars(top)),
          chartCard('Service requests', donutChart(countBy(srList, function (r) { return r.status; }, SR_STATUS), 'requests'))));
      }
      if (verified && c.canBuy) {
        var open = reqs.filter(function (r) { return r.status === 'OPEN'; }).length, waiting = reqs.reduce(function (a, r) { return a + (r.openQuotations || 0); }, 0);
        box.appendChild(h('h3', { class: 'section-title', text: 'Buying' }));
        box.appendChild(h('div', { class: 'kpis' },
          kpi('Open requests', String(open), 'taking quotations', 'send'), kpi('Quotations waiting', String(waiting), 'for your decision', 'inbox', waiting ? 'amber' : ''),
          kpi('Orders to confirm', String(pos.filter(function (p) { return p.status === 'ISSUED'; }).length), 'waiting for the farmer', 'clipboard', 'blue'),
          kpi('Produce to receive', String(pos.filter(function (p) { return p.status === 'CONFIRMED'; }).length), 'confirmed, not delivered', 'truck')));
        var spend = monthBuckets(6);
        pos.forEach(function (p) { if (p.status !== 'DECLINED' && p.status !== 'CANCELLED') addToMonth(spend, p.createdAt, Number(p.total)); });
        box.appendChild(h('div', { class: 'charts' },
          chartCard('Purchases per month (KSh)', barChart(spend)),
          chartCard('Produce requests', donutChart(countBy(reqs, function (r) { return r.status; }, REQ_STATUS), 'requests')),
          chartCard('Purchase orders', donutChart(countBy(pos, function (p) { return p.status; }, PO_STATUS), 'orders'))));
      }

      var items = [{ t: 'Register your business', done: true },
        { t: 'Upload registration and KRA PIN certificates', done: docsHave(c), link: '#documents' },
        { t: 'Get verified by the super admin', done: verified, hint: verified ? '' : STATUS[c.status] }];
      if (c.canSell) {
        items.push({ t: 'Add your payout account', done: c.payoutApproved, link: '#payouts', hint: c.payoutAccount && !c.payoutApproved ? 'Waiting for super admin approval' : '' });
        items.push({ t: 'List your first product', done: !!(s && s.activeProducts > 0), link: '#products' });
      }
      if (c.canBuy) items.push({ t: 'Post your first produce request', done: reqs.length > 0, link: '#requests' });
      if (items.some(function (i) { return !i.done; }))
        box.appendChild(h('div', { class: 'card' }, h('h3', { text: 'Get started' }), h('p', { class: 'sub', text: 'Finish these steps to start trading.' }),
          h('ul', { class: 'todo' }, items.map(function (i) {
            return h('li', { class: i.done ? 'done' : '' }, h('span', { class: 'tick' }, icon('check')), h('span', { class: 't' }, i.t, i.hint ? h('small', { text: i.hint }) : null),
              !i.done && i.link ? h('a', { class: 'btn small secondary', href: i.link, text: 'Go' }) : null);
          }))));

      if (verified) {
        var quick = [];
        if (c.canSell) { quick.push(btn('Add product', { icon: 'plus', onclick: function () { location.hash = '#products'; } })); quick.push(btn('View orders', { kind: 'secondary', icon: 'clipboard', onclick: function () { location.hash = '#orders'; } })); quick.push(btn('Offer a service', { kind: 'secondary', icon: 'wrench', onclick: function () { location.hash = '#services'; } })); }
        if (c.canBuy) quick.push(btn('New produce request', { icon: 'send', onclick: function () { location.hash = '#requests'; } }));
        box.appendChild(h('div', { class: 'card' }, h('h3', { text: 'Quick actions' }), h('div', { class: 'toolbar' }, h('div', { class: 'left' }, quick))));
      }
    }).catch(function (e) { box.textContent = ''; box.appendChild(empty('Could not load your overview', e.message, 'alert')); });
  }

  // ---------- profile + security ----------
  function changePasswordModal() {
    var cur = h('input', { type: 'password', required: true, autocomplete: 'current-password' });
    var neu = h('input', { type: 'password', required: true, autocomplete: 'new-password', minlength: 8 });
    var err = h('div', { class: 'error' }), b = btn('Change password', { type: 'submit' }), m;
    var form = h('form', { onsubmit: function (ev) {
      ev.preventDefault(); err.textContent = ''; busy(b, true);
      api('/api/auth/change-password', { method: 'POST', json: { currentPassword: cur.value, newPassword: neu.value } }).then(function (r) { return r.json(); })
        .then(function (u) { state.token = u.token; sessionStorage.setItem(TOKEN_KEY, u.token); m.close(); toast('Password changed'); })
        .catch(function (e) { err.textContent = e.message; }).then(function () { busy(b, false); });
    } }, h('h2', { text: 'Change password' }), field('Current password', cur), field('New password', neu, false, 'At least 8 characters'), err,
      h('div', { class: 'actions' }, btn('Cancel', { kind: 'secondary', onclick: function () { m.close(); } }), b));
    m = openModal(form, { small: true });
  }

  function profile(box) {
    box.appendChild(pageHead('Company profile', 'Your business details as shown to FarmConnect and its users.'));
    getCounties().then(function (counties) {
      box.appendChild(h('div', { class: 'card' }, detailsForm(state.company, counties, function () { render(); })));
      box.appendChild(h('div', { class: 'card' }, h('h3', {}, icon('lock'), 'Security'), h('p', { class: 'sub', text: 'Signed in as ' + (state.user ? state.user.email : '') }),
        btn('Change password', { kind: 'secondary', icon: 'lock', onclick: changePasswordModal })));
    });
  }

  // ---------- documents ----------
  function viewDoc(d) {
    api('/api/company/me/documents/' + d.id).then(function (r) { return r.blob(); }).then(function (blob) {
      var url = URL.createObjectURL(blob);
      window.open(url, '_blank', 'noopener');
      setTimeout(function () { URL.revokeObjectURL(url); }, 60000);
    }).catch(function (e) { toast(e.message, true); });
  }

  function documents(box) {
    var c = state.company, editable = c.status !== 'SUSPENDED', removable = c.status !== 'VERIFIED' && c.status !== 'UNDER_REVIEW';
    box.appendChild(pageHead('Documents', 'Your registration certificate and KRA PIN certificate are required before the super admin can verify you.'));
    var type = h('select', {}, Object.keys(DOC_TYPES).map(function (k) { return h('option', { value: k, text: DOC_TYPES[k] }); }));
    var file = h('input', { type: 'file', accept: 'image/jpeg,image/png,application/pdf' });
    var up = btn('Upload', { icon: 'download', onclick: function () {
      if (!file.files.length) { toast('Choose a file first', true); return; }
      var f = new FormData(); f.append('type', type.value); f.append('file', file.files[0]);
      busy(up, true);
      api('/api/company/me/documents', { method: 'POST', form: f }).then(function (r) { return r.json(); })
        .then(function (res) { state.company = res; toast('Uploaded'); render(); })
        .catch(function (e) { toast(e.message, true); busy(up, false); });
    } });
    up.disabled = !editable;
    box.appendChild(h('div', { class: 'card' }, h('h3', { text: 'Upload a document' }), h('p', { class: 'sub', text: 'JPG, PNG or PDF, up to 6 MB.' }),
      h('div', { class: 'form-grid' }, field('Type', type), field('File', file)), h('div', { class: 'sp' }), up));
    var list = h('div', { class: 'card' }, h('h3', { text: 'Your documents' }));
    if (!c.documents.length) list.appendChild(empty('Nothing uploaded yet', 'Start with your registration certificate.', 'file'));
    c.documents.forEach(function (d) {
      list.appendChild(h('div', { class: 'doc-row' }, icon('file'),
        h('div', { class: 'grow' }, h('b', { text: (DOC_TYPES[d.type] || d.type).replace(' *', '') }), ' · ' + (d.fileName || '')),
        h('span', { class: 'muted', text: when(d.uploadedAt) }),
        btn('View', { kind: 'secondary', small: true, onclick: function () { viewDoc(d); } }),
        removable ? btn('Delete', { kind: 'danger', small: true, onclick: function () {
          confirmBox('Delete document', 'This cannot be undone.', 'Delete', true).then(function (ok) {
            if (!ok) return;
            api('/api/company/me/documents/' + d.id, { method: 'DELETE' }).then(function (r) { return r.json(); })
              .then(function (res) { state.company = res; render(); }).catch(function (e) { toast(e.message, true); });
          });
        } }) : null));
    });
    box.appendChild(list);
  }

  // ---------- shared view helpers ----------
  function lockedView(box, title, why) {
    box.appendChild(pageHead(title));
    box.appendChild(h('div', { class: 'empty' }, icon('lock'), h('b', { text: why || (state.company.verified ? 'Not available for your company type' : 'Unlocks after verification') }),
      h('div', { text: state.company.verified ? '' : 'The FarmConnect super admin verifies every company before it can trade.' })));
  }
  function seg(options, current, onPick) {
    var wrap = h('div', { class: 'seg' }), buttons = [];
    options.forEach(function (o) {
      var b = h('button', { type: 'button', text: o[1], class: o[0] === current ? 'on' : '', onclick: function () { buttons.forEach(function (x) { x.className = ''; }); b.className = 'on'; onPick(o[0]); } });
      buttons.push(b); wrap.appendChild(b);
    });
    return wrap;
  }
  function fail(el, e) { el.textContent = ''; el.appendChild(empty('Something went wrong', e.message, 'alert')); }

  // ---------- products ----------
  var SUPPLY_CAT = { SEED: 'Seed', FERTILIZER: 'Fertiliser', PESTICIDE: 'Pesticide / agrochemical', ANIMAL_FEED: 'Animal feed', EQUIPMENT: 'Equipment', OTHER: 'Other' };
  var ORDER_STATUS = { PLACED: 'New', CONFIRMED: 'Confirmed', DISPATCHED: 'Dispatched', DELIVERED: 'Delivered', CANCELLED: 'Cancelled', REJECTED: 'Rejected' };
  var ORDER_TONE = { PLACED: 'amber', CONFIRMED: 'blue', DISPATCHED: 'purple', DELIVERED: 'green', CANCELLED: 'red', REJECTED: 'red' };

  function productBody(p, active) {
    return { name: p.name, category: p.category, description: p.description || '', unit: p.unit, price: p.price, stockQty: p.stockQty, minOrderQty: p.minOrderQty, licenceRef: p.licenceRef || '', active: active };
  }

  function productForm(existing, done) {
    var p = existing || { active: true, minOrderQty: 1, stockQty: 0, category: 'SEED' };
    var err = h('div', { class: 'error', role: 'alert' });
    var name = h('input', { required: true, maxlength: 120, value: p.name || '' });
    var cat = h('select', {}, Object.keys(SUPPLY_CAT).map(function (k) { return h('option', { value: k, text: SUPPLY_CAT[k] }); })); cat.value = p.category;
    var unit = h('input', { required: true, maxlength: 40, placeholder: 'e.g. 50 kg bag, litre, each', value: p.unit || '' });
    var price = h('input', { required: true, type: 'number', min: '0.01', step: '0.01', value: p.price != null ? String(p.price) : '' });
    var stock = h('input', { required: true, type: 'number', min: '0', step: '1', value: String(p.stockQty) });
    var minq = h('input', { required: true, type: 'number', min: '1', step: '1', value: String(p.minOrderQty) });
    var lic = h('input', { maxlength: 100, placeholder: 'KEPHIS / PCPB / batch number', value: p.licenceRef || '' });
    var desc = h('textarea', { maxlength: 1000, rows: 3 }); desc.value = p.description || '';
    var active = h('input', { type: 'checkbox' }); active.checked = !!p.active;
    var b = btn('Save', { type: 'submit' }), m;
    var form = h('form', { onsubmit: function (ev) {
      ev.preventDefault(); err.textContent = ''; busy(b, true);
      var body = { name: name.value.trim(), category: cat.value, description: desc.value.trim(), unit: unit.value.trim(), price: Number(price.value),
        stockQty: parseInt(stock.value, 10), minOrderQty: parseInt(minq.value, 10), licenceRef: lic.value.trim(), active: active.checked };
      api('/api/company/products' + (existing ? '/' + existing.id : ''), { method: existing ? 'PUT' : 'POST', json: body }).then(function (r) { return r.json(); })
        .then(function () { m.close(); toast('Product saved'); done(); }).catch(function (e) { err.textContent = e.message; busy(b, false); });
    } }, h('h2', { text: existing ? 'Edit product' : 'Add product' }),
      h('div', { class: 'form-grid' }, field('Name *', name), field('Category *', cat), field('Unit *', unit, false, 'What one unit is'), field('Price per unit (KSh) *', price),
        field('Stock available *', stock), field('Minimum order *', minq), field('Licence / batch reference', lic, true, 'Shown to farmers, e.g. KEPHIS certificate number'), field('Description', desc, true),
        h('div', { class: 'wide' }, h('label', { class: 'check' }, active, 'Visible to farmers'))),
      err, h('div', { class: 'actions' }, btn('Cancel', { kind: 'secondary', onclick: function () { m.close(); } }), b));
    m = openModal(form);
  }

  function products(box) {
    var c = state.company;
    if (!c.verified || !c.canSell) { lockedView(box, 'Products'); return; }
    var list = h('div');
    box.appendChild(pageHead('Products', 'Farmers see your active products that are in stock.', btn('Add product', { icon: 'plus', onclick: function () { productForm(null, load); } })));
    box.appendChild(list);
    function load() {
      list.textContent = ''; list.appendChild(skeleton(4));
      getJson('/api/company/products').then(function (rows) {
        list.textContent = '';
        if (!rows.length) { list.appendChild(empty('No products yet', 'Add your first product so farmers can order.', 'box')); return; }
        list.appendChild(tableOf(['Product', 'Category', 'Unit', 'Price', 'Stock', 'Min order', 'Status', ''], rows.map(function (p) {
          return h('tr', {}, h('td', {}, h('b', { text: p.name }), p.licenceRef ? h('div', { class: 'muted', text: p.licenceRef }) : null),
            h('td', { text: SUPPLY_CAT[p.category] || p.category }), h('td', { text: p.unit }), h('td', { text: money(p.price) }),
            h('td', {}, p.stockQty <= 5 ? pill(p.stockQty + ' left', p.stockQty === 0 ? 'red' : 'amber') : String(p.stockQty)), h('td', { text: String(p.minOrderQty) }),
            h('td', {}, pill(p.active ? 'Active' : 'Hidden', p.active ? 'green' : 'grey')),
            h('td', { class: 'actions' }, btn('Edit', { kind: 'secondary', small: true, onclick: function () { productForm(p, load); } }),
              btn(p.active ? 'Hide' : 'Show', { kind: 'ghost', small: true, onclick: function () {
                var req = p.active ? api('/api/company/products/' + p.id, { method: 'DELETE' }) : api('/api/company/products/' + p.id, { method: 'PUT', json: productBody(p, true) });
                req.then(function () { toast(p.active ? 'Product hidden' : 'Product visible again'); load(); }).catch(function (e) { toast(e.message, true); });
              } })));
        })));
      }).catch(function (e) { fail(list, e); });
    }
    load();
  }

  // ---------- orders + invoice ----------
  function invoiceModal(orderId) {
    getJson('/api/company/supply-orders/' + orderId + '/invoice').then(function (v) {
      var m, lines = v.lines.map(function (l) { return h('tr', {}, h('td', { text: l.description }), h('td', { text: l.quantity + ' × ' + l.unit }), h('td', { text: money(l.unitPrice) }), h('td', { text: money(l.lineTotal) })); });
      var paid = v.paymentStatus === 'PAID';
      m = openModal(h('div', { class: 'invoice' },
        h('div', { class: 'inv-head' }, h('div', {}, h('h2', { text: 'INVOICE' }), h('div', { class: 'muted', text: v.number + ' · ' + when(v.issuedAt) })),
          h('div', {}, h('b', { text: v.sellerName }), h('div', { text: 'Reg. no. ' + v.sellerRegNumber }), h('div', { text: 'KRA PIN ' + (v.sellerKraPin || '-') }), h('div', { text: v.sellerAddress || '' }), h('div', { text: [v.sellerPhone, v.sellerEmail].filter(Boolean).join(' · ') }))),
        h('div', { class: 'parties' }, h('div', {}, h('small', { text: 'Bill to' }), h('b', { text: v.buyerName }), h('div', { text: v.buyerPhone || '' })), h('div', {}, h('small', { text: 'Deliver to' }), h('div', { text: v.deliveryAddress || '' }))),
        tableOf(['Item', 'Quantity', 'Price', 'Amount'], lines),
        h('div', { class: 'total', text: 'Total ' + money(v.total) }),
        h('p', {}, h('span', { class: 'stamp' + (paid ? '' : ' unpaid'), text: paid ? 'PAID' : 'UNPAID' }), paid ? ' ' + (v.paymentMethod === 'MPESA' ? 'M-Pesa ' + (v.receipt || '') : 'Cash') + ' · ' + when(v.paidAt) : ''),
        h('div', { class: 'actions no-print' }, btn('Close', { kind: 'secondary', onclick: function () { m.close(); } }), btn('Print', { icon: 'printer', onclick: function () { window.print(); } }))));
    }).catch(function (e) { toast(e.message, true); });
  }

  function timeline(status) {
    var flow = ['PLACED', 'CONFIRMED', 'DISPATCHED', 'DELIVERED'], bad = status === 'CANCELLED' || status === 'REJECTED', idx = flow.indexOf(status);
    if (bad) return h('div', { class: 'timeline' }, h('div', { class: 't bad', text: ORDER_STATUS[status] }));
    return h('div', { class: 'timeline' }, flow.map(function (s, i) { return h('div', { class: 't' + (i < idx || status === 'DELIVERED' ? ' done' : i === idx ? ' now' : ''), text: ORDER_STATUS[s] }); }));
  }

  function orderDetail(id, done) {
    getJson('/api/company/supply-orders/' + id).then(function (o) {
      var m;
      function act(path, json, msg) {
        return post('/api/company/supply-orders/' + id + '/' + path, json).then(function () { m.close(); toast(msg); done(); }).catch(function (e) { toast(e.message, true); });
      }
      var acts = h('div', { class: 'actions' }, btn('Close', { kind: 'secondary', onclick: function () { m.close(); } }));
      if (o.status !== 'CANCELLED' && o.status !== 'REJECTED') acts.appendChild(btn('Invoice', { kind: 'secondary', icon: 'file', onclick: function () { invoiceModal(o.id); } }));
      if (o.status === 'PLACED' || o.status === 'CONFIRMED') acts.appendChild(btn('Reject', { kind: 'danger', onclick: function () {
        promptBox('Reject order #' + o.id, 'Reason shown to the farmer', { min: 5, multiline: true, ok: 'Reject order', danger: true }).then(function (r) { if (r) act('reject', { reason: r }, 'Order rejected'); });
      } }));
      if (o.status === 'PLACED') acts.appendChild(btn('Confirm order', { onclick: function () { act('confirm', undefined, 'Order confirmed'); } }));
      if (o.status === 'CONFIRMED') acts.appendChild(btn('Mark dispatched', { icon: 'truck', onclick: function () { act('dispatch', undefined, 'Marked dispatched'); } }));
      if (o.status === 'DISPATCHED') acts.appendChild(btn('Mark delivered', { icon: 'check', onclick: function () { act('deliver', undefined, 'Marked delivered'); } }));
      if (o.status === 'DELIVERED' && o.paymentStatus === 'UNPAID') acts.appendChild(btn('Mark paid (cash)', { onclick: function () {
        confirmBox('Mark as paid', 'Confirm you received ' + money(o.total) + ' in cash or directly from the farmer. If the farmer pays by M-Pesa through FarmConnect, this is recorded automatically.', 'Yes, it is paid').then(function (ok) { if (ok) act('mark-paid', undefined, 'Marked paid'); });
      } }));
      var lines = o.items.map(function (i) { return h('tr', {}, h('td', { text: i.name }), h('td', { text: i.quantity + ' × ' + i.unit }), h('td', { text: money(i.unitPrice) }), h('td', { text: money(i.lineTotal) })); });
      var payText = o.paymentStatus === 'PAID' ? 'Paid ' + when(o.paidAt) + (o.paymentMethod === 'MPESA' ? ' by M-Pesa ' + (o.mpesaReceipt || '') : ' (cash)') : o.paymentStatus === 'PENDING' ? 'M-Pesa payment in progress' : 'Not yet paid';
      m = openModal(h('div', {}, h('h2', {}, 'Order #' + o.id + ' ', pill(ORDER_STATUS[o.status] || o.status, ORDER_TONE[o.status])), timeline(o.status),
        h('div', { class: 'facts' }, h('div', {}, h('span', { text: 'Farmer' }), o.buyerName + ' · ' + (o.deliveryPhone || '')), h('div', {}, h('span', { text: 'Deliver to' }), o.deliveryAddress || ''),
          h('div', {}, h('span', { text: 'Placed' }), when(o.createdAt)), o.deliveryNote ? h('div', {}, h('span', { text: 'Note' }), o.deliveryNote) : null,
          o.rejectionReason ? h('div', {}, h('span', { text: 'Rejection reason' }), o.rejectionReason) : null),
        tableOf(['Item', 'Quantity', 'Price', 'Total'], lines),
        h('div', { class: 'facts' }, h('div', {}, h('span', { text: 'Order total' }), money(o.total)), h('div', {}, h('span', { text: 'FarmConnect fee' }), money(Number(o.platformFee) + Number(o.countyFee))),
          h('div', {}, h('span', { text: 'You receive' }), h('b', { text: money(o.companyNet) })), h('div', {}, h('span', { text: 'Payment' }), payText)), acts));
    }).catch(function (e) { toast(e.message, true); });
  }

  function orders(box) {
    var c = state.company;
    if (!c.verified || !c.canSell) { lockedView(box, 'Orders'); return; }
    var list = h('div'), status = '';
    box.appendChild(pageHead('Orders', 'Orders from farmers for your products.', btn('Refresh', { kind: 'secondary', icon: 'refresh', onclick: function () { load(); } })));
    box.appendChild(seg([['', 'All'], ['PLACED', 'New'], ['CONFIRMED', 'Confirmed'], ['DISPATCHED', 'Dispatched'], ['DELIVERED', 'Delivered'], ['REJECTED', 'Rejected']], '', function (v) { status = v; load(); }));
    box.appendChild(list);
    function load() {
      list.textContent = ''; list.appendChild(skeleton(4));
      getJson('/api/company/supply-orders' + (status ? '?status=' + status : '')).then(function (rows) {
        list.textContent = '';
        if (!rows.length) { list.appendChild(empty('No orders here', 'New orders from farmers appear here.', 'clipboard')); return; }
        list.appendChild(tableOf(['#', 'Date', 'Farmer', 'Items', 'Total', 'You receive', 'Payment', 'Status', ''], rows.map(function (o) {
          return h('tr', {}, h('td', { text: String(o.id) }), h('td', { text: when(o.createdAt) }), h('td', { text: o.buyerName }),
            h('td', { text: o.items.map(function (i) { return i.quantity + ' ' + i.name; }).join(', ') }), h('td', { text: money(o.total) }), h('td', { text: money(o.companyNet) }),
            h('td', {}, pill(o.paymentStatus === 'PAID' ? 'Paid' : o.paymentStatus === 'PENDING' ? 'In progress' : 'Unpaid', o.paymentStatus === 'PAID' ? 'green' : o.paymentStatus === 'PENDING' ? 'blue' : 'grey')),
            h('td', {}, pill(ORDER_STATUS[o.status] || o.status, ORDER_TONE[o.status])),
            h('td', { class: 'actions' }, btn('Open', { small: true, onclick: function () { orderDetail(o.id, load); } })));
        })));
      }).catch(function (e) { fail(list, e); });
    }
    load();
  }

  // ---------- payouts ----------
  var PAYOUT_TYPES = { MPESA_PHONE: 'M-Pesa phone number', PAYBILL: 'Paybill', TILL: 'Till number (Buy Goods)' };
  var PAYOUT_STATUS = { QUEUED: 'Queued', PROCESSING: 'Sending', SUCCESS: 'Paid out', FAILED: 'Failed', UNKNOWN: 'Being checked' };
  var PAYOUT_TONE = { QUEUED: 'amber', PROCESSING: 'blue', SUCCESS: 'green', FAILED: 'red', UNKNOWN: 'purple' };

  function payoutCard(c) {
    var err = h('div', { class: 'error', role: 'alert' });
    var type = h('select', {}, Object.keys(PAYOUT_TYPES).map(function (k) { return h('option', { value: k, text: PAYOUT_TYPES[k] }); }));
    type.value = c.payoutType || 'MPESA_PHONE';
    var account = h('input', { required: true, maxlength: 30, value: c.payoutAccount || '', placeholder: '07XXXXXXXX or paybill / till number' });
    var ref = h('input', { maxlength: 40, value: c.payoutReference || '', placeholder: 'Paybill account number' });
    var b = btn('Save payout account', { type: 'submit' });
    var status = !c.payoutAccount ? pill('Not set', 'red') : c.payoutApproved ? pill('Approved', 'green') : pill('Waiting for approval', 'amber');
    return h('div', { class: 'card' }, h('h3', {}, icon('wallet'), 'Payout account ', status),
      h('p', { class: 'sub', text: 'This is where FarmConnect sends your share when farmers pay by M-Pesa. The super admin must approve it, and every change needs approval again.' }),
      h('form', { onsubmit: function (ev) {
        ev.preventDefault(); err.textContent = ''; busy(b, true);
        api('/api/company/payout-account', { method: 'PUT', json: { type: type.value, account: account.value.trim(), reference: ref.value.trim() } }).then(function (r) { return r.json(); })
          .then(function (res) { state.company = res; toast('Saved. Waiting for the super admin to approve it.'); render(); })
          .catch(function (e) { err.textContent = e.message; busy(b, false); });
      } }, h('div', { class: 'form-grid' }, field('Type', type), field('Number', account), field('Account number (paybill only)', ref)), h('div', { class: 'sp' }), b, err));
  }

  function payouts(box) {
    var c = state.company;
    if (!c.canSell) { lockedView(box, 'Payouts'); return; }
    box.appendChild(pageHead('Payouts', 'Money FarmConnect sends to your payout account.'));
    box.appendChild(payoutCard(c));
    var list = h('div');
    box.appendChild(h('h3', { class: 'section-title', text: 'Payout history' }));
    box.appendChild(list); list.appendChild(skeleton(2));
    getJson('/api/company/payouts').then(function (rows) {
      list.textContent = '';
      if (!rows.length) { list.appendChild(empty('No payouts yet', 'They appear when a farmer pays an order by M-Pesa.', 'wallet')); return; }
      list.appendChild(tableOf(['#', 'Order', 'Amount', 'Status', 'Date', 'Note'], rows.map(function (p) {
        return h('tr', {}, h('td', { text: String(p.id) }), h('td', { text: '#' + p.orderId }), h('td', { text: money(p.amountKes) }),
          h('td', {}, pill(PAYOUT_STATUS[p.status] || p.status, PAYOUT_TONE[p.status])), h('td', { text: when(p.completedAt || p.createdAt) }),
          h('td', { text: p.status === 'SUCCESS' ? (p.providerTransactionId || '') : (p.failureReason || '') }));
      })));
    }).catch(function (e) { fail(list, e); });
  }

  // ---------- services ----------
  var SERVICE_CAT = { TRANSPORT: 'Transport and logistics', MACHINERY_HIRE: 'Machinery hire', SOIL_TESTING: 'Soil testing', IRRIGATION: 'Irrigation', AGRONOMY: 'Agronomy advice', STORAGE: 'Storage', OTHER: 'Other' };
  var SR_STATUS = { REQUESTED: 'New', ACCEPTED: 'Accepted', DECLINED: 'Declined', COMPLETED: 'Completed', CANCELLED: 'Cancelled' };
  var SR_TONE = { REQUESTED: 'amber', ACCEPTED: 'blue', DECLINED: 'red', COMPLETED: 'green', CANCELLED: 'grey' };

  function serviceForm(existing, done) {
    getCounties().then(function (counties) {
      var s = existing || { active: true, category: 'TRANSPORT', counties: [] };
      var err = h('div', { class: 'error', role: 'alert' });
      var name = h('input', { required: true, maxlength: 120, placeholder: 'e.g. Farm-to-market transport', value: s.name || '' });
      var cat = h('select', {}, Object.keys(SERVICE_CAT).map(function (k) { return h('option', { value: k, text: SERVICE_CAT[k] }); })); cat.value = s.category;
      var price = h('input', { type: 'number', min: '0.01', step: '0.01', placeholder: 'Leave empty for "price on request"', value: s.price != null ? String(s.price) : '' });
      var basis = h('input', { maxlength: 40, placeholder: 'per km, per acre, per visit, per day', value: s.priceBasis || '' });
      var grid = countyChecks(counties, s.counties);
      var desc = h('textarea', { maxlength: 1000, rows: 3 }); desc.value = s.description || '';
      var active = h('input', { type: 'checkbox' }); active.checked = !!s.active;
      var b = btn('Save', { type: 'submit' }), m;
      var form = h('form', { onsubmit: function (ev) {
        ev.preventDefault(); err.textContent = ''; busy(b, true);
        var body = { name: name.value.trim(), category: cat.value, description: desc.value.trim(), priceBasis: basis.value.trim(), price: price.value ? Number(price.value) : null, counties: grid.values(), active: active.checked };
        api('/api/company/services' + (existing ? '/' + existing.id : ''), { method: existing ? 'PUT' : 'POST', json: body }).then(function (r) { return r.json(); })
          .then(function () { m.close(); toast('Service saved'); done(); }).catch(function (e) { err.textContent = e.message; busy(b, false); });
      } }, h('h2', { text: existing ? 'Edit service' : 'Offer a service' }),
        h('div', { class: 'form-grid' }, field('Service name *', name), field('Category *', cat), field('Price (KSh)', price), field('Price basis', basis),
          field('Counties you serve', grid, true, 'Leave all unticked to serve every county you operate in'), field('Description', desc, true),
          h('div', { class: 'wide' }, h('label', { class: 'check' }, active, 'Visible to farmers'))),
        err, h('div', { class: 'actions' }, btn('Cancel', { kind: 'secondary', onclick: function () { m.close(); } }), b));
      m = openModal(form);
    });
  }

  function services(box) {
    var c = state.company;
    if (!c.verified || !c.canSell) { lockedView(box, 'Services'); return; }
    var list = h('div');
    box.appendChild(pageHead('Services', 'Transport, machinery hire, soil testing, irrigation, agronomy and more.', btn('Offer a service', { icon: 'plus', onclick: function () { serviceForm(null, load); } })));
    box.appendChild(list);
    function load() {
      list.textContent = ''; list.appendChild(skeleton(3));
      getJson('/api/company/services').then(function (rows) {
        list.textContent = '';
        if (!rows.length) { list.appendChild(empty('No services yet', 'Offer transport, machinery hire or advice to farmers.', 'wrench')); return; }
        list.appendChild(tableOf(['Service', 'Category', 'Price', 'Counties', 'Status', ''], rows.map(function (s) {
          return h('tr', {}, h('td', {}, h('b', { text: s.name }), s.description ? h('div', { class: 'muted', text: s.description.length > 70 ? s.description.slice(0, 70) + '…' : s.description }) : null),
            h('td', { text: SERVICE_CAT[s.category] || s.category }), h('td', { text: s.price != null ? money(s.price) + (s.priceBasis ? ' ' + s.priceBasis : '') : 'On request' }),
            h('td', { text: s.counties.length ? s.counties.join(', ') : 'All your counties' }), h('td', {}, pill(s.active ? 'Active' : 'Hidden', s.active ? 'green' : 'grey')),
            h('td', { class: 'actions' }, btn('Edit', { kind: 'secondary', small: true, onclick: function () { serviceForm(s, load); } }),
              s.active ? btn('Hide', { kind: 'ghost', small: true, onclick: function () {
                api('/api/company/services/' + s.id, { method: 'DELETE' }).then(function () { toast('Service hidden'); load(); }).catch(function (e) { toast(e.message, true); });
              } }) : null));
        })));
      }).catch(function (e) { fail(list, e); });
    }
    load();
  }

  function serviceRequestDetail(r, done) {
    var m;
    function act(path, json, msg) { return post('/api/company/service-requests/' + r.id + '/' + path, json).then(function () { m.close(); toast(msg); done(); }).catch(function (e) { toast(e.message, true); }); }
    var acts = h('div', { class: 'actions' }, btn('Close', { kind: 'secondary', onclick: function () { m.close(); } }));
    if (r.status === 'REQUESTED') {
      acts.appendChild(btn('Decline', { kind: 'danger', onclick: function () {
        promptBox('Decline request', 'Reason shown to the farmer', { min: 5, multiline: true, ok: 'Decline', danger: true }).then(function (t) { if (t) act('decline', { reason: t }, 'Declined'); });
      } }));
      acts.appendChild(btn('Accept', { onclick: function () {
        var price = h('input', { type: 'number', min: '0.01', step: '0.01', placeholder: 'Agreed price (optional)' }), note = h('textarea', { maxlength: 500, rows: 3, placeholder: 'Message to the farmer (optional)' });
        var b = btn('Accept request', { type: 'submit' }), am;
        am = openModal(h('form', { onsubmit: function (ev) {
          ev.preventDefault(); busy(b, true);
          act('accept', { quotedPrice: price.value ? Number(price.value) : null, note: note.value.trim() }, 'Request accepted').then(function () { am.close(); });
        } }, h('h2', { text: 'Accept request' }), field('Agreed price (KSh)', price, false, 'Leave empty to use your listed price'), field('Note', note),
          h('div', { class: 'actions' }, btn('Cancel', { kind: 'secondary', onclick: function () { am.close(); } }), b)), { small: true });
      } }));
    }
    if (r.status === 'ACCEPTED') acts.appendChild(btn('Mark completed', { icon: 'check', onclick: function () { confirmBox('Mark completed', 'Confirm the service has been done.', 'Yes, completed').then(function (ok) { if (ok) act('complete', undefined, 'Marked completed'); }); } }));
    m = openModal(h('div', {}, h('h2', {}, r.serviceName + ' ', pill(SR_STATUS[r.status] || r.status, SR_TONE[r.status])),
      h('div', { class: 'facts' }, h('div', {}, h('span', { text: 'Farmer' }), r.farmerName + ' · ' + r.phone), h('div', {}, h('span', { text: 'Location' }), r.location),
        h('div', {}, h('span', { text: 'Preferred date' }), dayText(r.preferredDate)), h('div', {}, h('span', { text: 'Requested' }), when(r.createdAt)),
        r.quotedPrice != null ? h('div', {}, h('span', { text: 'Agreed price' }), money(r.quotedPrice)) : null, r.companyNote ? h('div', {}, h('span', { text: 'Your note' }), r.companyNote) : null),
      h('div', {}, h('span', { class: 'muted', text: 'Details' }), h('p', { text: r.details })), acts));
  }

  function serviceRequests(box) {
    var c = state.company;
    if (!c.verified || !c.canSell) { lockedView(box, 'Service requests'); return; }
    var list = h('div'), status = '';
    box.appendChild(pageHead('Service requests', 'Farmers asking for your services.', btn('Refresh', { kind: 'secondary', icon: 'refresh', onclick: function () { load(); } })));
    box.appendChild(seg([['', 'All'], ['REQUESTED', 'New'], ['ACCEPTED', 'Accepted'], ['COMPLETED', 'Completed'], ['DECLINED', 'Declined']], '', function (v) { status = v; load(); }));
    box.appendChild(list);
    function load() {
      list.textContent = ''; list.appendChild(skeleton(3));
      getJson('/api/company/service-requests' + (status ? '?status=' + status : '')).then(function (rows) {
        list.textContent = '';
        if (!rows.length) { list.appendChild(empty('No requests here', 'When a farmer asks for one of your services it shows up here.', 'inbox')); return; }
        list.appendChild(tableOf(['Date', 'Service', 'Farmer', 'Location', 'Preferred date', 'Status', ''], rows.map(function (r) {
          return h('tr', {}, h('td', { text: when(r.createdAt) }), h('td', {}, h('b', { text: r.serviceName })), h('td', { text: r.farmerName }), h('td', { text: r.location }),
            h('td', { text: dayText(r.preferredDate) }), h('td', {}, pill(SR_STATUS[r.status] || r.status, SR_TONE[r.status])),
            h('td', { class: 'actions' }, btn('Open', { small: true, onclick: function () { serviceRequestDetail(r, load); } })));
        })));
      }).catch(function (e) { fail(list, e); });
    }
    load();
  }

  // ---------- bulk buying ----------
  var REQ_STATUS = { OPEN: 'Open', CLOSED: 'Closed', FULFILLED: 'Filled', CANCELLED: 'Cancelled' };
  var REQ_TONE = { OPEN: 'green', CLOSED: 'grey', FULFILLED: 'blue', CANCELLED: 'grey' };
  var QUOTE_STATUS = { SUBMITTED: 'Waiting', ACCEPTED: 'Accepted', REJECTED: 'Declined', WITHDRAWN: 'Withdrawn' };
  var QUOTE_TONE = { SUBMITTED: 'amber', ACCEPTED: 'green', REJECTED: 'red', WITHDRAWN: 'grey' };
  var PO_STATUS = { ISSUED: 'Waiting for farmer', CONFIRMED: 'Confirmed', DECLINED: 'Declined', DELIVERED: 'Delivered', CANCELLED: 'Cancelled' };
  var PO_TONE = { ISSUED: 'amber', CONFIRMED: 'blue', DECLINED: 'red', DELIVERED: 'green', CANCELLED: 'red' };

  function requestForm(existing, done) {
    getCounties().then(function (counties) {
      var r = existing || { quantity: '', unit: 'kg', neededBy: inDays(14), closesAt: inDays(7), deliveryCounty: state.company.county || '' };
      var err = h('div', { class: 'error', role: 'alert' });
      var produce = h('input', { required: true, maxlength: 80, placeholder: 'e.g. Maize, Beans, Tomatoes', value: r.produce || '' });
      var qty = h('input', { required: true, type: 'number', min: '0.01', step: '0.01', value: r.quantity === '' ? '' : String(r.quantity) });
      var unit = h('input', { required: true, maxlength: 20, placeholder: 'kg, bag, tonne', value: r.unit || '' });
      var maxp = h('input', { type: 'number', min: '0.01', step: '0.01', placeholder: 'optional', value: r.maxPrice != null ? String(r.maxPrice) : '' });
      var county = countySelect(counties, r.deliveryCounty);
      var loc = h('input', { required: true, maxlength: 250, placeholder: 'Where the produce must be delivered', value: r.deliveryLocation || '' });
      var closes = h('input', { required: true, type: 'date', value: r.closesAt || '' });
      var needed = h('input', { required: true, type: 'date', value: r.neededBy || '' });
      var desc = h('textarea', { maxlength: 1000, rows: 3, placeholder: 'Quality, grade, packaging, moisture...' }); desc.value = r.description || '';
      var b = btn('Save', { type: 'submit' }), m;
      var form = h('form', { onsubmit: function (ev) {
        ev.preventDefault(); err.textContent = ''; busy(b, true);
        var body = { produce: produce.value.trim(), description: desc.value.trim(), quantity: Number(qty.value), unit: unit.value.trim(),
          maxPrice: maxp.value ? Number(maxp.value) : null, deliveryCounty: county.value, deliveryLocation: loc.value.trim(), neededBy: needed.value, closesAt: closes.value };
        api('/api/company/requests' + (existing ? '/' + existing.id : ''), { method: existing ? 'PUT' : 'POST', json: body }).then(function (x) { return x.json(); })
          .then(function () { m.close(); toast('Request saved'); done(); }).catch(function (e) { err.textContent = e.message; busy(b, false); });
      } }, h('h2', { text: existing ? 'Edit request' : 'New produce request' }),
        h('div', { class: 'form-grid' }, field('Produce *', produce), field('Quantity needed *', qty), field('Unit *', unit), field('Highest price per unit (KSh)', maxp),
          field('Deliver to county *', county), field('Delivery location *', loc), field('Quotations close on *', closes), field('Needed by *', needed), field('Details', desc, true)),
        err, h('div', { class: 'actions' }, btn('Cancel', { kind: 'secondary', onclick: function () { m.close(); } }), b));
      m = openModal(form);
    });
  }

  function quotationsModal(req, done) {
    getJson('/api/company/requests/' + req.id + '/quotations').then(function (rows) {
      var m;
      function decide(q, accept) {
        if (accept) {
          confirmBox('Accept this quotation?', q.farmerName + ' will supply ' + num(q.quantity) + ' ' + q.unit + ' of ' + q.produce + ' at ' + money(q.pricePerUnit) + ' per ' + q.unit + '. Total ' + money(q.total) + '. This issues a purchase order.', 'Accept and issue order').then(function (ok) {
            if (ok) post('/api/company/quotations/' + q.id + '/accept').then(function () { toast('Accepted. Purchase order issued.'); m.close(); done(); }).catch(function (e) { toast(e.message, true); });
          });
        } else {
          promptBox('Decline quotation', 'Reason (optional, shown to the farmer)', { optional: true, multiline: true, ok: 'Decline', danger: true }).then(function (reason) {
            if (reason === null) return;
            post('/api/company/quotations/' + q.id + '/reject', { reason: reason }).then(function () { toast('Declined'); m.close(); done(); }).catch(function (e) { toast(e.message, true); });
          });
        }
      }
      var body = rows.length ? tableOf(['Farmer', 'Phone', 'Quantity', 'Price', 'Total', 'Available', 'Message', 'Status', ''], rows.map(function (q) {
        return h('tr', {}, h('td', { text: q.farmerName }), h('td', { text: q.farmerPhone || '' }), h('td', { text: num(q.quantity) + ' ' + q.unit }), h('td', { text: money(q.pricePerUnit) }),
          h('td', { text: money(q.total) }), h('td', { text: dayText(q.availableFrom) }), h('td', { text: q.message || '' }), h('td', {}, pill(QUOTE_STATUS[q.status] || q.status, QUOTE_TONE[q.status])),
          h('td', { class: 'actions' }, q.status === 'SUBMITTED' ? [btn('Accept', { small: true, onclick: function () { decide(q, true); } }), btn('Decline', { kind: 'secondary', small: true, onclick: function () { decide(q, false); } })] : null));
      })) : empty('No quotations yet', 'Verified farmers will quote when they see your request.', 'inbox');
      m = openModal(h('div', {}, h('h2', { text: 'Quotations for ' + req.produce }), h('p', { class: 'muted', text: 'Cheapest first. Still needed: ' + num(req.remaining) + ' ' + req.unit }), body,
        h('div', { class: 'actions' }, btn('Close', { kind: 'secondary', onclick: function () { m.close(); } }))));
    }).catch(function (e) { toast(e.message, true); });
  }

  function requests(box) {
    var c = state.company;
    if (!c.verified || !c.canBuy) { lockedView(box, 'Produce requests'); return; }
    var list = h('div');
    box.appendChild(pageHead('Produce requests', 'Verified farmers see your open requests and send quotations.', btn('New request', { icon: 'plus', onclick: function () { requestForm(null, load); } })));
    box.appendChild(list);
    function load() {
      list.textContent = ''; list.appendChild(skeleton(3));
      getJson('/api/company/requests').then(function (rows) {
        list.textContent = '';
        if (!rows.length) { list.appendChild(empty('No requests yet', 'Post what you want to buy and let farmers quote.', 'send')); return; }
        list.appendChild(tableOf(['Produce', 'Needed', 'Still needed', 'Max price', 'Deliver to', 'Closes', 'Quotes', 'Status', ''], rows.map(function (r) {
          return h('tr', {}, h('td', {}, h('b', { text: r.produce }), h('div', { class: 'muted', text: 'Needed by ' + dayText(r.neededBy) })), h('td', { text: num(r.quantity) + ' ' + r.unit }), h('td', { text: num(r.remaining) + ' ' + r.unit }),
            h('td', { text: r.maxPrice != null ? money(r.maxPrice) : '-' }), h('td', { text: r.deliveryCounty + ', ' + r.deliveryLocation }), h('td', { text: dayText(r.closesAt) }),
            h('td', {}, r.openQuotations ? pill(r.openQuotations + ' waiting', 'amber') : '0'), h('td', {}, pill(REQ_STATUS[r.status] || r.status, REQ_TONE[r.status])),
            h('td', { class: 'actions' }, btn('Quotations', { small: true, onclick: function () { quotationsModal(r, load); } }),
              r.status === 'OPEN' && !r.openQuotations ? btn('Edit', { kind: 'secondary', small: true, onclick: function () { requestForm(r, load); } }) : null,
              r.status === 'OPEN' ? btn('Close', { kind: 'ghost', small: true, onclick: function () {
                confirmBox('Close this request?', 'Waiting quotations will be declined and farmers told.', 'Close request', true).then(function (ok) {
                  if (ok) post('/api/company/requests/' + r.id + '/close').then(function () { toast('Request closed'); load(); }).catch(function (e) { toast(e.message, true); });
                });
              } }) : null));
        })));
      }).catch(function (e) { fail(list, e); });
    }
    load();
  }

  function bulkOrders(box) {
    var c = state.company;
    if (!c.verified || !c.canBuy) { lockedView(box, 'Purchase orders'); return; }
    var list = h('div');
    box.appendChild(pageHead('Purchase orders', 'Created when you accept a farmer\'s quotation.', btn('Refresh', { kind: 'secondary', icon: 'refresh', onclick: function () { load(); } })));
    box.appendChild(list);
    function load() {
      list.textContent = ''; list.appendChild(skeleton(3));
      getJson('/api/company/purchase-orders').then(function (rows) {
        list.textContent = '';
        if (!rows.length) { list.appendChild(empty('No purchase orders yet', 'Accept a quotation to issue your first one.', 'clipboard')); return; }
        list.appendChild(tableOf(['#', 'Produce', 'Farmer', 'Quantity', 'Total', 'Deliver to', 'Payment', 'Status', ''], rows.map(function (o) {
          function step(path, json, msg) { return post('/api/company/purchase-orders/' + o.id + '/' + path, json).then(function () { toast(msg); load(); }).catch(function (e) { toast(e.message, true); }); }
          var acts = [];
          if (o.status === 'CONFIRMED') acts.push(btn('Mark received', { small: true, onclick: function () {
            confirmBox('Mark as received', 'Confirm you received ' + num(o.quantity) + ' ' + o.unit + ' of ' + o.produce + '.', 'Yes, received').then(function (ok) { if (ok) step('deliver', undefined, 'Delivery recorded'); }); } }));
          if (o.status === 'DELIVERED' && o.paymentStatus !== 'PAID') acts.push(btn('Mark paid', { small: true, onclick: function () {
            confirmBox('Mark as paid', 'Confirm you paid ' + o.farmerName + ' ' + money(o.total) + '.', 'Yes, paid').then(function (ok) { if (ok) step('mark-paid', undefined, 'Marked paid'); }); } }));
          if (o.status === 'ISSUED' || o.status === 'CONFIRMED') acts.push(btn('Cancel', { kind: 'danger', small: true, onclick: function () {
            promptBox('Cancel purchase order #' + o.id, 'Reason shown to the farmer', { min: 5, multiline: true, ok: 'Cancel order', danger: true }).then(function (r) { if (r) step('cancel', { reason: r }, 'Cancelled'); }); } }));
          return h('tr', {}, h('td', { text: String(o.id) }), h('td', {}, h('b', { text: o.produce }), h('div', { class: 'muted', text: 'Needed by ' + dayText(o.neededBy) })),
            h('td', {}, o.farmerName, o.contactPhone ? h('div', { class: 'muted', text: o.contactPhone }) : null), h('td', { text: num(o.quantity) + ' ' + o.unit + ' × ' + money(o.pricePerUnit) }), h('td', { text: money(o.total) }),
            h('td', { text: o.deliveryCounty + ', ' + o.deliveryLocation }), h('td', {}, pill(o.paymentStatus === 'PAID' ? 'Paid' : 'Unpaid', o.paymentStatus === 'PAID' ? 'green' : 'grey')),
            h('td', {}, pill(PO_STATUS[o.status] || o.status, PO_TONE[o.status])), h('td', { class: 'actions' }, acts));
        })));
      }).catch(function (e) { fail(list, e); });
    }
    load();
  }

  // ---------- team ----------
  var MEMBER_ROLE = { OWNER: 'Owner', MANAGER: 'Manager', SALES: 'Sales', DRIVER: 'Driver', ACCOUNTANT: 'Accountant', OTHER: 'Other' };

  function memberForm(existing, done) {
    var mem = existing || { role: 'MANAGER', active: true };
    var err = h('div', { class: 'error', role: 'alert' });
    var name = h('input', { required: true, maxlength: 100, value: mem.name || '' });
    var role = h('select', {}, Object.keys(MEMBER_ROLE).map(function (k) { return h('option', { value: k, text: MEMBER_ROLE[k] }); })); role.value = mem.role;
    var phone = h('input', { type: 'tel', maxlength: 30, value: mem.phone || '' });
    var email = h('input', { type: 'email', maxlength: 150, value: mem.email || '' });
    var active = h('input', { type: 'checkbox' }); active.checked = !!mem.active;
    var b = btn('Save', { type: 'submit' }), m;
    var form = h('form', { onsubmit: function (ev) {
      ev.preventDefault(); err.textContent = ''; busy(b, true);
      api('/api/company/team' + (existing ? '/' + existing.id : ''), { method: existing ? 'PUT' : 'POST', json: { name: name.value.trim(), phone: phone.value.trim(), email: email.value.trim(), role: role.value, active: active.checked } })
        .then(function () { m.close(); toast('Team member saved'); done(); }).catch(function (e) { err.textContent = e.message; busy(b, false); });
    } }, h('h2', { text: existing ? 'Edit team member' : 'Add team member' }),
      h('div', { class: 'form-grid' }, field('Name *', name), field('Role *', role), field('Phone', phone), field('Email', email),
        h('div', { class: 'wide' }, h('label', { class: 'check' }, active, 'Currently working here'))),
      h('p', { class: 'hint', text: 'Team members are contacts for your company. They do not get a login.' }), err,
      h('div', { class: 'actions' }, btn('Cancel', { kind: 'secondary', onclick: function () { m.close(); } }), b));
    m = openModal(form, { small: true });
  }

  function team(box) {
    var list = h('div');
    box.appendChild(pageHead('Team', 'The people who work at your company.', btn('Add team member', { icon: 'plus', onclick: function () { memberForm(null, load); } })));
    box.appendChild(list);
    function load() {
      list.textContent = ''; list.appendChild(skeleton(3));
      getJson('/api/company/team').then(function (rows) {
        list.textContent = '';
        if (!rows.length) { list.appendChild(empty('No team members yet', 'Add your managers, sales staff and drivers.', 'users')); return; }
        list.appendChild(tableOf(['Name', 'Role', 'Phone', 'Email', 'Status', ''], rows.map(function (t) {
          return h('tr', {}, h('td', {}, h('b', { text: t.name })), h('td', { text: MEMBER_ROLE[t.role] || t.role }), h('td', { text: t.phone || '-' }), h('td', { text: t.email || '-' }),
            h('td', {}, pill(t.active ? 'Active' : 'Inactive', t.active ? 'green' : 'grey')),
            h('td', { class: 'actions' }, btn('Edit', { kind: 'secondary', small: true, onclick: function () { memberForm(t, load); } }),
              btn('Remove', { kind: 'ghost', small: true, onclick: function () {
                confirmBox('Remove ' + t.name + '?', 'They will be removed from your team list.', 'Remove', true).then(function (ok) {
                  if (ok) api('/api/company/team/' + t.id, { method: 'DELETE' }).then(function () { toast('Removed'); load(); }).catch(function (e) { toast(e.message, true); });
                });
              } })));
        })));
      }).catch(function (e) { fail(list, e); });
    }
    load();
  }

  // ---------- reports ----------
  function reports(box) {
    var c = state.company;
    box.appendChild(pageHead('Reports', 'Download your records as CSV files that open in Excel.'));
    var from = h('input', { type: 'date' }), to = h('input', { type: 'date', value: inDays(0) });
    var q = function () { var p = []; if (from.value) p.push('from=' + from.value); if (to.value) p.push('to=' + to.value); return p.length ? '?' + p.join('&') : ''; };
    function dl(path, name, b) { busy(b, true); download(path + q(), name).then(function () { toast('Download started'); }).catch(function (e) { toast(e.message, true); }).then(function () { busy(b, false); }); }
    var cards = [];
    if (c.canSell) {
      var b1 = btn('Download', { icon: 'download', onclick: function () { dl('/api/company/reports/sales.csv', 'farmconnect-sales.csv', b1); } });
      var b2 = btn('Download', { icon: 'download', onclick: function () { dl('/api/company/reports/payouts.csv', 'farmconnect-payouts.csv', b2); } });
      cards.push(h('div', { class: 'card' }, h('h3', {}, icon('chart'), 'Sales report'), h('p', { class: 'sub', text: 'Every order with totals, FarmConnect fees, what you receive and how it was paid.' }), b1));
      cards.push(h('div', { class: 'card' }, h('h3', {}, icon('wallet'), 'Payouts report'), h('p', { class: 'sub', text: 'Money FarmConnect sent to your payout account, with M-Pesa references.' }), b2));
    }
    if (c.canBuy) {
      var b3 = btn('Download', { icon: 'download', onclick: function () { dl('/api/company/reports/purchases.csv', 'farmconnect-purchases.csv', b3); } });
      cards.push(h('div', { class: 'card' }, h('h3', {}, icon('clipboard'), 'Purchases report'), h('p', { class: 'sub', text: 'Your bulk purchase orders with farmers, quantities and prices.' }), b3));
    }
    box.appendChild(h('div', { class: 'card' }, h('h3', { text: 'Date range' }), h('p', { class: 'sub', text: 'Leave "from" empty to include everything.' }),
      h('div', { class: 'form-grid' }, field('From', from), field('To', to))));
    box.appendChild(h('div', { class: 'grid2' }, cards));
  }

  // ---------- alerts ----------
  var NOTE_ICON = { SUPPLY: 'cart', BULK: 'send', SERVICE: 'wrench', COMPANY: 'shield' };
  var NOTE_LINK = { SUPPLY: '#orders', BULK: '#bulkorders', SERVICE: '#servicerequests', COMPANY: '#overview' };

  function refreshCounts() {
    if (!state.token) return;
    getJson('/api/notifications/counts').then(function (c) { state.unread = c.notifications || 0; updateBell(); }).catch(function () { /* the bell is optional */ });
  }
  function updateBell() {
    Array.prototype.forEach.call(document.querySelectorAll('[data-bell]'), function (el) {
      el.textContent = state.unread > 99 ? '99+' : String(state.unread); el.classList.toggle('hidden', !state.unread);
    });
  }

  function alerts(box) {
    var list = h('div');
    var markAll = btn('Mark all read', { kind: 'secondary', icon: 'check', onclick: function () {
      post('/api/notifications/read-all').then(function () { state.unread = 0; updateBell(); load(); }).catch(function (e) { toast(e.message, true); });
    } });
    box.appendChild(pageHead('Alerts', 'Orders, quotations, payments and verification updates.', markAll));
    box.appendChild(list);
    function load() {
      list.textContent = ''; list.appendChild(skeleton(4));
      getJson('/api/notifications').then(function (rows) {
        list.textContent = '';
        if (!rows.length) { list.appendChild(empty('No alerts yet', 'You will see new orders, quotations and payments here.', 'bell')); return; }
        list.appendChild(h('div', { class: 'feed' }, rows.map(function (n) {
          return h('div', { class: 'item' + (n.read ? '' : ' unread'), tabindex: '0', role: 'button', onclick: function () {
            if (!n.read) post('/api/notifications/' + n.id + '/read').then(function () { refreshCounts(); }).catch(function () { /* ignore */ });
            var link = NOTE_LINK[n.type]; if (link) location.hash = link;
          } }, h('div', { class: 'ico-wrap' }, icon(NOTE_ICON[n.type] || 'bell')), h('div', {}, h('b', { text: n.title }), h('div', { text: n.body || '' }), h('small', { text: when(n.createdAt) })));
        })));
      }).catch(function (e) { fail(list, e); });
    }
    load();
  }

  // ---------- frame ----------
  var ROUTES = {
    overview: { title: 'Overview', icon: 'home', view: overview },
    products: { title: 'Products', icon: 'box', view: products },
    orders: { title: 'Orders', icon: 'clipboard', view: orders },
    services: { title: 'Services', icon: 'wrench', view: services },
    servicerequests: { title: 'Service requests', icon: 'inbox', view: serviceRequests },
    payouts: { title: 'Payouts', icon: 'wallet', view: payouts },
    requests: { title: 'Produce requests', icon: 'send', view: requests },
    bulkorders: { title: 'Purchase orders', icon: 'clipboard', view: bulkOrders },
    team: { title: 'Team', icon: 'users', view: team },
    reports: { title: 'Reports', icon: 'chart', view: reports },
    alerts: { title: 'Alerts', icon: 'bell', view: alerts },
    profile: { title: 'Company profile', icon: 'user', view: profile },
    documents: { title: 'Documents', icon: 'file', view: documents }
  };

  function navFor(c) {
    var groups = [{ name: '', items: ['overview'] }];
    if (c.canSell) groups.push({ name: 'Sell', items: ['products', 'orders', 'services', 'servicerequests', 'payouts'] });
    if (c.canBuy) groups.push({ name: 'Buy', items: ['requests', 'bulkorders'] });
    groups.push({ name: 'Company', items: ['team', 'reports', 'alerts', 'profile', 'documents'] });
    return groups;
  }

  function shell() {
    var c = state.company, groups = navFor(c), valid = {};
    groups.forEach(function (g) { g.items.forEach(function (k) { valid[k] = true; }); });
    var v = (location.hash || '').replace('#', ''); if (!valid[v]) v = 'overview';
    var route = ROUTES[v];
    var frame = h('div', { class: 'frame' + (state.menuOpen ? ' open' : '') });
    function closeMenu() { state.menuOpen = false; frame.classList.remove('open'); }
    var nav = h('nav', { class: 'nav', 'aria-label': 'Main' });
    groups.forEach(function (g) {
      if (g.name) nav.appendChild(h('div', { class: 'group', text: g.name }));
      g.items.forEach(function (k) {
        var r = ROUTES[k], a = h('a', { href: '#' + k, class: k === v ? 'active' : null, onclick: closeMenu }, icon(r.icon), r.title);
        if (k === 'alerts') a.appendChild(h('span', { class: 'badge hidden', 'data-bell': '' }));
        nav.appendChild(a);
      });
    });
    var name = state.user ? state.user.name : '';
    var side = h('aside', { class: 'side' }, h('div', { class: 'brand' }, h('b', { text: 'FarmConnect' }), h('span', { text: 'Company portal' })),
      h('div', { class: 'company-chip' }, h('b', { text: c.name }), pill(STATUS[c.status] || c.status, STATUS_TONE[c.status])), nav,
      h('div', { class: 'me' }, h('div', { class: 'avatar', text: (name || '?').trim().charAt(0).toUpperCase() }),
        h('div', { class: 'who' }, h('b', { text: name }), h('span', { text: 'Company account' })),
        h('button', { title: 'Log out', 'aria-label': 'Log out', onclick: logout }, icon('logout'))));
    var bell = h('button', { class: 'icon-btn', title: 'Alerts', 'aria-label': 'Alerts', onclick: function () { location.hash = '#alerts'; } }, icon('bell'), h('span', { class: 'dot hidden', 'data-bell': '' }));
    var topbar = h('header', { class: 'topbar' },
      h('button', { class: 'menu-btn', 'aria-label': 'Menu', onclick: function () { state.menuOpen = !state.menuOpen; frame.classList.toggle('open', state.menuOpen); } }, icon('menu')),
      h('h2', { text: route.title }), h('div', { class: 'spacer' }),
      h('button', { class: 'icon-btn', title: 'Refresh', 'aria-label': 'Refresh', onclick: function () { loadCompany().then(render).catch(function (e) { toast(e.message, true); }); } }, icon('refresh')), bell);
    var body = h('main', { id: 'view' });
    frame.appendChild(side); frame.appendChild(h('div', { class: 'content' }, topbar, body));
    route.view(body);
    setTimeout(updateBell, 0);
    return frame;
  }

  function setupView(counties) {
    return h('div', { class: 'setup' }, h('div', { class: 'head' }, h('div', {}, h('h2', { text: 'Tell us about your business' }), h('p', { class: 'muted', text: 'Step 2 of 2. The FarmConnect super admin verifies every company before it can trade.' })),
      btn('Log out', { kind: 'secondary', small: true, onclick: logout })),
      h('div', { class: 'card' }, detailsForm(null, counties, function () { render(); })));
  }

  var bellTimer = null;
  function render() {
    var app = document.getElementById('app');
    app.textContent = '';
    if (!state.token) { app.appendChild(authView()); return; }
    if (!state.company) {
      app.appendChild(h('div', { class: 'setup' }, skeleton(3)));
      Promise.all([loadCompany(), getCounties()]).then(function (res) {
        app.textContent = '';
        if (state.company) { app.appendChild(shell()); refreshCounts(); return; }
        app.appendChild(setupView(res[1]));
      }).catch(function (e) { app.textContent = ''; app.appendChild(h('div', { class: 'setup' }, empty('Could not load your account', e.message, 'alert'), h('p', {}, btn('Log out', { kind: 'secondary', onclick: logout })))); });
      return;
    }
    app.appendChild(shell());
    refreshCounts();
  }

  window.addEventListener('hashchange', function () { if (state.token && state.company) { state.menuOpen = false; render(); } });
  if (!bellTimer) bellTimer = setInterval(refreshCounts, 60000);
  render();
})();