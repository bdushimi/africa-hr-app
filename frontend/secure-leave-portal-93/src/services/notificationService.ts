import api from './api';
import { NotificationType } from '@/contexts/NotificationContext';

// Backend Notification Type from API
export interface BackendNotification {
  id: number;
  userId: number;
  title: string;
  message: string;
  isRead: boolean;
  createdAt: string;
  updatedAt: string;
}

class NotificationService {
  private endPoint = '/notifications'; // Adjust based on your backend configuration

  // Fetch notifications from HTTP API
  async fetchNotifications(): Promise<BackendNotification[]> {
    try {
      const response = await api.get<BackendNotification[]>(this.endPoint);
      return response.data;
    } catch (error) {
      console.error('Failed to fetch notifications:', error);
      return [];
    }
  }

  // Mark a single notification as read
  async markNotificationAsRead(notificationId: number, markAsReadCallback: (id: string) => void): Promise<boolean> {
    try {
      const response = await api.patch(`${this.endPoint}/${notificationId}/read`);
      console.log('Notification marked as read:', response.data);
      // Update local state via callback
      markAsReadCallback(notificationId.toString());
      
      return true;
    } catch (error) {
      console.error(`Failed to mark notification ${notificationId} as read:`, error);
      return false;
    }
  }

  // Mark all notifications as read
  async markAllNotificationsAsRead(markAllAsReadCallback: () => void): Promise<boolean> {
    try {
      const response = await api.patch(`${this.endPoint}/mark-all-as-read`);
      
      // Update local state via callback
      markAllAsReadCallback();
      
      return true;
    } catch (error) {
      console.error('Failed to mark all notifications as read:', error);
      return false;
    }
  }

  // Handle WebSocket notification
  handleWebSocketNotification(notification: BackendNotification, addNotificationCallback: (notification: { title: string; message: string; type: NotificationType }) => void) {
    // Convert backend notification to context notification
    const contextNotification = {
      title: notification.title,
      message: notification.message,
      type: this.determineNotificationType(notification) as NotificationType
    };

    // Add notification via callback
    addNotificationCallback(contextNotification);
  }

  // Determine notification type based on content or other criteria
  public determineNotificationType(notification: BackendNotification) {
    // Example logic to determine notification type
    if (notification.title.toLowerCase().includes('error')) {
      return 'error';
    } else if (notification.title.toLowerCase().includes('success')) {
      return 'success';
    } else if (notification.title.toLowerCase().includes('warning')) {
      return 'warning';
    }
    return 'info';
  }
}

export default new NotificationService();
