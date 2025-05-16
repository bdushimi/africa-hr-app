
import React from 'react';
import { formatDistanceToNow } from 'date-fns';
import { X } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Notification } from '@/contexts/NotificationContext';

interface NotificationItemProps {
  notification: Notification;
  onMarkAsRead: () => void;
  onDismiss: () => void;
}

const NotificationItem: React.FC<NotificationItemProps> = ({
  notification,
  onMarkAsRead,
  onDismiss
}) => {
  const { title, message, type, timestamp, read } = notification;

  // Determine background color based on type and read status
  const getBgColor = () => {
    if (read) return 'bg-gray-50 hover:bg-gray-100';
    
    switch (type) {
      case 'success':
        return 'bg-green-50 hover:bg-green-100';
      case 'error':
        return 'bg-red-50 hover:bg-red-100';
      case 'warning':
        return 'bg-yellow-50 hover:bg-yellow-100';
      case 'info':
      default:
        return 'bg-blue-50 hover:bg-blue-100';
    }
  };

  // Determine dot color based on type
  const getDotColor = () => {
    if (read) return 'bg-gray-300';
    
    switch (type) {
      case 'success':
        return 'bg-green-500';
      case 'error':
        return 'bg-red-500';
      case 'warning':
        return 'bg-yellow-500';
      case 'info':
      default:
        return 'bg-blue-500';
    }
  };

  return (
    <div 
      className={cn(
        'border-b border-gray-200 last:border-b-0 p-4 relative cursor-default transition-colors',
        getBgColor(),
        read ? 'opacity-60' : 'opacity-100'
      )}
      onClick={onMarkAsRead}
    >
      <div className="flex items-start gap-2">
        <div className={cn('w-2 h-2 rounded-full mt-1.5', getDotColor())} />
        <div className="flex-1 min-w-0">
          <div className="flex justify-between items-start">
            <h4 className="text-sm font-medium text-gray-900 truncate pr-6">{title}</h4>
            <button
              onClick={(e) => {
                e.stopPropagation();
                onDismiss();
              }}
              className="text-gray-400 hover:text-gray-600 p-0.5 rounded-full hover:bg-gray-200"
              aria-label="Dismiss notification"
            >
              <X size={14} />
            </button>
          </div>
          <p className="text-sm text-gray-600 mt-1">{message}</p>
          <p className="text-xs text-gray-500 mt-1.5">
            {formatDistanceToNow(timestamp, { addSuffix: true })}
          </p>
        </div>
      </div>
    </div>
  );
};

export default NotificationItem;
