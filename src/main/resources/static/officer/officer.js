/* FarmConnect county dashboard (officers and administrators). Plain JavaScript, no libraries.
   All text goes in through textContent, never innerHTML. */
(function () {
  'use strict';

  var TOKEN_KEY = 'fc_token', USER_KEY = 'fc_user';
  var state = { token: sessionStorage.getItem(TOKEN_KEY), user: null, view: 'overview', menuOpen: false, scope: null, locations: null };
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
    security: ['M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z', 'M12 8v4', 'M12 16h.01'],
    countyadmins: ['M1 6v16l7-4 8 4 7-4V2l-7 4-8-4-7 4z', 'M8 2v16', 'M16 6v16'],
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
    payments: ['M12 1v22', 'M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6'],
    cash: ['M12 1v22', 'M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6'],
    book: ['M4 19.5A2.5 2.5 0 0 1 6.5 17H20', 'M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z'],
    group: ['M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2', 'M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z'],
    marketplace: ['M3 9l9-6 9 6v11a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1z', 'M9 21V12h6v9'],
    supplymoney: ['M12 1v22', 'M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6'],
    commission: ['M19 5L5 19', 'M6.5 9a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5z', 'M17.5 20a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5z'],
    companies: ['M3 21h18', 'M5 21V7l8-4v18', 'M19 21V11l-6-4', 'M9 9v.01', 'M9 13v.01', 'M9 17v.01']
  };
  ICONS.cropdoctor = ICONS.farmer;
  ICONS.system = ICONS.pulse;
  ICONS.cash = ICONS.supplymoney;
  ICONS.delivery = ['M1 3h15v13H1z', 'M16 8h4l3 3v5h-7z', 'M5.5 21a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5z', 'M18.5 21a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5z'];
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
  /** Super admin or county admin: both get the administration tools (a county admin only for their own county). */
  function isAdmin() { return !!state.user && (state.user.role === 'ADMIN' || state.user.role === 'COUNTY_ADMIN'); }
  function isSuper() { return !!state.user && state.user.role === 'ADMIN'; }
  function isCountyAdmin() { return !!state.user && state.user.role === 'COUNTY_ADMIN'; }

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
          if (u.role !== 'ADMIN' && u.role !== 'COUNTY_ADMIN' && u.role !== 'OFFICER') throw new Error('This dashboard is for county officers and administrators.');
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
      h('div', { class: 'login-card' }, h('h1', { text: '🌿 FarmConnect' }), h('p', { text: 'County dashboard · Kenya' }), form));
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

  // ---------- areas: counties and sub-counties ----------
  function getLocations() {
    if (state.locations) return Promise.resolve(state.locations);
    return getJson('/api/locations').then(function (l) { state.locations = l; return l; });
  }
  /** Counties the logged-in admin may assign officers to: all of them, or just a county admin's own. */
  function scopedLocations() {
    return getLocations().then(function (locs) {
      if (!isCountyAdmin()) return locs;
      return getJson('/api/analytics/scope').then(function (sc) {
        var mine = locs.filter(function (c) { return c.name === sc.county; });
        return mine.length ? mine : [];
      });
    });
  }
  function areaText(u) {
    if (!u.assignedCounty) return 'All counties (not assigned yet)';
    var subs = u.assignedSubCounties || [];
    return u.assignedCounty + ' County' + (subs.length ? ': ' + subs.join(', ') : ' (whole county)');
  }

  /** County drop-down plus sub-county tick boxes. get() returns {county, subCounties}. */
  function areaPicker(locs, county, subs) {
    var only = locs.length === 1;   // a county admin sees just their own county
    var sel = h('select', {}, (only ? [] : [h('option', { value: '', text: 'All counties (not restricted)' })]).concat(
      locs.map(function (c) { return h('option', { value: c.name, text: c.name }); })));
    var box = h('div');
    sel.value = county || (only ? locs[0].name : '');
    function draw(chosen) {
      box.textContent = '';
      var c = locs.filter(function (x) { return x.name === sel.value; })[0];
      if (!c) { box.appendChild(h('p', { class: 'muted', text: 'The officer sees every county until you choose one.' })); return; }
      box.appendChild(h('p', { class: 'muted', text: 'Tick the sub-counties this officer covers. Leave all unticked for the whole county.' }));
      box.appendChild(h('div', { class: 'check-grid' }, c.subCounties.map(function (s) {
        var cb = h('input', { type: 'checkbox', value: s });
        if (chosen.indexOf(s) >= 0) cb.checked = true;
        return h('label', { class: 'check' }, cb, ' ' + s);
      })));
    }
    sel.addEventListener('change', function () { draw([]); });
    draw(subs || []);
    return {
      el: h('div', {}, h('label', { text: 'County' }), sel, box),
      get: function () {
        return { county: sel.value || null,
          subCounties: Array.prototype.map.call(box.querySelectorAll('input:checked'), function (i) { return i.value; }) };
      }
    };
  }

  // ---------- overview ----------
  function overview(box) {
    loading(box);
    var jobs = [getJson('/api/analytics')];
    if (isAdmin()) jobs.push(getJson('/api/admin/stats').catch(function () { return null; }));
    Promise.all(jobs).then(function (res) {
      var a = res[0], st = res[1], i = a.indicators || {}, scoped = !!a.scoped;
      box.textContent = '';
      if (scoped) {
        box.appendChild(h('div', { class: 'banner' }, h('b', { text: 'Your area: ' }), a.scopeLabel,
          h('div', { class: 'muted', text: 'Everything on this dashboard, in the reports and in the verification queue is limited to your assigned area.' })));
      }

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
        scoped ? null : kpi('cart', 'purple', 'Registered buyers', num(i.registeredBuyers)),
        kpi('pulse', 'blue', scoped ? 'Active farmers (30 days)' : 'Active users (30 days)', num(i.activeUsers30d)),
        kpi('group', '', 'Groups onboarded', num(i.groupsOnboarded), plural(i.groupMembers, 'member')),
        scoped ? null : kpi('officers', 'amber', 'Officers active (30 days)', num(i.officersActive30d) + ' of ' + num(i.officersTotal))));

      box.appendChild(h('div', { class: 'section-title', text: 'Programme activity' }));
      box.appendChild(h('div', { class: 'kpis' },
        kpi('products', '', 'Live market listings', num(i.marketListings)),
        kpi('chat', 'blue', 'Farmer–buyer connections', num(i.farmerBuyerConnections)),
        scoped ? null : kpi('book', 'purple', 'Information interactions', num(i.informationInteractions)),
        scoped ? null : kpi('star', 'amber', 'Opportunities accessed', num(i.opportunitiesAccessed)),
        scoped ? null : kpi('reports', '', 'Programmes communicated', num(i.programmesCommunicated)),
        kpi('overview', 'blue', 'Farmers using digital records', num(i.farmersUsingRecords), plural(i.recordsTotal, 'record')),
        scoped ? null : kpi('star', 'amber', 'Farmer satisfaction', i.satisfactionResponses ? Number(i.satisfactionAverage).toFixed(1) + ' / 5' : '-', plural(i.satisfactionResponses, 'vote'))));

      box.appendChild(h('div', { class: 'section-title', text: 'Charts' }));
      var sub = entries(a.farmersBySubCounty, true, 12).map(function (r) { return [r[0], r[1], (a.verifiedBySubCounty || {})[r[0]] || 0]; });
      function makeLegend() {
        return h('div', { class: 'legend' },
          h('span', {}, (function () { var d = h('i'); d.style.background = LIGHT; return d; })(), 'Registered'),
          h('span', {}, (function () { var d = h('i'); d.style.background = GREEN; return d; })(), 'Verified'));
      }
      var counties = entries(a.farmersByCounty, true, 13).map(function (r) { return [r[0], r[1], (a.verifiedByCounty || {})[r[0]] || 0]; });
      var countyCard = !scoped && counties.length > 1 ? card('Farmers by county', pairBars(counties), makeLegend()) : null;
      box.appendChild(h('div', { class: 'charts' },
        countyCard,
        card('Farmers by sub-county', sub.length ? pairBars(sub) : h('div', { class: 'empty', text: 'No data yet' }), sub.length ? makeLegend() : null),
        chartCard('Live products by category', entries(a.productsByCategory, true, 8), function (r) { return donut(r, 'products'); }),
        chartCard('Orders per month', entries(a.ordersByMonth, false), function (r) { return columns(r); }),
        chartCard('Sales per month (KES)', entries(a.salesByMonth, false), function (r) { return columns(r, { color: AMBER, short: true }); }),
        chartCard('Recorded harvest by crop (kg)', entries(a.harvestKgByCrop, true, 10), function (r) { return hbars(r); }),
        chartCard('Groups by type', entries(a.groupsByType, true, 0, true), function (r) { return donut(r, 'groups'); }),
        chartCard('Largest groups (members)', entries(a.membersByGroup, true, 8), function (r) { return hbars(r, { color: BLUE }); }),
        scoped ? null : chartCard('Information published by type', entries(a.postsByType, true, 0, true), function (r) { return donut(r, 'posts'); })));
    }).catch(function (e) { failed(box, e); });
  }

  // ---------- verification ----------
  var STATUS_LABEL = { PENDING: 'Waiting', UNDER_REVIEW: 'Under review', VERIFIED: 'Verified', REJECTED: 'Rejected' };

  function verifications(box) {
    var current = 'PENDING', list = h('div');
    var select = h('select', { onchange: function () { current = select.value; load(); } },
      Object.keys(STATUS_LABEL).map(function (k) { return h('option', { value: k, text: STATUS_LABEL[k] }); }));
    box.appendChild(h('div', { class: 'toolbar' }, h('div', {}, h('label', { text: 'Show' }), select),
      state.scope && !state.scope.all ? h('div', { class: 'muted' }, 'Only farmers in ' + state.scope.label + ' are listed.') : null));
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
  /** Confirm / prompt dialogs in the dashboard's own style. Resolve true / false (confirm) or the typed text (prompt). */
  function dialogBox(o) {
    return new Promise(function (resolve) {
      var m = modalShell(), modal = m.modal, input = null;
      modal.textContent = '';
      if (o.input) input = o.multiline ? h('textarea', { rows: 3, maxlength: 500 }) : h('input', { maxlength: 500, value: o.value || '' });
      var err = h('div', { class: 'error' });
      var ok = h('button', { class: 'btn' + (o.danger ? ' danger' : ''), type: 'submit', text: o.ok || 'OK' });
      modal.appendChild(h('form', { onsubmit: function (ev) {
        ev.preventDefault();
        if (!o.input) { m.close(); resolve(true); return; }
        var v = input.value.trim();
        if (v.length < (o.min || 1)) { err.textContent = o.min > 1 ? 'Please enter at least ' + o.min + ' characters' : 'This is required'; return; }
        m.close(); resolve(v);
      } }, h('h2', { text: o.title }), o.message ? h('p', { class: 'muted', text: o.message }) : null,
        o.input ? [h('label', { text: o.label || '' }), input] : null, err,
        h('div', { class: 'actions' }, h('button', { class: 'btn secondary', type: 'button', text: 'Cancel', onclick: function () { m.close(); resolve(o.input ? null : false); } }), ok)));
      setTimeout(function () { (input || ok).focus(); }, 30);
    });
  }
  function confirmDialog(title, message, okLabel, danger) { return dialogBox({ title: title, message: message, ok: okLabel || 'Confirm', danger: danger }); }
  function promptDialog(title, label, opts) { opts = opts || {}; opts.title = title; opts.label = label; opts.input = true; return dialogBox(opts); }

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
  function emailCard() {
    var b = h('button', { class: 'btn secondary', text: 'Email it to me now', onclick: function () {
      b.disabled = true;
      api('/api/reports/email-me', { method: 'POST', json: {} }).then(function (r) { return r.json(); })
        .then(function (r) { toast(r.message); })
        .catch(function (e) { toast(e.message, true); })
        .then(function () { b.disabled = false; });
    } });
    return h('div', { class: 'card' }, h('div', { class: 'big' }, icon('reports')), h('h3', { text: 'Weekly report by email' }),
      h('p', { text: 'The PDF and Excel report for your area is emailed to you every Monday at 7:00. Send the latest one now.' }), b);
  }
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
      dl('County report (Excel)', 'The same report as an Excel workbook with a sheet for each table.', 'Download Excel', '/api/reports/county.xlsx', 'farmconnect-report-' + stamp + '.xlsx'),
      emailCard(),
      dl('County report (CSV)', 'All indicators and chart data in one spreadsheet file.', 'Download CSV', '/api/analytics/report.csv', 'farmconnect-report-' + stamp + '.csv', true),
      dl('Farmers list (CSV)', 'Every farmer with phone, sub-county and verification status, for follow-up.', 'Download CSV', '/api/analytics/farmers.csv', 'farmconnect-farmers-' + stamp + '.csv', true),
      dl('Orders list (CSV)', 'Every order with status and payment method.', 'Download CSV', '/api/analytics/orders.csv', 'farmconnect-orders-' + stamp + '.csv', true)));
  }

  // ---------- administration: users ----------
  var ROLE_LABEL = { ADMIN: 'Super admin', COUNTY_ADMIN: 'County admin', OFFICER: 'Officer', FARMER: 'Farmer', BUYER: 'Buyer' };

  function usersView(box, fixedRole) {
    var role = h('select', { onchange: load }, [['', 'Everyone'], ['FARMER', 'Farmers'], ['BUYER', 'Buyers'], ['OFFICER', 'Officers'], ['COUNTY_ADMIN', 'County admins'], ['ADMIN', 'Super admins']]
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
          var locked = u.role === 'ADMIN' || (u.role === 'COUNTY_ADMIN' && isCountyAdmin());
          return h('tr', {},
            h('td', {}, h('b', { text: u.name || '(no name)' }), h('div', { class: 'sub', text: u.email })), h('td', { text: u.phone || '-' }),
            h('td', {}, pill(ROLE_LABEL[u.role] || u.role, 'role-' + u.role), u.role === 'OFFICER' ? h('div', { class: 'sub', text: areaText(u) }) : null,
              u.role === 'COUNTY_ADMIN' ? h('div', { class: 'sub', text: (u.assignedCounty || 'No county yet') + (u.assignedCounty ? ' County' : '') }) : null),
            h('td', { text: u.role === 'FARMER' ? (u.verified ? '✓ Verified' : 'Not verified') : '-' }),
            h('td', {}, pill(u.enabled ? 'Active' : 'Suspended', u.enabled ? 'st-ok' : 'st-bad')),
            h('td', { text: day(u.createdAt) }),
            h('td', { class: 'actions' }, isSuper() && u.role !== 'ADMIN' ? [h('button', { class: 'btn small secondary', text: 'Sign out', title: 'End all their sessions', onclick: function () { forceLogout(u); } }), ' ',
              h('button', { class: 'btn small secondary', text: 'New password', onclick: function () { resetUserPassword(u); } }), ' '] : null,
              locked ? null : h('button', { class: 'btn small ' + (u.enabled ? 'danger' : ''), text: u.enabled ? 'Suspend' : 'Re-activate', onclick: function () {
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
    loading(box);
    scopedLocations().then(function (locs) {
      box.textContent = '';
      var f = {
        name: h('input', { required: true, maxlength: '80', autocomplete: 'off' }),
        email: h('input', { required: true, type: 'email', autocomplete: 'off' }),
        phone: h('input', { type: 'tel', placeholder: '07XXXXXXXX', autocomplete: 'off' }),
        password: h('input', { required: true, type: 'password', minlength: '8', autocomplete: 'new-password', placeholder: 'At least 8 characters' })
      };
      var picker = areaPicker(locs, '', []);
      var btn = h('button', { class: 'btn', type: 'submit', text: 'Create officer' });
      var form = h('form', { class: 'card', onsubmit: function (ev) {
        ev.preventDefault(); btn.disabled = true;
        var area = picker.get();
        api('/api/admin/officers', { method: 'POST', json: { name: f.name.value.trim(), email: f.email.value.trim(), phone: f.phone.value.trim() || null,
            password: f.password.value, county: area.county, subCounties: area.subCounties } })
          .then(function () { toast('Officer account created'); officersView(box); })
          .catch(function (e) { toast(e.message, true); })
          .then(function () { btn.disabled = false; });
      } },
        h('h3', { text: 'Add a county agricultural officer' }),
        h('div', { class: 'form-row' },
          h('div', {}, h('label', { text: 'Full name' }), f.name), h('div', {}, h('label', { text: 'Email' }), f.email),
          h('div', {}, h('label', { text: 'Phone' }), f.phone), h('div', {}, h('label', { text: 'Temporary password' }), f.password)),
        h('div', { class: 'area-box' }, picker.el), h('div', { class: 'actions-row' }, btn));
      var holder = h('div');
      box.appendChild(form);
      box.appendChild(h('div', { class: 'section-title', text: 'Officers and the areas they cover' }));
      box.appendChild(holder);
      loadOfficers(holder, locs);
    }).catch(function (e) { failed(box, e); });
  }

  function loadOfficers(holder, locs) {
    loading(holder);
    getJson('/api/admin/users?role=OFFICER&q=').then(function (rows) {
      holder.textContent = '';
      if (!rows.length) { holder.appendChild(emptyBox('No officers yet')); return; }
      holder.appendChild(table(['Officer', 'Phone', 'Area', 'Account', ''], rows.map(function (u) {
        return h('tr', {},
          h('td', {}, h('b', { text: u.name }), h('div', { class: 'sub', text: u.email })), h('td', { text: u.phone || '-' }),
          h('td', {}, h('span', { class: u.assignedCounty ? '' : 'warn', text: areaText(u) })),
          h('td', {}, pill(u.enabled ? 'Active' : 'Suspended', u.enabled ? 'st-ok' : 'st-bad')),
          h('td', { class: 'actions' },
            h('button', { class: 'btn small secondary', text: 'Assign area', onclick: function () { assignArea(u, locs, function () { loadOfficers(holder, locs); }); } }), ' ',
            h('button', { class: 'btn small ' + (u.enabled ? 'danger' : ''), text: u.enabled ? 'Suspend' : 'Re-activate', onclick: function () {
              if (!window.confirm((u.enabled ? 'Suspend ' : 'Re-activate ') + u.name + '?')) return;
              api('/api/admin/users/' + u.id + '/enabled', { method: 'POST', json: { active: !u.enabled } })
                .then(function () { toast(u.enabled ? 'Account suspended' : 'Account re-activated'); loadOfficers(holder, locs); })
                .catch(function (e) { toast(e.message, true); });
            } })));
      })));
    }).catch(function (e) { failed(holder, e); });
  }

  function assignArea(u, locs, done) {
    var m = modalShell(); m.modal.textContent = '';
    var picker = areaPicker(locs, u.assignedCounty || '', u.assignedSubCounties || []);
    var save = h('button', { class: 'btn', text: 'Save area', onclick: function () {
      var area = picker.get(); save.disabled = true;
      api('/api/admin/officers/' + u.id + '/assignment', { method: 'PUT', json: area })
        .then(function () { toast('Area saved for ' + u.name); m.close(); done(); })
        .catch(function (e) { toast(e.message, true); save.disabled = false; });
    } });
    m.modal.appendChild(h('h2', { text: 'Area for ' + u.name }));
    m.modal.appendChild(h('p', { class: 'muted', text: 'The officer will only see farmers, verifications, orders and reports for this area.' }));
    m.modal.appendChild(picker.el);
    m.modal.appendChild(h('div', { class: 'actions' }, h('button', { class: 'btn secondary', text: 'Cancel', onclick: m.close }), save));
  }

  // ---------- security ----------
  function forceLogout(u) {
    if (!window.confirm('Sign ' + u.name + ' out of every device? They will have to log in again.')) return;
    api('/api/admin/security/users/' + u.id + '/force-logout', { method: 'POST', json: {} })
      .then(function () { toast(u.name + ' was signed out everywhere'); })
      .catch(function (e) { toast(e.message, true); });
  }
  function resetUserPassword(u) {
    if (!window.confirm('Give ' + u.name + ' a new temporary password? Their old password stops working and they are signed out everywhere.')) return;
    api('/api/admin/security/users/' + u.id + '/reset-password', { method: 'POST', json: {} })
      .then(function (r) { return r.json(); })
      .then(function (r) {
        var m = modalShell(); m.modal.textContent = '';
        m.modal.appendChild(h('h2', { text: 'Temporary password' }));
        m.modal.appendChild(h('p', { class: 'muted', text: 'Give this to ' + r.email + ' privately. It is shown only once. Ask them to change it after they log in.' }));
        m.modal.appendChild(h('div', { class: 'card' }, h('b', { text: r.temporaryPassword })));
        m.modal.appendChild(h('div', { class: 'actions' }, h('button', { class: 'btn', text: 'Done', onclick: m.close })));
      })
      .catch(function (e) { toast(e.message, true); });
  }
  function changePasswordModal() {
    var m = modalShell(); m.modal.textContent = '';
    var cur = h('input', { type: 'password', autocomplete: 'current-password' });
    var nw = h('input', { type: 'password', autocomplete: 'new-password', placeholder: 'At least 8 characters, a letter and a number' });
    var save = h('button', { class: 'btn', text: 'Change password', onclick: function () {
      save.disabled = true;
      api('/api/auth/change-password', { method: 'POST', json: { currentPassword: cur.value, newPassword: nw.value } })
        .then(function (r) { return r.json(); })
        .then(function (u) {
          state.token = u.token; sessionStorage.setItem(TOKEN_KEY, u.token);   // the old token was just ended
          toast('Password changed. Other devices were signed out.'); m.close();
        })
        .catch(function (e) { toast(e.message, true); save.disabled = false; });
    } });
    m.modal.appendChild(h('h2', { text: 'Change my password' }));
    m.modal.appendChild(h('label', { text: 'Current password' })); m.modal.appendChild(cur);
    m.modal.appendChild(h('label', { text: 'New password' })); m.modal.appendChild(nw);
    m.modal.appendChild(h('div', { class: 'actions' }, h('button', { class: 'btn secondary', text: 'Cancel', onclick: m.close }), save));
  }

  var ACTION_LABEL = { LOGIN_OK: 'Signed in', LOGIN_FAILED: 'Wrong password', LOGIN_BLOCKED: 'Blocked (locked out)', LOGIN_REFUSED: 'Suspended account tried to sign in',
    ACCESS_DENIED: 'Access denied', STAFF_ACTION: 'Staff action', STAFF_ACTION_FAILED: 'Staff action failed', PASSWORD_RESET: 'Password reset (emailed code)',
    PASSWORD_CHANGED: 'Password changed', PASSWORD_CHANGE_FAILED: 'Password change failed', FORCE_LOGOUT: 'Signed out everywhere', ADMIN_PASSWORD_RESET: 'Temporary password issued', UNLOCK: 'Lockout cleared' };

  function securityView(box) {
    loading(box);
    Promise.all([getJson('/api/admin/security/summary'), getJson('/api/admin/security/locks')]).then(function (res) {
      var sum = res[0], locks = res[1];
      box.textContent = '';
      if (sum.defaultAdminPassword) box.appendChild(h('div', { class: 'card warn', text: '⚠ A super admin account still uses the default password. Change it now with "Change password" at the top.' }));
      box.appendChild(h('div', { class: 'kpis' },
        kpi('security', 'amber', 'Wrong passwords, 24h', num(sum.failedLogins24h)),
        kpi('security', 'amber', 'Blocked attempts, 24h', num(sum.blocked24h)),
        kpi('security', 'purple', 'Access denied, 24h', num(sum.denied24h)),
        kpi('users', '', 'Admin sign-ins, 24h', num(sum.adminLogins24h)),
        kpi('security', 'blue', 'Active lockouts', num(sum.activeLocks))));

      box.appendChild(h('div', { class: 'section-title', text: 'Active lockouts' }));
      if (!locks.length) box.appendChild(emptyBox('Nobody is locked out'));
      else box.appendChild(table(['Who', 'Type', 'Locked until', ''], locks.map(function (l) {
        var isIp = l.kind === 'ADDRESS';
        var subject = l.subject.split(' from ')[0];
        return h('tr', {}, h('td', { text: l.subject }), h('td', { text: l.kind === 'ADDRESS' ? 'Address' : 'Account' }), h('td', { text: when(l.until) }),
          h('td', { class: 'actions' }, h('button', { class: 'btn small', text: 'Unlock', onclick: function () {
            api('/api/admin/security/locks/unlock', { method: 'POST', json: isIp ? { ip: subject } : { email: subject } })
              .then(function () { toast('Unlocked'); securityView(box); }).catch(function (e) { toast(e.message, true); });
          } })));
      })));

      box.appendChild(h('div', { class: 'section-title', text: 'Audit log' }));
      var kind = h('select', { onchange: loadAudit }, [['', 'Everything'], ['LOGIN_FAILED', 'Wrong passwords'], ['LOGIN_BLOCKED', 'Blocked'], ['ACCESS_DENIED', 'Access denied'],
        ['LOGIN_OK', 'Sign-ins'], ['STAFF_ACTION', 'Staff actions'], ['ADMIN_PASSWORD_RESET', 'Password resets by you'], ['FORCE_LOGOUT', 'Forced sign-outs']]
        .map(function (o) { return h('option', { value: o[0], text: o[1] }); }));
      var q = h('input', { type: 'search', placeholder: 'Search by email, address or what was done', oninput: debounce(loadAudit, 300) });
      var list = h('div');
      box.appendChild(h('div', { class: 'toolbar' }, h('div', {}, h('label', { text: 'Show' }), kind), h('div', { class: 'grow' }, h('label', { text: 'Search' }), q)));
      box.appendChild(list);
      function loadAudit() {
        loading(list);
        getJson('/api/admin/security/audit?limit=300&action=' + encodeURIComponent(kind.value) + '&q=' + encodeURIComponent(q.value.trim())).then(function (rows) {
          list.textContent = '';
          if (!rows.length) { list.appendChild(emptyBox('Nothing logged yet')); return; }
          paged(list, rows, 25, function (a) {
            return h('tr', {}, h('td', { text: when(a.at) }),
              h('td', {}, h('b', { text: a.actorEmail || '(not signed in)' }), a.actorRole ? h('div', { class: 'sub', text: ROLE_LABEL[a.actorRole] || a.actorRole }) : null),
              h('td', {}, pill(ACTION_LABEL[a.action] || a.action, a.success ? 'st-ok' : 'st-bad')),
              h('td', {}, h('div', { text: clip(a.target || '', 70) }), a.detail ? h('div', { class: 'sub', text: clip(a.detail, 90) }) : null),
              h('td', { text: a.ip || '-' }));
          }, ['When', 'Who', 'What', 'Details', 'Address']);
        }).catch(function (e) { failed(list, e); });
      }
      loadAudit();
    }).catch(function (e) { failed(box, e); });
  }

  // ---------- super admin: county admins ----------
  function countyAdminsView(box) {
    loading(box);
    getLocations().then(function (locs) {
      box.textContent = '';
      function countySelect(value) {
        var sel = h('select', { required: true }, [h('option', { value: '', text: 'Choose a county' })].concat(
          locs.map(function (c) { return h('option', { value: c.name, text: c.name }); })));
        sel.value = value || '';
        return sel;
      }
      var f = {
        name: h('input', { required: true, maxlength: '80', autocomplete: 'off' }),
        email: h('input', { required: true, type: 'email', autocomplete: 'off' }),
        phone: h('input', { type: 'tel', placeholder: '07XXXXXXXX', autocomplete: 'off' }),
        password: h('input', { required: true, type: 'password', minlength: '8', autocomplete: 'new-password', placeholder: 'At least 8 characters' }),
        county: countySelect('')
      };
      var btn = h('button', { class: 'btn', type: 'submit', text: 'Create county admin' });
      var form = h('form', { class: 'card', onsubmit: function (ev) {
        ev.preventDefault(); btn.disabled = true;
        api('/api/admin/county-admins', { method: 'POST', json: { name: f.name.value.trim(), email: f.email.value.trim(),
            phone: f.phone.value.trim() || null, password: f.password.value, county: f.county.value } })
          .then(function () { toast('County admin created'); countyAdminsView(box); })
          .catch(function (e) { toast(e.message, true); })
          .then(function () { btn.disabled = false; });
      } },
        h('h3', { text: 'Appoint a county admin' }),
        h('p', { class: 'muted', text: 'A county admin manages one county only: its farmers, orders, products, prices, posts and officers. They cannot create other admins.' }),
        h('div', { class: 'form-row' },
          h('div', {}, h('label', { text: 'Full name' }), f.name), h('div', {}, h('label', { text: 'Email' }), f.email),
          h('div', {}, h('label', { text: 'Phone' }), f.phone), h('div', {}, h('label', { text: 'Temporary password' }), f.password),
          h('div', {}, h('label', { text: 'County they manage' }), f.county)),
        h('div', { class: 'actions-row' }, btn));
      var holder = h('div');
      box.appendChild(form);
      box.appendChild(h('div', { class: 'section-title', text: 'County admins' }));
      box.appendChild(holder);
      loadCountyAdmins(holder, locs, countySelect);
    }).catch(function (e) { failed(box, e); });
  }

  function loadCountyAdmins(holder, locs, countySelect) {
    loading(holder);
    getJson('/api/admin/county-admins').then(function (rows) {
      holder.textContent = '';
      if (!rows.length) { holder.appendChild(emptyBox('No county admins yet')); return; }
      holder.appendChild(table(['County admin', 'Phone', 'County', 'Account', ''], rows.map(function (u) {
        return h('tr', {},
          h('td', {}, h('b', { text: u.name }), h('div', { class: 'sub', text: u.email })), h('td', { text: u.phone || '-' }),
          h('td', {}, h('span', { class: u.assignedCounty ? '' : 'warn', text: u.assignedCounty ? u.assignedCounty + ' County' : 'No county. They see nothing yet.' })),
          h('td', {}, pill(u.enabled ? 'Active' : 'Suspended', u.enabled ? 'st-ok' : 'st-bad')),
          h('td', { class: 'actions' },
            h('button', { class: 'btn small secondary', text: 'Change county', onclick: function () { changeCounty(u, countySelect, function () { loadCountyAdmins(holder, locs, countySelect); }); } }), ' ',
            h('button', { class: 'btn small ' + (u.enabled ? 'danger' : ''), text: u.enabled ? 'Suspend' : 'Re-activate', onclick: function () {
              if (!window.confirm((u.enabled ? 'Suspend ' : 'Re-activate ') + u.name + '?')) return;
              api('/api/admin/users/' + u.id + '/enabled', { method: 'POST', json: { active: !u.enabled } })
                .then(function () { toast(u.enabled ? 'Account suspended' : 'Account re-activated'); loadCountyAdmins(holder, locs, countySelect); })
                .catch(function (e) { toast(e.message, true); });
            } })));
      })));
    }).catch(function (e) { failed(holder, e); });
  }

  function changeCounty(u, countySelect, done) {
    var m = modalShell(); m.modal.textContent = '';
    var sel = countySelect(u.assignedCounty || '');
    var save = h('button', { class: 'btn', text: 'Save county', onclick: function () {
      if (!sel.value) { toast('Choose a county', true); return; }
      save.disabled = true;
      api('/api/admin/county-admins/' + u.id + '/county', { method: 'PUT', json: { county: sel.value } })
        .then(function () { toast(u.name + ' now manages ' + sel.value); m.close(); done(); })
        .catch(function (e) { toast(e.message, true); save.disabled = false; });
    } });
    m.modal.appendChild(h('h2', { text: 'County for ' + u.name }));
    m.modal.appendChild(h('p', { class: 'muted', text: 'They will only see and manage this county.' }));
    m.modal.appendChild(h('label', { text: 'County' }));
    m.modal.appendChild(sel);
    m.modal.appendChild(h('div', { class: 'actions' }, h('button', { class: 'btn secondary', text: 'Cancel', onclick: m.close }), save));
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
            h('td', {}, document.createTextNode((o.paymentMethod || '').replace(/_/g, ' ').toLowerCase()),
              o.paymentMethod === 'MPESA_ONLINE' ? h('div', { class: 'sub', text: String(o.paymentStatus || 'UNPAID').toLowerCase() }) : null),
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
      fact('Deliver to', o.deliveryAddress), fact('Delivery phone', o.deliveryPhone), fact('Payment', (o.paymentMethod || '').replace(/_/g, ' ').toLowerCase() + (' · ' + String(o.paymentStatus || 'UNPAID').toLowerCase() + (o.mpesaReceipt ? ' (' + o.mpesaReceipt + ')' : ''))),
      fact('Note to seller', o.deliveryNote)));
    m.modal.appendChild(table(['Product', 'Farm', ['Qty', 'num'], ['Price', 'num'], ['Subtotal', 'num']], (o.items || []).map(function (i) {
      return h('tr', {}, h('td', { text: i.productName }), h('td', { text: i.farmName || '-' }), h('td', { class: 'num', text: num(i.quantity) + (i.unit ? ' ' + i.unit : '') }),
        h('td', { class: 'num', text: kes(i.price) }), h('td', { class: 'num', text: kes(Number(i.price) * i.quantity) }));
    })));
    m.modal.appendChild(h('p', {}, h('b', { text: 'Total: ' + kes(o.total) })));
    m.modal.appendChild(h('div', { class: 'actions' }, h('button', { class: 'btn secondary', text: 'Close', onclick: m.close })));
  }

  // ---------- administration: payments and payouts ----------
  function money(v) { return 'KES ' + Number(v || 0).toLocaleString('en-KE', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  function sp(status) { return pill(String(status || '-').replace(/_/g, ' ').toLowerCase(), 'st-' + status); }
  function ask(msg, def) { return window.prompt(msg, def || ''); }
  function abtn(text, fn, danger) { return h('button', { class: 'btn small ' + (danger ? 'danger' : 'secondary'), text: text, onclick: fn }); }
  function post(path, body, done, okMsg) {
    return api(path, { method: 'POST', json: body || {} })
      .then(function () { toast(okMsg || 'Done'); if (done) done(); })
      .catch(function (e) { toast(e.message, true); });
  }
  var PAY_BASE = '/api/admin/payments';

  function paymentsView(box) {
    var TABS = [['attention', 'Needs attention'], ['payments', 'All payments'], ['payouts', 'Payouts'], ['accounts', 'Payout accounts'], ['recon', 'Reconciliation']];
    var current = 'attention';
    if (isCountyAdmin()) {      // read-only view of their own county: money-changing screens stay with the super admin
      TABS = [['payments', 'Payments in my county'], ['payouts', 'Payouts']];
      current = 'payments';
    }
    var summaryBox = h('div'), tabBar = h('div', { class: 'toolbar' }), body = h('div');
    box.appendChild(summaryBox); box.appendChild(tabBar); box.appendChild(body);

    function reload() { drawSummary(); drawTab(); }

    function drawSummary() {
      getJson(PAY_BASE + '/summary').then(function (s) {
        summaryBox.textContent = '';
        var attention = Number(s.needsReview || 0) + Number(s.payoutsUnknown || 0) + Number(s.payoutsFailed || 0);
        summaryBox.appendChild(h('div', { class: 'kpis' },
          kpi('cash', 'amber', 'Collected from buyers', money(s.collectedKes)),
          kpi('farmer', '', 'Farmers', money(s.farmer.settledKes), money(s.farmer.owedKes) + ' still to pay'),
          kpi('cash', 'blue', 'County government', money(s.county.settledKes), money(s.county.owedKes) + ' still to pay'),
          kpi('cash', 'purple', 'FarmConnect', money(s.platform.settledKes), money(s.platform.owedKes) + ' not yet settled'),
          kpi('pulse', attention ? 'amber' : '', 'Needs attention', num(attention),
            num(s.needsReview) + ' to review · ' + num(s.payoutsUnknown) + ' unknown · ' + num(s.payoutsFailed) + ' failed')));
      }).catch(function (e) { failed(summaryBox, e); });
    }

    function drawTabs() {
      tabBar.textContent = '';
      TABS.forEach(function (t) {
        tabBar.appendChild(h('button', { class: 'btn small' + (t[0] === current ? '' : ' secondary'), text: t[1],
          onclick: function () { current = t[0]; drawTabs(); drawTab(); } }));
      });
    }

    function drawTab() {
      body.textContent = '';
      ({ attention: attentionTab, payments: paymentsTab, payouts: payoutsTab, accounts: accountsTab, recon: reconTab })[current](body, reload);
    }

    drawTabs();
    reload();
  }

  // ----- tables -----
  function paymentRow(p, reload) {
    var needsReceipt = !p.receipt;
    var acts = h('td', { class: 'actions' }, abtn('View', function () { paymentDetail(p.id, reload); }));
    if ((p.status === 'NEEDS_REVIEW' || p.status === 'PENDING') && needsReceipt) {
      acts.appendChild(document.createTextNode(' '));
      acts.appendChild(abtn('Confirm receipt', function () {
        var receipt = ask('M-Pesa receipt number (from the statement):'); if (!receipt) return;
        var amt = ask('Amount received (KES):'); if (!amt) return;
        post(PAY_BASE + '/' + p.id + '/confirm', { receipt: receipt.trim(), amountKes: Math.round(Number(amt)) }, reload, 'Payment confirmed and split');
      }));
    }
    if (Number(p.owedToBuyerKes) >= 1) {
      acts.appendChild(document.createTextNode(' '));
      acts.appendChild(abtn('Refund ' + money(p.owedToBuyerKes), function () {
        var phone = ask('Send the refund to which M-Pesa number? (leave empty to use the number that paid)');
        if (phone === null) return;
        if (!window.confirm('Refund ' + money(p.owedToBuyerKes) + ' for payment #' + p.id + '?')) return;
        post(PAY_BASE + '/' + p.id + '/refund-excess', { phone: phone || null }, reload, 'Refund queued');
      }, true));
    }
    return h('tr', {},
      h('td', {}, h('b', { text: '#' + p.id })), h('td', { text: 'Order #' + p.orderId }),
      h('td', { text: p.receipt || '-' }), h('td', { class: 'num', text: money(p.amountKes) }),
      h('td', {}, sp(p.status), p.note ? h('div', { class: 'sub', text: clip(p.note, 80) }) : null),
      h('td', { text: when(p.createdAt) }), acts);
  }
  var PAYMENT_HEADS = ['Payment', 'Order', 'M-Pesa receipt', ['Amount', 'num'], 'Status', 'When', ''];

  function payoutRow(po, reload) {
    var acts = h('td', { class: 'actions' });
    if (po.status === 'UNKNOWN' || po.status === 'PROCESSING' || po.status === 'FAILED') {
      acts.appendChild(abtn('Mark paid', function () {
        var ref = ask('M-Pesa or bank reference of the money that was sent:'); if (!ref) return;
        post(PAY_BASE + '/payouts/' + po.id + '/mark-paid', { reference: ref.trim() }, reload, 'Payout marked as paid');
      }));
    }
    if (po.status === 'UNKNOWN' || po.status === 'FAILED') {
      acts.appendChild(document.createTextNode(' '));
      acts.appendChild(abtn('Retry', function () {
        if (po.status === 'UNKNOWN' && !window.confirm('Check the M-Pesa statement first.\n\nPress OK ONLY if NO money was sent for this payout, otherwise the farmer or county would be paid twice.')) return;
        post(PAY_BASE + '/payouts/' + po.id + '/retry', { confirmNotSent: po.status === 'UNKNOWN' }, reload, 'Payout queued again');
      }, po.status === 'UNKNOWN'));
    }
    return h('tr', {},
      h('td', {}, h('b', { text: '#' + po.id }), h('div', { class: 'sub', text: po.kind === 'REFUND' ? 'refund' : 'payment #' + po.paymentId })),
      h('td', {}, document.createTextNode(String(po.beneficiaryType).toLowerCase() + ' ' + (po.beneficiaryType === 'FARMER' || po.beneficiaryType === 'BUYER' ? '#' + po.beneficiaryId : po.beneficiaryId)),
        h('div', { class: 'sub', text: String(po.accountType).replace(/_/g, ' ').toLowerCase() + ' ' + (po.destination || '') })),
      h('td', { class: 'num', text: money(po.amountKes) }),
      h('td', {}, sp(po.status), po.failureReason ? h('div', { class: 'sub', text: clip(po.failureReason, 90) }) : null),
      h('td', { class: 'num', text: po.attempts }), acts);
  }
  var PAYOUT_HEADS = ['Payout', 'To', ['Amount', 'num'], 'Status', ['Tries', 'num'], ''];

  function accountRow(a, reload) {
    var acts = h('td', { class: 'actions' });
    if (a.status !== 'VERIFIED') acts.appendChild(abtn('Approve', function () { post(PAY_BASE + '/accounts/' + a.id + '/approve', {}, reload, 'Account approved'); }));
    if (a.status !== 'REJECTED') {
      acts.appendChild(document.createTextNode(' '));
      acts.appendChild(abtn('Reject', function () { post(PAY_BASE + '/accounts/' + a.id + '/reject', {}, reload, 'Account rejected'); }, true));
    }
    return h('tr', {},
      h('td', { text: String(a.beneficiaryType).toLowerCase() + ' ' + a.beneficiaryId }),
      h('td', { text: String(a.accountType).replace(/_/g, ' ').toLowerCase() }),
      h('td', { text: (a.reference || '-') + (a.accountNumber ? ' / ' + a.accountNumber : '') }),
      h('td', {}, sp(a.status)), acts);
  }
  var ACCOUNT_HEADS = ['Belongs to', 'Type', 'Number / reference', 'Status', ''];

  // ----- tabs -----
  function attentionTab(box, reload) {
    loading(box);
    Promise.all([getJson(PAY_BASE + '?status=NEEDS_REVIEW'), getJson(PAY_BASE + '/payouts?status=UNKNOWN'),
      getJson(PAY_BASE + '/payouts?status=FAILED'), getJson(PAY_BASE + '/accounts?status=PENDING'),
      getJson(PAY_BASE + '?status=SUCCESS')]).then(function (r) {
      box.textContent = '';
      var owed = r[4].filter(function (p) { return Number(p.owedToBuyerKes) >= 1; });
      var any = false;
      function section(title, rows, rowFor, heads) {
        if (!rows.length) return; any = true;
        box.appendChild(h('div', { class: 'section-title', text: title + ' (' + rows.length + ')' }));
        box.appendChild(table(heads, rows.map(rowFor)));
      }
      section('Payments to review', r[0], function (p) { return paymentRow(p, reload); }, PAYMENT_HEADS);
      section('Money owed back to buyers', owed, function (p) { return paymentRow(p, reload); }, PAYMENT_HEADS);
      section('Payouts with an unknown result: check the M-Pesa statement', r[1], function (p) { return payoutRow(p, reload); }, PAYOUT_HEADS);
      section('Failed payouts', r[2], function (p) { return payoutRow(p, reload); }, PAYOUT_HEADS);
      section('Payout numbers waiting for approval', r[3], function (a) { return accountRow(a, reload); }, ACCOUNT_HEADS);
      if (!any) box.appendChild(emptyBox('Nothing needs attention right now.'));
    }).catch(function (e) { failed(box, e); });
  }

  function listTab(box, reload, statuses, path, rowFor, heads, empty) {
    var sel = h('select', { onchange: load }, [h('option', { value: '', text: 'All' })].concat(statuses.map(function (s) {
      return h('option', { value: s, text: s.replace(/_/g, ' ').toLowerCase() }); })));
    var list = h('div');
    box.appendChild(h('div', { class: 'toolbar' }, h('div', {}, h('label', { text: 'Status' }), sel)));
    box.appendChild(list);
    function load() {
      loading(list);
      getJson(path + (sel.value ? (path.indexOf('?') < 0 ? '?' : '&') + 'status=' + sel.value : '')).then(function (rows) {
        list.textContent = '';
        if (!rows.length) { list.appendChild(emptyBox(empty)); return; }
        paged(list, rows, 25, function (x) { return rowFor(x, reload); }, heads);
      }).catch(function (e) { failed(list, e); });
    }
    load();
  }
  function paymentsTab(box, reload) {
    listTab(box, reload, ['PENDING', 'SUCCESS', 'FAILED', 'NEEDS_REVIEW', 'REVERSED'], PAY_BASE, paymentRow, PAYMENT_HEADS, 'No payments yet');
  }
  function payoutsTab(box, reload) {
    listTab(box, reload, ['QUEUED', 'PROCESSING', 'SUCCESS', 'FAILED', 'UNKNOWN', 'CANCELLED'], PAY_BASE + '/payouts', payoutRow, PAYOUT_HEADS, 'No payouts yet');
  }
  function accountsTab(box, reload) {
    var sel = h('select', { onchange: load }, ['PENDING', 'VERIFIED', 'REJECTED'].map(function (s) { return h('option', { value: s, text: s.toLowerCase() }); }));
    var list = h('div');
    box.appendChild(h('div', { class: 'toolbar' }, h('div', {}, h('label', { text: 'Status' }), sel)));
    box.appendChild(list);
    function load() {
      loading(list);
      getJson(PAY_BASE + '/accounts?status=' + sel.value).then(function (rows) {
        list.textContent = '';
        if (!rows.length) { list.appendChild(emptyBox('No accounts with this status')); return; }
        list.appendChild(table(ACCOUNT_HEADS, rows.map(function (a) { return accountRow(a, function () { load(); reload(); }); })));
      }).catch(function (e) { failed(list, e); });
    }
    load();
  }

  function reconTab(box) {
    var days = h('select', {}, [['7', 'Last 7 days'], ['30', 'Last 30 days'], ['90', 'Last 90 days']].map(function (d) { return h('option', { value: d[0], text: d[1] }); }));
    var out = h('div');
    box.appendChild(h('div', { class: 'toolbar' }, h('div', {}, h('label', { text: 'Check our records for' }), days),
      h('button', { class: 'btn', text: 'Run check', onclick: run })));
    box.appendChild(out);
    function issues(list) {
      return list.length ? table(['Level', 'What', 'Where', 'Details'], list.map(function (i) {
        return h('tr', {}, h('td', {}, pill(i.severity.toLowerCase(), i.severity === 'ERROR' ? 'st-bad' : i.severity === 'WARN' ? 'st-PENDING' : 'st-HIDDEN')),
          h('td', { text: String(i.code).replace(/_/g, ' ').toLowerCase() }), h('td', { text: i.reference }), h('td', { text: i.detail || '' }));
      })) : emptyBox('No problems found. Everything adds up.');
    }
    function run() {
      loading(out);
      getJson(PAY_BASE + '/reconciliation?days=' + days.value).then(function (r) {
        out.textContent = '';
        out.appendChild(h('p', { class: 'muted', text: num(r.paymentsChecked) + ' payments checked · ' + plural(r.issues.length, 'issue') }));
        out.appendChild(issues(r.issues));
      }).catch(function (e) { failed(out, e); });
    }
    // compare against the M-Pesa statement
    var area = h('textarea', { rows: '6', placeholder: 'One payment per line:  RECEIPT,AMOUNT\nSJK1ABC234,1500', class: 'wide' });
    var cmpOut = h('div');
    box.appendChild(h('div', { class: 'section-title', text: 'Compare with the M-Pesa statement' }));
    box.appendChild(area);
    box.appendChild(h('div', { class: 'toolbar' }, h('button', { class: 'btn secondary', text: 'Compare', onclick: function () {
      var lines = area.value.split(/\r?\n/).map(function (l) { return l.split(','); }).filter(function (p) { return p.length >= 2 && p[0].trim(); })
        .map(function (p) { return { receipt: p[0].trim(), amountKes: Math.round(Number(p[1])) }; });
      if (!lines.length) { toast('Paste at least one line', true); return; }
      loading(cmpOut);
      api(PAY_BASE + '/reconciliation/statement', { method: 'POST', json: { lines: lines } }).then(function (r) { return r.json(); })
        .then(function (list) { cmpOut.textContent = ''; cmpOut.appendChild(issues(list)); }).catch(function (e) { failed(cmpOut, e); });
    } })));
    box.appendChild(cmpOut);
  }

  // ----- one payment in full -----
  function paymentDetail(id, reload) {
    var m = modalShell();
    function load() {
      getJson(PAY_BASE + '/' + id).then(function (d) {
        var p = d.payment; m.modal.textContent = '';
        m.modal.appendChild(h('div', {}, h('h2', { text: 'Payment #' + p.id + ' · Order #' + p.orderId }), sp(p.status)));
        m.modal.appendChild(h('div', { class: 'facts' },
          fact('M-Pesa receipt', p.receipt), fact('Paid', money(p.amountKes)), fact('Order total (rounded up)', money(p.expectedKes)),
          fact('Owed back to buyer', money(p.owedToBuyerKes)), fact('Already refunded', money(p.refundedKes)),
          fact('Paid from', p.phone), fact('Method', p.provider), fact('Created', when(p.createdAt)), fact('Notes', p.note)));

        m.modal.appendChild(h('div', { class: 'section-title', text: 'Who gets what' }));
        m.modal.appendChild(d.allocations.length ? table(['Beneficiary', ['Share', 'num'], ['Amount', 'num'], ['Paid', 'num'], 'Status', ''], d.allocations.map(function (a) {
          var acts = h('td', { class: 'actions' });
          if (a.status === 'RECORDED' || a.status === 'ELIGIBLE' || a.status === 'SETTLING') acts.appendChild(abtn('Hold', function () {
            var why = ask('Why hold this money? (e.g. dispute)'); if (why === null) return;
            post(PAY_BASE + '/allocations/' + a.id + '/hold', { reason: why }, function () { load(); reload(); }, 'Held');
          }));
          if (a.status === 'ON_HOLD') acts.appendChild(abtn('Release', function () {
            post(PAY_BASE + '/allocations/' + a.id + '/release', {}, function () { load(); reload(); }, 'Released');
          }));
          return h('tr', {}, h('td', { text: String(a.beneficiaryType).toLowerCase() + (a.beneficiaryType === 'FARMER' ? ' #' + a.beneficiaryId : ' · ' + a.beneficiaryId) }),
            h('td', { class: 'num', text: (a.basisPoints / 100).toFixed(2) + '%' }), h('td', { class: 'num', text: money(a.amountKes) }),
            h('td', { class: 'num', text: money(a.settledKes) }), h('td', {}, sp(a.status), a.holdReason ? h('div', { class: 'sub', text: a.holdReason }) : null), acts);
        })) : emptyBox('Nothing allocated'));

        if (d.payouts.length) {
          m.modal.appendChild(h('div', { class: 'section-title', text: 'Transfers' }));
          m.modal.appendChild(table(PAYOUT_HEADS, d.payouts.map(function (po) { return payoutRow(po, function () { load(); reload(); }); })));
        }
        m.modal.appendChild(h('div', { class: 'section-title', text: 'Ledger (every movement of money)' }));
        m.modal.appendChild(table(['When', 'Type', 'Account', 'Side', ['Amount', 'num'], 'Reference'], d.ledger.map(function (e) {
          return h('tr', {}, h('td', { text: when(e.createdAt) }), h('td', { text: String(e.entryType).toLowerCase() }), h('td', { text: e.account }),
            h('td', { text: e.direction.toLowerCase() }), h('td', { class: 'num', text: money(e.amountKes) }), h('td', { text: e.reference || '' }));
        })));

        var foot = h('div', { class: 'actions' });
        if (p.status === 'SUCCESS') foot.appendChild(abtn('Refund whole payment', function () {
          var why = ask('Reason for the refund:'); if (why === null) return;
          var phone = ask('Buyer M-Pesa number to refund to (empty = the number that paid):'); if (phone === null) return;
          if (!window.confirm('Reverse all three shares and refund the buyer ' + money(p.amountKes) + '?')) return;
          post(PAY_BASE + '/' + p.id + '/refund', { reason: why, phone: phone || null }, function () { load(); reload(); }, 'Refund queued');
        }, true));
        foot.appendChild(document.createTextNode(' '));
        foot.appendChild(h('button', { class: 'btn secondary', text: 'Close', onclick: m.close }));
        m.modal.appendChild(foot);
      }).catch(function (e) { m.modal.textContent = ''; m.modal.appendChild(emptyBox(e.message)); m.modal.appendChild(h('button', { class: 'btn secondary', text: 'Close', onclick: m.close })); });
    }
    load();
  }

  // ---------- companies (super admin only) ----------
  var CO_STATUS = { PENDING_VERIFICATION: 'Waiting', UNDER_REVIEW: 'Under review', VERIFIED: 'Verified', REJECTED: 'Rejected', SUSPENDED: 'Suspended' };
  var CO_CATEGORY = { AGRI_SUPPLIER: 'Agricultural supplier', PRODUCE_BUYER: 'Bulk produce buyer', TRANSPORT: 'Transport', SERVICE_PROVIDER: 'Service provider' };
  var CO_DOC = { REGISTRATION_CERT: 'Registration certificate', KRA_PIN_CERT: 'KRA PIN certificate', TRADE_LICENCE: 'Trade licence', SECTOR_LICENCE: 'Sector licence', REP_ID: 'Representative ID', OTHER: 'Other' };

  function companiesView(box) {
    var current = 'PENDING_VERIFICATION', list = h('div');
    var select = h('select', { onchange: function () { current = select.value; load(); } },
      Object.keys(CO_STATUS).map(function (k) { return h('option', { value: k, text: CO_STATUS[k] }); }));
    box.appendChild(h('div', { class: 'toolbar' }, h('div', {}, h('label', { text: 'Show' }), select),
      h('div', { class: 'muted' }, 'Only the super admin can verify companies.')));
    box.appendChild(list);
    function load() {
      loading(list);
      getJson('/api/admin/companies?status=' + encodeURIComponent(current)).then(function (rows) {
        list.textContent = '';
        if (!rows.length) { list.appendChild(emptyBox('Nothing here')); return; }
        list.appendChild(table(['Company', 'Category', 'County', 'Documents', 'Submitted', 'Status', ''], rows.map(function (r) {
          return h('tr', {},
            h('td', {}, h('b', { text: r.name })), h('td', { text: CO_CATEGORY[r.category] || r.category }), h('td', { text: r.county || '-' }),
            h('td', { text: String(r.documents) }), h('td', { text: when(r.submittedAt) }), h('td', {}, pill(CO_STATUS[r.status] || r.status, 'st-' + r.status)),
            h('td', { class: 'actions' }, h('button', { class: 'btn small', text: 'Open', onclick: function () { openCompany(r.id, load); } })));
        })));
      }).catch(function (e) { failed(list, e); });
    }
    load();
  }

  function openCompany(id, done) {
    var m = modalShell(), modal = m.modal, close = m.close;
    getJson('/api/admin/companies/' + id).then(function (d) {
      var c = d.company;
      modal.textContent = '';
      function act(path, json, okMsg) {
        return api('/api/admin/companies/' + id + '/' + path, { method: 'POST', json: json }).then(function () { toast(okMsg); close(); done(); })
          .catch(function (e) { toast(e.message, true); });
      }
      function ask(label, path, okMsg) {
        promptDialog(label, 'Reason (at least 5 characters)', { min: 5, multiline: true, ok: 'Send', danger: true }).then(function (reason) { if (reason) act(path, { reason: reason }, okMsg); });
      }
      var acts = h('div', { class: 'actions' }, h('button', { class: 'btn secondary', text: 'Close', onclick: close }));
      if (c.status === 'PENDING_VERIFICATION') acts.appendChild(h('button', { class: 'btn', text: 'Start review', onclick: function () { act('start-review', undefined, 'Review started'); } }));
      if (c.status === 'UNDER_REVIEW') {
        acts.appendChild(h('button', { class: 'btn danger', text: 'Reject…', onclick: function () { ask('Reason shown to the company', 'reject', 'Rejected. The company can fix and resubmit'); } }));
        acts.appendChild(h('button', { class: 'btn', text: 'Verify', onclick: function () {
          confirmDialog('Verify this company?', 'Confirm that the registration number, KRA PIN and documents are genuine and match the company details.', 'Verify company').then(function (ok) { if (ok) act('verify', undefined, 'Company verified'); });
        } }));
      }
      if (c.status === 'VERIFIED') acts.appendChild(h('button', { class: 'btn danger', text: 'Suspend…', onclick: function () { ask('Reason for suspending', 'suspend', 'Company suspended'); } }));
      if (c.status === 'SUSPENDED') acts.appendChild(h('button', { class: 'btn', text: 'Reinstate', onclick: function () { act('reinstate', undefined, 'Company reinstated'); } }));

      if (c.status === 'VERIFIED' && c.payoutAccount && !c.payoutApproved)
        acts.appendChild(h('button', { class: 'btn', text: 'Approve payout account', onclick: function () {
          confirmDialog('Approve payout account?', 'FarmConnect will send this company\'s money to ' + c.payoutType + ' ' + c.payoutAccount + '. Check that the account really belongs to the company.', 'Approve').then(function (ok) { if (ok) act('payout-account/approve', undefined, 'Payout account approved'); });
        } }));
      if (c.status === 'VERIFIED' && c.payoutApproved)
        acts.appendChild(h('button', { class: 'btn secondary', text: 'Revoke payout approval', onclick: function () { act('payout-account/revoke', undefined, 'Payout approval revoked'); } }));
      var docs = h('div', { class: 'facts' });
      if (!c.documents.length) docs.appendChild(h('div', { class: 'muted', text: 'No documents uploaded' }));
      c.documents.forEach(function (x) {
        docs.appendChild(h('div', {}, h('span', { text: CO_DOC[x.type] || x.type }),
          h('button', { class: 'btn secondary small', text: 'Open ' + (x.fileName || ''), onclick: function () {
            api('/api/admin/companies/' + id + '/documents/' + x.id).then(function (r) { return r.blob(); }).then(function (b) {
              var u = URL.createObjectURL(b); m.urls.push(u); window.open(u, '_blank', 'noopener');
            }).catch(function (e) { toast(e.message, true); });
          } })));
      });
      modal.appendChild(h('div', {}, h('h2', { text: c.name }), pill(CO_STATUS[c.status] || c.status, 'st-' + c.status)));
      modal.appendChild(h('div', { class: 'facts' },
        fact('Registration no.', c.businessRegNumber), fact('KRA PIN', c.kraPin), fact('Category', CO_CATEGORY[c.category] || c.category),
        fact('Role', [c.canBuy ? 'Buys produce' : '', c.canSell ? 'Sells products/services' : ''].filter(Boolean).join(' · ')),
        fact('Representative', [c.repName, c.repPhone, c.repEmail].filter(Boolean).join(' · ')),
        fact('Account', (d.ownerName || '') + ' · ' + (d.ownerEmail || '') + (d.ownerPhone ? ' · ' + d.ownerPhone : '')),
        fact('Address', [c.address, c.county].filter(Boolean).join(', ')),
        fact('Operates in', (c.operatingCounties || []).join(', ')), fact('About', c.description),
        fact('Payout account', c.payoutAccount ? (c.payoutType + ' ' + c.payoutAccount + (c.payoutReference ? ' / ' + c.payoutReference : '') + (c.payoutApproved ? '  (approved)' : '  (NOT approved)')) : 'Not set'),
        fact('Submitted', when(c.submittedAt)),
        fact('Reviewed', c.reviewedAt ? when(c.reviewedAt) + ' by ' + (d.reviewedByName || '') : '-'),
        c.rejectionReason ? fact('Rejection reason', c.rejectionReason) : null,
        c.suspensionReason ? fact('Suspension reason', c.suspensionReason) : null));
      modal.appendChild(h('h3', { text: 'Documents' }));
      modal.appendChild(docs);
      modal.appendChild(acts);
    }).catch(function (e) { toast(e.message, true); close(); });
  }

  // ---------- commission rates (super admin only) ----------
  var CM_TYPE = { SUPPLY_SALE: 'Supplier sells inputs to a farmer', BULK_PURCHASE: 'Company buys produce in bulk', SERVICE_FEE: 'Service or transport fee' };
  function pct(bps) { return (bps / 100).toFixed(2).replace(/\.?0+$/, '') + '%'; }

  function commissionView(box) {
    var list = h('div');
    box.appendChild(h('div', { class: 'toolbar' }, h('div', { class: 'muted' },
      'A change applies to NEW orders only. Orders keep the rate they were placed with. The 5% / 5% / 90% produce split is separate and is not changed here.')));
    box.appendChild(list);
    function load() {
      loading(list);
      getJson('/api/admin/commission-policies').then(function (rows) {
        list.textContent = '';
        list.appendChild(table(['Transaction', 'FarmConnect', 'County', 'County agreement', 'Since', ''], rows.map(function (r) {
          return h('tr', {}, h('td', {}, h('b', { text: CM_TYPE[r.type] || r.type })), h('td', { text: pct(r.platformBps) }), h('td', { text: pct(r.countyBps) }),
            h('td', { text: r.countyAgreementRef || '-' }), h('td', { text: when(r.effectiveFrom) }),
            h('td', { class: 'actions' },
              h('button', { class: 'btn small', text: 'Change', onclick: function () { editPolicy(r, load); } }),
              h('button', { class: 'btn secondary small', text: 'History', onclick: function () { policyHistory(r.type); } })));
        })));
      }).catch(function (e) { failed(list, e); });
    }
    load();
  }

  function editPolicy(r, done) {
    var m = modalShell(), modal = m.modal, close = m.close;
    var plat = h('input', { type: 'number', min: '0', max: '100', step: '0.01', value: String(r.platformBps / 100) });
    var county = h('input', { type: 'number', min: '0', max: '100', step: '0.01', value: String(r.countyBps / 100) });
    var ref = h('input', { maxlength: 200, placeholder: 'Required if the county share is above 0', value: r.countyAgreementRef || '' });
    var note = h('input', { maxlength: 300, placeholder: 'Why the rate is changing' });
    var err = h('div', { class: 'error' });
    modal.appendChild(h('form', { onsubmit: function (ev) {
      ev.preventDefault(); err.textContent = '';
      var body = { type: r.type, platformBps: Math.round(Number(plat.value) * 100), countyBps: Math.round(Number(county.value) * 100), countyAgreementRef: ref.value.trim(), note: note.value.trim() };
      api('/api/admin/commission-policies', { method: 'POST', json: body }).then(function () { toast('New rate saved'); close(); done(); })
        .catch(function (e) { err.textContent = e.message; });
    } }, h('h2', { text: CM_TYPE[r.type] || r.type }),
      h('label', { text: 'FarmConnect share (%)' }), plat, h('label', { text: 'County share (%) — only under a formally approved agreement' }), county,
      h('label', { text: 'County agreement reference' }), ref, h('label', { text: 'Note' }), note,
      h('div', { class: 'actions' }, h('button', { class: 'btn secondary', type: 'button', text: 'Cancel', onclick: close }), h('button', { class: 'btn', type: 'submit', text: 'Save new rate' })), err));
  }

  function policyHistory(type) {
    var m = modalShell(), modal = m.modal, close = m.close;
    modal.appendChild(h('h2', { text: 'Rate history' }));
    var box = h('div'); modal.appendChild(box);
    modal.appendChild(h('div', { class: 'actions' }, h('button', { class: 'btn secondary', text: 'Close', onclick: close })));
    getJson('/api/admin/commission-policies/' + encodeURIComponent(type) + '/history').then(function (rows) {
      box.appendChild(table(['From', 'To', 'FarmConnect', 'County', 'Set by', 'Note'], rows.map(function (r) {
        return h('tr', {}, h('td', { text: when(r.effectiveFrom) }), h('td', { text: r.effectiveTo ? when(r.effectiveTo) : 'now' }),
          h('td', { text: pct(r.platformBps) }), h('td', { text: pct(r.countyBps) }), h('td', { text: r.createdBy || '-' }), h('td', { text: r.note || '' }));
      })));
    }).catch(function (e) { failed(box, e); });
  }

  // ---------- farm-input money (super admin only) ----------
  function kes(n) { return 'KSh ' + Number(n || 0).toLocaleString('en-KE'); }

  function supplyMoneyView(box) {
    box.appendChild(h('div', { class: 'toolbar' }, h('div', { class: 'muted' },
      'Farm-input orders are paid after delivery. Payments that need a person, and the payouts to companies, are here.'),
      h('button', { class: 'btn secondary small', text: 'Check ledger', onclick: function () {
        getJson('/api/admin/supply-payments/ledger-check').then(function (r) {
          toast(r.balanced ? 'Ledger balances: every payment has equal debits and credits' : 'LEDGER DOES NOT BALANCE for payments: ' + r.unbalancedPayments.join(', '), !r.balanced);
        }).catch(function (e) { toast(e.message, true); });
      } })));

    var payBox = h('div'), payFilter = h('select', { onchange: loadPay },
      [['NEEDS_REVIEW', 'Needs review'], ['PENDING', 'Pending'], ['SUCCESS', 'Successful'], ['FAILED', 'Failed'], ['', 'Latest 100']].map(function (o) { return h('option', { value: o[0], text: o[1] }); }));
    box.appendChild(h('h3', { text: 'Collections from farmers' }));
    box.appendChild(h('div', { class: 'toolbar' }, h('div', {}, h('label', { text: 'Show' }), payFilter)));
    box.appendChild(payBox);
    function loadPay() {
      loading(payBox);
      getJson('/api/admin/supply-payments/payments' + (payFilter.value ? '?status=' + payFilter.value : '')).then(function (rows) {
        payBox.textContent = '';
        if (!rows.length) { payBox.appendChild(emptyBox('Nothing here')); return; }
        payBox.appendChild(table(['#', 'Order', 'Farmer', 'Expected', 'Paid', 'Owed back', 'Receipt', 'Status', 'Note', ''], rows.map(function (p) {
          var canConfirm = (p.status === 'NEEDS_REVIEW' || p.status === 'PENDING') && !p.receipt;
          return h('tr', {}, h('td', { text: String(p.id) }), h('td', { text: '#' + p.orderId }), h('td', { text: p.buyerName }), h('td', { text: kes(p.expectedKes) }),
            h('td', { text: kes(p.paidKes) }), h('td', { text: p.excessKes ? kes(p.excessKes) : '-' }), h('td', { text: p.receipt || '-' }),
            h('td', {}, h('span', { class: 'pill', text: p.status })), h('td', { text: p.note || '' }),
            h('td', { class: 'actions' }, canConfirm ? h('button', { class: 'btn small', text: 'Confirm…', onclick: function () {
              promptDialog('Confirm payment #' + p.id, 'M-Pesa receipt from the statement (e.g. SJK1ABC234)', { ok: 'Next' }).then(function (receipt) {
                if (!receipt) return;
                promptDialog('Confirm payment #' + p.id, 'Amount received in KSh (whole shillings)', { value: String(p.expectedKes), ok: 'Confirm payment' }).then(function (amt) {
                  if (!amt) return;
                  api('/api/admin/supply-payments/payments/' + p.id + '/confirm', { method: 'POST', json: { receipt: receipt, amountKes: parseInt(amt, 10) } })
                    .then(function () { toast('Payment confirmed'); loadPay(); loadOut(); }).catch(function (e) { toast(e.message, true); });
                });
              });
            } }) : null));
        })));
      }).catch(function (e) { failed(payBox, e); });
    }

    var outBox = h('div'), outFilter = h('select', { onchange: loadOut },
      [['', 'Latest 100'], ['QUEUED', 'Queued'], ['PROCESSING', 'Sending'], ['UNKNOWN', 'Unknown (check statement)'], ['FAILED', 'Failed'], ['SUCCESS', 'Paid out']].map(function (o) { return h('option', { value: o[0], text: o[1] }); }));
    box.appendChild(h('h3', { text: 'Payouts to companies' }));
    box.appendChild(h('div', { class: 'toolbar' }, h('div', {}, h('label', { text: 'Show' }), outFilter)));
    box.appendChild(outBox);
    function loadOut() {
      loading(outBox);
      getJson('/api/admin/supply-payments/payouts' + (outFilter.value ? '?status=' + outFilter.value : '')).then(function (rows) {
        outBox.textContent = '';
        if (!rows.length) { outBox.appendChild(emptyBox('Nothing here')); return; }
        outBox.appendChild(table(['#', 'Order', 'Company', 'Amount', 'Status', 'Note', ''], rows.map(function (p) {
          var fixable = p.status === 'FAILED' || p.status === 'UNKNOWN';
          return h('tr', {}, h('td', { text: String(p.id) }), h('td', { text: '#' + p.orderId }), h('td', { text: p.companyName }), h('td', { text: kes(p.amountKes) }),
            h('td', {}, h('span', { class: 'pill', text: p.status })), h('td', { text: p.status === 'SUCCESS' ? (p.providerTransactionId || '') : (p.failureReason || '') }),
            h('td', { class: 'actions' },
              fixable ? h('button', { class: 'btn small', text: 'Retry', onclick: function () {
                var go = function () {
                  api('/api/admin/supply-payments/payouts/' + p.id + '/requeue?confirmedNotSent=' + (p.status === 'UNKNOWN'), { method: 'POST' })
                    .then(function () { toast('Queued again'); loadOut(); }).catch(function (e) { toast(e.message, true); });
                };
                if (p.status !== 'UNKNOWN') go();
                else confirmDialog('Retry this payout?', 'Open the M-Pesa statement first. Retry ONLY if the money was NOT sent, or the company will be paid twice.', 'The money was not sent. Retry', true).then(function (ok) { if (ok) go(); });
              } }) : null,
              (fixable || p.status === 'QUEUED') ? h('button', { class: 'btn secondary small', text: 'Mark paid…', onclick: function () {
                promptDialog('Mark payout #' + p.id + ' as paid', 'M-Pesa reference of the payment you made by hand', { ok: 'Record as paid' }).then(function (ref) {
                  if (!ref) return;
                  api('/api/admin/supply-payments/payouts/' + p.id + '/mark-paid', { method: 'POST', json: { reference: ref } })
                    .then(function () { toast('Recorded as paid'); loadOut(); }).catch(function (e) { toast(e.message, true); });
                });
              } }) : null));
        })));
      }).catch(function (e) { failed(outBox, e); });
    }
    loadPay(); loadOut();
  }

  // ---------- marketplace oversight (super admin only) ----------
  var MK_TABS = [
    ['supply-orders', 'Input orders', ['', 'PLACED', 'CONFIRMED', 'DISPATCHED', 'DELIVERED', 'CANCELLED', 'REJECTED']],
    ['produce-requests', 'Produce requests', ['', 'OPEN', 'FULFILLED', 'CLOSED', 'CANCELLED']],
    ['purchase-orders', 'Purchase orders', ['', 'ISSUED', 'CONFIRMED', 'DELIVERED', 'DECLINED', 'CANCELLED']],
    ['service-requests', 'Service requests', ['', 'REQUESTED', 'ACCEPTED', 'COMPLETED', 'DECLINED', 'CANCELLED']]
  ];

  function marketplaceView(box) {
    var kpis = h('div'), tabs = h('div', { class: 'toolbar' }), body = h('div');
    var current = MK_TABS[0], status = '';
    box.appendChild(kpis); box.appendChild(tabs); box.appendChild(body);

    function kpi(label, value, note) { return h('div', { class: 'kpi' }, h('div', {}, h('div', { class: 'label', text: label }), h('div', { class: 'value', text: String(value) }), note ? h('div', { class: 'note', text: note }) : null)); }
    getJson('/api/admin/marketplace/overview').then(function (o) {
      kpis.textContent = '';
      kpis.appendChild(h('div', { class: 'kpis' },
        kpi('Verified companies', o.companies.VERIFIED || 0, (o.companies.PENDING_VERIFICATION || 0) + ' waiting, ' + (o.companies.UNDER_REVIEW || 0) + ' in review'),
        kpi('Input orders delivered', o.supplyOrders.DELIVERED || 0, 'KSh ' + Number(o.supplyDeliveredGross || 0).toLocaleString('en-KE')),
        kpi('Input orders open', (o.supplyOrders.PLACED || 0) + (o.supplyOrders.CONFIRMED || 0) + (o.supplyOrders.DISPATCHED || 0), 'new, confirmed or dispatched'),
        kpi('Produce requests open', o.produceRequests.OPEN || 0, (o.quotations.SUBMITTED || 0) + ' quotations waiting'),
        kpi('Purchase orders', (o.purchaseOrders.ISSUED || 0) + (o.purchaseOrders.CONFIRMED || 0), 'issued or confirmed'),
        kpi('Service requests open', (o.serviceRequests.REQUESTED || 0) + (o.serviceRequests.ACCEPTED || 0), 'new or accepted')));
    }).catch(function (e) { failed(kpis, e); });

    function draw() {
      tabs.textContent = '';
      var select = h('select', { onchange: function () { status = select.value; load(); } }, current[2].map(function (st) { return h('option', { value: st, text: st || 'All statuses' }); }));
      select.value = status;
      tabs.appendChild(h('div', { class: 'left' }, MK_TABS.map(function (t) {
        return h('button', { class: 'btn small' + (t === current ? '' : ' secondary'), text: t[1], onclick: function () { current = t; status = ''; draw(); load(); } });
      })));
      tabs.appendChild(h('div', {}, h('label', { text: 'Status' }), select));
    }
    function money2(n) { return 'KSh ' + Number(n || 0).toLocaleString('en-KE'); }
    var SPECS = {
      'supply-orders': [['#', 'Company', 'Farmer', 'Items', 'Total', 'FarmConnect fee', 'Payment', 'Status', 'Date'], function (o) {
        return [o.id, o.companyName, o.buyerName, o.items.map(function (i) { return i.quantity + ' ' + i.name; }).join(', '), money2(o.total), money2(Number(o.platformFee) + Number(o.countyFee)), o.paymentStatus + (o.paymentMethod ? ' (' + o.paymentMethod + ')' : ''), o.status, when(o.createdAt)];
      }],
      'produce-requests': [['#', 'Company', 'Produce', 'Quantity', 'Still needed', 'Deliver to', 'Closes', 'Status', 'Quotes waiting'], function (r) {
        return [r.id, r.companyName, r.produce, r.quantity + ' ' + r.unit, r.remaining + ' ' + r.unit, r.deliveryCounty, r.closesAt, r.status, r.openQuotations];
      }],
      'purchase-orders': [['#', 'Company', 'Farmer', 'Produce', 'Quantity', 'Total', 'FarmConnect fee', 'Payment', 'Status'], function (o) {
        return [o.id, o.companyName, o.farmerName, o.produce, o.quantity + ' ' + o.unit, money2(o.total), money2(Number(o.platformFee) + Number(o.countyFee)), o.paymentStatus, o.status];
      }],
      'service-requests': [['#', 'Company', 'Service', 'Farmer', 'Location', 'Price', 'Status', 'Date'], function (r) {
        return [r.id, r.companyName, r.serviceName, r.farmerName, r.location, r.quotedPrice != null ? money2(r.quotedPrice) : '-', r.status, when(r.createdAt)];
      }]
    };
    function load() {
      loading(body);
      getJson('/api/admin/marketplace/' + current[0] + (status ? '?status=' + status : '')).then(function (rows) {
        body.textContent = '';
        if (!rows.length) { body.appendChild(emptyBox('Nothing here')); return; }
        var spec = SPECS[current[0]];
        body.appendChild(table(spec[0], rows.map(function (r) { return h('tr', {}, spec[1](r).map(function (c) { return h('td', { text: c == null ? '' : String(c) }); })); })));
        body.appendChild(h('p', { class: 'muted', text: 'Showing the latest ' + rows.length + '. Read-only.' }));
      }).catch(function (e) { failed(body, e); });
    }
    draw(); load();
  }

  // ---------- crop doctor ----------
  var CROP_STATUS = { OPEN: 'Waiting for you', ANSWERED: 'Answered', CLOSED: 'Closed' };
  var CROP_TONE = { OPEN: 'st-PENDING_VERIFICATION', ANSWERED: 'st-VERIFIED', CLOSED: 'st-SUSPENDED' };

  function cropDoctorView(box) {
    var current = 'OPEN', list = h('div');
    var select = h('select', { onchange: function () { current = select.value; load(); } },
      Object.keys(CROP_STATUS).concat(['ALL']).map(function (k) { return h('option', { value: k, text: CROP_STATUS[k] || 'Everything' }); }));
    box.appendChild(h('div', { class: 'toolbar' }, h('div', {}, h('label', { text: 'Show' }), select),
      state.scope && !state.scope.all ? h('div', { class: 'muted' }, 'Only cases from farmers in ' + state.scope.label + ' are listed.') : null));
    box.appendChild(list);
    function load() {
      loading(list);
      getJson('/api/crop-doctor/cases?status=' + encodeURIComponent(current)).then(function (rows) {
        list.textContent = '';
        if (!rows.length) { list.appendChild(emptyBox('No crop cases here')); return; }
        paged(list, rows, 20, function (r) {
          var top = r.matches && r.matches.length ? r.matches[0] : null;
          return h('tr', {},
            h('td', {}, h('b', { text: r.crop })), h('td', { text: r.farmerName }), h('td', { text: r.subCounty || '-' }),
            h('td', { text: top ? top.name + ' (' + top.score + '%)' : 'No guess' }),
            h('td', { text: String(r.photos.length) }), h('td', { text: when(r.createdAt) }),
            h('td', {}, pill(CROP_STATUS[r.status] || r.status, CROP_TONE[r.status] || '')),
            h('td', { class: 'actions' }, h('button', { class: 'btn small', text: 'Open', onclick: function () { openCropCase(r.id, load); } })));
        }, ['Crop', 'Farmer', 'Sub-county', 'Guide thinks', 'Photos', 'Sent', 'Status', '']);
      }).catch(function (e) { failed(list, e); });
    }
    load();
  }

  function openCropCase(id, done) {
    var m = modalShell(), modal = m.modal;
    getJson('/api/crop-doctor/cases/' + id).then(function (c) {
      modal.textContent = '';
      var err = h('div', { class: 'error' });
      var photos = h('div', { class: 'docs' }, c.photos.map(function (u, i) {
        return h('figure', {}, h('img', { src: u, alt: 'Photo ' + (i + 1) }), h('figcaption', { text: 'Photo ' + (i + 1) }));
      }));
      var picks = h('select', {}, [h('option', { value: '', text: 'Not sure yet' })].concat((c.matches || []).map(function (x) {
        return h('option', { value: x.key, text: x.name });
      })));
      if (c.confirmedProblem) picks.value = c.confirmedProblem;
      var note = h('textarea', { rows: 4, maxlength: 1500, placeholder: 'What the farmer should do (plain words)' });
      note.value = c.reviewerNote || '';
      modal.appendChild(h('h2', { text: c.crop + ' - ' + c.farmerName }));
      modal.appendChild(h('div', { class: 'facts' },
        fact('Phone', c.farmerPhone), fact('Place', (c.subCounty || '-') + (c.county ? ', ' + c.county : '')),
        fact('Sent', when(c.createdAt)), fact('Status', CROP_STATUS[c.status] || c.status),
        fact('Signs ticked', (c.symptomLabels || []).join('; ')), fact('Farmer says', c.description)));
      if (c.photos.length) modal.appendChild(photos);
      (c.matches || []).forEach(function (x) {
        modal.appendChild(h('div', { class: 'note' }, h('b', { text: x.name + ' - ' + x.score + '% (' + x.confidence.toLowerCase() + ')' }),
          h('div', { text: x.cause }), h('ul', {}, x.advice.map(function (a) { return h('li', { text: a }); }))));
      });
      if (c.status !== 'CLOSED') {
        modal.appendChild(h('form', { onsubmit: function (ev) {
          ev.preventDefault();
          var text = note.value.trim();
          if (text.length < 5) { err.textContent = 'Please write a short answer for the farmer'; return; }
          api('/api/crop-doctor/cases/' + id + '/review', { method: 'POST', json: { note: text, confirmedProblem: picks.value || null } })
            .then(function () { toast('Answer sent to the farmer'); m.close(); if (done) done(); })
            .catch(function (e) { err.textContent = e.message; });
        } }, h('label', { text: 'What do you think it is?' }), picks, h('label', { text: 'Your answer to the farmer' }), note, err,
          h('div', { class: 'actions' },
            h('button', { class: 'btn secondary', type: 'button', text: 'Close case', onclick: function () {
              api('/api/crop-doctor/cases/' + id + '/close', { method: 'POST' }).then(function () { m.close(); if (done) done(); })
                .catch(function (e) { err.textContent = e.message; });
            } }),
            h('button', { class: 'btn', type: 'submit', text: c.status === 'ANSWERED' ? 'Update answer' : 'Send answer' }))));
      } else if (c.reviewerNote) {
        modal.appendChild(h('div', { class: 'note' }, h('b', { text: (c.reviewerName || 'Officer') + ' answered' }), h('div', { text: c.reviewerNote })));
      }
      if (c.status === 'CLOSED') modal.appendChild(h('div', { class: 'actions' }, h('button', { class: 'btn secondary', text: 'Close', onclick: m.close })));
    }).catch(function (e) { modal.textContent = ''; modal.appendChild(h('div', { class: 'table-empty', text: e.message })); });
  }


  // ---------- delivery (super admin) ----------
  var DELIVERY_STATUS = { REQUESTED: 'Waiting', ACCEPTED: 'Accepted', PICKED_UP: 'On the way', DELIVERED: 'Delivered', DECLINED: 'Declined', CANCELLED: 'Cancelled' };
  var DELIVERY_TONE = { REQUESTED: 'st-PENDING_VERIFICATION', ACCEPTED: 'st-UNDER_REVIEW', PICKED_UP: 'st-UNDER_REVIEW', DELIVERED: 'st-VERIFIED', DECLINED: 'st-SUSPENDED', CANCELLED: 'st-SUSPENDED' };

  function deliveryView(box) {
    var form = h('div', { class: 'card' }), list = h('div');
    box.appendChild(form);
    box.appendChild(h('h3', { text: 'All deliveries' }));
    box.appendChild(list);
    loading(form);
    getJson('/api/delivery/settings').then(function (r) {
      form.textContent = '';
      var base = h('input', { type: 'number', min: '0', step: '1', value: String(r.baseFee) });
      var perKm = h('input', { type: 'number', min: '0', step: '1', value: String(r.perKmRate) });
      var factor = h('input', { type: 'number', min: '1', max: '3', step: '0.05', value: String(r.roadFactor) });
      var max = h('input', { type: 'number', min: '1', max: '2000', step: '10', value: String(r.maxKm) });
      var err = h('div', { class: 'error' });
      form.appendChild(h('h3', { text: 'Delivery price list' }));
      form.appendChild(h('p', { class: 'muted', text: 'Fee = base fee + rate per km x distance from the farm to the buyer, rounded up to a whole shilling. The buyer pays the transporter directly; FarmConnect only records it.' }));
      form.appendChild(h('form', { onsubmit: function (ev) {
        ev.preventDefault();
        api('/api/delivery/settings', { method: 'PUT', json: { baseFee: Number(base.value), perKmRate: Number(perKm.value), roadFactor: Number(factor.value), maxKm: Number(max.value) } })
          .then(function () { toast('Delivery rates saved'); err.textContent = ''; })
          .catch(function (e) { err.textContent = e.message; });
      } }, h('div', { class: 'facts' },
        h('div', {}, h('label', { text: 'Base fee (KES)' }), base), h('div', {}, h('label', { text: 'Rate per km (KES)' }), perKm),
        h('div', {}, h('label', { text: 'Road factor (roads are longer than a straight line)' }), factor), h('div', {}, h('label', { text: 'Longest delivery (km)' }), max)),
        err, h('div', { class: 'actions' }, h('button', { class: 'btn', type: 'submit', text: 'Save rates' }))));
    }).catch(function (e) { failed(form, e); });
    loading(list);
    getJson('/api/delivery/jobs?status=ALL').then(function (rows) {
      list.textContent = '';
      if (!rows.length) { list.appendChild(emptyBox('No deliveries yet')); return; }
      paged(list, rows, 20, function (j) {
        return h('tr', {},
          h('td', { text: '#' + j.orderId }), h('td', { text: j.transporterName }), h('td', { text: j.buyerName }),
          h('td', { text: (j.pickupPlace || '-') + ' to ' + (j.dropAddress || '-') }),
          h('td', { text: j.distanceKm == null ? '-' : j.distanceKm + ' km' }), h('td', { text: j.fee == null ? '-' : 'KES ' + num(j.fee) + (j.feePaid ? ' (paid)' : '') }),
          h('td', { text: when(j.createdAt) }), h('td', {}, pill(DELIVERY_STATUS[j.status] || j.status, DELIVERY_TONE[j.status] || '')));
      }, ['Order', 'Transporter', 'Buyer', 'Route', 'Distance', 'Fee', 'Requested', 'Status']);
    }).catch(function (e) { failed(list, e); });
  }


  // ---------- cash approvals and commission bills (super admin) ----------
  var CASH_STATUS = { RECORDED: 'Waiting for buyer', BUYER_CONFIRMED: 'Buyer confirmed', BUYER_DISPUTED: 'Buyer disputed', APPROVED: 'Approved', REJECTED: 'Rejected' };
  var CASH_TONE = { RECORDED: 'st-PENDING_VERIFICATION', BUYER_CONFIRMED: 'st-UNDER_REVIEW', BUYER_DISPUTED: 'st-SUSPENDED', APPROVED: 'st-VERIFIED', REJECTED: 'st-SUSPENDED' };
  var BILL_STATUS = { UNPAID: 'Unpaid', PENDING: 'Waiting for M-Pesa', PAID: 'Paid' };
  var BILL_TONE = { UNPAID: 'st-SUSPENDED', PENDING: 'st-UNDER_REVIEW', PAID: 'st-VERIFIED' };
  function cashKes(n) { return 'KES ' + Number(n || 0).toLocaleString('en-KE', { maximumFractionDigits: 2 }); }

  function cashView(box) {
    var pendingBox = h('div'), billsBox = h('div'), historyBox = h('div');
    box.appendChild(h('p', { class: 'muted', text: 'Every payment goes through the app. When a farmer is paid in cash, the farmer records it, the buyer confirms it and you approve it here. Approval marks the order paid and sends the farmer a commission bill, which the farmer pays by M-Pesa in the app.' }));
    box.appendChild(h('h3', { text: 'Waiting for your decision' }));
    box.appendChild(pendingBox);
    box.appendChild(h('h3', { text: 'Commission bills' }));
    box.appendChild(billsBox);
    box.appendChild(h('h3', { text: 'Recent cash payments' }));
    box.appendChild(historyBox);

    function decide(c, approve) {
      var ask = approve
        ? confirmDialog('Approve this cash payment?', cashKes(c.amount) + ' from ' + c.buyerName + ' to ' + c.farmerName + ' for order #' + c.orderId + '. The order becomes paid and ' + c.farmerName + ' gets a commission bill of ' + cashKes(c.platformAmount + c.countyAmount) + '.' + (c.status === 'BUYER_DISPUTED' ? ' The buyer disputed it: "' + (c.buyerNote || '') + '".' : c.status === 'RECORDED' ? ' The buyer has not answered yet.' : ''), 'Approve')
        : promptDialog('Reject this cash payment', 'Reason (the farmer and buyer will see it)', { ok: 'Reject', danger: true, min: 3 });
      ask.then(function (v) {
        if (!v) return;
        api('/api/cash/' + c.id + (approve ? '/approve' : '/reject'), { method: 'POST', json: { note: approve ? null : v } })
          .then(function () { toast(approve ? 'Approved. The farmer was sent the commission bill.' : 'Rejected'); load(); })
          .catch(function (e) { toast(e.message, true); });
      });
    }
    function markPaid(b) {
      promptDialog('Mark commission as paid', 'How was it received? (M-Pesa code, till, bank)', { ok: 'Mark paid', min: 3 }).then(function (v) {
        if (!v) return;
        api('/api/cash/bills/' + b.id + '/mark-paid', { method: 'POST', json: { note: v } })
          .then(function () { toast('Marked paid'); load(); }).catch(function (e) { toast(e.message, true); });
      });
    }
    function load() {
      loading(pendingBox); loading(billsBox); loading(historyBox);
      getJson('/api/cash/admin?status=PENDING').then(function (rows) {
        pendingBox.textContent = '';
        if (!rows.length) { pendingBox.appendChild(emptyBox('Nothing is waiting for approval')); return; }
        pendingBox.appendChild(table(['Order', 'Farmer', 'Buyer', 'Cash', 'Commission', 'Buyer says', 'Recorded', ''], rows.map(function (c) {
          return h('tr', {},
            h('td', { text: '#' + c.orderId }), h('td', { text: c.farmerName + (c.farmerPhone ? ' · ' + c.farmerPhone : '') }),
            h('td', { text: c.buyerName + (c.buyerPhone ? ' · ' + c.buyerPhone : '') }), h('td', { text: cashKes(c.amount) }),
            h('td', { text: cashKes(c.platformAmount + c.countyAmount) }),
            h('td', {}, pill(CASH_STATUS[c.status] || c.status, CASH_TONE[c.status] || ''), c.buyerNote ? h('div', { class: 'muted', text: c.buyerNote }) : null),
            h('td', { text: when(c.recordedAt) }),
            h('td', {}, h('button', { class: 'btn', text: 'Approve', onclick: function () { decide(c, true); } }),
              h('button', { class: 'btn secondary', text: 'Reject', onclick: function () { decide(c, false); } })));
        })));
      }).catch(function (e) { failed(pendingBox, e); });
      getJson('/api/cash/admin/bills?status=ALL').then(function (rows) {
        billsBox.textContent = '';
        var owed = rows.filter(function (b) { return b.status !== 'PAID'; }).reduce(function (a, b) { return a + b.amount; }, 0);
        var got = rows.filter(function (b) { return b.status === 'PAID'; }).reduce(function (a, b) { return a + b.amount; }, 0);
        billsBox.appendChild(h('div', { class: 'kpis' }, kpi('commission', 'amber', 'Commission still owed', cashKes(owed), 'unpaid + waiting for M-Pesa'), kpi('commission', '', 'Commission received', cashKes(got), 'from cash sales')));
        if (!rows.length) { billsBox.appendChild(emptyBox('No commission bills yet')); return; }
        var billTable = h('div');
        billsBox.appendChild(billTable);
        paged(billTable, rows, 15, function (b) {
          return h('tr', {},
            h('td', { text: '#' + b.id }), h('td', { text: 'Order #' + b.orderId }), h('td', { text: b.farmerName }),
            h('td', { text: cashKes(b.saleAmount) }), h('td', { text: cashKes(b.platformAmount) + ' + ' + cashKes(b.countyAmount) }),
            h('td', { text: cashKes(b.amount) }), h('td', { text: when(b.createdAt) }),
            h('td', {}, pill(BILL_STATUS[b.status] || b.status, BILL_TONE[b.status] || ''), b.overdue ? h('div', { class: 'error', text: 'Overdue' }) : null,
              b.mpesaReceipt ? h('div', { class: 'muted', text: b.mpesaReceipt }) : b.paidVia === 'ADMIN' ? h('div', { class: 'muted', text: 'by admin' }) : null),
            h('td', {}, b.status === 'PAID' ? null : h('button', { class: 'btn secondary', text: 'Mark paid', onclick: function () { markPaid(b); } })));
        }, ['Bill', 'Order', 'Farmer', 'Cash sale', 'Platform + county', 'Farmer owes', 'Created', 'Status', '']);
      }).catch(function (e) { failed(billsBox, e); });
      getJson('/api/cash/admin?status=ALL').then(function (rows) {
        historyBox.textContent = '';
        rows = rows.filter(function (c) { return c.status === 'APPROVED' || c.status === 'REJECTED'; });
        if (!rows.length) { historyBox.appendChild(emptyBox('No decisions yet')); return; }
        paged(historyBox, rows, 15, function (c) {
          return h('tr', {}, h('td', { text: '#' + c.orderId }), h('td', { text: c.farmerName }), h('td', { text: c.buyerName }),
            h('td', { text: cashKes(c.amount) }), h('td', { text: when(c.decidedAt) }),
            h('td', {}, pill(CASH_STATUS[c.status], CASH_TONE[c.status]), c.decisionNote ? h('div', { class: 'muted', text: c.decisionNote }) : null));
        }, ['Order', 'Farmer', 'Buyer', 'Cash', 'Decided', 'Result']);
      }).catch(function (e) { failed(historyBox, e); });
    }
    load();
  }


  // ---------- system health and backups (super admin) ----------
  function bytesText(n) {
    if (n == null) return '-';
    var u = ['B', 'KB', 'MB', 'GB', 'TB'], i = 0, v = Number(n);
    while (v >= 1024 && i < u.length - 1) { v /= 1024; i++; }
    return (i === 0 ? v : v.toFixed(1)) + ' ' + u[i];
  }
  function ago(sec) {
    if (sec < 120) return sec + ' seconds';
    if (sec < 7200) return Math.round(sec / 60) + ' minutes';
    if (sec < 172800) return Math.round(sec / 3600) + ' hours';
    return Math.round(sec / 86400) + ' days';
  }

  function systemView(box) {
    var top = h('div'), files = h('div'), note = h('div', { class: 'muted' });
    var btn = h('button', { class: 'btn', text: 'Back up now', onclick: function () {
      btn.disabled = true;
      api('/api/admin/backups/run', { method: 'POST' }).then(function () { toast('Backup started. This page refreshes in a moment.'); setTimeout(load, 4000); })
        .catch(function (e) { toast(e.message, true); btn.disabled = false; });
    } });
    box.appendChild(h('div', { class: 'toolbar' }, btn, h('button', { class: 'btn secondary', text: 'Refresh', onclick: load })));
    box.appendChild(top);
    box.appendChild(note);
    box.appendChild(h('h3', { text: 'Backup files' }));
    box.appendChild(files);
    function load() {
      loading(top);
      getJson('/api/admin/health').then(function (d) {
        btn.disabled = !!d.backupRunning;
        top.textContent = '';
        top.appendChild(h('div', { class: 'kpis' },
          kpi('system', d.database === 'UP' ? '' : 'amber', 'Database', d.database === 'UP' ? 'Working' : 'DOWN', d.databaseMillis + ' ms'),
          kpi('products', d.diskLow ? 'amber' : '', 'Disk free', bytesText(d.diskFreeBytes), 'of ' + bytesText(d.diskTotalBytes)),
          kpi('reports', 'blue', 'Memory used', bytesText(d.memoryUsedBytes), 'of ' + bytesText(d.memoryMaxBytes)),
          kpi('pulse', 'purple', 'Running for', ago(d.uptimeSeconds), d.profile),
          kpi('book', d.lastBackupOk === false || !d.backupsEnabled ? 'amber' : '', 'Last good backup',
            d.lastSuccessfulBackupAt ? when(d.lastSuccessfulBackupAt) : 'None yet', d.backupsEnabled ? 'Every night at 02:30' : 'Nightly backups are OFF')));
        note.textContent = (d.backupRunning ? 'A backup is running now. ' : '') + (d.lastBackupMessage || 'No backup has run since the server started.') + '  Folder: ' + d.backupFolder;
      }).catch(function (e) { failed(top, e); });
      loading(files);
      getJson('/api/admin/backups').then(function (rows) {
        files.textContent = '';
        if (!rows.length) { files.appendChild(emptyBox('No backup files yet')); return; }
        files.appendChild(table(['File', 'Size', 'Made'], rows.map(function (f) {
          return h('tr', {}, h('td', { text: f.name }), h('td', { text: bytesText(f.bytes) }), h('td', { text: when(f.modified) }));
        })));
      }).catch(function (e) { failed(files, e); });
    }
    load();
  }


  // ---------- frame ----------
  var NAV = [
    { group: 'Dashboard', items: [['overview', 'Overview', 'Overview']] },
    { group: 'County work', items: [['verifications', 'Verification', 'Farmer verification'], ['prices', 'Market prices', 'Market price board'], ['reports', 'Reports', 'Reports'], ['cropdoctor', 'Crop doctor', 'Crop doctor cases']] },
    { group: 'Administration', admin: true, items: [['users', 'Users', 'Users'], ['officers', 'Officers', 'County officers'], ['products', 'Products', 'Product moderation'], ['orders', 'Orders', 'Orders'], ['payments', 'Payments', 'Payments & payouts']] },
    { group: 'Super admin', superOnly: true, items: [['companies', 'Companies', 'Company verification'], ['marketplace', 'Marketplace', 'Marketplace overview'], ['commission', 'Commission', 'Commission rates'], ['supplymoney', 'Farm-input money', 'Farm-input payments and payouts'], ['cash', 'Cash approvals', 'Cash payments and commission bills'], ['delivery', 'Delivery', 'Delivery rates and jobs'], ['system', 'System health', 'System health and backups'], ['countyadmins', 'County admins', 'County admins'], ['security', 'Security', 'Security and audit log']] }
  ];
  var VIEWS = { overview: overview, verifications: verifications, prices: prices, reports: reports, cropdoctor: cropDoctorView, users: function (b) { usersView(b); }, officers: officersView, products: productsView, orders: ordersView, payments: paymentsView, companies: companiesView, marketplace: marketplaceView, commission: commissionView, supplymoney: supplyMoneyView, countyadmins: countyAdminsView, delivery: deliveryView, cash: cashView, system: systemView, security: securityView };
  var ADMIN_ONLY = { users: 1, officers: 1, products: 1, orders: 1, payments: 1, companies: 1, marketplace: 1, commission: 1, supplymoney: 1, countyadmins: 1, delivery: 1, cash: 1, system: 1, security: 1 };
  var SUPER_ONLY = { companies: 1, marketplace: 1, commission: 1, supplymoney: 1, countyadmins: 1, delivery: 1, cash: 1, system: 1, security: 1 };

  function currentView() {
    var v = (location.hash || '').replace('#', '');
    if (!VIEWS[v] || (ADMIN_ONLY[v] && !isAdmin()) || (SUPER_ONLY[v] && !isSuper())) v = 'overview';
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
    var areaLine = h('div', { text: state.scope ? state.scope.label : 'Kenya' });

    var nav = h('nav', { class: 'nav' });
    NAV.forEach(function (g) {
      if (g.admin && !isAdmin()) return;
      if (g.superOnly && !isSuper()) return;
      nav.appendChild(h('div', { class: 'group', text: g.group }));
      g.items.forEach(function (i) {
        nav.appendChild(h('a', { href: '#' + i[0], class: i[0] === state.view ? 'active' : null, onclick: function () { state.menuOpen = false; frame.classList.remove('open'); } },
          icon(i[0]), i[1], i[0] === 'verifications' ? queueBadge : null));
      });
    });
    var name = state.user ? state.user.name : '';
    var side = h('aside', { class: 'side' },
      h('div', { class: 'brand' }, h('h1', { text: '🌿 FarmConnect' }), areaLine),
      nav,
      h('div', { class: 'me' }, h('div', { class: 'avatar', text: (name || '?').trim().charAt(0).toUpperCase() }),
        h('div', { class: 'who' }, h('b', { text: name }), h('span', { text: isSuper() ? 'Super admin' : isCountyAdmin() ? 'County admin' : 'County officer' })),
        h('button', { title: 'Log out', 'aria-label': 'Log out', onclick: logout }, icon('logout'))));

    var bar = h('header', { class: 'bar' },
      h('button', { class: 'menu-btn', 'aria-label': 'Menu', onclick: function () { state.menuOpen = !state.menuOpen; frame.classList.toggle('open', state.menuOpen); } }, icon('menu')),
      h('h2', { text: titleOf(state.view) }), h('div', { class: 'spacer' }),
      h('button', { class: 'btn secondary small', text: 'Change password', onclick: changePasswordModal }),
      state.view === 'overview' ? h('button', { class: 'btn secondary small', text: 'Refresh', onclick: render }) : null,
      state.view === 'overview' ? h('button', { class: 'btn secondary small', text: 'Print', onclick: function () { window.print(); } }) : null,
      h('span', { class: 'date', text: new Date().toLocaleDateString('en-KE', { dateStyle: 'full' }) }));

    var body = h('div');
    frame.appendChild(side);
    frame.appendChild(h('div', { class: 'content' }, bar, h('main', {}, body)));
    VIEWS[state.view](body);

    getJson('/api/analytics/scope').then(function (sc) { state.scope = sc; areaLine.textContent = sc.label; }).catch(function () { /* optional */ });
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
