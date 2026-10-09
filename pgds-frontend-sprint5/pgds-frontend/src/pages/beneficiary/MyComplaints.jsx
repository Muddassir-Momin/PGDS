import { useState } from 'react';
import api, { errMsg } from '../../api';
import { Panel, Badge, Loading, ErrorBox, Empty, Flash, Pager, useLoad, dt } from '../../components/ui';

export default function MyComplaints() {
  const [page, setPage] = useState(0);
  const [text, setText] = useState('');
  const [fpsId, setFpsId] = useState('');
  const [flash, setFlash] = useState(null);
  const list = useLoad(() => api.get(`/api/beneficiary/complaints?page=${page}&size=10`).then((r) => r.data), [page]);
  const shops = useLoad(() => api.get('/api/beneficiary/shops').then((r) => r.data).catch(() => []));

  async function submit(e) {
    e.preventDefault();
    try {
      const c = (await api.post('/api/beneficiary/complaints', { description: text, fpsId: fpsId ? Number(fpsId) : null })).data;
      setFlash({ type: 'success', text: `Complaint #${c.id} submitted. Categorised as ${c.category} (${c.priority} priority) and routed to ${c.department}.` });
      setText(''); setFpsId(''); setPage(0); list.reload();
    } catch (err) { setFlash({ type: 'danger', text: errMsg(err) }); }
  }

  return (
    <>
      <Flash flash={flash} onClose={() => setFlash(null)} />
      <Panel title="Submit a complaint" icon="bi-chat-left-dots">
        <form onSubmit={submit}>
          <textarea className="form-control mb-2" rows={3} minLength={10} maxLength={2000} placeholder="Describe the problem (at least 10 characters)" value={text} onChange={(e) => setText(e.target.value)} required />
          <div className="d-flex gap-2 flex-wrap">
            <select className="form-select" style={{ maxWidth: 280 }} value={fpsId} onChange={(e) => setFpsId(e.target.value)}>
              <option value="">Related shop (optional)</option>{(shops.data || []).map((s) => <option key={s.id} value={s.id}>{s.shopCode} - {s.name}</option>)}
            </select>
            <button className="btn btn-success">Submit</button>
          </div>
        </form>
      </Panel>

      <Panel title="My complaints" icon="bi-list-check" className="mt-3">
        {list.loading && !list.data ? <Loading /> : <ErrorBox error={list.error} />}
        {list.data && (list.data.items.length === 0 ? <Empty text="You have not submitted any complaints." /> : list.data.items.map((c) => (
          <div key={c.id} className="gg-insight">
            <Badge value={c.status} />
            <div>
              <div>{c.description}</div>
              <div className="small text-muted">#{c.id} · {dt(c.createdAt)} · {c.category} · {c.department}</div>
              {c.resolutionNote && <div className="small text-success">Officer note: {c.resolutionNote}</div>}
            </div>
          </div>
        )))}
        {list.data && <Pager page={page} totalPages={list.data.totalPages} onPage={setPage} />}
      </Panel>
    </>
  );
}
