import { useEffect, useState } from 'react';
import type { Trace, Span } from '../types';
import { getTraces } from '../api/client';

type Exchange = {
  service: string;
  method: string;
  url: string;
  status: string;
  duration: number;
};

function tagOf(span: Span, key: string): string | undefined {
  const t = span.tags?.find((x) => x.key === key);
  return t ? String(t.value) : undefined;
}

function spanToExchange(span: Span): Exchange | null {
  const method = tagOf(span, 'http.method') ?? tagOf(span, 'http.request.method');
  const url = tagOf(span, 'http.url') ?? tagOf(span, 'url.full') ?? tagOf(span, 'http.target') ?? span.operationName;
  const status = tagOf(span, 'http.status_code') ?? tagOf(span, 'http.response.status_code') ?? '';
  if (!method) return null;
  return {
    service: span.process?.serviceName ?? '?',
    method,
    url,
    status,
    duration: span.duration,
  };
}

export default function PacketsPanel() {
  const [exchanges, setExchanges] = useState<Exchange[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let cancelled = false;
    const tick = async () => {
      setLoading(true);
      try {
        const traces: Trace[] = await getTraces('mcp-lab-host', 5);
        const all: Exchange[] = [];
        for (const t of traces) {
          for (const s of t.spans ?? []) {
            const ex = spanToExchange(s);
            if (ex) all.push(ex);
          }
        }
        if (!cancelled) setExchanges(all.slice(0, 40));
      } catch {
        if (!cancelled) setExchanges([]);
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    tick();
    const id = window.setInterval(tick, 5000);
    return () => { cancelled = true; window.clearInterval(id); };
  }, []);

  if (loading && exchanges.length === 0) {
    return <div className="text-r40 text-sm">로딩 중...</div>;
  }
  if (exchanges.length === 0) {
    return (
      <div className="text-r40 text-sm text-center py-12">
        HTTP 패킷 (span)이 아직 없음. 메시지를 보내거나 잠시 대기.
      </div>
    );
  }

  const statusColor = (status: string) => {
    if (!status) return 'text-r40';
    if (status.startsWith('2')) return 'text-r60';
    if (status.startsWith('4') || status.startsWith('5')) return 'text-r100';
    return 'text-r40';
  };

  return (
    <div className="space-y-2">
      {exchanges.map((ex, i) => (
        <div key={i} className="border border-r20 rounded-md bg-r5 px-3.5 py-2.5">
          <div className="flex items-center gap-2 mono text-xs text-r100 font-bold">
            <span className="text-r100">{ex.method}</span>
            <span className="font-semibold text-r80 truncate flex-1">{ex.url}</span>
            <span className={`font-bold ${statusColor(ex.status)}`}>
              {ex.status || '—'}
            </span>
            <span className="text-r40 font-medium">{Math.round(ex.duration / 1000)}ms</span>
          </div>
          <div className="text-[11px] font-medium text-r40 mt-1">{ex.service}</div>
        </div>
      ))}
    </div>
  );
}
