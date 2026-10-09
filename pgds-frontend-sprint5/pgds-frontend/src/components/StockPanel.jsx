import { useState } from 'react';
import api from '../api';
import { Panel, useLoad, Loading, ErrorBox, Empty, Pager, Badge, kg, dt, GRAIN_COLOR } from './ui';

const TXN_TONE = { RECEIPT: 'success', OPENING: 'success', TRANSFER_IN: 'success', TRANSFER_OUT: 'info', DISTRIBUTION: 'info', DAMAGE: 'danger' };

/** Balances + ledger for a warehouse (/api/warehouse/{id}) or a shop (/api/fps/{id}). */
export default function StockPanel({ base, refreshKey = 0 }) {
  const [page, setPage] = useState(0);
  const stock = useLoad(() => api.get(`${base}/stock`).then((r) => r.data), [base, refreshKey]);
  const txns = useLoad(() => api.get(`${base}/transactions?page=${page}&size=10`).then((r) => r.data), [base, page, refreshKey]);

  return (
    <>
      <Panel title={stock.data ? `Stock - ${stock.data.name}` : 'Stock'} icon="bi-box-seam">
        {stock.loading && !stock.data ? <Loading /> : <ErrorBox error={stock.error} />}
        {stock.data && (
          <div className="row g-3">
            {stock.data.balances.map((b) => (
              <div className="col-12 col-md-4" key={b.grainType}>
                <div className="gg-balance" style={{ borderColor: GRAIN_COLOR[b.grainType] }}>
                  <div className="small text-muted">{b.grainType}</div>
                  <div className="fs-4 fw-semibold">{kg(b.quantityKg)}</div>
                </div>
              </div>
            ))}
          </div>
        )}
      </Panel>

      <Panel title="Stock ledger" icon="bi-journal-text" className="mt-3">
        {txns.loading && !txns.data ? <Loading /> : <ErrorBox error={txns.error} />}
        {txns.data && (txns.data.items.length === 0 ? <Empty text="No stock movements yet." /> : (
          <div className="table-responsive">
            <table className="table table-sm align-middle gg-table">
              <thead><tr><th>When</th><th>Type</th><th>Grain</th><th className="text-end">Quantity</th><th>By</th><th>Remarks</th></tr></thead>
              <tbody>
                {txns.data.items.map((t) => (
                  <tr key={t.id}>
                    <td className="text-nowrap">{dt(t.createdAt)}</td>
                    <td><Badge value={t.txnType} tone={TXN_TONE[t.txnType]} /></td>
                    <td>{t.grainType}</td>
                    <td className="text-end">{kg(t.quantityKg)}</td>
                    <td>{t.createdBy}</td>
                    <td className="text-muted small">{t.remarks}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ))}
        {txns.data && <Pager page={page} totalPages={txns.data.totalPages} onPage={setPage} />}
      </Panel>
    </>
  );
}
