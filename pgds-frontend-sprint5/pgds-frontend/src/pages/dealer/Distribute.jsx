import { useState } from 'react';
import api, { errMsg } from '../../api';
import { useAuth } from '../../auth';
import { Panel, Flash, Loading, ErrorBox, Empty, Pager, useLoad, kg, dt } from '../../components/ui';

export default function Distribute() {
  const { user } = useAuth();
  const id = user.fpsId;
  const [cardNo, setCardNo] = useState('');
  const [bal, setBal] = useState(null);
  const [qty, setQty] = useState({ RICE: '', WHEAT: '' });
  const [receipt, setReceipt] = useState(null);
  const [flash, setFlash] = useState(null);
  const [page, setPage] = useState(0);
  const recent = useLoad(() => (id ? api.get(`/api/fps/${id}/distributions?page=${page}&size=8`).then((r) => r.data) : Promise.resolve(null)), [id, page]);

  if (!id) return <div className="alert alert-warning">Your account is not linked to a shop.</div>;

  async function check(e) {
    e.preventDefault();
    setReceipt(null); setBal(null); setFlash(null);
    try {
      const b = (await api.get(`/api/fps/${id}/cards/${encodeURIComponent(cardNo.trim())}/balance`)).data;
      setBal(b);
      setQty(Object.fromEntries(b.grains.map((g) => [g.grainType, g.remainingKg])));
    } catch (err) { setFlash({ type: 'danger', text: errMsg(err) }); }
  }

  async function issue(e) {
    e.preventDefault();
    const items = Object.entries(qty).filter(([, v]) => Number(v) > 0).map(([g, v]) => ({ grainType: g, quantityKg: Number(v) }));
    if (items.length === 0) { setFlash({ type: 'warning', text: 'Enter a quantity for at least one grain.' }); return; }
    try {
      const r = (await api.post(`/api/fps/${id}/distributions`, { cardNumber: bal.cardNumber, items })).data;
      setReceipt(r); setBal(null); setCardNo(''); setFlash(null); recent.reload();
    } catch (err) { setFlash({ type: 'danger', text: errMsg(err) }); }
  }

  return (
    <>
      <Flash flash={flash} onClose={() => setFlash(null)} />
      <div className="row g-3">
        <div className="col-12 col-lg-6">
          <Panel title="Distribute grain" icon="bi-box-arrow-up-right">
            <form onSubmit={check} className="input-group mb-3">
              <input className="form-control" placeholder="Ration card number" value={cardNo} onChange={(e) => setCardNo(e.target.value)} required />
              <button className="btn btn-success">Check entitlement</button>
            </form>
            {bal && (
              <form onSubmit={issue}>
                <div className="mb-2"><strong>{bal.headOfFamily}</strong> <span className="text-muted">· {bal.cardNumber} · {bal.period}</span></div>
                {!bal.eligible && <div className="alert alert-danger py-2">This card is not eligible.</div>}
                {bal.grains.map((g) => (
                  <div className="gg-grain-row" key={g.grainType}>
                    <div className="flex-grow-1">
                      <div className="fw-semibold">{g.grainType}</div>
                      <div className="small text-muted">Entitled {kg(g.entitledKg)} · received {kg(g.distributedKg)} · <strong>remaining {kg(g.remainingKg)}</strong></div>
                      <div className="progress mt-1" style={{ height: 5 }}><div className="progress-bar bg-success" style={{ width: `${Number(g.entitledKg) ? (Number(g.distributedKg) / Number(g.entitledKg)) * 100 : 0}%` }} /></div>
                    </div>
                    <input type="number" min="0" step="0.01" max={g.remainingKg} className="form-control" style={{ width: 110 }}
                      value={qty[g.grainType] ?? ''} onChange={(e) => setQty({ ...qty, [g.grainType]: e.target.value })} disabled={Number(g.remainingKg) <= 0} />
                  </div>
                ))}
                <button className="btn btn-success w-100 mt-3" disabled={!bal.eligible}>Issue grain and generate receipt</button>
              </form>
            )}
          </Panel>
        </div>

        <div className="col-12 col-lg-6">
          {receipt ? (
            <Panel title="Receipt" icon="bi-receipt" actions={<button className="btn btn-sm btn-outline-success" onClick={() => window.print()}><i className="bi bi-printer" /> Print</button>}>
              <div className="receipt">
                <div className="text-center mb-2"><strong>GrainGuard AI · PDS Receipt</strong><div className="small">{receipt.shopCode} - {receipt.shopName}</div></div>
                <table className="table table-sm mb-2"><tbody>
                  <tr><td>Receipt no.</td><td className="text-end"><strong>{receipt.receiptNo}</strong></td></tr>
                  <tr><td>Ration card</td><td className="text-end">{receipt.cardNumber}</td></tr>
                  <tr><td>Head of family</td><td className="text-end">{receipt.headOfFamily}</td></tr>
                  <tr><td>Period</td><td className="text-end">{receipt.period}</td></tr>
                  <tr><td>Date</td><td className="text-end">{dt(receipt.distributedAt)}</td></tr>
                </tbody></table>
                <table className="table table-sm"><thead><tr><th>Grain</th><th className="text-end">Quantity</th></tr></thead>
                  <tbody>{receipt.items.map((i) => <tr key={i.grainType}><td>{i.grainType}</td><td className="text-end">{kg(i.quantityKg)}</td></tr>)}</tbody></table>
                <div className="small text-muted">Issued by {receipt.distributedBy}</div>
              </div>
            </Panel>
          ) : (
            <Panel title="Recent distributions at this shop" icon="bi-clock-history">
              {recent.loading && !recent.data ? <Loading /> : <ErrorBox error={recent.error} />}
              {recent.data && (recent.data.items.length === 0 ? <Empty text="No distributions yet." /> : (
                <div className="table-responsive">
                  <table className="table table-sm gg-table">
                    <thead><tr><th>When</th><th>Card</th><th>Grain</th><th className="text-end">Kg</th></tr></thead>
                    <tbody>{recent.data.items.map((d) => <tr key={d.id}><td className="text-nowrap">{dt(d.distributedAt)}</td><td>{d.cardNumber}</td><td>{d.grainType}</td><td className="text-end">{kg(d.quantityKg)}</td></tr>)}</tbody>
                  </table>
                </div>
              ))}
              {recent.data && <Pager page={page} totalPages={recent.data.totalPages} onPage={setPage} />}
            </Panel>
          )}
        </div>
      </div>
    </>
  );
}
