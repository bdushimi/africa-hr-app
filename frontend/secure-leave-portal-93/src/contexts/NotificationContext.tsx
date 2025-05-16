import React, { createContext, useContext, useState, useEffect } from 'react';
import { formatDistanceToNow } from 'date-fns';
import websocketService from '@/services/websocketService';
import notificationService from '@/services/notificationService';
import { useAuth } from './AuthContext';

// Define notification types
export type NotificationType = 'info' | 'success' | 'warning' | 'error';

export interface Notification {
  id: string;
  title: string;
  message: string;
  type: NotificationType;
  timestamp: Date;
  read: boolean;
}

interface NotificationContextType {
  notifications: Notification[];
  unreadCount: number;
  addNotification: (notification: Omit<Notification, 'id' | 'timestamp' | 'read'>) => void;
  markAsRead: (id: string) => void;
  markAllAsRead: () => void;
  dismissNotification: (id: string) => void;
  dismissAll: () => void;
  addRandomNotification: () => void;
}

const NotificationContext = createContext<NotificationContextType | undefined>(undefined);

export const useNotifications = () => {
  const context = useContext(NotificationContext);
  if (context === undefined) {
    throw new Error('useNotifications must be used within a NotificationProvider');
  }
  return context;
};

// Sample notification content for random demo notifications
const sampleNotifications = [
  { title: 'Leave Request Approved', message: 'Your annual leave request has been approved', type: 'success' as NotificationType },
  { title: 'Leave Request Rejected', message: 'Your sick leave request has been rejected', type: 'error' as NotificationType },
  { title: 'New Leave Request', message: 'John Doe has submitted a leave request', type: 'info' as NotificationType },
  { title: 'Reminder', message: 'You have unused leave days', type: 'warning' as NotificationType },
  { title: 'System Update', message: 'The leave management system will be updated tomorrow', type: 'info' as NotificationType },
];

export const NotificationProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const { user } = useAuth();

  // Fetch notifications from backend
  const fetchNotifications = async () => {
    if (!user) return;

    try {
      const backendNotifications = await notificationService.fetchNotifications();

      // Convert backend notifications to frontend notifications
      const convertedNotifications = backendNotifications.map(notification => ({
        id: notification.id.toString(),
        title: notification.title,
        message: notification.message,
        type: notificationService.determineNotificationType(notification) as NotificationType,
        timestamp: new Date(notification.createdAt),
        read: notification.read
      }));

      setNotifications(convertedNotifications);
    } catch (error) {
      console.error('Failed to fetch notifications:', error);
    }
  };

  // Calculate unread notifications count
  const unreadCount = notifications.filter(notification => !notification.read).length;

  // Fetch notifications when user is available
  useEffect(() => {
    if (user) {
      fetchNotifications();
    }
  }, [user]);

  // Load notifications from localStorage on initial render
  useEffect(() => {
    const storedNotifications = localStorage.getItem('notifications');
    if (storedNotifications) {
      try {
        const parsedNotifications = JSON.parse(storedNotifications).map((n: any) => ({
          ...n,
          timestamp: new Date(n.timestamp)
        }));
        setNotifications(parsedNotifications);
      } catch (error) {
        console.error('Failed to parse notifications from localStorage:', error);
      }
    }
  }, []);

  // Save notifications to localStorage when they change
  useEffect(() => {
    localStorage.setItem('notifications', JSON.stringify(notifications));
  }, [notifications]);

  // Connect to WebSocket when the component mounts and user is authenticated
  // useEffect(() => {
  //   if (user) {
  //     // Connect to WebSocket
  //     websocketService.connect();

  //     // Subscribe to user-specific notifications
  //     const userNotificationsTopic = `/user/${user.id}/notifications`;
  //     try {
  //       websocketService.subscribe(
  //         userNotificationsTopic,
  //         'user-notifications',
  //         (message) => {
  //           const notification = JSON.parse(message.body);
  //           addNotification({
  //             title: notification.title,
  //             message: notification.message,
  //             type: notification.type as NotificationType
  //           });
  //         }
  //       );

  //       // Subscribe to broadcast notifications
  //       websocketService.subscribe(
  //         '/topic/broadcast',
  //         'broadcast-notifications',
  //         (message) => {
  //           const notification = JSON.parse(message.body);
  //           addNotification({
  //             title: notification.title,
  //             message: notification.message,
  //             type: notification.type as NotificationType
  //           });
  //         }
  //       );
  //     } catch (error) {
  //       console.error('Failed to subscribe to notifications:', error);
  //     }

  //     // Cleanup function
  //     return () => {
  //       websocketService.unsubscribe(userNotificationsTopic);
  //       websocketService.unsubscribe('/topic/broadcast');
  //       websocketService.disconnect();
  //     };
  //   }
  // }, [user]);

  // Generate a random ID
  const generateId = () => Math.random().toString(36).substring(2, 9);

  // Add a new notification
  const addNotification = (notification: Omit<Notification, 'id' | 'timestamp' | 'read'>) => {
    const newNotification: Notification = {
      id: generateId(),
      timestamp: new Date(),
      read: false,
      ...notification
    };
    setNotifications(prev => [newNotification, ...prev]);
  };

  // Add a random notification (demo functionality)
  const addRandomNotification = () => {
    const randomNotification = sampleNotifications[Math.floor(Math.random() * sampleNotifications.length)];
    addNotification(randomNotification);
  };

  // Mark a notification as read
  const markAsRead = async (id: string) => {
    try {
      const numericId = parseInt(id, 10);
      if (isNaN(numericId)) {
        console.error(`Invalid notification ID: ${id}`);
        return;
      }

      // Optimistically update local state
      setNotifications(prev =>
        prev.map(notification =>
          notification.id === id ? { ...notification, read: true } : notification
        )
      );

      // Call backend to mark notification as read
      const success = await notificationService.markNotificationAsRead(numericId, () => {
        console.log(`Notification ${id} marked as read`);
      });

      // If backend call fails, revert local state
      if (!success) {
        setNotifications(prev =>
          prev.map(notification =>
            notification.id === id ? { ...notification, read: false } : notification
          )
        );
      } else {
        // Re-fetch notifications to ensure UI is in sync with backend
        await fetchNotifications();
      }
    } catch (error) {
      console.error(`Failed to mark notification ${id} as read:`, error);
      // Revert local state on error
      setNotifications(prev =>
        prev.map(notification =>
          notification.id === id ? { ...notification, read: false } : notification
        )
      );
    }
  };

  // Mark all notifications as read
  const markAllAsRead = async () => {
    try {
      // Call backend to mark all notifications as read
      const success = await notificationService.markAllNotificationsAsRead(() => {
        // Update local state to mark all notifications as read
        setNotifications(prev =>
          prev.map(notification => ({ ...notification, read: true }))
        );
      });
    } catch (error) {
      console.error('Failed to mark all notifications as read:', error);
    }
  };

  // Dismiss a notification
  const dismissNotification = (id: string) => {
    setNotifications(prev =>
      prev.filter(notification => notification.id !== id)
    );
  };

  // Dismiss all notifications
  const dismissAll = () => {
    setNotifications([]);
  };

  return (
    <NotificationContext.Provider
      value={{
        notifications,
        unreadCount,
        addNotification,
        markAsRead,
        markAllAsRead,
        dismissNotification,
        dismissAll,
        addRandomNotification,
      }}
    >
      {children}
    </NotificationContext.Provider>
  );
};
