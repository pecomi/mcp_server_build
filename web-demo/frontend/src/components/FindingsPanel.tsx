import { useEffect, useState } from 'react';
import type { Finding, ScanResponse } from '../types';
import { postScan } from '../api/client';

export default function FindingsPanel() {
  const [resp, setResp] = useState<ScanResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const runScan = async () => {
    setLoading(true);
    setErr(null);
    try {
      const r = await postScan();
      setResp(r);
    } catch (e: any) {
      setErr(e?.message ?? String(e));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    runScan();
    const id = window.setInterval(runScan, 15000);
    return () => window.clearInterval(id);
  }, []);

  if (loading && !resp) {
    return <div className="text-r40 text-sm">스캐닝 중...</div>;
  }
  if (err) {
    return <div className="text-r100 text-sm font-bold">scan error: {err}</div>;
  }
  if (!resp) {
    return <div className="text-r40 text-sm">스캐너 응답 없음</div>;
  }
  if (resp.findings.length === 0) {
    return (
      <div className="text-r40 text-sm text-center py-12">
        <div className="mb-2">✓ {resp.scannedTools} tools 스캔 완료</div>
        <div>위험 finding 없음 (baseline OK).</div>
      </div>
    );
  }

  const high = resp.findings.filter((f) => f.severity === 'high');

  return (
    <div className="space-y-3">
      {high.length > 0 && (
        <div className="bg-r100 text-white font-extrabold text-sm rounded-lg px-3.5 py-2.5 flex items-center gap-2">
          ⚠ ACTIVE · {high.length} HIGH severity finding(s) · {resp.scannedTools} tools scanned
        </div>
      )}
      {resp.findings.map((f, i) => (
        <FindingCard key={i} f={f} />
      ))}
    </div>
  );
}

function FindingCard({ f }: { f: Finding }) {
  return (
    <div className="bg-white border border-r20 border-t-[3px] border-t-r100 rounded-md p-3.5">
      <div className="flex justify-between items-center mb-1.5">
        <span className="font-extrabold text-xs text-r100 break-all">
          {f.rule} — {f.tool}
        </span>
        <span className={`text-[11px] font-bold px-2 py-0.5 rounded-full text-white ${
          f.severity === 'high' ? 'bg-r100' : 'bg-r60'
        }`}>
          {f.severity.toUpperCase()}
        </span>
      </div>
      <div className="mono text-[11px] font-medium bg-r10 text-r100 rounded px-2.5 py-2 mt-1.5 break-all whitespace-pre-wrap">
        {f.evidence}
      </div>
      {f.rtCandidates && f.rtCandidates.length > 0 && (
        <div className="text-[11px] text-r60 font-bold mt-1.5">
          → {f.rtCandidates.join(' · ')}
        </div>
      )}
    </div>
  );
}
