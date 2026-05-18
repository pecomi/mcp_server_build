export type ToolCall = {
  name: string;
  arguments: Record<string, unknown>;
  result: string;
  isError: boolean;
};

export type RunRequest = {
  scenarioId: string;
  prompt: string;
};

export type RunResponse = {
  scenarioId: string;
  finalText: string;
  toolCalls: ToolCall[];
  llmMode: string;
  iterations: number;
};

export type ChatMessage =
  | { role: 'user'; text: string; ts: string }
  | { role: 'bot'; text: string; ts: string; toolCalls: ToolCall[]; llmMode: string };

export type SpanTag = { key: string; type: string; value: any };

export type Span = {
  spanID: string;
  operationName: string;
  startTime: number;
  duration: number;
  process: { serviceName: string };
  tags?: SpanTag[];
};

export type Trace = {
  traceID: string;
  spans: Span[];
};

export type Finding = {
  tool: string;
  rule: string;
  severity: string;
  evidence: string;
  rtCandidates: string[];
};

export type ScanResponse = {
  targetUrl: string;
  scannedTools: number;
  findings: Finding[];
};
