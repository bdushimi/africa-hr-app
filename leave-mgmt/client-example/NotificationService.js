import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

/**
 * A service for managing WebSocket connections and notifications.
 * This is a client-side example for React applications.
 */
class NotificationService {
    constructor() {
        this.stompClient = null;
        this.connected = false;
        this.subscriptions = {};
        this.handlers = {
            onConnect: null,
            onDisconnect: null,
            onNotification: null,
            onError: null
        };
    }

    /**
     * Register event handlers
     * 
     * @param {Object} handlers - Event handlers
     * @param {Function} handlers.onConnect - Called when successfully connected
     * @param {Function} handlers.onDisconnect - Called when disconnected
     * @param {Function} handlers.onNotification - Called when a notification is received
     * @param {Function} handlers.onError - Called when an error occurs
     */
    registerHandlers(handlers) {
        this.handlers = { ...this.handlers, ...handlers };
    }

    /**
     * Connect to the WebSocket server
     * 
     * @param {string} serverUrl - The server URL
     * @param {string} accessToken - JWT access token for authentication
     */
    connect(serverUrl, accessToken) {
        if (this.connected) {
            console.log('Already connected to WebSocket');
            return;
        }

        // Create a SockJS instance for the WebSocket connection
        const socket = new SockJS(`${serverUrl}/ws`);

        // Create a STOMP client
        this.stompClient = new Client({
            webSocketFactory: () => socket,
            connectHeaders: {
                Authorization: `Bearer ${accessToken}`
            },
            debug: function (str) {
                console.debug('STOMP: ' + str);
            },
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000
        });

        // Set up connection handlers
        this.stompClient.onConnect = (frame) => {
            this.connected = true;
            console.log('Connected to WebSocket:', frame);

            // Subscribe to personal notifications
            this.subscribeToNotifications();

            // Call the onConnect handler if provided
            if (this.handlers.onConnect) {
                this.handlers.onConnect(frame);
            }
        };

        this.stompClient.onStompError = (frame) => {
            console.error('STOMP error:', frame);
            if (this.handlers.onError) {
                this.handlers.onError(frame);
            }
        };

        this.stompClient.onWebSocketClose = () => {
            this.connected = false;
            console.log('WebSocket connection closed');
            if (this.handlers.onDisconnect) {
                this.handlers.onDisconnect();
            }
        };

        // Activate the connection
        this.stompClient.activate();
    }

    /**
     * Disconnect from the WebSocket server
     */
    disconnect() {
        if (this.stompClient) {
            Object.keys(this.subscriptions).forEach(key => {
                if (this.subscriptions[key]) {
                    this.subscriptions[key].unsubscribe();
                    delete this.subscriptions[key];
                }
            });

            this.stompClient.deactivate();
            this.connected = false;
            console.log('Disconnected from WebSocket');
        }
    }

    /**
     * Subscribe to personal notifications
     */
    subscribeToNotifications() {
        if (!this.connected || !this.stompClient) {
            console.warn('Cannot subscribe: not connected to WebSocket');
            return;
        }

        // Subscribe to personal notifications queue
        this.subscriptions.notifications = this.stompClient.subscribe(
            '/user/queue/notifications',
            (message) => {
                try {
                    const notification = JSON.parse(message.body);
                    console.log('Received notification:', notification);

                    if (this.handlers.onNotification) {
                        this.handlers.onNotification(notification);
                    }
                } catch (error) {
                    console.error('Error parsing notification:', error);
                }
            }
        );

        // Request initial notifications from the server
        this.stompClient.publish({
            destination: '/app/notifications',
            body: JSON.stringify({}),
            headers: { 'content-type': 'application/json' }
        });
    }

    /**
     * Mark a notification as read
     * 
     * @param {string} notificationId - The notification ID to mark as read
     */
    markAsRead(notificationId) {
        if (!this.connected || !this.stompClient) {
            console.warn('Cannot mark as read: not connected to WebSocket');
            return;
        }

        this.stompClient.publish({
            destination: '/app/notifications/mark-read',
            body: notificationId,
            headers: { 'content-type': 'text/plain' }
        });
    }

    /**
     * Mark all notifications as read
     */
    markAllAsRead() {
        if (!this.connected || !this.stompClient) {
            console.warn('Cannot mark all as read: not connected to WebSocket');
            return;
        }

        this.stompClient.publish({
            destination: '/app/notifications/mark-all-read',
            body: JSON.stringify({}),
            headers: { 'content-type': 'application/json' }
        });
    }
}

// Create a singleton instance
const notificationService = new NotificationService();
export default notificationService; 