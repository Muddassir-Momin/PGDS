import api from '../../api';
import { Panel, Badge, Loading, ErrorBox, Empty, useLoad, kg, dt } from '../../components/ui';

const get = (u) => () => api.get(u).then((r) => r.data);

export default function MyRation() {
  const me = useLoad(get('/api/beneficiary/me'));
  const ent = useLoad(get('/api/beneficiary/entitlement'));
  const hist = useLoad(get('/api/beneficiary/distributions'));
  const shops = useLoad(get('/api/beneficiary/shops'));

  if (me.loading && !me.data) return <Loading />;
  if (me.error) return (
    <div className="alert alert-info">
      <i className="bi bi-info-circle me-2" />{me.error}. Please ask your Government Officer to link your login to your ration card.
    </div>
  );
  const card = me.data.card;

  return (
    <div className="row g-3">
      <div className="col-12 col-lg-6">
        <Panel title="My ration card" icon="bi-person-vcard">
          <div className="fs-5 fw-semibold">{card.cardNumber}</div>
          <div className="text-muted mb-2">{card.headOfFamily} · {card.district}</div>
          <div className="mb-2"><Badge value={card.category} tone="success" /> <Badge value={card.eligible ? 'OK' : 'REJECTED'} tone={card.eligible ? 'success' : 'secondary'} /> {card.eligible ? 'Eligible' : 'Not eligible'}</div>
          <div className="small text-muted mb-1">Family members ({card.familyMembers} allowed)</div>
          <ul className="mb-0">{me.data.familyMembers.map((m) => <li key={m.id}>{m.name}, age {m.age}</li>)}</ul>
        </Panel>
      </div>

      <div className="col-12 col-lg-6">
        <Panel title={`This month's entitlement${ent.data ? ` (${ent.data.period})` : ''}`} icon="bi-basket2">
          {ent.loading && !ent.data ? <Loading /> : <ErrorBox error={ent.error} />}
          {ent.data && ent.data.grains.map((g) => {
            const pct = Number(g.entitledKg) ? (Number(g.distributedKg) / Number(g.entitledKg)) * 100 : 0;
            return (
              <div className="mb-3" key={g.grainType}>
                <div className="d-flex justify-content-between"><strong>{g.grainType}</strong><span>{kg(g.remainingKg)} remaining</span></div>
                <div className="progress" style={{ height: 10 }}><div className="progress-bar bg-success" style={{ width: `${pct}%` }} /></div>
                <div className="small text-muted">Received {kg(g.distributedKg)} of {kg(g.entitledKg)}</div>
              </div>
            );
          })}
        </Panel>
      </div>

      <div className="col-12 col-lg-7">
        <Panel title="Distribution history" icon="bi-clock-history">
          {hist.loading && !hist.data ? <Loading /> : <ErrorBox error={hist.error} />}
          {hist.data && (hist.data.length === 0 ? <Empty text="No grain received yet." /> : (
            <div className="table-responsive">
              <table className="table table-sm gg-table">
                <thead><tr><th>Receipt</th><th>Date</th><th>Shop</th><th>Items</th></tr></thead>
                <tbody>{hist.data.map((r) => (
                  <tr key={r.receiptNo}><td><strong>{r.receiptNo}</strong></td><td className="text-nowrap">{dt(r.distributedAt)}</td><td>{r.shopCode}</td>
                    <td>{r.items.map((i) => `${i.grainType} ${kg(i.quantityKg)}`).join(', ')}</td></tr>
                ))}</tbody>
              </table>
            </div>
          ))}
        </Panel>
      </div>

      <div className="col-12 col-lg-5">
        <Panel title="Ration shops in my district" icon="bi-shop">
          {shops.loading && !shops.data ? <Loading /> : <ErrorBox error={shops.error} />}
          {shops.data && (shops.data.length === 0 ? <Empty text="No shops found." /> : (
            <ul className="list-group list-group-flush">
              {shops.data.map((s) => <li key={s.id} className="list-group-item px-0"><strong>{s.shopCode}</strong> · {s.name}<div className="small text-muted">{s.address}</div></li>)}
            </ul>
          ))}
        </Panel>
      </div>
    </div>
  );
}
