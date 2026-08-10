import axiosClient from '../api/axiosClient';

// Admin CSV/Excel ixrac düymələri üçün ortaq helper — backend endpoint-ləri
// JWT tələb etdiyi üçün (bax AdminExportController) sadə <a href> işləmir,
// axios ilə blob kimi yükləyib brauzerdə fayl endirməsini əl ilə tetikləyirik.
export async function downloadCsv(url, filename) {
  const res = await axiosClient.get(url, { responseType: 'blob' });
  const blobUrl = window.URL.createObjectURL(new Blob([res.data], { type: 'text/csv;charset=utf-8;' }));
  const link = document.createElement('a');
  link.href = blobUrl;
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(blobUrl);
}
