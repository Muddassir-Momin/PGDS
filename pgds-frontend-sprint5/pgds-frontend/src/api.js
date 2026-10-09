import axios from 'axios';

const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080' });

api.interceptors.request.use((cfg) => {
  const t = localStorage.getItem('pgds_token');
  if (t) cfg.headers.Authorization = `Bearer ${t}`;
  return cfg;
});

api.interceptors.response.use(
  (r) => r,
  (err) => {
    const url = err.config?.url || '';
    if (err.response?.status === 401 && !url.includes('/api/auth/')) {
      localStorage.removeItem('pgds_token');
      localStorage.removeItem('pgds_user');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

/** Turns a backend error into a readable message. */
export function errMsg(e) {
  const d = e?.response?.data;
  if (d?.details) return Object.entries(d.details).map(([k, v]) => `${k}: ${v}`).join('; ');
  return d?.error || e?.message || 'Something went wrong';
}

export default api;
