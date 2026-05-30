import { useState, useRef, useEffect } from 'react';
import Bubble from './Bubble';
import type { ChatMessage, ToolCall } from '../types';
import { postChat } from '../api/client';

type Props = {
  scenario: string;
  onToolCalls: (calls: ToolCall[]) => void;
};

export default function ChatPanel({ scenario, onToolCalls }: Props) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' });
  }, [messages]);

  const send = async () => {
    if (!input.trim() || sending) return;
    const userMsg: ChatMessage = {
      role: 'user',
      text: input,
      ts: new Date().toLocaleTimeString('ko-KR'),
    };
    setMessages((m) => [...m, userMsg]);
    setInput('');
    setSending(true);
    try {
      const resp = await postChat({ scenarioId: scenario, prompt: userMsg.text });
      const botMsg: ChatMessage = {
        role: 'bot',
        text: resp.finalText,
        ts: new Date().toLocaleTimeString('ko-KR'),
        toolCalls: resp.toolCalls,
        llmMode: resp.llmMode,
      };
      setMessages((m) => [...m, botMsg]);
      onToolCalls(resp.toolCalls);
    } catch (e: any) {
      const errMsg: ChatMessage = {
        role: 'bot',
        text: `[error] ${e?.message ?? String(e)}`,
        ts: new Date().toLocaleTimeString('ko-KR'),
        toolCalls: [],
        llmMode: 'error',
      };
      setMessages((m) => [...m, errMsg]);
    } finally {
      setSending(false);
    }
  };

  const onKey = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      send();
    }
  };

  return (
    <div className="w-3/5 flex flex-col">
      <div ref={scrollRef} className="flex-1 overflow-y-auto px-7 py-6 flex flex-col gap-4">
        {messages.length === 0 && (
          <div className="m-auto text-r40 text-sm text-center">
            <div className="w-10 h-10 mx-auto mb-3 border-2 border-r20 rounded-full flex items-center justify-center text-r40 font-extrabold">💬</div>
            새 대화를 시작해보세요.
          </div>
        )}
        {messages.map((m, i) => (
          <Bubble key={i} msg={m} />
        ))}
        {sending && (
          <div className="self-start text-r40 text-sm font-semibold animate-pulse">생각하는 중…</div>
        )}
      </div>
      <div className="border-t border-r20 bg-r5 px-5 py-3.5 flex gap-2.5 items-center">
        <textarea
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={onKey}
          placeholder="메시지를 입력하세요... (Shift+Enter for newline)"
          className="flex-1 border border-r20 rounded px-3 py-2.5 text-sm font-semibold text-r100 bg-white resize-none min-h-[38px] outline-none focus:border-r60 placeholder:text-r40 placeholder:font-medium"
          rows={1}
        />
        <div className="border border-r20 rounded bg-white h-[38px] flex items-center px-2.5 text-xs font-semibold text-r80">
          {scenario}
        </div>
        <button
          onClick={send}
          disabled={sending || !input.trim()}
          className="bg-r100 text-white font-bold text-sm rounded-md px-4 py-2.5 disabled:opacity-40"
        >
          Send
        </button>
      </div>
    </div>
  );
}
