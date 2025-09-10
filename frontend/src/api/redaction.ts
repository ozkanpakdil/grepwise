import { apiUrl, config } from '@/config';

const API_URL = apiUrl('/api/redaction');

export async function getRedactionConfig(): Promise<{ keys: string[]; patterns: string[]; groups: Record<string, { patterns: string[] }> }> {
  const res = await fetch(`${API_URL}/config`);
  if (!res.ok) throw new Error(await res.text().catch(()=>'Failed to fetch redaction config'));
  return res.json();
}

export async function setRedactionGroupedConfig(groups: Record<string, { patterns: string[] }>): Promise<{ keys: string[]; patterns: string[]; groups: Record<string, { patterns: string[] }> }> {
  const res = await fetch(`${API_URL}/config`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(groups),
  });
  if (!res.ok) throw new Error(await res.text().catch(()=>'Failed to save redaction config'));
  return res.json();
}

export async function reloadRedaction(): Promise<void> {
  const res = await fetch(`${API_URL}/reload`, { method: 'POST' });
  if (!res.ok) throw new Error(await res.text().catch(()=>'Failed to reload redaction config'));
}
