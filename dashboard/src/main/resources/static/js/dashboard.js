let tpsChart = null;
let chanRatioChart = null;
let latencyChart = null;

document.addEventListener('DOMContentLoaded', () => {
  initCharts();
  fetchMetrics();
  setInterval(fetchMetrics, 1000);
});

function initCharts() {
  if (typeof Chart === 'undefined') {
    console.warn('Chart.js not loaded, canvas fallback will be used.');
    return;
  }

  const commonOptions = {
    responsive: true,
    maintainAspectRatio: false,
    animation: false,
    plugins: {
      legend: {
        display: false
      },
      tooltip: {
        mode: 'index',
        intersect: false,
        backgroundColor: '#1f2937',
        borderColor: 'rgba(255,255,255,0.1)',
        borderWidth: 1,
        titleColor: '#f3f4f6',
        bodyColor: '#9ca3af'
      }
    },
    scales: {
      x: {
        grid: { color: 'rgba(255, 255, 255, 0.04)' },
        ticks: { color: '#6b7280', maxRotation: 0, font: { size: 10 } }
      },
      y: {
        grid: { color: 'rgba(255, 255, 255, 0.04)' },
        ticks: { color: '#6b7280', font: { size: 10 } },
        beginAtZero: true
      }
    }
  };

  // 1. TPS Chart
  const ctxTps = document.getElementById('tpsChart');
  if (ctxTps) {
    tpsChart = new Chart(ctxTps, {
      type: 'line',
      data: {
        labels: [],
        datasets: [{
          label: 'TPS',
          data: [],
          borderColor: '#38bdf8',
          backgroundColor: 'rgba(56, 189, 248, 0.1)',
          fill: true,
          tension: 0.3,
          borderWidth: 2,
          pointRadius: 0,
          pointHoverRadius: 4
        }]
      },
      options: commonOptions
    });
  }

  // 2. CHAN Ratio Chart
  const ctxChan = document.getElementById('chanRatioChart');
  if (ctxChan) {
    chanRatioChart = new Chart(ctxChan, {
      type: 'line',
      data: {
        labels: [],
        datasets: [{
          label: 'Tỷ lệ CHAN (%)',
          data: [],
          borderColor: '#f87171',
          backgroundColor: 'rgba(248, 113, 113, 0.1)',
          fill: true,
          tension: 0.3,
          borderWidth: 2,
          pointRadius: 0,
          pointHoverRadius: 4
        }]
      },
      options: {
        ...commonOptions,
        scales: {
          ...commonOptions.scales,
          y: {
            ...commonOptions.scales.y,
            max: 100
          }
        }
      }
    });
  }

  // 3. Latency Chart (p50 & p99)
  const ctxLatency = document.getElementById('latencyChart');
  if (ctxLatency) {
    const latencyOptions = {
      ...commonOptions,
      plugins: {
        ...commonOptions.plugins,
        legend: {
          display: true,
          labels: { color: '#9ca3af', font: { size: 11 }, boxWidth: 12 }
        }
      }
    };

    latencyChart = new Chart(ctxLatency, {
      type: 'line',
      data: {
        labels: [],
        datasets: [
          {
            label: 'p50 Latency (ms)',
            data: [],
            borderColor: '#60a5fa',
            backgroundColor: 'transparent',
            tension: 0.3,
            borderWidth: 2,
            pointRadius: 0,
            pointHoverRadius: 4
          },
          {
            label: 'p99 Latency (ms)',
            data: [],
            borderColor: '#fbbf24',
            backgroundColor: 'transparent',
            tension: 0.3,
            borderWidth: 2,
            pointRadius: 0,
            pointHoverRadius: 4
          }
        ]
      },
      options: latencyOptions
    });
  }
}

async function fetchMetrics() {
  try {
    const response = await fetch('/api/metrics');
    if (!response.ok) {
      throw new Error(`HTTP error: ${response.status}`);
    }
    const data = await response.json();
    updateUI(data);

    const statusEl = document.getElementById('connectionStatus');
    if (statusEl) {
      statusEl.textContent = 'Đang giám sát thời gian thực';
      statusEl.style.color = 'var(--accent-green)';
    }
  } catch (err) {
    console.error('Lỗi lấy dữ liệu metrics:', err);
    const statusEl = document.getElementById('connectionStatus');
    if (statusEl) {
      statusEl.textContent = 'Mất kết nối tới API';
      statusEl.style.color = 'var(--accent-red)';
    }
  }
}

function updateUI(data) {
  // 1. Update Last Updated Time
  const now = new Date();
  const timeStr = now.toTimeString().split(' ')[0];
  const lastUpdatedEl = document.getElementById('lastUpdated');
  if (lastUpdatedEl) {
    lastUpdatedEl.textContent = `Cập nhật: ${timeStr}`;
  }

  // 2. Update KPI Cards
  const kpiTotal = document.getElementById('kpiTotalTx');
  if (kpiTotal) kpiTotal.textContent = (data.totalTransactions || 0).toLocaleString();

  const kpiBreakdown = document.getElementById('kpiBreakdown');
  if (kpiBreakdown) {
    kpiBreakdown.textContent = `CHAN: ${(data.totalChan || 0).toLocaleString()} | XEM_XET: ${(data.totalXemXet || 0).toLocaleString()} | CHO_QUA: ${(data.totalChoQua || 0).toLocaleString()}`;
  }

  const kpiTps = document.getElementById('kpiTps');
  if (kpiTps) {
    const tpsVal = Number(data.currentTps || 0).toFixed(1);
    kpiTps.innerHTML = `${tpsVal} <span class="kpi-unit">tx/s</span>`;
  }

  const kpiChanRatio = document.getElementById('kpiChanRatio');
  if (kpiChanRatio) {
    const ratioVal = Number(data.chanRatio || 0).toFixed(1);
    kpiChanRatio.innerHTML = `${ratioVal} <span class="kpi-unit">%</span>`;
  }

  const kpiChanCount = document.getElementById('kpiChanCount');
  if (kpiChanCount) {
    kpiChanCount.textContent = `${(data.totalChan || 0).toLocaleString()} giao dịch bị chặn`;
  }

  const kpiP50 = document.getElementById('kpiP50');
  if (kpiP50) {
    const p50Val = Number(data.p50LatencyMs || 0).toFixed(0);
    kpiP50.innerHTML = `${p50Val} <span class="kpi-unit">ms</span>`;
  }

  const kpiP99 = document.getElementById('kpiP99');
  if (kpiP99) {
    const p99Val = Number(data.p99LatencyMs || 0).toFixed(0);
    kpiP99.innerHTML = `${p99Val} <span class="kpi-unit">ms</span>`;
  }

  // 3. Update Charts
  updateCharts(data);

  // 4. Update Top 10 Risk Table
  updateRiskTable(data.top10RiskTransactions);
}

function updateCharts(data) {
  if (tpsChart && data.tpsHistory) {
    tpsChart.data.labels = data.tpsHistory.map(p => p.time);
    tpsChart.data.datasets[0].data = data.tpsHistory.map(p => p.value);
    tpsChart.update('none');
  }

  if (chanRatioChart && data.chanRatioHistory) {
    chanRatioChart.data.labels = data.chanRatioHistory.map(p => p.time);
    chanRatioChart.data.datasets[0].data = data.chanRatioHistory.map(p => p.value);
    chanRatioChart.update('none');
  }

  if (latencyChart && data.latencyHistory) {
    latencyChart.data.labels = data.latencyHistory.map(p => p.time);
    latencyChart.data.datasets[0].data = data.latencyHistory.map(p => p.p50);
    latencyChart.data.datasets[1].data = data.latencyHistory.map(p => p.p99);
    latencyChart.update('none');
  }
}

function updateRiskTable(transactions) {
  const tbody = document.getElementById('topRiskBody');
  const countEl = document.getElementById('riskTableCount');
  if (!tbody) return;

  if (!transactions || transactions.length === 0) {
    tbody.innerHTML = `
      <tr class="empty-row">
        <td colspan="8">Chưa có giao dịch nào được ghi nhận trong 1 giờ qua.</td>
      </tr>
    `;
    if (countEl) countEl.textContent = '0 giao dịch';
    return;
  }

  if (countEl) countEl.textContent = `${transactions.length} giao dịch`;

  tbody.innerHTML = transactions.map(tx => {
    const timeStr = tx.timestamp ? new Date(tx.timestamp).toTimeString().split(' ')[0] : '-';
    const amountStr = tx.amount ? Number(tx.amount).toLocaleString('vi-VN') + ' đ' : '-';

    let badgeClass = 'badge-cho-qua';
    let decisionText = tx.decision || 'CHO_QUA';
    if (decisionText === 'CHAN') {
      badgeClass = 'badge-chan';
    } else if (decisionText === 'XEM_XET') {
      badgeClass = 'badge-xem-xet';
    }

    let riskScoreDisplay = '-';
    if (tx.decision === 'CHAN') {
      riskScoreDisplay = '<span class="highlight-red risk-pill">1.00 (Rule)</span>';
    } else if (tx.riskScore !== null && tx.riskScore !== undefined) {
      riskScoreDisplay = `<span class="risk-pill">${Number(tx.riskScore).toFixed(2)}</span>`;
    }

    const ruleDisplay = tx.triggeredRule ? `<span class="rule-tag">${escapeHtml(tx.triggeredRule)}</span>` : '<span style="color:var(--text-subtle)">—</span>';
    const latencyDisplay = tx.latencyMs !== null && tx.latencyMs !== undefined ? `${tx.latencyMs} ms` : '-';

    return `
      <tr>
        <td>${escapeHtml(timeStr)}</td>
        <td><code>${escapeHtml(tx.transactionId || '')}</code></td>
        <td><strong>${escapeHtml(tx.cardId || '')}</strong></td>
        <td>${amountStr}</td>
        <td>${riskScoreDisplay}</td>
        <td><span class="badge ${badgeClass}">${escapeHtml(decisionText)}</span></td>
        <td>${ruleDisplay}</td>
        <td>${escapeHtml(latencyDisplay)}</td>
      </tr>
    `;
  }).join('');
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
