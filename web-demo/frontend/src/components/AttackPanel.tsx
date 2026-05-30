import { useEffect, useState } from 'react';
import Tabs from './Tabs';
import ToolCallCard from './ToolCallCard';
import TraceGantt from './TraceGantt';
import PacketsPanel from './PacketsPanel';
import FindingsPanel from './FindingsPanel';
import type { ToolCall, Trace } from '../types';
import { getTraces } from '../api/client';

type Props = { toolCalls: ToolCall[] };

const TABS = ['Tool Calls', 'Traces', 'Packets', 'Findings'];

export default function AttackPanel({ toolCalls }: Props) {
  const [active, setActive] = useState('Tool Calls');
  const [traces, setTraces] = useState<Trace[]>([]);
  const [loadingTraces, setLoadingTraces] = useState(false);

  useEffect(() => {
    if (active !== 'Traces') return;
    let cancelled = false;
    const tick = async () => {
      setLoadingTraces(true);
      try {
        const t = await getTraces('mcp-lab-host', 10);
        if (!cancelled) setTraces(t);
      } catch (e) {
        if (!cancelled) setTraces([]);
      } finally {
        if (!cancelled) setLoadingTraces(false);
      }
    };
    tick();
    const id = window.setInterval(tick, 5000);
    return () => {
      cancelled = true;
      window.clearInterval(id);
    };
  }, [active]);

  return (
    <div className="w-2/5 border-r border-r20 flex flex-col">
      <Tabs tabs={TABS} active={active} onChange={setActive} />
      <div className="flex-1 overflow-y-auto p-5">
        {active === 'Tool Calls' && (
          toolCalls.length === 0 ? (
            <div className="text-r40 text-sm text-center py-20">
              <div className="w-10 h-10 mx-auto mb-3 border-2 border-r20 rounded-full flex items-center justify-center text-r40 font-extrabold">→</div>
              메시지를 보내면<br />tool 호출 체인이 여기 표시됩니다.
            </div>
          ) : (
            toolCalls.map((c, i) => (
              <ToolCallCard key={i} step={i + 1} total={toolCalls.length} call={c} />
            ))
          )
        )}
        {active === 'Traces' && <TraceGantt traces={traces} isLoading={loadingTraces} />}
        {active === 'Packets' && <PacketsPanel />}
        {active === 'Findings' && <FindingsPanel />}
      </div>
    </div>
  );
}
