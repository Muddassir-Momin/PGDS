import { useState } from 'react';
import api, { errMsg } from '../../api';
import { useAuth } from '../../auth';
import StockPanel from '../../components/StockPanel';
import { Panel, Flash, GRAINS } from '../../components/ui';

export default function ShopStock() {
  const { user } = useAuth();
  const id = user.fpsId;
  const [f, setF] = useState({ grainType: 'RICE', quantityKg: '', remarks: '' });
  const [flash, setFlash] = useState(null);
  const [refresh, setRefresh] = useState(0);
  if (!id) return <div className="alert alert-warning">Your account is not linked to a shop.</div>;

  async function damage(e) {
    e.preventDefault();
    try {
      await api.post(`/api/fps/${id}/damage`, { grainType: f.grainType, quantityKg: Number(f.quantityKg), remarks: f.remarks || null });
      setFlash({ type: 'success', text: 'Damage recorded.' }); setF({ ...f, quantityKg: '', remarks: '' }); setRefresh((n) => n + 1);
    } catch (err) { setFlash({ type: 'danger', text: errMsg(err) }); }
  }

  return (
    <>
      <Flash flash={flash} onClose={() => setFlash(null)} />
      <StockPanel base={`/api/fps/${id}`} refreshKey={refresh} />
      <Panel title="Report damaged stock" icon="bi-exclamation-octagon" className="mt-3">
        <form onSubmit={damage} className="row g-2">
          <div className="col-6 col-md-2"><select className="form-select" value={f.grainType} onChange={(e) => setF({ ...f, grainType: e.target.value })}>{GRAINS.map((g) => <option key={g}>{g}</option>)}</select></div>
          <div className="col-6 col-md-2"><input type="number" min="0.01" step="0.01" className="form-control" placeholder="Kg" value={f.quantityKg} onChange={(e) => setF({ ...f, quantityKg: e.target.value })} required /></div>
          <div className="col-md-5"><input className="form-control" placeholder="What happened?" maxLength={255} value={f.remarks} onChange={(e) => setF({ ...f, remarks: e.target.value })} /></div>
          <div className="col-md-3"><button className="btn btn-outline-danger w-100">Record damage</button></div>
        </form>
      </Panel>
    </>
  );
}
