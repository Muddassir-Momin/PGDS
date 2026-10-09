import { createContext, useContext, useState } from 'react';
import api from './api';

const AuthCtx = createContext(null);
export const useAuth = () => useContext(AuthCtx);

export const ROLES = {
  ADMIN: 'SUPER_ADMIN',
  OFFICER: 'GOVT_OFFICER',
  WAREHOUSE: 'WAREHOUSE_MANAGER',
  DEALER: 'FPS_DEALER',
  BENEFICIARY: 'BENEFICIARY',
  AUDITOR: 'AUDITOR',
};

export const ROLE_LABEL = {
  SUPER_ADMIN: 'Super Admin',
  GOVT_OFFICER: 'Government Officer',
  WAREHOUSE_MANAGER: 'Warehouse Manager',
  FPS_DEALER: 'FPS Dealer',
  BENEFICIARY: 'Beneficiary',
  AUDITOR: 'Auditor',
};

function readUser() {
  try { return JSON.parse(localStorage.getItem('pgds_user')); } catch { return null; }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readUser);

  async function finish(authResponse) {
    localStorage.setItem('pgds_token', authResponse.token);
    // /me adds warehouseId / fpsId, which the dealer and warehouse screens need
    const me = (await api.get('/api/users/me')).data;
    const u = { username: me.username, fullName: me.fullName, role: me.role, warehouseId: me.warehouseId, fpsId: me.fpsId };
    localStorage.setItem('pgds_user', JSON.stringify(u));
    setUser(u);
    return u;
  }

  const login = async (username, password) =>
    finish((await api.post('/api/auth/login', { username, password })).data);

  const register = async (payload) =>
    finish((await api.post('/api/auth/register', payload)).data);

  const logout = () => {
    localStorage.removeItem('pgds_token');
    localStorage.removeItem('pgds_user');
    setUser(null);
  };

  return <AuthCtx.Provider value={{ user, login, register, logout }}>{children}</AuthCtx.Provider>;
}
