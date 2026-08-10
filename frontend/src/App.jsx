import { Routes, Route } from 'react-router-dom';
import ProtectedRoute from './components/ProtectedRoute.jsx';

import Home from './pages/public/Home.jsx';
import Login from './pages/public/Login.jsx';
import ForgotPassword from './pages/public/ForgotPassword.jsx';
import CustomerRegister from './pages/public/CustomerRegister.jsx';
import JobApplicationForm from './pages/public/JobApplicationForm.jsx';
import ApplicationStatusCheck from './pages/public/ApplicationStatusCheck.jsx';
import TrackingSearch from './pages/public/TrackingSearch.jsx';

import AdminLayout from './layouts/AdminLayout.jsx';
import AdminHome from './pages/admin/AdminHome.jsx';
import AdminApplications from './pages/admin/AdminApplications.jsx';
import AdminDrivers from './pages/admin/AdminDrivers.jsx';
import AdminWarehouses from './pages/admin/AdminWarehouses.jsx';
import AdminVehicles from './pages/admin/AdminVehicles.jsx';
import AdminTrips from './pages/admin/AdminTrips.jsx';
import AdminDispatchers from './pages/admin/AdminDispatchers.jsx';
import AdminAnomalies from './pages/admin/AdminAnomalies.jsx';
import AdminPayments from './pages/admin/AdminPayments.jsx';

import AdminAuditLog from './pages/admin/AdminAuditLog.jsx';
import AdminCustomsTariffs from './pages/admin/AdminCustomsTariffs.jsx';
import AdminRatings from './pages/admin/AdminRatings.jsx';
import AdminChat from './pages/admin/AdminChat.jsx';
import AdminMaintenance from './pages/admin/AdminMaintenance.jsx';

import DispatcherLayout from './layouts/DispatcherLayout.jsx';
import ControlTower from './pages/dispatcher/ControlTower.jsx';
import CargoQueue from './pages/dispatcher/CargoQueue.jsx';
import DispatcherTrips from './pages/dispatcher/DispatcherTrips.jsx';
import NewCargo from './pages/dispatcher/NewCargo.jsx';
import DispatcherRatings from './pages/dispatcher/DispatcherRatings.jsx';
import DispatcherChat from './pages/dispatcher/DispatcherChat.jsx';
import DispatcherPayments from './pages/dispatcher/DispatcherPayments.jsx';
import TrailerPool from './pages/dispatcher/TrailerPool.jsx';

import DriverLayout from './layouts/DriverLayout.jsx';
import DriverCurrentTrip from './pages/driver/DriverCurrentTrip.jsx';
import DriverHistory from './pages/driver/DriverHistory.jsx';
import DriverRatings from './pages/driver/DriverRatings.jsx';
import DriverChat from './pages/driver/DriverChat.jsx';

import CustomerLayout from './layouts/CustomerLayout.jsx';
import CustomerHome from './pages/customer/CustomerHome.jsx';
import NewOrder from './pages/customer/NewOrder.jsx';
import MyOrders from './pages/customer/MyOrders.jsx';
import LiveTracking from './pages/customer/LiveTracking.jsx';
import CustomsCalculator from './pages/customer/CustomsCalculator.jsx';
import CustomerChat from './pages/customer/CustomerChat.jsx';
import CustomerInvoices from './pages/customer/CustomerInvoices.jsx';

import ProfilePage from './pages/shared/ProfilePage.jsx';

function App() {
  return (
    <Routes>
      {/* Public */}
      <Route path="/" element={<Home />} />
      <Route path="/login" element={<Login />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/register" element={<CustomerRegister />} />
      <Route path="/apply" element={<JobApplicationForm />} />
      <Route path="/apply/status" element={<ApplicationStatusCheck />} />
      <Route path="/tracking" element={<TrackingSearch />} />

      {/* Admin */}
      <Route path="/admin" element={<ProtectedRoute allowedRoles={['ADMIN']}><AdminLayout /></ProtectedRoute>}>
        <Route index element={<AdminHome />} />
        <Route path="applications" element={<AdminApplications />} />
        <Route path="drivers" element={<AdminDrivers />} />
        <Route path="warehouses" element={<AdminWarehouses />} />
        <Route path="vehicles" element={<AdminVehicles />} />
        <Route path="trips" element={<AdminTrips />} />
        <Route path="dispatchers" element={<AdminDispatchers />} />
        <Route path="customs-tariffs" element={<AdminCustomsTariffs />} />
        <Route path="anomalies" element={<AdminAnomalies />} />
        <Route path="payments" element={<AdminPayments />} />
        <Route path="ratings" element={<AdminRatings />} />
        <Route path="chat" element={<AdminChat />} />
        <Route path="maintenance" element={<AdminMaintenance />} />
        <Route path="audit-log" element={<AdminAuditLog />} />
        <Route path="profile" element={<ProfilePage />} />
      </Route>

      {/* Dispatcher */}
      <Route path="/dispatcher" element={<ProtectedRoute allowedRoles={['DISPATCHER', 'ADMIN']}><DispatcherLayout /></ProtectedRoute>}>
        <Route index element={<ControlTower />} />
        <Route path="new-cargo" element={<NewCargo />} />
        <Route path="queue" element={<CargoQueue />} />
        <Route path="trips" element={<DispatcherTrips />} />
        <Route path="trailers" element={<TrailerPool />} />
        <Route path="chat" element={<DispatcherChat />} />
        <Route path="payments" element={<DispatcherPayments />} />
        <Route path="ratings" element={<DispatcherRatings />} />
        <Route path="profile" element={<ProfilePage />} />
      </Route>

      {/* Driver */}
      <Route path="/driver" element={<ProtectedRoute allowedRoles={['DRIVER']}><DriverLayout /></ProtectedRoute>}>
        <Route index element={<DriverCurrentTrip />} />
        <Route path="history" element={<DriverHistory />} />
        <Route path="chat" element={<DriverChat />} />
        <Route path="ratings" element={<DriverRatings />} />
        <Route path="profile" element={<ProfilePage />} />
      </Route>

      {/* Customer */}
      <Route path="/customer" element={<ProtectedRoute allowedRoles={['CUSTOMER']}><CustomerLayout /></ProtectedRoute>}>
        <Route index element={<CustomerHome />} />
        <Route path="new" element={<NewOrder />} />
        <Route path="orders" element={<MyOrders />} />
        <Route path="chat" element={<CustomerChat />} />
        <Route path="invoices" element={<CustomerInvoices />} />
        <Route path="track/:trackingNumber" element={<LiveTracking />} />
        <Route path="customs-calculator" element={<CustomsCalculator />} />
        <Route path="profile" element={<ProfilePage />} />
      </Route>

      <Route path="*" element={<Home />} />
    </Routes>
  );
}

export default App;
