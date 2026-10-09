import { useState } from 'react';
import api from '../../api';
import { Panel, Badge, Loading, ErrorBox, Empty, useLoad, kg } from '../../components/ui';

export default function Forecasts() {
  const [type, setType] = useState('FPS');
  const stock = useLoad(() => api.get(`/api/officer/ai/stock-forecast?type=${type}`).then((r) => r.data), [type]);
  const demand = useLoad(() => api.get('/api/officer/ai/demand').then((r) => r.data));

  return (
    <>
      <Panel title="Stock exhaustion forecast" icon="bi-hourglass-split" actions={
        <div className="btn-group btn-group-sm">
          {['FPS', 'WAREHOUSE'].map((t) => <button key={t} className={`btn ${type === t ? 'btn-success' : 'btn-outline-success'}`} onClick={() => setType(t)}>{t === 'FPS' ? 'Shops' : 'Warehouses'}</button>)}
        </div>}>
        <p className="small text-muted">Days left = current stock / average daily outflow over the last 30 days. No recent outflow means no estimate.</p>
        {stock.loading && !stock.data ? <Loading /> : <ErrorBox error={stock.error} />}
        {stock.data && (stock.data.length === 0 ? <Empty /> : (
          <div className="table-responsive" style={{ maxHeight: 360 }}>
            <table className="table table-sm align-middle gg-table">
              <thead><tr><th>Location</th><th>Grain</th><th className="text-end">Stock</th><th className="text-end">Daily use</th><th className="text-end">Days left</th><th>Runs out</th><th>Level</th></tr></thead>
              <tbody>
                {stock.data.map((f) => (
                  <tr key={`${f.locationId}-${f.grainType}`}>
                    <td>{f.name}</td><td>{f.grainType}</td><td className="text-end">{kg(f.stockKg)}</td><td className="text-end">{kg(f.dailyUseKg)}</td>
                    <td className="text-end">{f.daysToExhaustion ?? '-'}</td><td>{f.exhaustionDate || '-'}</td><td><Badge value={f.level} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ))}
      </Panel>

      <Panel title={`Demand prediction${demand.data?.[0] ? ` for ${demand.data[0].forPeriod}` : ''}`} icon="bi-bar-chart-line" className="mt-3">
        <p className="small text-muted">Trend + seasonal model, capped by the monthly entitlement of each shop's cards. Shortfall = predicted demand minus current stock.</p>
        {demand.loading && !demand.data ? <Loading /> : <ErrorBox error={demand.error} />}
        {demand.data && (demand.data.length === 0 ? <Empty /> : (
          <div className="table-responsive" style={{ maxHeight: 360 }}>
            <table className="table table-sm align-middle gg-table">
              <thead><tr><th>Shop</th><th>Grain</th><th className="text-end">Predicted</th><th className="text-end">Requirement</th><th className="text-end">In stock</th><th className="text-end">Shortfall</th><th>Method</th></tr></thead>
              <tbody>
                {demand.data.map((d) => (
                  <tr key={`${d.fpsId}-${d.grainType}`}>
                    <td><strong>{d.shopCode}</strong></td><td>{d.grainType}</td>
                    <td className="text-end">{kg(d.predictedKg)}</td><td className="text-end">{kg(d.requirementKg)}</td><td className="text-end">{kg(d.currentStockKg)}</td>
                    <td className={`text-end ${Number(d.shortfallKg) > 0 ? 'text-danger fw-semibold' : ''}`}>{kg(d.shortfallKg)}</td>
                    <td className="small">{d.method}<div className="text-muted">{d.confidence} confidence · {d.historyMonths} mo.</div></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ))}
      </Panel>
    </>
  );
}
