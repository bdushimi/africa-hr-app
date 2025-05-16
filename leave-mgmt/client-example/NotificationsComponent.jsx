import React, { useState, useEffect } from 'react';
import notificationService from './NotificationService';

/**
 * Component for displaying and managing user notifications.
 * This is a client-side example for React applications.
 */
const NotificationsComponent = ({ authToken, serverUrl }) => {
    const [notifications, setNotifications] = useState([]);
    const [isConnected, setIsConnected] = useState(false);
    const [error, setError] = useState(null);

    useEffect(() => {
        // Set up event handlers for the notification service
        notificationService.registerHandlers({
            onConnect: () => {
                setIsConnected(true);
                setError(null);
            },
            onDisconnect: () => {
                setIsConnected(false);
            },
            onNotification: (notification) => {
                setNotifications(prev => {
                    // Check if the notification already exists
                    const exists = prev.some(n => n.id === notification.id);
                    if (exists) {
                        // Update the existing notification
                        return prev.map(n => n.id === notification.id ? notification : n);
                    } else {
                        // Add the new notification
                        return [notification, ...prev];
                    }
                });
            },
            onError: (error) => {
                setError(`Connection error: ${error.headers?.message || 'Unknown error'}`);
                setIsConnected(false);
            }
        });

        // Connect to the WebSocket when the component mounts
        if (authToken && serverUrl) {
            notificationService.connect(serverUrl, authToken);
        }

        // Disconnect when the component unmounts
        return () => {
            notificationService.disconnect();
        };
    }, [authToken, serverUrl]);

    // Mark a notification as read
    const handleMarkAsRead = (notificationId) => {
        notificationService.markAsRead(notificationId);

        // Optimistically update the UI
        setNotifications(prev =>
            prev.map(n => n.id === notificationId
                ? { ...n, isRead: true }
                : n
            )
        );
    };

    // Mark all notifications as read
    const handleMarkAllAsRead = () => {
        notificationService.markAllAsRead();

        // Optimistically update the UI
        setNotifications(prev =>
            prev.map(n => ({ ...n, isRead: true }))
        );
    };

    return (
        <div className="notifications-container">
            <div className="notifications-header">
                <h2>Notifications</h2>
                <div className="notifications-status">
                    {isConnected ? (
                        <span className="status-connected">Connected</span>
                    ) : (
                        <span className="status-disconnected">Disconnected</span>
                    )}
                </div>
            </div>

            {error && (
                <div className="error-message">{error}</div>
            )}

            <div className="notifications-actions">
                <button
                    onClick={handleMarkAllAsRead}
                    disabled={!isConnected || notifications.every(n => n.isRead)}
                >
                    Mark All as Read
                </button>
            </div>

            {notifications.length === 0 ? (
                <div className="no-notifications">No notifications</div>
            ) : (
                <ul className="notifications-list">
                    {notifications.map(notification => (
                        <li
                            key={notification.id}
                            className={`notification-item ${notification.isRead ? 'read' : 'unread'}`}
                        >
                            <div className="notification-header">
                                <span className="notification-title">{notification.title}</span>
                                <span className="notification-time">
                                    {new Date(notification.createdAt).toLocaleString()}
                                </span>
                            </div>
                            <div className="notification-message">{notification.message}</div>
                            {!notification.isRead && (
                                <button
                                    className="mark-as-read-btn"
                                    onClick={() => handleMarkAsRead(notification.id)}
                                >
                                    Mark as Read
                                </button>
                            )}
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
};

export default NotificationsComponent; 