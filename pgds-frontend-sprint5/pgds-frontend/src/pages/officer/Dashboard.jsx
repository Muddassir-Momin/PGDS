import { Line, Doughnut } from 'react-chartjs-2';
import {
  Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, ArcElement, Tooltip, Legend, Filler,
} from 'chart.js';
import api from '../../api';
import { useAuth } from '../../auth';
import { Panel, StatCard, Badge, Loading, ErrorBox, Empty, useLoad, kg, num, GRAIN_COLOR } from '../../components/ui';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, ArcElement, Tooltip, Legend, Filler);

const get = (url) => () => api.get(url).then((r) => r.data);
const perfColor = (p) => (p >= 80 ? 'success' : p >= 60 ? 'warning' : 'danger');

export default function Dashboard() {
  const { user } = useAuth();
  const summary = useLoad(get('/api/officer/dashboard/summary'));
  const trend = useLoad(get('/api/officer/dashboard/trend?months=6'));
  const grains = useLoad(get('/api/officer/dashboard/stock-by-grain'));
  const shops = useLoad(get('/api/officer/dashboard/shops'));
  const top = useLoad(get('/api/officer/dashboard/top-shops?limit=5'));
  const alerts = useLoad(get('/api/officer/ai/alerts?status=OPEN&size=5'));
  const s = summary.data;

  const lineData = trend.data && {
    labels: trend.data.periods,
    datasets: Object.entries(trend.data.series)
      .filter(([g, v]) => g !== 'OTHER' || v.some((x) => Number(x) > 0))
      .map(([g, v]) => ({
        label: g, data: v.map(Number), borderColor: GRAIN_COLOR[g], backgroundColor: GRAIN_COLOR[g] + '22',
        tension: 0.35, fill: true, pointRadius: 3,
      })),
  };
  const donutData = grains.data && {
    labels: grains.data.map((g) => g.grainType),
    datasets: [{ data: grains.data.map((g) => Number(g.kg)), backgroundColor: grains.data.map((g) => GRAIN_COLOR[g.grainType]), borderWidth: 0 }],
  };

  return (
    <>
      <div className="gg-hero">
        <div>
          <h2>Ensuring Food Security<br />for a Stronger India</h2>
          <p className="mb-0">Welcome, {user.fullName}. Transparent · Efficient · Accountable · AI Powered</p>
        </div>
        {s && <div className="gg-hero-chip">{s.period}</div>}
      </div>

      <ErrorBox error={summary.error} />
      {s ? (
        <div className="row g-3 mt-1">
          <div className="col-6 col-xl-3"><StatCard icon="bi-people-fill" label="Total Beneficiaries" value={num(s.totalBeneficiaries)} sub={`${num(s.totalRationCards)} ration cards`} /></div>
          <div className="col-6 col-xl-3"><StatCard icon="bi-shop" tone="blue" label="Ration Shops (FPS)" value={num(s.totalShops)} sub={`${num(s.totalWarehouses)} warehouses`} /></div>
          <div className="col-6 col-xl-3">
            <StatCard icon="bi-basket2-fill" tone="amber" label="Total Grain Stock" value={kg(s.totalStockKg)}>
              <div className="progress mt-1" style={{ height: 6 }}><div className="progress-bar bg-success" style={{ width: `${s.healthyShopPercent}%` }} /></div>
              <div className="gg-stat-sub">{s.healthyShopPercent}% of shops healthy</div>
            </StatCard>
          </div>
          <div className="col-6 col-xl-3"><StatCard icon="bi-exclamation-triangle-fill" tone="red" label="AI Alerts" value={num(s.openAiAlerts)} sub={`${num(s.highPriorityAiAlerts)} high priority`} /></div>
        </div>
      ) : summary.loading && <Loading />}

      <div className="row g-3 mt-1">
        <div className="col-12 col-lg-8">
          <Panel title="Grain Distribution Trend (kg per month)" icon="bi-graph-up">
            {trend.loading && !trend.data ? <Loading /> : <ErrorBox error={trend.error} />}
            {lineData && <div style={{ height: 280 }}><Line data={lineData} options={{ responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } }, scales: { y: { beginAtZero: true } } }} /></div>}
          </Panel>
        </div>
        <div className="col-12 col-lg-4">
          <Panel title="Stock by Grain Type" icon="bi-pie-chart-fill">
            {grains.loading && !grains.data ? <Loading /> : <ErrorBox error={grains.error} />}
            {donutData && (
              <>
                <div style={{ height: 190 }}><Doughnut data={donutData} options={{ responsive: true, maintainAspectRatio: false, cutout: '68%', plugins: { legend: { display: false } } }} /></div>
                <ul className="list-unstyled mt-3 mb-0 small">
                  {grains.data.map((g) => (
                    <li key={g.grainType} className="d-flex justify-content-between">
                      <span><span className="gg-dot" style={{ background: GRAIN_COLOR[g.grainType] }} />{g.grainType}</span>
                      <span>{kg(g.kg)} · {g.percent}%</span>
                    </li>
                  ))}
                </ul>
              </>
            )}
          </Panel>
        </div>
      </div>

      <div className="row g-3 mt-1">
        <div className="col-12 col-xl-7">
          <Panel title="Ration Shop Stock Status" icon="bi-geo-alt-fill">
            {shops.loading && !shops.data ? <Loading /> : <ErrorBox error={shops.error} />}
            {shops.data && (shops.data.length === 0 ? <Empty /> : (
              <div className="table-responsive" style={{ maxHeight: 320 }}>
                <table className="table table-sm align-middle gg-table">
                  <thead><tr><th>Shop</th><th>District</th><th className="text-end">Stock</th><th className="text-end">Monthly need</th><th>Status</th></tr></thead>
                  <tbody>
                    {shops.data.map((r) => (
                      <tr key={r.fpsId}>
                        <td><strong>{r.shopCode}</strong><div className="small text-muted">{r.name}</div></td>
                        <td>{r.district}</td>
                        <td className="text-end">{kg(r.stockKg)}</td>
                        <td className="text-end">{kg(r.monthlyRequirementKg)}</td>
                        <td><Badge value={r.status} tone={r.status === 'LOW' ? 'warning' : undefined} /></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ))}
          </Panel>
        </div>
        <div className="col-12 col-xl-5">
          <Panel title="Top Performing Ration Shops" icon="bi-trophy-fill">
            {top.loading && !top.data ? <Loading /> : <ErrorBox error={top.error} />}
            {top.data && (top.data.length === 0 ? <Empty /> : top.data.map((t) => (
              <div key={t.shopCode} className="mb-3">
                <div className="d-flex justify-content-between small">
                  <span><strong>{t.shopCode}</strong> · {t.name}</span>
                  <span>{t.performancePercent}%</span>
                </div>
                <div className="progress" style={{ height: 8 }}><div className={`progress-bar bg-${perfColor(t.performancePercent)}`} style={{ width: `${t.performancePercent}%` }} /></div>
                <div className="small text-muted">{num(t.beneficiaries)} beneficiaries · {kg(t.distributedKg)} issued</div>
              </div>
            )))}
          </Panel>
        </div>
      </div>

      <Panel title="AI Insights - open alerts" icon="bi-stars" className="mt-3">
        {alerts.loading && !alerts.data ? <Loading /> : <ErrorBox error={alerts.error} />}
        {alerts.data && (alerts.data.items.length === 0 ? <Empty text="No open AI alerts." /> : alerts.data.items.map((a) => (
          <div key={a.id} className="gg-insight">
            <Badge value={a.severity} />
            <div>
              <div className="fw-semibold">{a.title}</div>
              <div className="small text-muted">{a.details}</div>
            </div>
          </div>
        )))}
      </Panel>
    </>
  );
}
