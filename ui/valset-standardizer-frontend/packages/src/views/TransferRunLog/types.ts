export type SystemOutputLogStreamItem = {
  sequence?: number;
  timestamp?: string;
  nodeId?: string;
  nodeName?: string;
  serviceName?: string;
  level?: string;
  threadName?: string;
  loggerName?: string;
  message?: string;
  formattedLine?: string;
};

export type SystemOutputLogStreamPayload = {
  type?: string;
  source?: string;
  data?: SystemOutputLogStreamItem | SystemOutputLogStreamItem[];
};

export type SystemOutputLogNodeOption = {
  label: string;
  value: string;
};

export type RunLogPage = {
  setMonacoRef: (instance: any) => void;
  isStreaming: boolean;
  cleanupLoading: boolean;
  nodeLoading: boolean;
  logCount: number;
  totalLines: number;
  selectedNodeId: string;
  nodeOptions: SystemOutputLogNodeOption[];
  changeNode: (nodeId: string) => void;
  startLogStream: () => void;
  stopLogStream: () => void;
  clearLogs: () => Promise<void>;
  scrollToBottom: () => void;
  handleLineExceed: (lines: number) => void;
};
