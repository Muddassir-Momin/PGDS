import { useState } from 'react';
import api, { errMsg } from '../../api';
import { Panel, Badge, Loading, ErrorBox, Empty, Flash, Pager, useLoad, dt } from '../../components/ui';

const STATUSES = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'REJECTED'];
const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH'];

export default function Complaints() {
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [edit, setEdit] = useState(null);
  const [flash, setFlash] = useState(null);
  const list = useLoad(() => api.get(`/api/officer/complaints?page=${page}&size=10${status ? `&status=${status}` : ''}`).then((r) => r.data), [page, status]);

  async function save(e) {
    e.preventDefault();
    try {
      await api.patch(`/api/officer/complaints/${edit.id}`, {
        status: edit.status, priority: edit.priority, department: edit.department, category: edit.category, resolutionNote: edit.resolutionNote || null,
      });
      setFlash({ type: 'success', text: 'Complaint updated.' }); setEdit(null); list.reload();
    } catch (err) { setFlash({ type: 'danger', text: errMsg(err) }); }
  }

  return (
    <>
      <Flash flash={flash} onClose={() => setFlash(null)} />
      <Panel title="Complaints (auto-classified by AI)" icon="bi-chat-left-text" actions={
        <select className="form-select form-select-sm" style={{ width: 160 }} value={status} onChange={(e) => { setPage(0); setStatus(e.target.value); }}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s}>{s}</option>)}
        </select>}>
        {list.loading && !list.data ? <Loading /> : <ErrorBox error={list.error} />}
        {list.data && (list.data.items.length === 0 ? <Empty text="No complaints." /> : (
          <div className="table-responsive">
            <table className="table table-sm align-middle gg-table">
              <thead><tr><th>#</th><th>Complaint</th><th>Category</th><th>Priority</th><th>Department</th><th>Status</th><th /></tr></thead>
              <tbody>
                {list.data.items.map((c) => (
                  <tr key={c.id}>
                    <td>{c.id}</td>
                    <td style={{ maxWidth: 320 }}>{c.description}<div className="small text-muted">{dt(c.createdAt)}{c.fpsId ? ` · shop #${c.fpsId}` : ''}</div>
                      {c.resolutionNote && <div className="small text-success">Note: {c.resolutionNote}</div>}</td>
                    <td>{c.category}</td><td><Badge value={c.priority} /></td><td>{c.department}</td><td><Badge value={c.status} /></td>
                    <td><button className="btn btn-sm btn-outline-success" onClick={() => setEdit({ ...c })}>Review</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ))}
        {list.data && <Pager page={page} totalPages={list.data.totalPages} onPage={setPage} />}
      </Panel>

      {edit && (
        <Panel title={`Review complaint #${edit.id}`} icon="bi-pencil-square" className="mt-3">
          <p className="text-muted">{edit.description}</p>
          <form onSubmit={save} className="row g-2">
            <div className="col-md-3"><label className="form-label small">Status</label>
              <select className="form-select" value={edit.status} onChange={(e) => setEdit({ ...edit, status: e.target.value })}>{STATUSES.map((s) => <option key={s}>{s}</option>)}</select></div>
            <div className="col-md-3"><label className="form-label small">Priority</label>
              <select className="form-select" value={edit.priority || 'LOW'} onChange={(e) => setEdit({ ...edit, priority: e.target.value })}>{PRIORITIES.map((s) => <option key={s}>{s}</option>)}</select></div>
            <div className="col-md-3"><label className="form-label small">Category</label>
              <input className="form-control" value={edit.category || ''} onChange={(e) => setEdit({ ...edit, category: e.target.value })} /></div>
            <div className="col-md-3"><label className="form-label small">Department</label>
              <input className="form-control" value={edit.department || ''} onChange={(e) => setEdit({ ...edit, department: e.target.value })} /></div>
            <div className="col-12"><label className="form-label small">Resolution note</label>
              <input className="form-control" maxLength={1000} value={edit.resolutionNote || ''} onChange={(e) => setEdit({ ...edit, resolutionNote: e.target.value })} /></div>
            <div className="col-12 d-flex gap-2"><button className="btn btn-success">Save</button><button type="button" className="btn btn-outline-secondary" onClick={() => setEdit(null)}>Cancel</button></div>
          </form>
        </Panel>
      )}
    </>
  );
}
