// Loads the company portal in a simulated browser (jsdom) with a fake API and opens every screen.
// Run: cd webtest && npm install && npm test
const { JSDOM } = require('jsdom');
const fs = require('fs');
const base = require('path').join(__dirname, '..', 'src', 'main', 'resources', 'static', 'company') + '/';
const html = fs.readFileSync(base + 'index.html', 'utf8').replace(/<script src="company.js"><\/script>/, '');
const js = fs.readFileSync(base + 'company.js', 'utf8');

const company = { id: 1, name: 'Acme Agro Ltd', businessRegNumber: 'PVT-123', kraPin: 'A123456789B', category: 'AGRI_SUPPLIER', canBuy: true, canSell: true,
  repName: 'Jane', repPhone: '0712345678', repEmail: 'j@a.co', description: 'x', address: 'Kitale', county: 'Trans Nzoia', operatingCounties: ['Trans Nzoia'],
  status: 'VERIFIED', verified: true, documents: [{ id: 5, type: 'REGISTRATION_CERT', fileName: 'a.pdf', uploadedAt: '2026-10-01T10:00:00Z' }, { id: 6, type: 'KRA_PIN_CERT', fileName: 'b.pdf', uploadedAt: '2026-10-01T10:00:00Z' }],
  payoutType: 'MPESA_PHONE', payoutAccount: '254712345678', payoutApproved: false };
const order = { id: 9, status: 'DELIVERED', paymentStatus: 'UNPAID', buyerName: 'Farmer Joe', deliveryPhone: '0711', deliveryAddress: 'Farm', items: [{ productId: 1, name: 'Maize seed', unit: '2kg', quantity: 3, unitPrice: 500, lineTotal: 1500 }], total: 1500, platformFee: 75, countyFee: 0, companyNet: 1425, createdAt: '2026-10-02T10:00:00Z' };
const routes = {
  'GET /api/company/me': company,
  'GET /api/locations': [{ name: 'Trans Nzoia' }, { name: 'Kisumu' }],
  'GET /api/company/summary': { newOrders: 2, inProgress: 1, delivered: 4, activeProducts: 3, lowStock: 1, grossDelivered: 9000, feesDelivered: 450, netDelivered: 8550, netUnpaid: 1425 },
  'GET /api/company/requests': [{ id: 3, produce: 'Maize', quantity: 1000, unit: 'kg', remaining: 800, maxPrice: 50, deliveryCounty: 'Kisumu', deliveryLocation: 'Depot', neededBy: '2026-11-30', closesAt: '2026-11-10', status: 'OPEN', openQuotations: 0 }],
  'GET /api/company/purchase-orders': [{ id: 4, produce: 'Maize', unit: 'kg', farmerName: 'Joe', contactPhone: '0700', quantity: 200, pricePerUnit: 45, total: 9000, deliveryCounty: 'Kisumu', deliveryLocation: 'Depot', neededBy: '2026-11-30', status: 'CONFIRMED', paymentStatus: 'UNPAID' }],
  'GET /api/company/products': [{ id: 1, name: 'Maize seed', category: 'SEED', unit: '2kg', price: 500, stockQty: 3, minOrderQty: 1, active: true, licenceRef: 'KEPHIS-1' }],
  'GET /api/company/supply-orders': [order], 'GET /api/company/supply-orders/9': order,
  'GET /api/company/supply-orders/9/invoice': { number: 'INV-000009', issuedAt: '2026-10-02T10:00:00Z', sellerName: 'Acme', sellerRegNumber: 'PVT-123', sellerKraPin: 'A123456789B', sellerAddress: 'Kitale', sellerPhone: '0712', sellerEmail: 'j@a.co', buyerName: 'Farmer Joe', buyerPhone: '0711', deliveryAddress: 'Farm', lines: [{ description: 'Maize seed', quantity: 3, unit: '2kg', unitPrice: 500, lineTotal: 1500 }], total: 1500, paymentStatus: 'UNPAID' },
  'GET /api/company/payouts': [{ id: 1, orderId: 9, amountKes: 1425, status: 'SUCCESS', providerTransactionId: 'SJK1', createdAt: '2026-10-03T10:00:00Z' }],
  'GET /api/company/services': [{ id: 2, name: 'Transport', category: 'TRANSPORT', price: 50, priceBasis: 'per km', counties: [], active: true, description: 'Trucks' }],
  'GET /api/company/service-requests': [{ id: 7, serviceId: 2, serviceName: 'Transport', category: 'TRANSPORT', farmerName: 'Joe', phone: '0700', details: 'Move maize', location: 'Kitale', status: 'REQUESTED', createdAt: '2026-10-04T10:00:00Z' }],
  'GET /api/company/team': [{ id: 1, name: 'Peter', role: 'DRIVER', phone: '0722', email: '', active: true }],
  'GET /api/notifications': [{ id: 1, type: 'SUPPLY', title: 'New order #9', body: 'x', read: false, createdAt: '2026-10-05T10:00:00Z' }],
  'GET /api/notifications/counts': { notifications: 3, messages: 0 },
  'GET /api/company/requests/3/quotations': [{ id: 11, requestId: 3, produce: 'Maize', unit: 'kg', farmerName: 'Joe', farmerPhone: '0700', quantity: 200, pricePerUnit: 45, total: 9000, status: 'SUBMITTED' }],
};
const calls = []; const errors = [];
(async () => {
  const dom = new JSDOM(html, { url: 'http://localhost/company/index.html', runScripts: 'outside-only', pretendToBeVisual: true });
  const w = dom.window;
  w.sessionStorage.setItem('fc_company_token', 'tok'); w.sessionStorage.setItem('fc_company_user', JSON.stringify({ name: 'Jane Doe', email: 'j@a.co' }));
  w.fetch = (path, init) => {
    const method = (init && init.method) || 'GET', key = method + ' ' + String(path).split('?')[0];
    calls.push(key + (init && init.body ? ' ' + String(init.body).slice(0, 80) : ''));
    const body = routes[key];
    if (body === undefined && method === 'GET') return Promise.resolve({ ok: false, status: 404, text: () => Promise.resolve(JSON.stringify({ message: 'no mock for ' + key })) });
    return Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve(body === undefined ? {} : body), text: () => Promise.resolve(body === undefined ? '' : JSON.stringify(body)), blob: () => Promise.resolve(new w.Blob(['x'])) });
  };
  w.addEventListener('error', e => errors.push('error: ' + e.message));
  w.eval(js);
  const wait = ms => new Promise(r => setTimeout(r, ms));
  await wait(80);
  const text = () => w.document.getElementById('app').textContent;
  const results = [];
  for (const r of ['overview', 'products', 'orders', 'services', 'servicerequests', 'payouts', 'requests', 'bulkorders', 'team', 'reports', 'alerts', 'profile', 'documents']) {
    w.location.hash = '#' + r; await wait(120);
    const t = text();
    const title = w.document.querySelector('.topbar h2');
    results.push([r, title && title.textContent, t.length, /Something went wrong|no mock/.test(t) ? 'ERROR-TEXT' : 'ok']);
  }
  console.log(results.map(r => r.join(' | ')).join('\n'));
  // interactions: open add product modal and submit
  w.location.hash = '#products'; await wait(100);
  [...w.document.querySelectorAll('button')].find(b => b.textContent === 'Add product').click(); await wait(50);
  console.log('modal open:', !!w.document.querySelector('.modal'), 'form fields:', w.document.querySelectorAll('.modal input').length);
  const f = w.document.querySelector('.modal form');
  const inp = f.querySelectorAll('input');
  inp[0].value = 'Test product'; inp[1].value = 'bag'; inp[2].value = '100'; inp[3].value = '5'; inp[4].value = '1';
  f.dispatchEvent(new w.Event('submit', { cancelable: true })); await wait(80);
  console.log('POST product:', calls.filter(c => c.startsWith('POST /api/company/products')).join(' / '));
  // invoice modal
  w.location.hash = '#orders'; await wait(100);
  [...w.document.querySelectorAll('button')].find(b => b.textContent === 'Open').click(); await wait(100);
  [...w.document.querySelectorAll('.modal button')].find(b => b.textContent === 'Invoice').click(); await wait(100);
  console.log('invoice shown:', !!w.document.querySelector('.invoice'), /INV-000009/.test(w.document.body.textContent));
  // confirm dialog: documents delete
  w.document.querySelectorAll('.modal-back').forEach(m => m.remove());
  console.log('unread bell:', [...w.document.querySelectorAll('[data-bell]')].map(e => e.textContent).join(','));
  console.log('errors:', errors.length ? errors : 'none');
  console.log('calls without mock:', calls.filter(c => c.startsWith('GET') && !routes[c.split(' ').slice(0,2).join(' ')]).join(', ') || 'none');
  process.exit(0);
})();
