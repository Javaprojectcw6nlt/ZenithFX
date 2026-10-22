const $ = (id) => document.getElementById(id);

async function requestJson(url, options = {}) {
  const response = await fetch(url, options);
  let data;
  try { data = await response.json(); } catch { data = { error: `Unexpected response (${response.status})` }; }
  if (!response.ok) throw new Error(data.error || data.message || `Request failed (${response.status})`);
  return data;
}

function status(id, message, kind = '') {
  const el = $(id); if (!el) return;
  el.textContent = message; el.className = `status ${kind}`.trim();
}

function showJson(id, data) {
  const el = $(id); if (!el) return;
  el.textContent = JSON.stringify(data, null, 2); el.classList.remove('hidden');
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

function initHnb() {
  if (!$('hnbForm')) return;
  
  const today = new Date();
  const end = today.toISOString().slice(0, 10);
  const startDate = new Date(today); startDate.setDate(startDate.getDate() - 14);
  $('startDate').value = startDate.toISOString().slice(0, 10);
  $('endDate').value = end;

  $('hnbForm').addEventListener('submit', async e => {
    e.preventDefault(); 
    status('hnbStatus', 'Contacting the HNB SOAP service…');
    
    try {
      const params = new URLSearchParams({
        currency: $('currency').value,
        startDate: $('startDate').value,
        endDate: $('endDate').value
      });
      
      const data = await requestJson('/api/hnb?' + params);
      const rows = data.rates || [];
      
      $('ratesBody').innerHTML = rows.length ? rows.map(r => `<tr><td>${escapeHtml(r.date)}</td><td>${escapeHtml(r.currency)}</td><td>${escapeHtml(r.unit)}</td><td>${escapeHtml(r.value)}</td></tr>`).join('') : '<tr><td colspan="4" class="empty">The service returned no rates for this range.</td></tr>';
      
      status('hnbStatus', `Loaded ${rows.length} rate row(s) for ${data.currency}.`, 'success');

      const canvas = $('rateChart');
      if (!canvas) return; 
      
      // Bulletproof chart replacement
      if (window.rateChart) {
          try {
              if (window.rateChart instanceof Chart) {
                  window.rateChart.destroy();
              }
          } catch (e) {
              console.warn("Could not destroy old chart:", e);
          }
      }

      const ctx = canvas.getContext('2d');
      window.rateChart = new Chart(ctx, {
        type: 'line',
        data: {
          labels: rows.map(r => r.date),
          datasets: [{
            label: `${data.currency} rate (HUF)`,
            data: rows.map(r => Number(r.value)),
            borderColor: '#a78bfa',
            backgroundColor: 'rgba(167,139,250,.13)',
            fill: true,
            tension: .3,
            pointRadius: 3
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { labels: { color: '#c5c0db' } } },
          scales: {
            x: { ticks: { color: '#8983a7' }, grid: { color: 'rgba(167,139,250,.08)' } },
            y: { ticks: { color: '#8983a7' }, grid: { color: 'rgba(167,139,250,.08)' } }
          }
        }
      });
      
    } catch (err) { 
      console.error(err);
      status('hnbStatus', err.message, 'error'); 
    }
  });
}

async function loadAccount() {
  status('accountStatus', 'Requesting OANDA account summary…');
  try { 
    const d = await requestJson('/api/oanda/account'); 
    showJson('accountOutput', d); 
    const a = d.account || {}; 
    status('accountStatus', `Account summary loaded. Currency: ${a.currency || '—'} · Balance: ${a.balance || '—'}`, 'success'); 
  } catch (e) { 
    status('accountStatus', e.message, 'error'); 
  }
}

if ($('priceForm')) $('priceForm').addEventListener('submit', async e => {
  e.preventDefault(); 
  const instrument = $('priceInstrument').value; 
  status('priceStatus', `Requesting ${instrument}…`);
  try {
    const d = await requestJson('/api/oanda/pricing?instrument=' + encodeURIComponent(instrument));
    const prices = d.prices || [];
    $('priceCards').innerHTML = prices.length ? prices.map(p => `<div class="price-card"><small>${escapeHtml(p.instrument || instrument)}</small><strong>${escapeHtml(p.closeoutBid || p.bids?.[0]?.price || '—')}</strong><span>Bid / closeout bid</span><p>Ask: ${escapeHtml(p.closeoutAsk || p.asks?.[0]?.price || '—')}</p></div>`).join('') : '<div class="status">No pricing returned. Check instrument availability and account permissions.</div>';
    showJson('priceRaw', d); 
    status('priceStatus', `Price request completed for ${instrument}.`, 'success');
  } catch (err) { 
    status('priceStatus', err.message, 'error'); 
  }
});

if ($('historyForm')) $('historyForm').addEventListener('submit', async e => {
  e.preventDefault(); 
  const instrument = $('historyInstrument').value, granularity = $('granularity').value; 
  status('historyStatus', 'Loading candle history…');
  try {
    const d = await requestJson('/api/oanda/candles?' + new URLSearchParams({ instrument, granularity }));
    const candles = d.candles || [];
    $('historyBody').innerHTML = candles.length ? candles.map(c => `<tr><td>${escapeHtml(c.time)}</td><td>${escapeHtml(c.mid?.o)}</td><td>${escapeHtml(c.mid?.h)}</td><td>${escapeHtml(c.mid?.l)}</td><td>${escapeHtml(c.mid?.c)}</td><td>${escapeHtml(c.volume)}</td></tr>`).join('') : '<tr><td colspan="6" class="empty">No candles returned.</td></tr>';
    showJson('historyRaw', d); 
    status('historyStatus', `Loaded ${candles.length} candle(s) for ${instrument} (${granularity}).`, 'success');
  } catch (err) { 
    status('historyStatus', err.message, 'error'); 
  }
});

async function loadPositions() {
  status('positionsStatus', 'Loading open positions…');
  try {
    const d = await requestJson('/api/oanda/positions'); 
    const rows = d.positions || [];
    $('positionsBody').innerHTML = rows.length ? rows.map(p => `<tr><td>${escapeHtml(p.instrument)}</td><td>${escapeHtml(p.long?.units)}</td><td>${escapeHtml(p.short?.units)}</td><td>${escapeHtml([...(p.long?.tradeIDs || []), ...(p.short?.tradeIDs || [])].join(', ') || '—')}</td></tr>`).join('') : '<tr><td colspan="4" class="empty">No open positions in this account.</td></tr>';
    showJson('positionsRaw', d); 
    status('positionsStatus', `Loaded ${rows.length} position group(s).`, 'success');
  } catch (err) { 
    status('positionsStatus', err.message, 'error'); 
  }
}

if ($('tradeForm')) $('tradeForm').addEventListener('submit', async e => {
  e.preventDefault();
  const instrument = $('tradeInstrument').value, units = Number($('tradeUnits').value);
  if (!Number.isInteger(units) || units === 0) { status('tradeStatus', 'Enter a non-zero whole-number quantity.', 'error'); return; }
  if (!confirm(`Send a ${units > 0 ? 'LONG' : 'SHORT'} market order for ${Math.abs(units)} units of ${instrument} to your OANDA PRACTICE account?`)) return;
  status('tradeStatus', 'Submitting practice market order…');
  try {
    const d = await requestJson('/api/oanda/orders?' + new URLSearchParams({ instrument, units: String(units) }), { method: 'POST' });
    showJson('tradeOutput', d); 
    status('tradeStatus', 'Order request completed. Review the response for fill and trade details.', 'success');
  } catch (err) { 
    status('tradeStatus', err.message, 'error'); 
  }
});

if ($('closeForm')) $('closeForm').addEventListener('submit', async e => {
  e.preventDefault(); 
  const tradeId = $('tradeId').value;
  if (!confirm(`Close trade ${tradeId} in your OANDA PRACTICE account?`)) return;
  status('closeStatus', 'Sending close request…');
  try {
    const d = await requestJson('/api/oanda/trades/' + encodeURIComponent(tradeId) + '/close', { method: 'POST' });
    showJson('closeOutput', d); 
    status('closeStatus', 'Close request completed. Review the API response.', 'success');
  } catch (err) { 
    status('closeStatus', err.message, 'error'); 
  }
});