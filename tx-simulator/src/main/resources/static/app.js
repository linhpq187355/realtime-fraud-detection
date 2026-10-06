// Real-Time Fraud Detection Simulator JS Client

let autoModeEnabled = false;
let currentRate = 5;
let cardsList = [];
let pollingTimer = null;

document.addEventListener('DOMContentLoaded', () => {
  init();
});

async function init() {
  await loadCards();
  await loadAutoModeStatus();
  await pollFeedAndStats();

  // Start polling every 1 second
  pollingTimer = setInterval(pollFeedAndStats, 1000);
}

// 1. Load Demo Cards
async function loadCards() {
  const cardSelect = document.getElementById('card-select');
  try {
    const res = await fetch('/simulator/cards');
    if (!res.ok) throw new Error('Không thể tải danh sách thẻ');
    cardsList = await res.json();

    cardSelect.innerHTML = '';
    cardsList.forEach(card => {
      const opt = document.createElement('option');
      opt.value = card.cardId;
      const avgFormatted = formatVND(card.historicalAverageAmount);
      opt.textContent = `${card.cardId} (TB: ${avgFormatted})`;
      cardSelect.appendChild(opt);
    });

    if (cardsList.length > 0) {
      cardSelect.value = cardsList[0].cardId;
      // Pre-fill a reasonable default amount
      const defAmount = Math.round(cardsList[0].historicalAverageAmount);
      document.getElementById('amount-input').value = defAmount;
    }
  } catch (err) {
    showAlert('Không thể tải danh sách thẻ giả lập từ backend: ' + err.message);
  }
}

// 2. Load Auto Mode Status
async function loadAutoModeStatus() {
  try {
    const res = await fetch('/simulator/auto-mode');
    if (res.ok) {
      const data = await res.json();
      autoModeEnabled = data.enabled;
      currentRate = data.ratePerSecond || 5;

      document.getElementById('auto-mode-toggle').checked = autoModeEnabled;
      document.getElementById('auto-mode-status-text').textContent = autoModeEnabled ? 'Đang chạy' : 'Đang tắt';
      document.getElementById('rate-slider').value = currentRate;
      document.getElementById('rate-display').textContent = currentRate;
    }
  } catch (err) {
    console.warn('Could not load auto mode status:', err);
  }
}

// 3. Poll Feed and Stats
async function pollFeedAndStats() {
  try {
    // Stats
    const statsRes = await fetch('/simulator/stats');
    if (statsRes.ok) {
      const stats = await statsRes.json();
      document.getElementById('stat-total').textContent = stats.totalTransactions.toLocaleString();
      document.getElementById('stat-choqua').textContent = stats.totalChoQua.toLocaleString();
      document.getElementById('stat-xemxet').textContent = stats.totalXemXet.toLocaleString();
      document.getElementById('stat-chan').textContent = stats.totalChan.toLocaleString();
    }

    // Live Feed
    const feedRes = await fetch('/simulator/live-feed');
    if (feedRes.ok) {
      const feed = await feedRes.json();
      renderLiveFeed(feed);
    }
  } catch (err) {
    // Silently continue polling
  }
}

function renderLiveFeed(items) {
  const tbody = document.getElementById('live-feed-body');
  if (!items || items.length === 0) {
    return;
  }

  tbody.innerHTML = '';
  items.forEach(item => {
    const tr = document.createElement('tr');

    const timeStr = item.timestamp ? new Date(item.timestamp).toLocaleTimeString() : '--';
    const amountStr = formatVND(item.amount);
    const badgeHtml = renderDecisionBadgeSm(item.decision);
    const scoreOrRule = item.triggeredRule
      ? `<span style="color:#f87171; font-weight:500;">${item.triggeredRule}</span>`
      : (item.riskScore != null ? `Điểm ML: ${item.riskScore.toFixed(4)}` : '--');

    tr.innerHTML = `
      <td>${timeStr}</td>
      <td style="font-family:monospace;">${item.transactionId || '--'}</td>
      <td><strong>${item.cardId}</strong></td>
      <td>${amountStr}</td>
      <td>${item.merchant || '--'}</td>
      <td>${item.city || '--'}</td>
      <td>${badgeHtml}</td>
      <td>${scoreOrRule}</td>
      <td>${item.latencyMs != null ? item.latencyMs + ' ms' : '--'}</td>
    `;
    tbody.appendChild(tr);
  });
}

// 4. Submit Manual Transaction
async function submitManualTransaction(event) {
  event.preventDefault();
  const btn = document.getElementById('btn-submit-tx');
  const cardId = document.getElementById('card-select').value;
  const amountVal = document.getElementById('amount-input').value;
  const merchant = document.getElementById('merchant-select').value;
  const city = document.getElementById('city-select').value;

  if (!cardId || !amountVal) {
    showAlert('Vui lòng chọn thẻ và nhập số tiền hợp lệ');
    return;
  }

  const payload = {
    cardId: cardId,
    amount: parseFloat(amountVal),
    merchant: merchant,
    city: city
  };

  btn.disabled = true;
  btn.textContent = 'Đang xử lý...';
  dismissAlert();

  try {
    const res = await fetch('/simulator/manual-transaction', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!res.ok) {
      const errData = await res.json().catch(() => ({}));
      throw new Error(errData.message || `Lỗi HTTP ${res.status}`);
    }

    const result = await res.json();
    displayLatestDecision(result);
    await pollFeedAndStats();
  } catch (err) {
    showAlert('Gửi giao dịch thất bại: ' + err.message);
  } finally {
    btn.disabled = false;
    btn.textContent = 'Gửi giao dịch';
  }
}

// 5. Run Preset Scenarios
async function runScenario(scenarioName) {
  const logBox = document.getElementById('scenario-log');
  const progressBar = document.getElementById('scenario-progress-bar');
  const statusText = document.getElementById('scenario-status-text');
  const detailsText = document.getElementById('scenario-details-text');
  const resultsBody = document.getElementById('scenario-results-body');

  const btnRapid = document.getElementById('btn-scen-rapid');
  const btnTravel = document.getElementById('btn-scen-travel');
  const btnAmount = document.getElementById('btn-scen-amount');

  // Disable buttons
  btnRapid.disabled = true;
  btnTravel.disabled = true;
  btnAmount.disabled = true;
  dismissAlert();

  logBox.classList.add('active');
  progressBar.style.width = '20%';
  statusText.textContent = `Đang khởi chạy kịch bản [${scenarioName}]...`;
  detailsText.textContent = 'Đang gửi giao dịch đến hệ thống...';
  resultsBody.innerHTML = '';

  const selectedCard = document.getElementById('card-select').value;

  try {
    progressBar.style.width = '50%';
    const res = await fetch(`/simulator/scenario/${scenarioName}?cardId=${encodeURIComponent(selectedCard)}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ cardId: selectedCard })
    });

    if (!res.ok) {
      throw new Error(`Kịch bản thất bại với mã lỗi HTTP ${res.status}`);
    }

    progressBar.style.width = '80%';
    const data = await res.json();

    // Render results step by step with smooth delay
    const results = data.results || [];
    statusText.textContent = `Hoàn thành kịch bản: ${data.scenario}`;
    detailsText.textContent = `${data.description} -> ${data.triggeredOn}`;

    for (let i = 0; i < results.length; i++) {
      const r = results[i];
      const tr = document.createElement('tr');
      const badgeHtml = renderDecisionBadgeSm(r.decision);
      const note = r.triggeredRule ? `<span style="color:#f87171; font-weight:600;">${r.triggeredRule}</span>` :
        (r.riskScore != null ? `Điểm ML: ${r.riskScore.toFixed(4)}` : 'Bình thường');

      tr.innerHTML = `
        <td><strong>#${i + 1}</strong></td>
        <td style="font-family:monospace;">${r.transactionId}</td>
        <td>${formatVND(r.amount)}</td>
        <td>${r.city || '--'}</td>
        <td>${badgeHtml}</td>
        <td>${note}</td>
      `;

      if (r.decision === 'CHAN') {
        tr.style.backgroundColor = 'rgba(239, 68, 68, 0.15)';
      }
      resultsBody.appendChild(tr);

      // Show latest decision on the right card for the last item
      if (i === results.length - 1) {
        displayLatestDecision(r);
      }
    }

    progressBar.style.width = '100%';
    await pollFeedAndStats();
  } catch (err) {
    showAlert('Không thể chạy kịch bản: ' + err.message);
    statusText.textContent = 'Lỗi thực thi kịch bản';
    detailsText.textContent = err.message;
    progressBar.style.width = '0%';
  } finally {
    btnRapid.disabled = false;
    btnTravel.disabled = false;
    btnAmount.disabled = false;
  }
}

// 6. Display Latest Decision Card
function displayLatestDecision(result) {
  if (!result) return;

  const badgeEl = document.getElementById('latest-decision-badge');
  const decision = (result.decision || 'CHUA_XAC_DINH').toUpperCase();

  badgeEl.className = 'decision-badge-large';
  if (decision === 'CHO_QUA') {
    badgeEl.classList.add('badge-cho-qua');
    badgeEl.textContent = 'CHO_QUA (HỢP LỆ)';
  } else if (decision === 'XEM_XET') {
    badgeEl.classList.add('badge-xem-xet');
    badgeEl.textContent = 'XEM_XET (NGHI NGỜ)';
  } else if (decision === 'CHAN') {
    badgeEl.classList.add('badge-chan');
    badgeEl.textContent = 'CHAN (GIAN LẬN)';
  } else {
    badgeEl.classList.add('badge-empty');
    badgeEl.textContent = decision;
  }

  // Risk Score
  const scoreEl = document.getElementById('latest-risk-score');
  if (result.riskScore != null) {
    scoreEl.textContent = result.riskScore.toFixed(4);
    scoreEl.style.color = result.riskScore > 0.8 ? '#ef4444' : (result.riskScore > 0.4 ? '#f59e0b' : '#10b981');
  } else {
    scoreEl.textContent = 'N/A (Khớp Rule)';
    scoreEl.style.color = '#94a3b8';
  }

  // Meta
  document.getElementById('latest-tx-id').textContent = result.transactionId || '--';
  document.getElementById('latest-rule').textContent = result.triggeredRule ? result.triggeredRule : 'Không có (Đánh giá ML)';
  document.getElementById('latest-card-amount').textContent = `${result.cardId} • ${formatVND(result.amount)}`;
  document.getElementById('latest-merchant-city').textContent = `${result.merchant || '--'} • ${result.city || '--'}`;
  document.getElementById('decision-latency').textContent = `Độ trễ: ${result.latencyMs != null ? result.latencyMs : '--'} ms`;

  // Features Breakdown
  const feat = result.features;
  if (feat) {
    document.getElementById('feat-so-gd').textContent = `${feat.soGiaoDich5Phut} GD`;
    document.getElementById('feat-tong-tien').textContent = formatVND(feat.tongTien1Gio);
    document.getElementById('feat-trung-binh').textContent = formatVND(feat.trungBinhLichSu);

    const lechVal = feat.lechSoVoiTrungBinh != null ? (feat.lechSoVoiTrungBinh * 100).toFixed(1) + '%' : '--';
    document.getElementById('feat-lech-tb').textContent = lechVal;

    document.getElementById('feat-khoang-cach').textContent = feat.khoangCachBatThuong ? 'CÓ (Bất thường)' : 'Không';
    document.getElementById('feat-khoang-cach').style.color = feat.khoangCachBatThuong ? '#ef4444' : '#10b981';
  } else {
    document.getElementById('feat-so-gd').textContent = '--';
    document.getElementById('feat-tong-tien').textContent = '--';
    document.getElementById('feat-trung-binh').textContent = '--';
    document.getElementById('feat-lech-tb').textContent = '--';
    document.getElementById('feat-khoang-cach').textContent = '--';
  }
}

// 7. Auto Mode Controls
async function toggleAutoMode(enabled) {
  autoModeEnabled = enabled;
  document.getElementById('auto-mode-status-text').textContent = enabled ? 'Đang chạy' : 'Đang tắt';

  try {
    const res = await fetch('/simulator/auto-mode', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ enabled: enabled, ratePerSecond: currentRate })
    });
    if (!res.ok) throw new Error('Cập nhật chế độ tự động thất bại');
  } catch (err) {
    showAlert('Lỗi điều khiển Auto Mode: ' + err.message);
  }
}

function updateRateDisplay(val) {
  currentRate = parseInt(val, 10);
  document.getElementById('rate-display').textContent = currentRate;
}

async function changeAutoModeRate(val) {
  currentRate = parseInt(val, 10);
  if (autoModeEnabled) {
    try {
      await fetch('/simulator/auto-mode', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ enabled: true, ratePerSecond: currentRate })
      });
    } catch (err) {
      console.warn('Lỗi cập nhật tốc độ auto mode:', err);
    }
  }
}

// 8. Helpers
function formatVND(amount) {
  if (amount == null) return '--';
  const num = typeof amount === 'number' ? amount : parseFloat(amount);
  return num.toLocaleString('vi-VN') + ' đ';
}

function renderDecisionBadgeSm(decision) {
  const dec = (decision || '').toUpperCase();
  if (dec === 'CHO_QUA') {
    return '<span class="badge badge-sm-pass">CHO_QUA</span>';
  } else if (dec === 'XEM_XET') {
    return '<span class="badge badge-sm-review">XEM_XET</span>';
  } else if (dec === 'CHAN') {
    return '<span class="badge badge-sm-block">CHAN</span>';
  }
  return `<span class="badge" style="background:#334155; color:#94a3b8;">${dec || '--'}</span>`;
}

function showAlert(message) {
  const el = document.getElementById('alert-banner');
  const msgEl = document.getElementById('alert-message');
  msgEl.textContent = message;
  el.style.display = 'flex';
}

function dismissAlert() {
  document.getElementById('alert-banner').style.display = 'none';
}

function clearLiveFeed() {
  document.getElementById('live-feed-body').innerHTML = `
    <tr><td colspan="9" class="empty-feed-text">Bảng đã được xóa. Đang chờ giao dịch mới...</td></tr>
  `;
}
