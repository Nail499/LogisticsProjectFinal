import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import axiosClient from '../../api/axiosClient';

export default function AdminVehicles() {
  const { t } = useTranslation();

  const TRANSPORT_MODES = [
    { value: 'TRUCK', label: t('dispatcher.newCargoTransportTruck') },
    { value: 'RAIL', label: t('dispatcher.newCargoTransportRail') },
    { value: 'SEA', label: t('dispatcher.newCargoTransportSea') },
    { value: 'AIR', label: t('dispatcher.newCargoTransportAir') },
  ];

  const OWNER_TYPES = [
    { value: 'COMPANY', label: t('admin.ownerCompanyOption') },
    { value: 'DRIVER_OWNED', label: t('admin.ownerDriverOption') },
  ];

  const OwnerBadge = ({ ownerType, driverName }) => {
    if (ownerType === 'DRIVER_OWNED') {
      return (
        <span style={{ fontSize: 11.5, padding: '2px 8px', borderRadius: 999, background: '#eef2ff', color: '#4338ca' }}>
          {driverName ? t('admin.ownerDriverBadgeNamed', { name: driverName }) : t('admin.ownerDriverBadge')}
        </span>
      );
    }
    return (
      <span style={{ fontSize: 11.5, padding: '2px 8px', borderRadius: 999, background: '#f0fdf4', color: '#16a34a' }}>
        {t('dispatcher.ownerCompanySuffix')}
      </span>
    );
  };

  const [vehicles, setVehicles] = useState([]);
  const [trailers, setTrailers] = useState([]);
  const [drivers, setDrivers] = useState([]);
  const [form, setForm] = useState({ plateNumber: '', brand: '', fuelConsumption: '', transportMode: 'TRUCK', ownerType: 'COMPANY', driverId: '' });
  const [trailerForm, setTrailerForm] = useState({ plateNumber: '', capacity: '', ownerType: 'COMPANY', driverId: '' });
  const [error, setError] = useState('');
  const [trailerError, setTrailerError] = useState('');

  const load = () => {
    axiosClient.get('/api/admin/vehicles').then((res) => setVehicles(res.data));
    axiosClient.get('/api/admin/trailers').then((res) => setTrailers(res.data));
    axiosClient.get('/api/admin/drivers').then((res) => setDrivers(res.data));
  };

  useEffect(load, []);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });
  const handleTrailerChange = (e) => setTrailerForm({ ...trailerForm, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await axiosClient.post('/api/admin/vehicles', {
        ...form,
        fuelConsumption: form.fuelConsumption ? parseFloat(form.fuelConsumption) : null,
        driverId: form.ownerType === 'DRIVER_OWNED' && form.driverId ? parseInt(form.driverId) : null,
      });
      setForm({ plateNumber: '', brand: '', fuelConsumption: '', transportMode: 'TRUCK', ownerType: 'COMPANY', driverId: '' });
      load();
    } catch (err) {
      setError(err.response?.data?.message || t('admin.errCreateVehicle'));
    }
  };

  const handleTrailerSubmit = async (e) => {
    e.preventDefault();
    setTrailerError('');
    try {
      await axiosClient.post('/api/admin/trailers', {
        ...trailerForm,
        capacity: trailerForm.capacity ? parseFloat(trailerForm.capacity) : null,
        driverId: trailerForm.ownerType === 'DRIVER_OWNED' && trailerForm.driverId ? parseInt(trailerForm.driverId) : null,
      });
      setTrailerForm({ plateNumber: '', capacity: '', ownerType: 'COMPANY', driverId: '' });
      load();
    } catch (err) {
      setTrailerError(err.response?.data?.message || t('admin.errCreateTrailer'));
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm(t('admin.vehicleDeleteConfirm'))) return;
    setError('');
    try {
      await axiosClient.delete(`/api/admin/vehicles/${id}`);
      load();
    } catch (err) {
      setError(err.response?.data?.message || t('admin.errDeleteVehicle'));
    }
  };

  const handleDeleteTrailer = async (id) => {
    if (!window.confirm(t('admin.trailerDeleteConfirm'))) return;
    setTrailerError('');
    try {
      await axiosClient.delete(`/api/admin/trailers/${id}`);
      load();
    } catch (err) {
      setTrailerError(err.response?.data?.message || t('admin.errDeleteTrailer'));
    }
  };

  return (
    <div>
      <h2>{t('admin.vehiclesTitle')}</h2>
      <p>{t('admin.vehiclesDesc')}</p>

      <div className="grid grid-2 mt-16" style={{ alignItems: 'start' }}>
        <div className="card">
          <h3>{t('admin.newVehicleTitle')}</h3>
          {error && <div className="alert alert-error">{error}</div>}
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="label">{t('admin.plateLabel')}</label>
              <input className="input" name="plateNumber" value={form.plateNumber} onChange={handleChange} required />
            </div>
            <div className="form-group">
              <label className="label">{t('admin.brandLabel')}</label>
              <input className="input" name="brand" value={form.brand} onChange={handleChange} required />
            </div>
            <div className="form-group">
              <label className="label">{t('admin.fuelConsumptionLabel')}</label>
              <input className="input" name="fuelConsumption" value={form.fuelConsumption} onChange={handleChange} />
            </div>
            <div className="form-group">
              <label className="label">{t('dispatcher.colType')}</label>
              <select className="input" name="transportMode" value={form.transportMode} onChange={handleChange}>
                {TRANSPORT_MODES.map((m) => (
                  <option key={m.value} value={m.value}>{m.label}</option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label className="label">{t('admin.ownershipLabel')}</label>
              <select className="input" name="ownerType" value={form.ownerType} onChange={handleChange}>
                {OWNER_TYPES.map((o) => (
                  <option key={o.value} value={o.value}>{o.label}</option>
                ))}
              </select>
            </div>
            {form.ownerType === 'DRIVER_OWNED' && (
              <div className="form-group">
                <label className="label">{t('admin.ownerDriverSelectLabel')}</label>
                <select className="input" name="driverId" value={form.driverId} onChange={handleChange} required>
                  <option value="">{t('common.select')}</option>
                  {drivers.map((d) => (
                    <option key={d.driverId} value={d.driverId}>{d.fullName} — {d.phone}</option>
                  ))}
                </select>
              </div>
            )}
            <button className="btn btn-primary btn-block" type="submit">{t('admin.addBtn')}</button>
          </form>
        </div>

        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>{t('admin.plateLabel')}</th><th>{t('admin.brandLabel')}</th><th>{t('admin.colType')}</th><th>{t('admin.colOwnership')}</th><th></th></tr>
            </thead>
            <tbody>
              {vehicles.map((v) => (
                <tr key={v.id}>
                  <td>{v.plateNumber}</td>
                  <td>{v.brand}</td>
                  <td>{TRANSPORT_MODES.find((m) => m.value === v.transportMode)?.label || '—'}</td>
                  <td><OwnerBadge ownerType={v.ownerType} driverName={v.driver?.fullName} /></td>
                  <td><button className="btn btn-sm btn-danger" onClick={() => handleDelete(v.id)}>{t('common.delete')}</button></td>
                </tr>
              ))}
              {vehicles.length === 0 && (
                <tr><td colSpan={5} className="text-center text-muted">{t('admin.noVehicles')}</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      <div className="grid grid-2 mt-24" style={{ alignItems: 'start' }}>
        <div className="card">
          <h3>{t('admin.newTrailerTitle')}</h3>
          {trailerError && <div className="alert alert-error">{trailerError}</div>}
          <form onSubmit={handleTrailerSubmit}>
            <div className="form-group">
              <label className="label">{t('admin.plateLabel')}</label>
              <input className="input" name="plateNumber" value={trailerForm.plateNumber} onChange={handleTrailerChange} required />
            </div>
            <div className="form-group">
              <label className="label">{t('admin.capacityTonsLabel')}</label>
              <input className="input" name="capacity" value={trailerForm.capacity} onChange={handleTrailerChange} />
            </div>
            <div className="form-group">
              <label className="label">{t('admin.ownershipLabel')}</label>
              <select className="input" name="ownerType" value={trailerForm.ownerType} onChange={handleTrailerChange}>
                {OWNER_TYPES.map((o) => (
                  <option key={o.value} value={o.value}>{o.label}</option>
                ))}
              </select>
            </div>
            {trailerForm.ownerType === 'DRIVER_OWNED' && (
              <div className="form-group">
                <label className="label">{t('admin.ownerDriverSelectLabel')}</label>
                <select className="input" name="driverId" value={trailerForm.driverId} onChange={handleTrailerChange} required>
                  <option value="">{t('common.select')}</option>
                  {drivers.map((d) => (
                    <option key={d.driverId} value={d.driverId}>{d.fullName} — {d.phone}</option>
                  ))}
                </select>
              </div>
            )}
            <button className="btn btn-primary btn-block" type="submit">{t('admin.addBtn')}</button>
          </form>
        </div>

        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>{t('admin.plateLabel')}</th><th>{t('admin.capacityTonsLabel')}</th><th>{t('admin.colOwnership')}</th><th></th></tr>
            </thead>
            <tbody>
              {trailers.map((tr) => (
                <tr key={tr.id}>
                  <td>{tr.plateNumber}</td>
                  <td>{tr.capacity}</td>
                  <td><OwnerBadge ownerType={tr.ownerType} driverName={tr.driver?.fullName} /></td>
                  <td><button className="btn btn-sm btn-danger" onClick={() => handleDeleteTrailer(tr.id)}>{t('common.delete')}</button></td>
                </tr>
              ))}
              {trailers.length === 0 && (
                <tr><td colSpan={4} className="text-center text-muted">{t('admin.noTrailers')}</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
