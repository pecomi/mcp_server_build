import type { Trace } from '../types';

type Props = { traces: Trace[]; isLoading: boolean };

export default function TraceGantt({ traces, isLoading }: Props) {
  if (isLoading) {
    return <div className="text-r40 text-sm">로딩 중...</div>;
  }
  if (traces.length === 0) {
    return (
      <div className="text-r40 text-sm text-center py-12">
        아직 trace 없음. 메시지를 보내거나 ~10초 대기.
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {traces.slice(0, 5).map((t) => {
        const spans = t.spans ?? [];
        if (spans.length === 0) return null;
        const minStart = Math.min(...spans.map((s) => s.startTime));
        const maxEnd = Math.max(...spans.map((s) => s.startTime + s.duration));
        const totalDuration = maxEnd - minStart || 1;
        return (
          <div key={t.traceID} className="border-b border-r10 pb-3 last:border-b-0">
            <div className="text-xs font-bold text-r100 mb-2">
              Trace <span className="mono text-r60">{t.traceID.slice(0, 8)}</span> · {Math.round(totalDuration / 1000)}ms · {spans.length} spans
            </div>
            <div className="space-y-1">
              {spans.slice(0, 10).map((s) => {
                const leftPct = ((s.startTime - minStart) / totalDuration) * 100;
                const widthPct = (s.duration / totalDuration) * 100;
                return (
                  <div key={s.spanID} className="flex items-center gap-2.5 text-xs">
                    <span className="w-48 truncate font-bold text-r100">
                      <span className="text-r40 font-medium">{s.process?.serviceName ?? '?'}</span>{' '}
                      {s.operationName}
                    </span>
                    <span className="flex-1 h-3.5 bg-r5 rounded-sm relative">
                      <span
                        className="absolute top-0 bottom-0 bg-r60 rounded-sm"
                        style={{ left: `${leftPct}%`, width: `${Math.max(widthPct, 1)}%` }}
                      ></span>
                    </span>
                    <span className="w-16 text-right mono text-[11px] text-r40 font-medium">
                      {Math.round(s.duration / 1000)}ms
                    </span>
                  </div>
                );
              })}
            </div>
          </div>
        );
      })}
    </div>
  );
}
