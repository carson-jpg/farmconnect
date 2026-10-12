/* FarmConnect county dashboard (officers and administrators). Plain JavaScript, no libraries.
   All text goes in through textContent, never innerHTML. */
(function () {
  'use strict';

  var TOKEN_KEY = 'fc_token', USER_KEY = 'fc_user';
  var state = { token: sessionStorage.getItem(TOKEN_KEY), user: null, view: 'overview', menuOpen: false };
  try { state.user = JSON.parse(sessionStorage.getItem(USER_KEY) || 'null'); } catch (e) { state.user = null; }

  var GREEN = '#2e7d32', LIGHT = '#c8e6c9', AMBER = '#f9a825', BLUE = '#1e88e5';
  var PALETTE = ['#2e7d32', '#f9a825', '#1e88e5', '#8e24aa', '#e53935', '#00897b', '#6d4c41', '#78909c'];
  var SVG_NS = 'http://www.w3.org/2000/svg';

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
      else if (k.indexOf('on') === 0) el.addEventListener(k.slice(2), v);
      else el.setAttribute(k, v === true ? '' : v);
    }
    for (var i = 2; i < arguments.length; i++) add(el, arguments[i]);
    return el;
  }
  function svg(tag, attrs, text) {
    var el = document.createElementNS(SVG_NS, tag);
    for (var k in attrs) el.setAttribute(k, attrs[k]);
    if (text != null) el.textContent = text;
    return el;
  }
  var ICONS = {
    overview: ['M3 3h7v7H3z', 'M14 3h7v7h-7z', 'M14 14h7v7h-7z', 'M3 14h7v7H3z'],
    verifications: ['M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z', 'M9 12l2 2 4-4'],
    prices: ['M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z', 'M7 7h.01'],
    reports: ['M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z', 'M14 2v6h6', 'M16 13H8', 'M16 17H8'],
    users: ['M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2', 'M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z', 'M23 21v-2a4 4 0 0 0-3-3.87', 'M16 3.13a4 4 0 0 1 0 7.75'],
    officers: ['M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2', 'M8.5 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z', 'M20 8v6', 'M23 11h-6'],
    products: ['M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z', 'M3.27 6.96L12 12.01l8.73-5.05', 'M12 22.08V12'],
    orders: ['M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z', 'M3 6h18', 'M16 10a4 4 0 0 1-8 0'],
    logout: ['M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4', 'M16 17l5-5-5-5', 'M21 12H9'],
    menu: ['M3 12h18', 'M3 6h18', 'M3 18h18'],
    farmer: ['M12 22V8', 'M5 12c0-4 3-7 7-7 0 4-3 7-7 7z', 'M19 14c0-4-3-6-7-6 0 4 3 6 7 6z'],
    cart: ['M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z', 'M3 6h18'],
    chat: ['M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z'],
    pulse: ['M22 12h-4l-3 9L9 3l-3 9H2'],
    star: ['M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01z'],
    cash: ['M12 1v22', 'M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6'],
    book: ['M4 19.5A2.5 2.5 0 0 1 6.5 17H20', 'M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z'],
    group: ['M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2', 'M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z']
  };
  function icon(name) {
    var s = svg('svg', { viewBox: '0 0 24 24', 'aria-hidden': 'true' });
    (ICONS[name] || []).forEach(function (d) { s.appendChild(svg('path', { d: d })); });
    return s;
  }
  function num(n) { return Number(n || 0).toLocaleString('en-KE'); }
  function plural(n, one, many) { return num(n) + ' ' + (Number(n) === 1 ? one : (many || one + 's')); }
  function short(n) {
    n = Number(n || 0);
    if (n >= 1e6) return (n / 1e6).toFixed(1).replace(/\.0$/, '') + 'M';
    if (n >= 1e3) return (n / 1e3).toFixed(1).replace(/\.0$/, '') + 'K';
    return String(n);
  }
  function kes(n) { return 'KES ' + num(Math.round(Number(n || 0))); }
  function when(iso) { return iso ? new Date(iso).toLocaleString('en-KE', { dateStyle: 'medium', timeStyle: 'short' }) : '-'; }
  function day(iso) { return iso ? new Date(iso).toLocaleDateString('en-KE', { dateStyle: 'medium' }) : '-'; }
  function clip(s, n) { s = String(s == null ? '' : s); return s.length > n ? s.slice(0, n - 1) + '…' : s; }
  function isAdmin() { return !!state.user && state.user.role === 'ADMIN'; }

  function toast(msg, bad) {
    var t = h('div', { class: 'toast' + (bad ? ' bad' : ''), text: msg });
    document.getElementById('toasts').appendChild(t);
    setTimeout(function () { t.remove(); }, bad ? 6000 : 3500);
  }

  // ---------- server calls ----------
  function api(path, opts) {
    opts = opts || {};
    var headers = { 'Accept': 'application/json' };
    if (state.token) headers['Authorization'] = 'Bearer ' + state.token;
    var init = { method: opts.method || 'GET', headers: headers };
    if (opts.json !== undefined) { headers['Content-Type'] = 'application/json'; init.body = JSON.stringify(opts.json); }
    return fetch(path, init).then(function (r) {
      if (r.status === 401 && state.token) { logout(); throw new Error('Please log in again'); }
      if (!r.ok) {
        return r.text().then(function (t) {
          var m = 'Something went wrong (' + r.status + ')';
          try { var j = JSON.parse(t); if (j.message) m = j.message; } catch (e) { /* not JSON */ }
          throw new Error(m);
        });
      }
      return r;
    });
  }
  function getJson(path) { return api(path).then(function (r) { return r.json(); }); }
  function download(path, filename) {
    return api(path).then(function (r) { return r.blob(); }).then(function (blob) {
      var url = URL.createObjectURL(blob);
      var a = h('a', { href: url, download: filename });
      document.body.appendChild(a); a.click(); a.remove();
      setTimeout(function () { URL.revokeObjectURL(url); }, 4000);
    }).catch(function (e) { toast(e.message, true); });
  }

  // ---------- sign in / out ----------
  function logout() {
    sessionStorage.removeItem(TOKEN_KEY); sessionStorage.removeItem(USER_KEY);
    state.token = null; state.user = null; state.menuOpen = false;
    render();
  }

  function loginView() {
    var err = h('div', { class: 'error', role: 'alert' });
    var email = h('input', { type: 'email', autocomplete: 'username', required: true, placeholder: 'you@example.com' });
    var pass = h('input', { type: 'password', autocomplete: 'current-password', required: true });
    var btn = h('button', { class: 'btn', type: 'submit', text: 'Log in' });
    var form = h('form', { onsubmit: function (ev) {
      ev.preventDefault();
      err.textContent = ''; btn.disabled = true;
      api('/api/auth/login', { method: 'POST', json: { email: email.value.trim(), password: pass.value } })
        .then(function (r) { return r.json(); })
        .then(function (u) {
          if (u.role !== 'ADMIN' && u.role !== 'OFFICER') throw new Error('This dashboard is for county officers and administrators.');
          state.token = u.token; state.user = { name: u.name, role: u.role };
          sessionStorage.setItem(TOKEN_KEY, u.token); sessionStorage.setItem(USER_KEY, JSON.stringify(state.user));
          location.hash = '#overview';
          render();
        })
        .catch(function (e) { err.textContent = e.message; })
        .then(function () { btn.disabled = false; });
    } },
      h('label', { text: 'Email' }), email, h('label', { text: 'Password' }), pass, btn, err);
    return h('div', { class: 'login-wrap' },
      h('div', { class: 'login-card' }, h('h1', { text: '🌿 FarmConnect' }), h('p', { text: 'County dashboard · Trans Nzoia' }), form));
  }

  // ---------- charts (SVG) ----------
  function chartBox(w, hgt) {
    return svg('svg', { 'class': 'chart', viewBox: '0 0 ' + w + ' ' + hgt, role: 'img', preserveAspectRatio: 'xMidYMid meet' });
  }
  function labelText(x, y, text) {
    var t = svg('text', { x: x, y: y }, clip(text, 24));
    t.appendChild(svg('title', {}, text));
    return t;
  }

  function hbars(rows, o) {
    o = o || {};
    var rowH = 30, labelW = 150, W = 560, barW = W - labelW - 70;
    var max = Math.max.apply(null, rows.map(function (r) { return r[1]; }).concat([1]));
    var s = chartBox(W, rows.length * rowH);
    rows.forEach(function (r, i) {
      var y = i * rowH, w = Math.max(r[1] > 0 ? 3 : 0, barW * r[1] / max);
      s.appendChild(labelText(0, y + 19, r[0]));
      s.appendChild(svg('rect', { x: labelW, y: y + 5, width: barW, height: 16, rx: 8, fill: '#eef3ec' }));
      s.appendChild(svg('rect', { x: labelW, y: y + 5, width: w, height: 16, rx: 8, fill: o.color || GREEN }));
      s.appendChild(svg('text', { x: labelW + w + 8, y: y + 18, 'class': 'val' }, (o.prefix || '') + num(r[1])));
    });
    return s;
  }

  function pairBars(rows) {
    var rowH = 40, labelW = 150, W = 560, barW = W - labelW - 120;
    var max = Math.max.apply(null, rows.map(function (r) { return r[1]; }).concat([1]));
    var s = chartBox(W, rows.length * rowH);
    rows.forEach(function (r, i) {
      var y = i * rowH, w1 = Math.max(r[1] > 0 ? 3 : 0, barW * r[1] / max), w2 = Math.max(r[2] > 0 ? 3 : 0, barW * r[2] / max);
      s.appendChild(labelText(0, y + 23, r[0]));
      s.appendChild(svg('rect', { x: labelW, y: y + 4, width: w1, height: 13, rx: 6, fill: LIGHT }));
      s.appendChild(svg('rect', { x: labelW, y: y + 20, width: w2, height: 13, rx: 6, fill: GREEN }));
      s.appendChild(svg('text', { x: labelW + w1 + 8, y: y + 15, 'class': 'val' }, num(r[1])));
      s.appendChild(svg('text', { x: labelW + w2 + 8, y: y + 31, 'class': 'axis' }, num(r[2]) + ' verified'));
    });
    return s;
  }

  function monthLabel(k) {
    var m = /^(\d{4})-(\d{2})$/.exec(k || '');
    if (!m) return k;
    return ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'][Number(m[2]) - 1] + " '" + m[1].slice(2);
  }
  function columns(rows, o) {
    o = o || {};
    var W = 560, H = 230, padB = 36, padT = 26, n = rows.length || 1;
    var max = Math.max.apply(null, rows.map(function (r) { return r[1]; }).concat([1]));
    var slot = W / n, bw = Math.min(56, slot * 0.58);
    var s = chartBox(W, H);
    [0.25, 0.5, 0.75, 1].forEach(function (f) {
      var y = H - padB - (H - padB - padT) * f;
      s.appendChild(svg('line', { x1: 0, y1: y, x2: W, y2: y, stroke: '#edf2ea' }));
    });
    s.appendChild(svg('line', { x1: 0, y1: H - padB, x2: W, y2: H - padB, stroke: '#cfdcc9' }));
    rows.forEach(function (r, i) {
      var x = i * slot + (slot - bw) / 2, bh = (H - padB - padT) * r[1] / max, y = H - padB - bh;
      if (r[1] > 0) {
        s.appendChild(svg('rect', { x: x, y: y, width: bw, height: Math.max(3, bh), rx: 6, fill: o.color || GREEN }));
        s.appendChild(svg('text', { x: x + bw / 2, y: y - 7, 'text-anchor': 'middle', 'class': 'val' }, o.short ? short(r[1]) : num(r[1])));
      }
      s.appendChild(svg('text', { x: x + bw / 2, y: H - 14, 'text-anchor': 'middle', 'class': 'axis' }, monthLabel(r[0])));
    });
    return s;
  }

  function donut(rows, centerLabel) {
    var total = rows.reduce(function (a, r) { return a + r[1]; }, 0) || 1;
    var R = 58, C = 2 * Math.PI * R, offset = 0;
    var s = svg('svg', { viewBox: '0 0 170 170', role: 'img' });
    s.appendChild(svg('circle', { cx: 85, cy: 85, r: R, fill: 'none', stroke: '#eef3ec', 'stroke-width': 24 }));
    rows.forEach(function (r, i) {
      var len = C * r[1] / total;
      s.appendChild(svg('circle', { cx: 85, cy: 85, r: R, fill: 'none', stroke: PALETTE[i % PALETTE.length], 'stroke-width': 24,
        'stroke-dasharray': len + ' ' + (C - len), 'stroke-dashoffset': -offset, transform: 'rotate(-90 85 85)' }));
      offset += len;
    });
    s.appendChild(svg('text', { x: 85, y: 88, 'text-anchor': 'middle' }, num(total)));
    s.appendChild(svg('text', { x: 85, y: 106, 'text-anchor': 'middle', 'class': 'small' }, centerLabel || 'total'));
    var list = h('ul', { class: 'dl' }, rows.map(function (r, i) {
      var sw = h('i'); sw.style.background = PALETTE[i % PALETTE.length];
      return h('li', {}, sw, h('span', { text: clip(r[0], 22), title: r[0] }),
        h('span', { class: 'n', text: num(r[1]) }), h('span', { class: 'p', text: Math.round(r[1] * 100 / total) + '%' }));
    }));
    return h('div', { class: 'donut' }, s, list);
  }

  /** FARMER_GROUP -> "Farmer group" (SACCO stays as it is). */
  function nice(s) {
    return /^[A-Z][A-Z_]+$/.test(s) && s !== 'SACCO' ? s.charAt(0) + s.slice(1).toLowerCase().replace(/_/g, ' ') : s;
  }
  function entries(obj, sortDesc, limit, pretty) {
    var rows = Object.keys(obj || {}).map(function (k) { return [pretty ? nice(k) : k, Number(obj[k])]; });
    if (sortDesc) rows.sort(function (a, b) { return b[1] - a[1]; });
    return limit ? rows.slice(0, limit) : rows;
  }
  function card(title, body, legend) { return h('div', { class: 'card' }, h('h3', { text: title }), legend || null, body); }
  function chartCard(title, rows, make) {
    var any = rows.some(function (r) { return r[1] > 0; });
    return card(title, any ? make(rows) : h('div', { class: 'empty', text: 'No data yet' }));
  }
  function kpi(ico, tone, label, value, note) {
    return h('div', { class: 'kpi' }, h('div', { class: 'ico ' + (tone || '') }, icon(ico)),
      h('div', {}, h('div', { class: 'label', text: label }), h('div', { class: 'value', text: value }), note ? h('div', { class: 'note', text: note }) : null));
  }

  // ---------- table helpers ----------
  function table(heads, rows) {
    return h('div', { class: 'table-wrap' }, h('table', {},
      h('thead', {}, h('tr', {}, heads.map(function (t) { return h('th', { class: Array.isArray(t) ? t[1] : null, text: Array.isArray(t) ? t[0] : t }); }))),
      h('tbody', {}, rows)));
  }
  function pill(text, cls) { return h('span', { class: 'pill ' + cls, text: text }); }
  function loading(box) { box.textContent = ''; box.appendChild(h('div', { class: 'loading', text: 'Loading…' })); }
  function failed(box, e) { box.textContent = ''; box.appendChild(h('div', { class: 'table-empty', text: e.message })); }
  function emptyBox(text) { return h('div', { class: 'table-empty', text: text }); }
  function debounce(fn, ms) { var t; return function () { clearTimeout(t); t = setTimeout(fn, ms); }; }

  /** Client-side paging so a long list stays easy to scan. */
  function paged(box, items, perPage, rowFor, heads) {
    var page = 0, pages = Math.max(1, Math.ceil(items.length / perPage));
    function draw() {
      box.textContent = '';
      var slice = items.slice(page * perPage, (page + 1) * perPage);
      box.appendChild(table(heads, slice.map(rowFor)));
      if (pages > 1) {
        box.appendChild(h('div', { class: 'pager' },
          h('span', { text: (page * perPage + 1) + '–' + (page * perPage + slice.length) + ' of ' + num(items.length) }),
          h('button', { class: 'btn secondary small', text: 'Previous', disabled: page === 0, onclick: function () { page--; draw(); } }),
          h('button', { class: 'btn secondary small', text: 'Next', disabled: page >= pages - 1, onclick: function () { page++; draw(); } })));
      }
    }
    draw();
  }

  // ---------- overview ----------
  function overview(box) {
    loading(box);
    var jobs = [getJson('/api/analytics')];
    if (isAdmin()) jobs.push(getJson('/api/admin/stats').catch(function () { return null; }));
    Promise.all(jobs).then(function (res) {
      var a = res[0], st = res[1], i = a.indicators || {};
      box.textContent = '';

      if (st) {
        box.appendChild(h('div', { class: 'section-title', text: 'Marketplace today' }));
        box.appendChild(h('div', { class: 'kpis' },
          kpi('cash', 'amber', 'Revenue (delivered orders)', kes(st.revenue)),
          kpi('orders', 'blue', 'Orders', num(st.orders), num(st.pendingOrders) + ' pending · ' + num(st.deliveredOrders) + ' delivered'),
          kpi('products', '', 'Products', num(st.activeProducts), 'of ' + num(st.products) + ' listed'),
          kpi('verifications', 'purple', 'Verification queue', num(Number(st.pendingVerifications) + Number(st.underReview)), num(st.pendingVerifications) + ' waiting · ' + num(st.underReview) + ' under review'),
          kpi('users', '', 'Accounts', num(st.users), plural(st.officers, 'officer'))));
      }

      box.appendChild(h('div', { class: 'section-title', text: 'People' }));
      box.appendChild(h('div', { class: 'kpis' },
        kpi('farmer', '', 'Registered farmers', num(i.registeredFarmers), num(i.verifiedFarmers) + ' verified'),
        kpi('cart', 'purple', 'Registered buyers', num(i.registeredBuyers)),
        kpi('pulse', 'blue', 'Active users (30 days)', num(i.activeUsers30d)),
        kpi('group', '', 'Groups onboarded', num(i.groupsOnboarded), plural(i.groupMembers, 'member')),
        kpi('officers', 'amber', 'Officers active (30 days)', num(i.officersActive30d) + ' of ' + num(i.officersTotal))));

      box.appendChild(h('div', { class: 'section-title', text: 'Programme activity' }));
      box.appendChild(h('div', { class: 'kpis' },
        kpi('products', '', 'Live market listings', num(i.marketListings)),
        kpi('chat', 'blue', 'Farmer–buyer connections', num(i.farmerBuyerConnections)),
        kpi('book', 'purple', 'Information interactions', num(i.informationInteractions)),
        kpi('star', 'amber', 'Opportunities accessed', num(i.opportunitiesAccessed)),
        kpi('reports', '', 'Programmes communicated', num(i.programmesCommunicated)),
        kpi('overview', 'blue', 'Farmers using digital records', num(i.farmersUsingRecords), plural(i.recordsTotal, 'record')),
        kpi('star', 'amber', 'Farmer satisfaction', i.satisfactionResponses ? Number(i.satisfactionAverage).toFixed(1) + ' / 5' : '-', plural(i.satisfactionResponses, 'vote'))));

      box.appendChild(h('div', { class: 'section-title', text: 'Charts' }));
      var sub = entries(a.farmersBySubCounty, true, 12).map(function (r) { return [r[0], r[1], (a.verifiedBySubCounty || {})[r[0]] || 0]; });
      var legend = h('div', { class: 'legend' },
        h('span', {}, (function () { var d = h('i'); d.style.background = LIGHT; return d; })(), 'Registered'),
        h('span', {}, (function () { var d = h('i'); d.style.background = GREEN; return d; })(), 'Verified'));
      box.appendChild(h('div', { class: 'charts' },
        card('Farmers by sub-county', sub.length ? pairBars(sub) : h('div', { class: 'empty', text: 'No data yet' }), sub.length ? legend : null),
        chartCard('Live products by category', entries(a.productsByCategory, true, 8), function (r) { return donut(r, 'products'); }),
        chartCard('Orders per month', entries(a.ordersByMonth, false), function (r) { return columns(r); }),
        chartCard('Sales per month (KES)', entries(a.salesByMonth, false), function (r) { return columns(r, { color: AMBER, short: true }); }),
        chartCard('Recorded harvest by crop (kg)', entries(a.harvestKgByCrop, true, 10), function (r) { return hbars(r); }),
        chartCard('Groups by type', entries(a.groupsByType, true, 0, true), function (r) { return donut(r, 'groups'); }),
        chartCard('Largest groups (members)', entries(a.membersByGroup, true, 8), function (r) { return hbars(r, { color: BLUE }); }),
        chartCard('Information published by type', entries(a.postsByType, true, 0, true), function (r) { return donut(r, 'posts'); })));
    }).catch(function (e) { failed(box, e); });
  }

  // ---------- verification ----------
  var STATUS_LABEL = { PENDING: 'Waiting', UNDER_REVIEW: 'Under review', VERIFIED: 'Verified', REJECTED: 'Rejected' };

  function verifications(box) {
    var current = 'PENDING', list = h('div');
    var select = h('select', { onchange: function () { current = select.value; load(); } },
      Object.keys(STATUS_LABEL).map(function (k) { return h('option', { value: k, text: STATUS_LABEL[k] }); }));
    box.appendChild(h('div', { class: 'toolbar' }, h('div', {}, h('label', { text: 'Show' }), select)));
    box.appendChild(list);
    function load() {
      loading(list);
      getJson('/api/admin/verifications?status=' + encodeURIComponent(current)).then(function (rows) {
        list.textContent = '';
        if (!rows.length) { list.appendChild(emptyBox('Nothing here')); return; }
        list.appendChild(table(['Farmer', 'Sub-county', 'Ward', 'Submitted', 'Status', ''], rows.map(function (r) {
          return h('tr', {},
            h('td', {}, h('b', { text: r.fullName || '(no name)' })), h('td', { text: r.subCounty || '-' }), h('td', { text: r.ward || '-' }),
            h('td', { text: when(r.submittedAt) }), h('td', {}, pill(STATUS_LABEL[r.status] || r.status, 'st-' + r.status)),
            h('td', { class: 'actions' }, h('button', { class: 'btn small', text: 'Open', onclick: function () { openReview(r.id, load); } })));
        })));
      }).catch(function (e) { failed(list, e); });
    }
    load();
  }

  function modalShell() {
    var urls = [];
    var back = h('div', { class: 'modal-back', onclick: function (ev) { if (ev.target === back) close(); } });
    var modal = h('div', { class: 'modal', role: 'dialog', 'aria-modal': 'true' }, h('div', { class: 'loading', text: 'Loading…' }));
    back.appendChild(modal);
    document.body.appendChild(back);
    function close() { urls.forEach(function (u) { URL.revokeObjectURL(u); }); back.remove(); document.removeEventListener('keydown', esc); }
    function esc(ev) { if (ev.key === 'Escape') close(); }
    document.addEventListener('keydown', esc);
    return { modal: modal, close: close, urls: urls };
  }
  function fact(k, v) { return h('div', {}, h('span', { text: k }), document.createTextNode(v == null || v === '' ? '-' : String(v))); }

  function openReview(id, done) {
    var m = modalShell(), modal = m.modal, close = m.close;
    getJson('/api/admin/verifications/' + id).then(function (d) {
      modal.textContent = '';
      var docs = h('div', { class: 'docs' });
      [['ID_FRONT', 'hasIdFront', 'ID – front'], ['ID_BACK', 'hasIdBack', 'ID – back'], ['SELFIE', 'hasSelfie', 'Selfie'], ['PROOF', 'hasProof', 'Proof of farming']]
        .forEach(function (t) {
          if (!d[t[1]]) return;
          var img = h('img', { alt: t[2] });
          docs.appendChild(h('figure', {}, img, h('figcaption', { text: t[2] })));
          api('/api/admin/verifications/' + id + '/documents/' + t[0]).then(function (r) { return r.blob(); })
            .then(function (b) { var u = URL.createObjectURL(b); m.urls.push(u); img.src = u; })
            .catch(function () { img.alt = t[2] + ' could not be loaded'; });
        });
      var acts = h('div', { class: 'actions' }, h('button', { class: 'btn secondary', text: 'Close', onclick: close }));
      function act(path, json, okMsg) {
        return api(path, { method: 'POST', json: json }).then(function () { toast(okMsg); close(); done(); })
          .catch(function (e) { toast(e.message, true); });
      }
      if (d.status === 'PENDING') acts.appendChild(h('button', { class: 'btn', text: 'Start review', onclick: function () { act('/api/admin/verifications/' + id + '/start-review', undefined, 'Review started'); } }));
      if (d.status === 'UNDER_REVIEW') {
        acts.appendChild(h('button', { class: 'btn danger', text: 'Reject…', onclick: function () {
          var reason = window.prompt('Reason shown to the farmer (at least 5 characters):');
          if (reason && reason.trim().length >= 5) act('/api/admin/verifications/' + id + '/reject', { reason: reason.trim() }, 'Rejected. The farmer can fix and resubmit');
          else if (reason !== null) toast('Please give a reason of at least 5 characters', true);
        } }));
        acts.appendChild(h('button', { class: 'btn', text: 'Approve', onclick: function () {
          if (window.confirm('Confirm that the name and ID number match the ID photos, the selfie matches, and the farm proof looks genuine.')) act('/api/admin/verifications/' + id + '/approve', undefined, 'Farmer verified');
        } }));
      }
      modal.appendChild(h('div', {}, h('h2', { text: d.fullName || d.accountName || 'Farmer' }), pill(STATUS_LABEL[d.status] || d.status, 'st-' + d.status)));
      modal.appendChild(h('div', { class: 'facts' },
        fact('Account', (d.accountName || '') + ' · ' + (d.accountEmail || '')), fact('National ID', d.nationalId),
        fact('Phone', d.phone + (d.phoneVerified ? '  ✓ OTP verified' : '  (not verified)')),
        fact('Location', [d.county, d.subCounty, d.ward, d.village].filter(Boolean).join(' · ')),
        fact('Farming', [d.farmingType, d.mainProduce, d.farmSize ? d.farmSize + ' acres' : ''].filter(Boolean).join(' · ')),
        fact('Farm GPS', d.farmLat != null ? d.farmLat + ', ' + d.farmLng : '-'), fact('Submitted', when(d.submittedAt)),
        fact('Reviewed', d.reviewedAt ? when(d.reviewedAt) + ' by ' + (d.reviewedByName || '') : '-'),
        d.rejectionReason ? fact('Rejection reason', d.rejectionReason) : null));
      modal.appendChild(docs);
      if (d.farmLat != null) modal.appendChild(h('a', { href: 'https://www.google.com/maps?q=' + encodeURIComponent(d.farmLat + ',' + d.farmLng), target: '_blank', rel: 'noopener noreferrer', text: 'Open farm location in Google Maps' }));
      modal.appendChild(acts);
    }).catch(function (e) { toast(e.message, true); close(); });
  }

  // ---------- market prices ----------
  function prices(box) {
    var f = {
      crop: h('input', { required: true, maxlength: '60', placeholder: 'Maize' }),
      market: h('input', { required: true, maxlength: '60', placeholder: 'Kitale' }),
      unit: h('input', { required: true, maxlength: '30', value: 'kg' }),
      price: h('input', { required: true, type: 'number', min: '0.01', step: '0.01' }),
      date: h('input', { type: 'date', value: new Date().toISOString().slice(0, 10) })
    };
    var btn = h('button', { class: 'btn', type: 'submit', text: 'Post price' });
    var form = h('form', { class: 'card', onsubmit: function (ev) {
      ev.preventDefault(); btn.disabled = true;
      api('/api/prices', { method: 'POST', json: { crop: f.crop.value.trim(), market: f.market.value.trim(), unit: f.unit.value.trim(), price: Number(f.price.value), date: f.date.value || null } })
        .then(function () { toast('Price posted. Farmers who follow this crop were notified'); f.price.value = ''; load(); })
        .catch(function (e) { toast(e.message, true); })
        .then(function () { btn.disabled = false; });
    } },
      h('h3', { text: 'Post today’s market price' }),
      h('div', { class: 'form-row' },
        h('div', {}, h('label', { text: 'Crop' }), f.crop), h('div', {}, h('label', { text: 'Market' }), f.market),
        h('div', {}, h('label', { text: 'Unit' }), f.unit), h('div', {}, h('label', { text: 'Price (KES)' }), f.price),
        h('div', {}, h('label', { text: 'Date' }), f.date), btn));
    var list = h('div');
    box.appendChild(form);
    box.appendChild(h('div', { class: 'section-title', text: 'Latest prices' }));
    box.appendChild(list);
    function load() {
      getJson('/api/prices').then(function (rows) {
        list.textContent = '';
        if (!rows.length) { list.appendChild(emptyBox('No prices posted yet')); return; }
        list.appendChild(table(['Crop', 'Market', 'Unit', ['Price (KES)', 'num'], ['Change', 'num'], 'Date', 'Posted by', ''], rows.map(function (r) {
          var ch = r.changePct;
          return h('tr', {}, h('td', {}, h('b', { text: r.crop })), h('td', { text: r.market }), h('td', { text: r.unit }),
            h('td', { class: 'num', text: num(r.price) }),
            h('td', { class: 'num ' + (ch == null ? '' : ch > 0 ? 'up' : ch < 0 ? 'down' : ''), text: ch == null ? '-' : (ch > 0 ? '▲ ' : ch < 0 ? '▼ ' : '') + Math.abs(ch) + '%' }),
            h('td', { text: r.date }), h('td', { text: r.postedBy }),
            h('td', { class: 'actions' }, h('button', { class: 'btn small danger', text: 'Delete', onclick: function () {
              if (!window.confirm('Delete this price?')) return;
              api('/api/prices/' + r.id, { method: 'DELETE' }).then(function () { toast('Deleted'); load(); }).catch(function (e) { toast(e.message, true); });
            } })));
        })));
      }).catch(function (e) { failed(list, e); });
    }
    load();
  }

  // ---------- reports ----------
  function reports(box) {
    var stamp = new Date().toISOString().slice(0, 10);
    function dl(title, text, label, path, file, secondary) {
      var b = h('button', { class: 'btn' + (secondary ? ' secondary' : ''), text: label, onclick: function () {
        b.disabled = true; download(path, file).then(function () { b.disabled = false; });
      } });
      return h('div', { class: 'card' }, h('div', { class: 'big' }, icon('reports')), h('h3', { text: title }), h('p', { text: text }), b);
    }
    box.appendChild(h('div', { class: 'reports' },
      dl('County report (PDF)', 'Indicators, charts and the sub-county table, ready to print or send.', 'Download PDF', '/api/analytics/report.pdf', 'farmconnect-report-' + stamp + '.pdf'),
      dl('County report (CSV)', 'All indicators and chart data in one spreadsheet file.', 'Download CSV', '/api/analytics/report.csv', 'farmconnect-report-' + stamp + '.csv', true),
      dl('Farmers list (CSV)', 'Every farmer with phone, sub-county and verification status, for follow-up.', 'Download CSV', '/api/analytics/farmers.csv', 'farmconnect-farmers-' + stamp + '.csv', true),
      dl('Orders list (CSV)', 'Every order with status and payment method.', 'Download CSV', '/api/analytics/orders.csv', 'farmconnect-orders-' + stamp + '.csv', true)));
  }

  // ---------- administration: users ----------
  var ROLE_LABEL = { ADMIN: 'Administrator', OFFICER: 'Officer', FARMER: 'Farmer', BUYER: 'Buyer' };

  function usersView(box, fixedRole) {
    var role = h('select', { onchange: load }, [['', 'Everyone'], ['FARMER', 'Farmers'], ['BUYER', 'Buyers'], ['OFFICER', 'Officers'], ['ADMIN', 'Administrators']]
      .map(function (o) { return h('option', { value: o[0], text: o[1] }); }));
    var q = h('input', { type: 'search', placeholder: 'Search by name or email', oninput: debounce(load, 300) });
    var list = h('div');
    box.appendChild(h('div', { class: 'toolbar' },
      fixedRole ? null : h('div', {}, h('label', { text: 'Show' }), role),
      h('div', { class: 'grow' }, h('label', { text: 'Search' }), q)));
    box.appendChild(list);
    function load() {
      loading(list);
      var r = fixedRole || role.value;
      getJson('/api/admin/users?' + (r ? 'role=' + r + '&' : '') + 'q=' + encodeURIComponent(q.value.trim())).then(function (rows) {
        list.textContent = '';
        if (!rows.length) { list.appendChild(emptyBox('No users match')); return; }
        paged(list, rows, 25, function (u) {
          var locked = u.role === 'ADMIN';
          return h('tr', {},
            h('td', {}, h('b', { text: u.name || '(no name)' }), h('div', { class: 'sub', text: u.email })), h('td', { text: u.phone || '-' }),
            h('td', {}, pill(ROLE_LABEL[u.role] || u.role, 'role-' + u.role)),
            h('td', { text: u.role === 'FARMER' ? (u.verified ? '✓ Verified' : 'Not verified') : '-' }),
            h('td', {}, pill(u.enabled ? 'Active' : 'Suspended', u.enabled ? 'st-ok' : 'st-bad')),
            h('td', { text: day(u.createdAt) }),
            h('td', { class: 'actions' }, locked ? null : h('button', { class: 'btn small ' + (u.enabled ? 'danger' : ''), text: u.enabled ? 'Suspend' : 'Re-activate', onclick: function () {
              var msg = u.enabled ? 'Suspend ' + u.name + '? They will be logged out and blocked until you re-activate them.' : 'Re-activate ' + u.name + '?';
              if (!window.confirm(msg)) return;
              api('/api/admin/users/' + u.id + '/enabled', { method: 'POST', json: { active: !u.enabled } })
                .then(function () { toast(u.enabled ? 'Account suspended' : 'Account re-activated'); load(); })
                .catch(function (e) { toast(e.message, true); });
            } })));
        }, ['User', 'Phone', 'Role', 'Farmer status', 'Account', 'Joined', '']);
      }).catch(function (e) { failed(list, e); });
    }
    load();
  }

  function officersView(box) {
    var f = {
      name: h('input', { required: true, maxlength: '80', autocomplete: 'off' }),
      email: h('input', { required: true, type: 'email', autocomplete: 'off' }),
      phone: h('input', { type: 'tel', placeholder: '07XXXXXXXX', autocomplete: 'off' }),
      password: h('input', { required: true, type: 'password', minlength: '8', autocomplete: 'new-password', placeholder: 'At least 8 characters' })
    };
    var btn = h('button', { class: 'btn', type: 'submit', text: 'Create officer' });
    var form = h('form', { class: 'card', onsubmit: function (ev) {
      ev.preventDefault(); btn.disabled = true;
      api('/api/admin/officers', { method: 'POST', json: { name: f.name.value.trim(), email: f.email.value.trim(), phone: f.phone.value.trim() || null, password: f.password.value } })
        .then(function () { toast('Officer account created'); form.reset(); refresh(); })
        .catch(function (e) { toast(e.message, true); })
        .then(function () { btn.disabled = false; });
    } },
      h('h3', { text: 'Add a county agricultural officer' }),
      h('div', { class: 'form-row' },
        h('div', {}, h('label', { text: 'Full name' }), f.name), h('div', {}, h('label', { text: 'Email' }), f.email),
        h('div', {}, h('label', { text: 'Phone' }), f.phone), h('div', {}, h('label', { text: 'Temporary password' }), f.password), btn));
    var holder = h('div');
    box.appendChild(form);
    box.appendChild(h('div', { class: 'section-title', text: 'Officers' }));
    box.appendChild(holder);
    function refresh() { holder.textContent = ''; usersView(holder, 'OFFICER'); }
    refresh();
  }

  // ---------- administration: products ----------
  function productsView(box) {
    var q = h('input', { type: 'search', placeholder: 'Search products', oninput: debounce(load, 300) });
    var list = h('div');
    box.appendChild(h('div', { class: 'toolbar' }, h('div', { class: 'grow' }, h('label', { text: 'Search' }), q)));
    box.appendChild(list);
    function load() {
      loading(list);
      getJson('/api/admin/products?q=' + encodeURIComponent(q.value.trim())).then(function (rows) {
        list.textContent = '';
        if (!rows.length) { list.appendChild(emptyBox('No products found')); return; }
        paged(list, rows, 25, function (p) {
          return h('tr', {},
            h('td', {}, h('b', { text: p.name }), h('div', { class: 'sub', text: p.category || '' })),
            h('td', {}, document.createTextNode(p.farmName || '-'), h('div', { class: 'sub', text: p.farmerVerified ? '✓ Verified farmer' : 'Not verified' })),
            h('td', { class: 'num', text: kes(p.price) + (p.unit ? ' / ' + p.unit : '') }), h('td', { class: 'num', text: num(p.quantity) }),
            h('td', {}, pill(p.active ? 'Live' : 'Hidden', p.active ? 'st-LIVE' : 'st-HIDDEN')),
            h('td', { class: 'actions' }, h('button', { class: 'btn small ' + (p.active ? 'danger' : ''), text: p.active ? 'Take down' : 'Restore', onclick: function () {
              if (!window.confirm((p.active ? 'Take down ' : 'Restore ') + p.name + '?')) return;
              api('/api/admin/products/' + p.id + '/active', { method: 'POST', json: { active: !p.active } })
                .then(function () { toast(p.active ? 'Product taken down' : 'Product restored'); load(); })
                .catch(function (e) { toast(e.message, true); });
            } })));
        }, ['Product', 'Farm', ['Price', 'num'], ['In stock', 'num'], 'Status', '']);
      }).catch(function (e) { failed(list, e); });
    }
    load();
  }

  // ---------- administration: orders ----------
  var ORDER_STATUSES = ['PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'];

  function ordersView(box) {
    var status = h('select', { onchange: load }, [h('option', { value: '', text: 'All orders' })].concat(
      ORDER_STATUSES.map(function (s) { return h('option', { value: s, text: s.charAt(0) + s.slice(1).toLowerCase() }); })));
    var list = h('div');
    box.appendChild(h('div', { class: 'toolbar' }, h('div', {}, h('label', { text: 'Status' }), status)));
    box.appendChild(list);
    function load() {
      loading(list);
      getJson('/api/admin/orders' + (status.value ? '?status=' + status.value : '')).then(function (rows) {
        list.textContent = '';
        if (!rows.length) { list.appendChild(emptyBox('No orders here')); return; }
        paged(list, rows, 25, function (o) {
          var closed = o.status === 'CANCELLED';
          var move = h('select', { disabled: closed, 'aria-label': 'Move order ' + o.id }, [h('option', { value: '', text: closed ? 'Closed' : 'Move to…' })].concat(
            closed ? [] : ORDER_STATUSES.filter(function (s) { return s !== o.status; }).map(function (s) { return h('option', { value: s, text: s.charAt(0) + s.slice(1).toLowerCase() }); })));
          move.addEventListener('change', function () {
            if (!move.value) return;
            if (!window.confirm('Move order #' + o.id + ' to ' + move.value.toLowerCase() + '? The buyer will be notified.')) { move.value = ''; return; }
            api('/api/admin/orders/' + o.id + '/status', { method: 'PATCH', json: { status: move.value } })
              .then(function () { toast('Order updated'); load(); }).catch(function (e) { toast(e.message, true); move.value = ''; });
          });
          return h('tr', {},
            h('td', {}, h('b', { text: '#' + o.id })), h('td', { text: when(o.createdAt) }),
            h('td', {}, document.createTextNode(o.buyerName || '-'), h('div', { class: 'sub', text: o.buyerPhone || '' })),
            h('td', { class: 'num', text: plural((o.items || []).length, 'item') }), h('td', { class: 'num', text: kes(o.total) }),
            h('td', { text: (o.paymentMethod || '').replace(/_/g, ' ').toLowerCase() }),
            h('td', {}, pill(o.status.charAt(0) + o.status.slice(1).toLowerCase(), 'st-' + o.status)),
            h('td', { class: 'actions' }, h('button', { class: 'btn small secondary', text: 'View', onclick: function () { orderDetail(o); } }), ' ', move));
        }, ['Order', 'Placed', 'Buyer', ['Items', 'num'], ['Total', 'num'], 'Payment', 'Status', '']);
      }).catch(function (e) { failed(list, e); });
    }
    load();
  }

  function orderDetail(o) {
    var m = modalShell(); m.modal.textContent = '';
    m.modal.appendChild(h('div', {}, h('h2', { text: 'Order #' + o.id }), pill(o.status.charAt(0) + o.status.slice(1).toLowerCase(), 'st-' + o.status)));
    m.modal.appendChild(h('div', { class: 'facts' },
      fact('Buyer', (o.buyerName || '') + (o.buyerPhone ? ' · ' + o.buyerPhone : '')), fact('Placed', when(o.createdAt)),
      fact('Deliver to', o.deliveryAddress), fact('Delivery phone', o.deliveryPhone), fact('Payment', (o.paymentMethod || '').replace(/_/g, ' ').toLowerCase()),
      fact('Note to seller', o.deliveryNote)));
    m.modal.appendChild(table(['Product', 'Farm', ['Qty', 'num'], ['Price', 'num'], ['Subtotal', 'num']], (o.items || []).map(function (i) {
      return h('tr', {}, h('td', { text: i.productName }), h('td', { text: i.farmName || '-' }), h('td', { class: 'num', text: num(i.quantity) + (i.unit ? ' ' + i.unit : '') }),
        h('td', { class: 'num', text: kes(i.price) }), h('td', { class: 'num', text: kes(Number(i.price) * i.quantity) }));
    })));
    m.modal.appendChild(h('p', {}, h('b', { text: 'Total: ' + kes(o.total) })));
    m.modal.appendChild(h('div', { class: 'actions' }, h('button', { class: 'btn secondary', text: 'Close', onclick: m.close })));
  }

  // ---------- frame ----------
  var NAV = [
    { group: 'Dashboard', items: [['overview', 'Overview', 'Overview']] },
    { group: 'County work', items: [['verifications', 'Verification', 'Farmer verification'], ['prices', 'Market prices', 'Market price board'], ['reports', 'Reports', 'Reports']] },
    { group: 'Administration', admin: true, items: [['users', 'Users', 'Users'], ['officers', 'Officers', 'County officers'], ['products', 'Products', 'Product moderation'], ['orders', 'Orders', 'Orders']] }
  ];
  var VIEWS = { overview: overview, verifications: verifications, prices: prices, reports: reports, users: function (b) { usersView(b); }, officers: officersView, products: productsView, orders: ordersView };
  var ADMIN_ONLY = { users: 1, officers: 1, products: 1, orders: 1 };

  function currentView() {
    var v = (location.hash || '').replace('#', '');
    if (!VIEWS[v] || (ADMIN_ONLY[v] && !isAdmin())) v = 'overview';
    return v;
  }
  function titleOf(view) {
    var t = 'Overview';
    NAV.forEach(function (g) { g.items.forEach(function (i) { if (i[0] === view) t = i[2]; }); });
    return t;
  }

  function shell() {
    state.view = currentView();
    var frame = h('div', { class: 'frame' + (state.menuOpen ? ' open' : '') });
    var queueBadge = h('span', { class: 'badge hidden', text: '0' });

    var nav = h('nav', { class: 'nav' });
    NAV.forEach(function (g) {
      if (g.admin && !isAdmin()) return;
      nav.appendChild(h('div', { class: 'group', text: g.group }));
      g.items.forEach(function (i) {
        nav.appendChild(h('a', { href: '#' + i[0], class: i[0] === state.view ? 'active' : null, onclick: function () { state.menuOpen = false; frame.classList.remove('open'); } },
          icon(i[0]), i[1], i[0] === 'verifications' ? queueBadge : null));
      });
    });
    var name = state.user ? state.user.name : '';
    var side = h('aside', { class: 'side' },
      h('div', { class: 'brand' }, h('h1', { text: '🌿 FarmConnect' }), h('div', { text: 'Trans Nzoia County' })),
      nav,
      h('div', { class: 'me' }, h('div', { class: 'avatar', text: (name || '?').trim().charAt(0).toUpperCase() }),
        h('div', { class: 'who' }, h('b', { text: name }), h('span', { text: isAdmin() ? 'Administrator' : 'County officer' })),
        h('button', { title: 'Log out', 'aria-label': 'Log out', onclick: logout }, icon('logout'))));

    var bar = h('header', { class: 'bar' },
      h('button', { class: 'menu-btn', 'aria-label': 'Menu', onclick: function () { state.menuOpen = !state.menuOpen; frame.classList.toggle('open', state.menuOpen); } }, icon('menu')),
      h('h2', { text: titleOf(state.view) }), h('div', { class: 'spacer' }),
      state.view === 'overview' ? h('button', { class: 'btn secondary small', text: 'Refresh', onclick: render }) : null,
      state.view === 'overview' ? h('button', { class: 'btn secondary small', text: 'Print', onclick: function () { window.print(); } }) : null,
      h('span', { class: 'date', text: new Date().toLocaleDateString('en-KE', { dateStyle: 'full' }) }));

    var body = h('div');
    frame.appendChild(side);
    frame.appendChild(h('div', { class: 'content' }, bar, h('main', {}, body)));
    VIEWS[state.view](body);

    getJson('/api/admin/verifications?status=PENDING').then(function (rows) {
      if (rows.length) { queueBadge.textContent = rows.length; queueBadge.classList.remove('hidden'); }
    }).catch(function () { /* the badge is optional */ });
    return frame;
  }

  function render() {
    var app = document.getElementById('app');
    app.textContent = '';
    app.appendChild(state.token ? shell() : loginView());
  }

  window.addEventListener('hashchange', function () { if (state.token) render(); });
  render();
})();
