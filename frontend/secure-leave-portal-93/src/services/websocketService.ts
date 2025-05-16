import { Client, Message } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import notificationService from './notificationService';

export type WebSocketSubscription = {
  id: string;
  unsubscribe: () => void;
};

class WebSocketService {
  private client: Client | null = null;
  private subscriptions: Map<string, WebSocketSubscription> = new Map();
  private reconnectAttempts = 0;
  private readonly maxReconnectAttempts = 5;
  private readonly reconnectDelay = 3000; // 3 seconds

  constructor() {
    this.initializeClient();
  }

  private initializeClient() {
    const wsUrl = import.meta.env.VITE_API_URL || 'http://localhost:8082/api';
    // Retrieve JWT token from localStorage or sessionStorage
    const token = localStorage.getItem('auth_token') || sessionStorage.getItem('auth_token');

    this.client = new Client({
      webSocketFactory: () => new SockJS(`${wsUrl}/ws`),
      // Add Authorization header if token is present
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      debug: (str) => {
        if (import.meta.env.DEV) {
          console.log(str);
        }
      },
      reconnectDelay: this.reconnectDelay,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      connectionTimeout: 10000,
    });

    this.client.onConnect = () => {
      console.log('WebSocket Connected!');
      this.reconnectAttempts = 0;
      // Resubscribe to all topics after reconnection
      this.resubscribeAll();
    };

    this.client.onStompError = (frame) => {
      console.error('STOMP Error:', frame);
    };

    this.client.onWebSocketClose = () => {
      console.log('WebSocket Connection Closed');
      this.handleReconnect();
    };

    this.client.onWebSocketError = (event) => {
      console.error('WebSocket Error:', event);
    };
  }

  private handleReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`Attempting to reconnect... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
      setTimeout(() => {
        this.connect();
      }, this.reconnectDelay * this.reconnectAttempts); // Exponential backoff
    } else {
      console.error('Max reconnection attempts reached');
    }
  }

  private resubscribeAll() {
    // Store the current subscriptions
    const currentSubscriptions = Array.from(this.subscriptions.entries());
    
    // Clear the subscriptions map
    this.subscriptions.clear();
    
    // Resubscribe to all previous topics
    currentSubscriptions.forEach(([topic, subscription]) => {
      try {
        if (topic && subscription?.id) {
          this.subscribe(topic, subscription.id, subscription.unsubscribe);
        } else {
          console.warn(`Skipping invalid subscription: topic=${topic}, subscription=${subscription}`);
        }
      } catch (error) {
        console.error(`Failed to resubscribe to topic ${topic}:`, error);
      }
    });
  }

  connect() {
    if (!this.client) {
      this.initializeClient();
    }
    
    if (this.client?.connected) {
      return;
    }

    try {
      this.client?.activate();
    } catch (error) {
      console.error('Failed to connect to WebSocket:', error);
      this.handleReconnect();
    }
  }

  disconnect() {
    if (this.client?.connected) {
      this.client.deactivate();
    }
  }

  subscribe(topic: string, subscriptionId: string, callback: (message: Message) => void): WebSocketSubscription {
    if (!this.client?.connected) {
      throw new Error('WebSocket is not connected');
    }

    // Unsubscribe if there's an existing subscription
    if (this.subscriptions.has(topic)) {
      this.unsubscribe(topic);
    }

    const subscription = this.client.subscribe(topic, callback, { id: subscriptionId });
    const wsSubscription = {
      id: subscriptionId,
      unsubscribe: () => {
        subscription.unsubscribe();
        this.subscriptions.delete(topic);
      }
    };

    this.subscriptions.set(topic, wsSubscription);
    return wsSubscription;
  }

  unsubscribe(topic: string) {
    const subscription = this.subscriptions.get(topic);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(topic);
    }
  }

  send(destination: string, body: any) {
    if (!this.client?.connected) {
      throw new Error('WebSocket is not connected');
    }

    this.client.publish({
      destination,
      body: JSON.stringify(body)
    });
  }

  isConnected(): boolean {
    return this.client?.connected ?? false;
  }
}

// Create a singleton instance
const websocketService = new WebSocketService();
export default websocketService; 