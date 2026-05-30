import type { ChatMessage } from '../types';
import { useState } from 'react';

type Props = { msg: ChatMessage };

export default function Bubble({ msg }: Props) {
  const [open, setOpen] = useState(false);
  if (msg.role === 'user') {
    return (
      <div className="self-end max-w-[80%] bg-r80 text-white rounded-2xl rounded-br-sm px-4 py-3 text-sm font-semibold leading-relaxed min-w-0">
        <div className="break-all">{msg.text}</div>
        <div className="text-[11px] font-medium mt-1.5 text-white/85">{msg.ts}</div>
      </div>
    );
  }
  return (
    <div className="self-start max-w-[80%] bg-white text-r100 border border-r20 rounded-2xl rounded-bl-sm px-4 py-3 text-sm font-semibold leading-relaxed min-w-0">
      <div className="whitespace-pre-wrap break-all">{msg.text}</div>
      <div className="text-[11px] font-medium mt-1.5 text-r40">
        {msg.ts} · llmMode: {msg.llmMode}
      </div>
      {msg.toolCalls.length > 0 && (
        <>
          <div
            onClick={() => setOpen(!open)}
            className="inline-block mt-2 px-2.5 py-1 bg-r10 text-r100 rounded-md text-[11px] font-bold cursor-pointer"
          >
            {open ? '▴' : '▾'} tool calls ({msg.toolCalls.length})
          </div>
          {open && (
            <div className="mt-2 space-y-2">
              {msg.toolCalls.map((t, i) => (
                <div key={i} className="bg-r10 rounded p-2 mono text-[11px] text-r100">
                  <div className="font-extrabold">{t.name}</div>
                  <div className="opacity-70">
                    args: {JSON.stringify(t.arguments)}
                  </div>
                  <div className="opacity-70 truncate">
                    result: {t.result.slice(0, 200)}
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}
