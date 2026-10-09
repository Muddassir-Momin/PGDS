import { useState } from 'react';
import api, { errMsg } from '../../api';
import { ROLE_LABEL } from '../../auth';
import { Panel, Badge, Loading, ErrorBox, Flash, useLoad } from '../../components/ui';

const STAFF_ROLES = ['GOVT_OFFICER', 'WAREHOUSE_MANAGER', 'FPS_DEALER', 'AUDITOR', 'SUPER_ADMIN'];
const EMPTY = { username: '', password: '', fullName: '', email: '', role: 'GOVT_OFFICER', warehouseId: '', fpsId: '' };

export default function AdminTools() {
  const [f, setF] = useState(EMPTY);
  const [flash, setFlash] = useState(null);
  const [demoShop, setDemoShop] = useState('');
  const users = useLoad(() => api.get('/api/admin/users').then((r) => r.data));
  const whs = useLoad(() => api.get('/api/admin/warehouses').then((r) => r.data));
  const shops = useLoad(() => api.get('/api/admin/fps').then((r) => r.data));
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  async function create(e) {
    e.preventDefault();
    try {
      await api.post('/api/admin/users', { ...f, email: f.email || null, warehouseId: f.warehouseId ? Number(f.warehouseId) : null, fpsId: f.fpsId ? Number(f.fpsId) : null });
      setFlash({ type: 'success', text: 'User created.' }); setF(EMPTY); users.reload();
    } catch (err) { setFlash({ type: 'danger', text: errMsg(err) }); }
  }
  async function demo() {
    if (!window.confirm('Generate synthetic distribution history with injected anomalies for this shop? (demo/testing only)')) return;
    try {
      const r = (await api.post(`/api/admin/ai/demo-history?fpsId=${demoShop || shops.data?.[0]?.id || 1}`)).data;
      setFlash({ type: 'success', text: `Created ${r.cards} demo cards, ${r.distributions} distributions (${r.injectedAnomalies} injected anomalies). Now run the analysis on the AI Alerts page.` });
    } catch (err) { setFlash({ type: 'danger', text: errMsg(err) }); }
  }

  return (
    <>
      <Flash flash={flash} onClose={() => setFlash(null)} />
      <Panel title="Create staff account" icon="bi-person-plus">
        <form onSubmit={create} className="row g-2">
          <div className="col-md-3"><input className="form-control" placeholder="Full name" value={f.fullName} onChange={set('fullName')} required /></div>
          <div className="col-md-3"><input className="form-control" placeholder="Username" minLength={4} maxLength={30} value={f.username} onChange={set('username')} required /></div>
          <div className="col-md-3"><input type="password" className="form-control" placeholder="Password (min 8)" minLength={8} value={f.password} onChange={set('password')} required /></div>
          <div className="col-md-3"><input type="email" className="form-control" placeholder="Email (optional)" value={f.email} onChange={set('email')} /></div>
          <div className="col-md-3"><select className="form-select" value={f.role} onChange={set('role')}>{STAFF_ROLES.map((r) => <option key={r} value={r}>{ROLE_LABEL[r]}</option>)}</select></div>
          {f.role === 'WAREHOUSE_MANAGER' && <div className="col-md-3"><select className="form-select" value={f.warehouseId} onChange={set('warehouseId')} required><option value="">Warehouse...</option>{(whs.data || []).map((w) => <option key={w.id} value={w.id}>{w.name}</option>)}</select></div>}
          {f.role === 'FPS_DEALER' && <div className="col-md-3"><select className="form-select" value={f.fpsId} onChange={set('fpsId')} required><option value="">Shop...</option>{(shops.data || []).map((s) => <option key={s.id} value={s.id}>{s.shopCode} - {s.name}</option>)}</select></div>}
          <div className="col-12"><button className="btn btn-success">Create user</button></div>
        </form>
      </Panel>

      <Panel title="Users" icon="bi-people" className="mt-3">
        {users.loading && !users.data ? <Loading /> : <ErrorBox error={users.error} />}
        {users.data && (
          <div className="table-responsive">
            <table className="table table-sm align-middle gg-table">
              <thead><tr><th>Username</th><th>Name</th><th>Role</th><th>Warehouse</th><th>Shop</th></tr></thead>
              <tbody>{users.data.map((u) => (
                <tr key={u.id}><td>{u.username}</td><td>{u.fullName}</td><td><Badge value={u.role} tone="success" /></td><td>{u.warehouseId ?? '-'}</td><td>{u.fpsId ?? '-'}</td></tr>
              ))}</tbody>
            </table>
          </div>
        )}
      </Panel>

      <Panel title="AI demo data (development only)" icon="bi-flask" className="mt-3">
        <p className="small text-muted">Creates 60 demo cards and about 5 months of back-dated distributions for one shop, plus known anomalies (night-time issues and a one-hour burst), so the AI features have something to analyse.</p>
        <div className="d-flex gap-2 align-items-center">
          <select className="form-select" style={{ maxWidth: 260 }} value={demoShop} onChange={(e) => setDemoShop(e.target.value)}>
            <option value="">First shop</option>{(shops.data || []).map((s) => <option key={s.id} value={s.id}>{s.shopCode} - {s.name}</option>)}
          </select>
          <button className="btn btn-outline-success" onClick={demo}>Generate demo history</button>
        </div>
      </Panel>
    </>
  );
}
