import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth';
import { errMsg } from '../api';
import { firstPath } from '../routes';

const DEMO = [
  ['admin', 'Super Admin'], ['officer', 'Officer'], ['warehouse', 'Warehouse'],
  ['dealer', 'FPS Dealer'], ['auditor', 'Auditor'], ['beneficiary', 'Beneficiary'],
];

export default function Login() {
  const { user, login } = useAuth();
  const nav = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to={firstPath(user.role)} replace />;

  async function submit(e) {
    e.preventDefault();
    setBusy(true); setError('');
    try { const u = await login(username.trim(), password); nav(firstPath(u.role), { replace: true }); }
    catch (err) { setError(errMsg(err)); }
    finally { setBusy(false); }
  }

  return (
    <div className="gg-login">
      <div className="gg-login-hero d-none d-md-flex">
        <div>
          <i className="bi bi-flower1 display-3" />
          <h1 className="mt-3">GrainGuard AI</h1>
          <p className="lead">AI-Powered Public Grain Distribution System</p>
          <p>Transparent · Efficient · Accountable · AI Powered</p>
        </div>
      </div>
      <div className="gg-login-form">
        <form onSubmit={submit} className="w-100" style={{ maxWidth: 380 }}>
          <h3 className="mb-1">Sign in</h3>
          <p className="text-muted">Access your PGDS portal</p>
          {error && <div className="alert alert-danger py-2">{error}</div>}
          <label className="form-label">Username</label>
          <input className="form-control mb-3" value={username} onChange={(e) => setUsername(e.target.value)} autoFocus required />
          <label className="form-label">Password</label>
          <input type="password" className="form-control mb-3" value={password} onChange={(e) => setPassword(e.target.value)} required />
          <button className="btn btn-success w-100" disabled={busy}>{busy ? 'Signing in...' : 'Sign in'}</button>
          <div className="text-center mt-3 small">New beneficiary? <Link to="/register">Create an account</Link></div>
          {import.meta.env.DEV && (
            <div className="mt-4">
              <div className="small text-muted mb-1">Demo accounts (development only, password Pgds@12345)</div>
              <div className="d-flex flex-wrap gap-1">
                {DEMO.map(([u, l]) => (
                  <button type="button" key={u} className="btn btn-sm btn-outline-secondary" onClick={() => { setUsername(u); setPassword('Pgds@12345'); }}>{l}</button>
                ))}
              </div>
            </div>
          )}
        </form>
      </div>
    </div>
  );
}
