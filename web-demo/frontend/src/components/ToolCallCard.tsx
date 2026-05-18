import type { ToolCall } from '../types';

type Props = { step: number; total: number; call: ToolCall };

export default function ToolCallCard({ step, total, call }: Props) {
  return (
    <div className="border border-r20 rounded-lg p-4 mb-3 bg-white">
      <div className="flex justify-between items-center mb-2">
        <span className="font-extrabold text-sm text-r100">
          Step {step} / {total} · {call.name}
        </span>
        <span className={`text-[11px] font-bold px-2 py-0.5 rounded-full text-white ${
          call.isError ? 'bg-r100' : 'bg-r60'
        }`}>
          {call.isError ? 'ERROR' : 'SUCCESS'}
        </span>
      </div>
      <div className="text-[11px] font-bold text-r40 uppercase tracking-wider">args</div>
      <pre className="mono text-xs bg-r10 text-r100 rounded px-2.5 py-2 my-1.5 whitespace-pre-wrap break-all">
{JSON.stringify(call.arguments, null, 2)}
      </pre>
      <div className="text-[11px] font-bold text-r40 uppercase tracking-wider">result</div>
      <pre className="mono text-xs bg-r10 text-r100 rounded px-2.5 py-2 my-1.5 whitespace-pre-wrap break-all">
{call.result.length > 600 ? call.result.slice(0, 600) + '…' : call.result}
      </pre>
    </div>
  );
}
