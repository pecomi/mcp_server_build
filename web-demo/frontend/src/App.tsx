import { useState } from 'react';
import Header from './components/Header';
import AttackPanel from './components/AttackPanel';
import ChatPanel from './components/ChatPanel';
import type { ToolCall } from './types';

export default function App() {
  const [scenario, setScenario] = useState('smoke-storedetail');
  const [llmMode, setLlmMode] = useState('mock');
  const [latestToolCalls, setLatestToolCalls] = useState<ToolCall[]>([]);

  return (
    <div className="h-full flex flex-col bg-white">
      <Header
        scenario={scenario}
        onScenarioChange={setScenario}
        llmMode={llmMode}
        onLlmModeChange={setLlmMode}
      />
      <div className="flex-1 flex overflow-hidden">
        <AttackPanel toolCalls={latestToolCalls} />
        <ChatPanel scenario={scenario} onToolCalls={setLatestToolCalls} />
      </div>
    </div>
  );
}
