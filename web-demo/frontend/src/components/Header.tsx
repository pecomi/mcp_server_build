type Props = {
  llmMode: string;
  scenario: string;
  onScenarioChange: (s: string) => void;
  onLlmModeChange: (m: string) => void;
};

const SCENARIOS = [
  'smoke-storedetail',
  'smoke-storelist',
  'smoke-readfile',
  'smoke-lookup',
  'rt-002-citizen-self-lookup',
  'rt-003-cross-server-lookup',
];

const LLM_MODES = [
  'mock',
  'mock_poisoned',
  'mock_cross_poisoned',
  'real_deterministic',
];

export default function Header({ llmMode, scenario, onScenarioChange, onLlmModeChange }: Props) {
  const llmWarning = llmMode !== 'mock';
  return (
    <div className="h-16 bg-r5 border-b border-r20 flex items-center px-5 gap-3">
      <span className="font-extrabold text-r100 text-base">MCP Pentest Demo</span>
      <span className="flex-1"></span>
      <span className="text-xs font-semibold text-r80 bg-white border border-r20 rounded-full px-3 py-1.5">
        user@tenant-a
      </span>
      <span className={`text-xs font-semibold rounded-full px-3 py-1.5 flex items-center gap-1.5 ${
        llmWarning ? 'bg-r100 text-white border border-r100' : 'bg-white text-r80 border border-r20'
      }`}>
        {llmWarning && <span className="w-1.5 h-1.5 rounded-full bg-white"></span>}
        LLM:&nbsp;
        <select
          value={llmMode}
          onChange={(e) => onLlmModeChange(e.target.value)}
          className={`bg-transparent border-0 outline-none font-semibold text-xs ${
            llmWarning ? 'text-white' : 'text-r80'
          }`}
        >
          {LLM_MODES.map((m) => (
            <option key={m} value={m} className="text-r100">{m}</option>
          ))}
        </select>
      </span>
      <span className="text-xs font-semibold text-r80 bg-white border border-r20 rounded-full px-3 py-1.5">
        Scenario:&nbsp;
        <select
          value={scenario}
          onChange={(e) => onScenarioChange(e.target.value)}
          className="bg-transparent border-0 outline-none font-semibold text-xs text-r80"
        >
          {SCENARIOS.map((s) => (
            <option key={s} value={s}>{s}</option>
          ))}
        </select>
      </span>
    </div>
  );
}
