import { ROLES } from './auth';
import Dashboard from './pages/officer/Dashboard';
import Cards from './pages/officer/Cards';
import Complaints from './pages/officer/Complaints';
import AiAlerts from './pages/officer/AiAlerts';
import Forecasts from './pages/officer/Forecasts';
import AdminTools from './pages/admin/AdminTools';
import Warehouse from './pages/warehouse/Warehouse';
import Distribute from './pages/dealer/Distribute';
import ShopStock from './pages/dealer/ShopStock';
import MyRation from './pages/beneficiary/MyRation';
import MyComplaints from './pages/beneficiary/MyComplaints';

const { ADMIN, OFFICER, WAREHOUSE, DEALER, BENEFICIARY, AUDITOR } = ROLES;

/** One entry per screen: which roles see it and where it lives in the sidebar. */
export const ROUTES = [
  { path: '/dashboard', label: 'Dashboard', icon: 'bi-speedometer2', roles: [ADMIN, OFFICER], element: <Dashboard /> },
  { path: '/cards', label: 'Ration Cards', icon: 'bi-card-list', roles: [ADMIN, OFFICER], element: <Cards /> },
  { path: '/complaints', label: 'Complaints', icon: 'bi-chat-left-text', roles: [ADMIN, OFFICER], element: <Complaints /> },
  { path: '/alerts', label: 'AI Alerts', icon: 'bi-shield-exclamation', roles: [ADMIN, OFFICER, AUDITOR], element: <AiAlerts /> },
  { path: '/forecasts', label: 'AI Forecasts', icon: 'bi-graph-up-arrow', roles: [ADMIN, OFFICER], element: <Forecasts /> },
  { path: '/users', label: 'Users & Tools', icon: 'bi-gear', roles: [ADMIN], element: <AdminTools /> },
  { path: '/warehouse', label: 'Warehouse Stock', icon: 'bi-building', roles: [WAREHOUSE], element: <Warehouse /> },
  { path: '/distribute', label: 'Distribute Grain', icon: 'bi-box-arrow-up-right', roles: [DEALER], element: <Distribute /> },
  { path: '/shop-stock', label: 'Shop Stock', icon: 'bi-boxes', roles: [DEALER], element: <ShopStock /> },
  { path: '/my-ration', label: 'My Ration', icon: 'bi-person-vcard', roles: [BENEFICIARY], element: <MyRation /> },
  { path: '/my-complaints', label: 'Complaints', icon: 'bi-chat-left-dots', roles: [BENEFICIARY], element: <MyComplaints /> },
];

export const firstPath = (role) => ROUTES.find((r) => r.roles.includes(role))?.path || '/login';
