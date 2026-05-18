import type { RunRequest, RunResponse, Trace, ScanResponse } from '../types';

export async function postChat(req: RunRequest): Promise<RunResponse> {
  const resp = await fetch('/api/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
  if (!resp.ok) {
    const text = await resp.text();
    throw new Error(`chat failed: ${resp.status} ${text}`);
  }
  return resp.json();
}

export async function getTraces(service: string, limit = 10): Promise<Trace[]> {
  const resp = await fetch(`/api/traces?service=${encodeURIComponent(service)}&limit=${limit}`);
  if (!resp.ok) return [];
  const body = await resp.json();
  return (body?.data ?? []) as Trace[];
}

export async function postScan(): Promise<ScanResponse | null> {
  const resp = await fetch('/api/scan', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: '{}',
  });
  if (!resp.ok) return null;
  return resp.json();
}
