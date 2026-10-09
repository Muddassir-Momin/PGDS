import { useState } from 'react';
import api, { errMsg } from '../../api';
import { Panel, Badge, Loading, ErrorBox, Empty, Flash, Pager, useLoad, kg } from '../../components/ui';

const EMPTY = { cardNumber: '', headOfFamily: '', address: '', district: 'Pune', category: 'PHH', familyMembers: 4, fpsId: '' };
const EMPTY_MEMBER = { name: '', age: '', aadhaarLast4: '', linkUsername: '' };

export default function Cards() {
  const [page, setPage] = useState(0);
  const [district, setDistrict] = useState('');
  const [show, setShow] = useState(false);
  const [form, setForm] = useState(EMPTY);
  const [flash, setFlash] = useState(null);
  const [selected, setSelected] = useState(null);
  const [member, setMember] = useState(EMPTY_MEMBER);

  const list = useLoad(() => api.get(`/api/officer/ration-cards?page=${page}&size=10${district ? `&district=${encodeURIComponent(district)}` : ''}`).then((r) => r.data), [page, district]);
  const shops = useLoad(() => api.get('/api/officer/fps').then((r) => r.data));
  const members = useLoad(() => (selected ? api.get(`/api/officer/beneficiaries?cardId=${selected.id}`).then((r) => r.data) : Promise.resolve([])), [selected?.id]);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });
  const ok = (text) => setFlash({ type: 'success', text });
  const bad = (e) => setFlash({ type: 'danger', text: errMsg(e) });

  async function createCard(e) {
    e.preventDefault();
    try {
      await api.post('/api/officer/ration-cards', { ...form, familyMembers: Number(form.familyMembers), fpsId: form.fpsId ? Number(form.fpsId) : null });
      ok('Ration card created.'); setForm(EMPTY); setShow(false); list.reload();
    } catch (err) { bad(err); }
  }
  async function toggle(c) {
    try { await api.patch(`/api/officer/ration-cards/${c.id}/eligibility?eligible=${!c.eligible}`); list.reload(); }
    catch (err) { bad(err); }
  }
  async function addMember(e) {
    e.preventDefault();
    try {
      await api.post('/api/officer/beneficiaries', { name: member.name, age: Number(member.age), aadhaarLast4: member.aadhaarLast4, rationCardId: selected.id, linkUsername: member.linkUsername || null });
      ok('Member added.'); setMember(EMPTY_MEMBER); members.reload();
    } catch (err) { bad(err); }
  }
  async function removeMember(id) {
    if (!window.confirm('Remove this member?')) return;
    try { await api.delete(`/api/officer/beneficiaries/${id}`); members.reload(); } catch (err) { bad(err); }
  }

  return (
    <>
      <Flash flash={flash} onClose={() => setFlash(null)} />
      <Panel title="Ration Cards" icon="bi-card-list" actions={
        <>
          <input className="form-control form-control-sm" style={{ width: 150 }} placeholder="Filter by district" value={district} onChange={(e) => { setPage(0); setDistrict(e.target.value); }} />
          <button className="btn btn-sm btn-success" onClick={() => setShow(!show)}><i className="bi bi-plus-lg" /> New card</button>
        </>}>
        {show && (
          <form onSubmit={createCard} className="row g-2 mb-3 p-3 bg-light rounded">
            <div className="col-md-4"><input className="form-control" placeholder="Card number" value={form.cardNumber} onChange={set('cardNumber')} required /></div>
            <div className="col-md-4"><input className="form-control" placeholder="Head of family" value={form.headOfFamily} onChange={set('headOfFamily')} required /></div>
            <div className="col-md-4"><input className="form-control" placeholder="District" value={form.district} onChange={set('district')} required /></div>
            <div className="col-md-6"><input className="form-control" placeholder="Address" value={form.address} onChange={set('address')} /></div>
            <div className="col-6 col-md-2"><select className="form-select" value={form.category} onChange={set('category')}><option>PHH</option><option>AAY</option></select></div>
            <div className="col-6 col-md-2"><input type="number" min="1" max="30" className="form-control" value={form.familyMembers} onChange={set('familyMembers')} required /></div>
            <div className="col-md-2">
              <select className="form-select" value={form.fpsId} onChange={set('fpsId')}>
                <option value="">Shop...</option>
                {(shops.data || []).map((s) => <option key={s.id} value={s.id}>{s.shopCode}</option>)}
              </select>
            </div>
            <div className="col-12"><button className="btn btn-success btn-sm">Save card</button></div>
          </form>
        )}
        {list.loading && !list.data ? <Loading /> : <ErrorBox error={list.error} />}
        {list.data && (list.data.items.length === 0 ? <Empty text="No ration cards found." /> : (
          <div className="table-responsive">
            <table className="table table-sm align-middle gg-table">
              <thead><tr><th>Card</th><th>Head of family</th><th>District</th><th>Cat.</th><th>Members</th><th>Monthly entitlement</th><th>Status</th><th /></tr></thead>
              <tbody>
                {list.data.items.map((c) => (
                  <tr key={c.id} className={selected?.id === c.id ? 'table-active' : ''}>
                    <td><strong>{c.cardNumber}</strong></td>
                    <td>{c.headOfFamily}</td><td>{c.district}</td><td>{c.category}</td><td>{c.familyMembers}</td>
                    <td className="small">{kg(c.monthlyEntitlement.riceKg)} rice + {kg(c.monthlyEntitlement.wheatKg)} wheat</td>
                    <td><Badge value={c.eligible ? 'OK' : 'REJECTED'} tone={c.eligible ? 'success' : 'secondary'} />{' '}{c.eligible ? 'Eligible' : 'Inactive'}</td>
                    <td className="text-nowrap">
                      <button className="btn btn-sm btn-outline-success me-1" onClick={() => setSelected(c)}>Members</button>
                      <button className="btn btn-sm btn-outline-secondary" onClick={() => toggle(c)}>{c.eligible ? 'Deactivate' : 'Activate'}</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ))}
        {list.data && <Pager page={page} totalPages={list.data.totalPages} onPage={setPage} />}
      </Panel>

      {selected && (
        <Panel title={`Members of ${selected.cardNumber} (${selected.familyMembers} allowed)`} icon="bi-people" className="mt-3"
          actions={<button className="btn btn-sm btn-outline-secondary" onClick={() => setSelected(null)}>Close</button>}>
          {members.loading && !members.data ? <Loading /> : <ErrorBox error={members.error} />}
          {members.data && (members.data.length === 0 ? <Empty text="No members registered yet." /> : (
            <ul className="list-group mb-3">
              {members.data.map((m) => (
                <li key={m.id} className="list-group-item d-flex justify-content-between align-items-center">
                  <span><strong>{m.name}</strong> · age {m.age} · {m.aadhaarMasked}</span>
                  <button className="btn btn-sm btn-outline-danger" onClick={() => removeMember(m.id)}>Remove</button>
                </li>
              ))}
            </ul>
          ))}
          <form onSubmit={addMember} className="row g-2">
            <div className="col-md-3"><input className="form-control" placeholder="Name" value={member.name} onChange={(e) => setMember({ ...member, name: e.target.value })} required /></div>
            <div className="col-6 col-md-2"><input type="number" min="0" max="120" className="form-control" placeholder="Age" value={member.age} onChange={(e) => setMember({ ...member, age: e.target.value })} required /></div>
            <div className="col-6 col-md-2"><input className="form-control" placeholder="Aadhaar last 4" pattern="\d{4}" maxLength={4} value={member.aadhaarLast4} onChange={(e) => setMember({ ...member, aadhaarLast4: e.target.value })} required /></div>
            <div className="col-md-3"><input className="form-control" placeholder="Link login username (optional)" value={member.linkUsername} onChange={(e) => setMember({ ...member, linkUsername: e.target.value })} /></div>
            <div className="col-md-2"><button className="btn btn-success w-100">Add member</button></div>
          </form>
        </Panel>
      )}
    </>
  );
}
