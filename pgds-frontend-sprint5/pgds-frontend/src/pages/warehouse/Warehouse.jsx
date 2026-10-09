import { useState } from 'react';
import api, { errMsg } from '../../api';
import { useAuth } from '../../auth';
import StockPanel from '../../components/StockPanel';
import { Panel, Flash, useLoad, GRAINS } from '../../components/ui';

const blank = { grainType: 'RICE', quantityKg: '', remarks: '', fpsId: '' };

export default function Warehouse() {
  const { user } = useAuth();
  const id = user.warehouseId;
  const base = `/api/warehouse/${id}`;
  const [tab, setTab] = useState('receipts');
  const [f, setF] = useState(blank);
  const [flash, setFlash] = useState(null);
  const [refresh, setRefresh] = useState(0);
  const shops = useLoad(() => (id ? api.get(`${base}/shops`).then((r) => r.data) : Promise.resolve([])), [id]);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  if (!id) return <div className="alert alert-warning">Your account is not linked to a warehouse. Ask the administrator to assign one.</div>;

  async function submit(e) {
    e.preventDefault();
    const body = { grainType: f.grainType, quantityKg: Number(f.quantityKg), remarks: f.remarks || null };
    try {
      if (tab === 'transfers') await api.post(`${base}/transfers`, { ...body, fpsId: Number(f.fpsId) });
      else await api.post(`${base}/${tab}`, body);
      setFlash({ type: 'success', text: 'Recorded.' }); setF({ ...blank, grainType: f.grainType }); setRefresh((n) => n + 1);
    } catch (err) { setFlash({ type: 'danger', text: errMsg(err) }); }
  }

  const titles = { receipts: 'Receive stock', transfers: 'Transfer to shop', damage: 'Report damage' };
  return (
    <>
      <Flash flash={flash} onClose={() => setFlash(null)} />
      <Panel title="Record stock movement" icon="bi-plus-slash-minus">
        <ul className="nav nav-pills mb-3">
          {Object.entries(titles).map(([k, v]) => <li className="nav-item" key={k}><button className={`nav-link ${tab === k ? 'active' : ''}`} onClick={() => setTab(k)}>{v}</button></li>)}
        </ul>
        <form onSubmit={submit} className="row g-2">
          {tab === 'transfers' && (
            <div className="col-md-3"><select className="form-select" value={f.fpsId} onChange={set('fpsId')} required>
              <option value="">Shop...</option>{(shops.data || []).map((s) => <option key={s.id} value={s.id}>{s.shopCode} - {s.name}</option>)}
            </select></div>
          )}
          <div className="col-6 col-md-2"><select className="form-select" value={f.grainType} onChange={set('grainType')}>{GRAINS.map((g) => <option key={g}>{g}</option>)}</select></div>
          <div className="col-6 col-md-2"><input type="number" min="0.01" step="0.01" className="form-control" placeholder="Kg" value={f.quantityKg} onChange={set('quantityKg')} required /></div>
          <div className="col-md-3"><input className="form-control" placeholder="Remarks (optional)" maxLength={255} value={f.remarks} onChange={set('remarks')} /></div>
          <div className="col-md-2"><button className="btn btn-success w-100">{titles[tab]}</button></div>
        </form>
        <div className="small text-muted mt-2">Stock is never edited directly: each entry is added to the ledger below, and the balance is calculated from it.</div>
      </Panel>
      <div className="mt-3"><StockPanel base={base} refreshKey={refresh} /></div>
    </>
  );
}
