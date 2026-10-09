import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth';
import { errMsg } from '../api';
import { firstPath } from '../routes';

export default function Register() {
  const { register } = useAuth();
  const nav = useNavigate();
  const [f, setF] = useState({ username: '', password: '', fullName: '', email: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  async function submit(e) {
    e.preventDefault();
    setBusy(true); setError('');
    try { const u = await register(f); nav(firstPath(u.role), { replace: true }); }
    catch (err) { setError(errMsg(err)); }
    finally { setBusy(false); }
  }

  return (
    <div className="gg-login">
      <div className="gg-login-hero d-none d-md-flex">
        <div><i className="bi bi-people display-3" /><h1 className="mt-3">Beneficiary registration</h1>
          <p>Create your login, then ask your Government Officer to link it to your ration card.</p></div>
      </div>
      <div className="gg-login-form">
        <form onSubmit={submit} className="w-100" style={{ maxWidth: 380 }}>
          <h3 className="mb-3">Create account</h3>
          {error && <div className="alert alert-danger py-2">{error}</div>}
          <label className="form-label">Full name</label>
          <input className="form-control mb-3" value={f.fullName} onChange={set('fullName')} required />
          <label className="form-label">Username (4-30 characters)</label>
          <input className="form-control mb-3" value={f.username} onChange={set('username')} minLength={4} maxLength={30} required />
          <label className="form-label">Email (optional)</label>
          <input type="email" className="form-control mb-3" value={f.email} onChange={set('email')} />
          <label className="form-label">Password (min 8 characters)</label>
          <input type="password" className="form-control mb-3" value={f.password} onChange={set('password')} minLength={8} required />
          <button className="btn btn-success w-100" disabled={busy}>{busy ? 'Creating...' : 'Register'}</button>
          <div className="text-center mt-3 small">Already registered? <Link to="/login">Sign in</Link></div>
        </form>
      </div>
    </div>
  );
}
