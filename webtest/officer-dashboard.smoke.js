// Opens the super-admin tabs (companies, marketplace, commission, farm-input money) in jsdom with a fake API.
const { JSDOM } = require('jsdom'); const fs = require('fs');
const base = require('path').join(__dirname, '..', 'src', 'main', 'resources', 'static', 'officer') + '/';
const html = fs.readFileSync(base + 'index.html', 'utf8').replace(/<script src="officer.js"><\/script>/, '');
const js = fs.readFileSync(base + 'officer.js', 'utf8');
const wait = ms => new Promise(r => setTimeout(r, ms));
const co = { company: { id: 1, name: 'Acme', businessRegNumber: 'P1', kraPin: 'A1', category: 'AGRI_SUPPLIER', canBuy: false, canSell: true, repName: 'J', repPhone: '07', status: 'VERIFIED', operatingCounties: [], documents: [], payoutType: 'MPESA_PHONE', payoutAccount: '2547', payoutApproved: false, verified: true }, ownerName: 'J', ownerEmail: 'j@a', ownerPhone: '07' };
const routes = {
  'GET /api/admin/companies': [{ id: 1, name: 'Acme', category: 'AGRI_SUPPLIER', county: 'Kisumu', status: 'VERIFIED', documents: 2, submittedAt: '2026-10-01T10:00:00Z' }],
  'GET /api/admin/companies/1': co,
  'GET /api/admin/commission-policies': [{ id: 1, type: 'SUPPLY_SALE', platformBps: 500, countyBps: 0, effectiveFrom: '2026-10-01T10:00:00Z' }],
  'GET /api/admin/supply-payments/payments': [{ id: 1, orderId: 9, buyerName: 'Joe', status: 'NEEDS_REVIEW', expectedKes: 1500, paidKes: 0, excessKes: 0, note: 'x', createdAt: '2026-10-01T10:00:00Z' }],
  'GET /api/admin/supply-payments/payouts': [{ id: 1, orderId: 9, companyName: 'Acme', amountKes: 1425, status: 'UNKNOWN', failureReason: 'check', createdAt: '2026-10-01T10:00:00Z' }],
  'GET /api/admin/marketplace/overview': { companies: { VERIFIED: 3, PENDING_VERIFICATION: 1 }, supplyOrders: { DELIVERED: 5, PLACED: 2 }, supplyDeliveredGross: 50000, produceRequests: { OPEN: 2 }, quotations: { SUBMITTED: 4 }, purchaseOrders: { ISSUED: 1 }, serviceRequests: { REQUESTED: 2 } },
  'GET /api/admin/marketplace/supply-orders': [{ id: 9, companyName: 'Acme', buyerName: 'Joe', items: [{ quantity: 2, name: 'Seed' }], total: 1500, platformFee: 75, countyFee: 0, paymentStatus: 'UNPAID', status: 'DELIVERED', createdAt: '2026-10-01T10:00:00Z' }],
  'GET /api/admin/marketplace/produce-requests': [{ id: 3, companyName: 'Acme', produce: 'Maize', quantity: 100, unit: 'kg', remaining: 50, deliveryCounty: 'Kisumu', closesAt: '2026-11-01', status: 'OPEN', openQuotations: 1 }],
  'GET /api/admin/marketplace/purchase-orders': [], 'GET /api/admin/marketplace/service-requests': [],
};
(async () => {
  const dom = new JSDOM(html, { url: 'http://localhost/officer/index.html', runScripts: 'outside-only', pretendToBeVisual: true });
  const w = dom.window, errs = [];
  w.sessionStorage.setItem('fc_token', 't'); w.sessionStorage.setItem('fc_user', JSON.stringify({ name: 'Root', role: 'ADMIN', email: 'a@a' }));
  w.fetch = (p, i) => { const k = ((i && i.method) || 'GET') + ' ' + String(p).split('?')[0]; const r = routes[k];
    if (r === undefined && (!i || !i.method || i.method === 'GET')) return Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve({}), text: () => Promise.resolve('{}') });
    return Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve(r === undefined ? {} : r), text: () => Promise.resolve(JSON.stringify(r || {})), blob: () => Promise.resolve(new w.Blob(['x'])) }); };
  w.addEventListener('error', e => errs.push(e.message));
  w.eval(js); await wait(150);
  for (const r of ['companies', 'marketplace', 'commission', 'supplymoney']) {
    w.location.hash = '#' + r; await wait(150);
    const t = w.document.body.textContent.replace(/\s+/g, ' ');
    console.log(r, '|', (w.document.querySelector('.bar h2') || {}).textContent, '|', t.length, '| has rows:', w.document.querySelectorAll('tbody tr').length);
  }
  // marketplace tab switch
  w.location.hash = '#marketplace'; await wait(150);
  [...w.document.querySelectorAll('button')].find(b => b.textContent === 'Produce requests').click(); await wait(100);
  console.log('produce-requests rows:', w.document.querySelectorAll('tbody tr').length);
  // company modal: Approve payout -> confirm dialog -> accept
  w.location.hash = '#companies'; await wait(150);
  [...w.document.querySelectorAll('button')].find(b => b.textContent === 'Open').click(); await wait(150);
  const approve = [...w.document.querySelectorAll('.modal button')].find(b => b.textContent === 'Approve payout account');
  console.log('approve button present:', !!approve);
  approve.click(); await wait(60);
  const okBtn = [...w.document.querySelectorAll('.modal button')].find(b => b.textContent === 'Approve');
  console.log('confirm dialog shown:', !!okBtn);
  okBtn.click(); await wait(100);
  console.log('errors:', errs.length ? errs : 'none');
  process.exit(0);
})();
