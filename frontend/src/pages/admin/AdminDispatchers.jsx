import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import axiosClient from '../../api/axiosClient';

export default function AdminDispatchers() {
  const { t } = useTranslation();
  const [dispatchers, setDispatchers] = useState([]);
  const [form, setForm] = useState({ username: '', password: '', fullName: '' });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const load = () => {
    axiosClient.get('/api/admin/dispatchers').then((res) => setDispatchers(res.data));
  };

  useEffect(load, []);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    try {
      await axiosClient.post('/api/admin/dispatchers', form);
      setSuccess(t('admin.dispatcherCreated', { username: form.username }));
      setForm({ username: '', password: '', fullName: '' });
      load();
    } catch (err) {
      setError(err.response?.data || t('admin.errCreateDispatcher'));
    }
  };

  const handleDelete = async (dispatcher) => {
    if (!window.confirm(t('admin.dispatcherDeleteConfirm', { name: dispatcher.fullName, username: dispatcher.username }))) return;
    setError('');
    try {
      await axiosClient.delete(`/api/admin/dispatchers/${dispatcher.id}`);
      load();
    } catch (err) {
      setError(err.response?.data?.message || t('admin.errDeleteDispatcher'));
    }
  };

  return (
    <div>
      <h2>{t('admin.dispatchersTitle')}</h2>
      <p>{t('admin.dispatchersDesc')}</p>

      <div className="grid grid-2 mt-16" style={{ alignItems: 'start' }}>
        <div className="card">
          {error && <div className="alert alert-error">{String(error)}</div>}
          {success && <div className="alert alert-success">{success}</div>}
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="label">{t('admin.fullNameLabel')}</label>
              <input className="input" name="fullName" value={form.fullName} onChange={handleChange} required />
            </div>
            <div className="form-group">
              <label className="label">{t('admin.usernameLabel')}</label>
              <input className="input" name="username" value={form.username} onChange={handleChange} required />
            </div>
            <div className="form-group">
              <label className="label">{t('auth.passwordLabel')}</label>
              <input type="password" className="input" name="password" value={form.password} onChange={handleChange} required />
            </div>
            <button className="btn btn-primary btn-block" type="submit">{t('admin.createAccountBtn')}</button>
          </form>
        </div>

        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>{t('admin.colFullName')}</th><th>{t('admin.colUsername')}</th><th></th></tr>
            </thead>
            <tbody>
              {dispatchers.map((d) => (
                <tr key={d.id}>
                  <td>{d.fullName}</td>
                  <td>{d.username}</td>
                  <td><button className="btn btn-sm btn-danger" onClick={() => handleDelete(d)}>{t('common.delete')}</button></td>
                </tr>
              ))}
              {dispatchers.length === 0 && (
                <tr><td colSpan={3} className="text-center text-muted">{t('admin.noDispatchers')}</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
