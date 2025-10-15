type MessageHandler<T = unknown> = (payload: T) => void;

interface OutboundMessage {
  type: string;
  topics?: string[];
}

interface InboundMessage<T = unknown> {
  type: string;
  payload: T;
}

const typeListeners = new Map<string, Set<MessageHandler<unknown>>>();
const topicRefCounts = new Map<string, number>();

let socket: WebSocket | null = null;
let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
let heartbeatTimer: ReturnType<typeof setInterval> | null = null;
let intentionallyClosed = false;

const pendingSubscribe = new Set<string>();
const pendingUnsubscribe = new Set<string>();

const WS_PATH = "/ws";
const RECONNECT_DELAY_MS = 3000;
const HEARTBEAT_INTERVAL_MS = 20000;

const resolveTopic = (messageType: string): string => {
  const [topic] = messageType.split(".");
  return topic ?? messageType;
};

const computeUrl = () => {
  const { protocol, host } = window.location;
  const wsProtocol = protocol === "https:" ? "wss:" : "ws:";
  return `${wsProtocol}//${host}${WS_PATH}`;
};

const scheduleHeartbeat = () => {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer);
  }
  heartbeatTimer = setInterval(() => {
    if (socket?.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify({ type: "ping" }));
    }
  }, HEARTBEAT_INTERVAL_MS);
};

const clearHeartbeat = () => {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer);
    heartbeatTimer = null;
  }
};

const sendMessage = (message: OutboundMessage) => {
  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify(message));
    return true;
  }
  return false;
};

const flushTopicSubscriptions = () => {
  const activeTopics = Array.from(topicRefCounts.entries())
    .filter(([, count]) => count > 0)
    .map(([topic]) => topic);

  if (activeTopics.length > 0) {
    pendingSubscribe.clear();
    sendMessage({ type: "subscribe", topics: activeTopics });
  }
};

const drainPending = () => {
  if (pendingSubscribe.size > 0) {
    sendMessage({ type: "subscribe", topics: Array.from(pendingSubscribe) });
    pendingSubscribe.clear();
  }
  if (pendingUnsubscribe.size > 0) {
    sendMessage({ type: "unsubscribe", topics: Array.from(pendingUnsubscribe) });
    pendingUnsubscribe.clear();
  }
};

const notifyListeners = (type: string, payload: unknown) => {
  const listeners = typeListeners.get(type);
  if (listeners && payload !== undefined) {
    listeners.forEach((listener) => {
      try {
        listener(payload);
      } catch (error) {
        console.error("WebSocket listener error:", error);
      }
    });
  }
};

const handleMessage = (event: MessageEvent<string>) => {
  try {
    const data = JSON.parse(event.data) as InboundMessage;
    if (!data?.type) {
      return;
    }

    notifyListeners(data.type, data.payload);
  } catch (error) {
    console.error("Failed to parse WebSocket message", error);
  }
};

const setupSocket = () => {
  if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
    return;
  }

  intentionallyClosed = false;
  const url = computeUrl();
  socket = new WebSocket(url);

  socket.onopen = () => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }
    scheduleHeartbeat();

    if (pendingSubscribe.size > 0 || pendingUnsubscribe.size > 0) {
      drainPending();
    } else {
      flushTopicSubscriptions();
    }
  };

  socket.onmessage = handleMessage;

  socket.onclose = (event) => {
    clearHeartbeat();
    socket = null;

    if (intentionallyClosed) {
      return;
    }

    notifyListeners("system.disconnected", {
      code: event.code,
      reason: event.reason,
      wasClean: event.wasClean,
    });

    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
    }
    reconnectTimer = setTimeout(() => {
      setupSocket();
    }, RECONNECT_DELAY_MS);
  };

  socket.onerror = (event) => {
    console.warn("WebSocket encountered an error, closing connection.", event);
    socket?.close();
  };
};

export const connectWebSocket = () => {
  setupSocket();
};

export const disconnectWebSocket = () => {
  intentionallyClosed = true;
  clearHeartbeat();
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }
  socket?.close();
  socket = null;
};

export const subscribeToMessage = <T = unknown>(messageType: string, handler: MessageHandler<T>) => {
  const topic = resolveTopic(messageType);

  const wrappedHandler: MessageHandler<unknown> = (payload) => {
    handler(payload as T);
  };

  const listeners = typeListeners.get(messageType) ?? new Set<MessageHandler<unknown>>();
  listeners.add(wrappedHandler);
  typeListeners.set(messageType, listeners);

  const currentCount = topicRefCounts.get(topic) ?? 0;
  topicRefCounts.set(topic, currentCount + 1);

  setupSocket();

  if (currentCount === 0) {
    if (!sendMessage({ type: "subscribe", topics: [topic] })) {
      pendingSubscribe.add(topic);
    }
  }

  return () => {
    const topicListeners = typeListeners.get(messageType);
    if (topicListeners) {
      topicListeners.delete(wrappedHandler);
      if (topicListeners.size === 0) {
        typeListeners.delete(messageType);
      }
    }

    const updatedCount = (topicRefCounts.get(topic) ?? 1) - 1;
    if (updatedCount <= 0) {
      topicRefCounts.delete(topic);
      if (!sendMessage({ type: "unsubscribe", topics: [topic] })) {
        pendingUnsubscribe.add(topic);
      }
    } else {
      topicRefCounts.set(topic, updatedCount);
    }
  };
};
