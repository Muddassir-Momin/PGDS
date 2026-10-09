import { useCallback, useEffect, useState } from 'react';
import { errMsg } from '../api';

export const kg = (v) => (v == null ? '-' : Number(v).toLocaleString('en-IN', { maximumFractionDigits: 2 }) + ' kg');
export const num = (v) => (v == null ? '-' : Number(v).toLocaleString('en-IN'));
export const dt = (v) => (v ? new Date(v).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : '-');

/** Loads data on mount and whenever deps change. */
export function useLoad(fn, deps = []) {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const reload = useCallback(async () => {
    setLoading(true);
    try { setData(await fn()); setError(''); }
    catch (e) { setError(errMsg(e)); }
    finally { setLoading(false); }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);
  useEffect(() => { reload(); }, [reload]);
  return { data, error, loading, reload };
}

export function Panel({ title, icon, actions, children, className = '', bodyClass = '' }) {
  return (
    <div className={`gg-panel ${className}`}>
      {(title || actions) && (
        <div className="gg-panel-head">
          <div className="gg-panel-title">{icon && <i className={`bi ${icon} me-2`} />}{title}</div>
          <div className="d-flex gap-2 align-items-center">{actions}</div>
        </div>
      )}
      <div className={`gg-panel-body ${bodyClass}`}>{children}</div>
    </div>
  );
}

export function StatCard({ icon, tone = 'green', label, value, sub, children }) {
  return (
    <div className="gg-stat">
      <div className={`gg-stat-icon tone-${tone}`}><i className={`bi ${icon}`} /></div>
      <div className="flex-grow-1">
        <div className="gg-stat-label">{label}</div>
        <div className="gg-stat-value">{value}</div>
        {sub && <div className="gg-stat-sub">{sub}</div>}
        {children}
      </div>
    </div>
  );
}

const TONES = {
  HIGH: 'danger', CRITICAL: 'danger', OPEN: 'danger',
  MEDIUM: 'warning', WARNING: 'warning', UNDER_REVIEW: 'warning', IN_PROGRESS: 'warning',
  LOW: 'secondary', WATCH: 'info', ADEQUATE: 'info',
  OK: 'success', HEALTHY: 'success', RESOLVED: 'success',
  DISMISSED: 'secondary', REJECTED: 'secondary',
};
export function Badge({ value, tone }) {
  if (!value) return <span className="text-muted">-</span>;
  return <span className={`badge text-bg-${tone || TONES[value] || 'secondary'}`}>{String(value).replace('_', ' ')}</span>;
}

export const Loading = () => (
  <div className="text-center text-muted py-4"><span className="spinner-border spinner-border-sm me-2" />Loading...</div>
);
export const ErrorBox = ({ error }) => (error ? <div className="alert alert-danger py-2 my-2">{error}</div> : null);
export const Empty = ({ text = 'Nothing to show yet.' }) => <div className="text-center text-muted py-4">{text}</div>;

export function Flash({ flash, onClose }) {
  if (!flash) return null;
  return (
    <div className={`alert alert-${flash.type} alert-dismissible py-2`} role="alert">
      {flash.text}
      <button type="button" className="btn-close" onClick={onClose} />
    </div>
  );
}

export function Pager({ page, totalPages, onPage }) {
  if (!totalPages || totalPages <= 1) return null;
  return (
    <div className="d-flex justify-content-between align-items-center mt-2">
      <button className="btn btn-sm btn-outline-secondary" disabled={page === 0} onClick={() => onPage(page - 1)}>Previous</button>
      <span className="small text-muted">Page {page + 1} of {totalPages}</span>
      <button className="btn btn-sm btn-outline-secondary" disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>Next</button>
    </div>
  );
}

export const GRAINS = ['RICE', 'WHEAT', 'OTHER'];
export const GRAIN_COLOR = { RICE: '#137a43', WHEAT: '#f59e0b', OTHER: '#3b82f6' };
