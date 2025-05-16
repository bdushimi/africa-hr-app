
import React, { useState, useRef, useEffect } from 'react';
import { useNotifications } from '@/contexts/NotificationContext';
import { Bell, BellOff } from 'lucide-react';
import { useOnClickOutside } from '@/hooks/use-on-click-outside';
import NotificationItem from './NotificationItem';
import { cn } from '@/lib/utils';

const NotificationsDropdown: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const {
    notifications,
    unreadCount,
    markAsRead,
    markAllAsRead,
    dismissNotification,
    dismissAll,
    addRandomNotification
  } = useNotifications();

  // Close dropdown when clicked outside
  useOnClickOutside(dropdownRef, () => setIsOpen(false));

  // Handle toggling the dropdown
  const toggleDropdown = () => {
    setIsOpen(!isOpen);
  };

  return (
    <div className="relative" ref={dropdownRef}>
      {/* Notification Bell Button */}
      <button
        onClick={toggleDropdown}
        className="relative rounded-full p-2 text-gray-600 hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
        aria-label="Notifications"
      >
        <Bell className="h-5 w-5" />
        {unreadCount > 0 && (
          <span className="absolute top-0 right-0 flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-[10px] font-medium text-white">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {/* Notification Dropdown */}
      {isOpen && (
        <div className="absolute right-0 mt-2 w-80 bg-white rounded-md shadow-lg overflow-hidden z-50 border border-gray-200">
          {/* Dropdown Header */}
          <div className="px-4 py-3 border-b border-gray-200 flex justify-between items-center">
            <h3 className="text-sm font-semibold text-gray-700">
              Notifications
              {unreadCount > 0 && (
                <span className="ml-1 bg-blue-100 text-blue-600 py-0.5 px-2 rounded-full text-xs">
                  {unreadCount} new
                </span>
              )}
            </h3>
            <div className="space-x-2">
              {unreadCount > 0 && (
                <button
                  onClick={async () => await markAllAsRead()}
                  className="text-xs text-blue-600 hover:text-blue-800 font-medium"
                >
                  Mark all read
                </button>
              )}
            </div>
          </div>

          {/* Notifications List */}
          <div className={cn("overflow-y-auto", notifications.length > 0 ? "max-h-80" : "")}>
            {notifications.length > 0 ? (
              notifications.map(notification => (
                <NotificationItem
                  key={notification.id}
                  notification={notification}
                  onMarkAsRead={async () => await markAsRead(notification.id)}
                  onDismiss={() => dismissNotification(notification.id)}
                />
              ))
            ) : (
              <div className="p-6 text-center">
                <BellOff className="h-8 w-8 mx-auto text-gray-400 mb-2" />
                <p className="text-gray-500 text-sm">No notifications</p>
              </div>
            )}
          </div>

        </div>
      )}
    </div>
  );
};

export default NotificationsDropdown;
