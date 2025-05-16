# Real-Time Notifications with WebSockets

This document explains how the WebSocket-based real-time notification system works in the leave management application.

## Overview

The system uses WebSockets with STOMP (Simple Text Oriented Messaging Protocol) to deliver real-time notifications to users. When events occur in the application (e.g., leave request approval), notifications are sent to relevant users via WebSocket if they are online, and are also stored in the database for retrieval later.

## Server-Side Implementation

### Components

1. **Notification Entity**: Represents a notification stored in the database.
2. **WebSocketConfig**: Configures the WebSocket connection points and message broker.
3. **WebSocketSecurityConfig**: Secures WebSocket connections with authentication requirements.
4. **AuthChannelInterceptor**: Authenticates WebSocket connections using JWT tokens.
5. **WebSocketSessionRegistry**: Tracks which users are currently connected.
6. **WebSocketNotificationService**: Sends notifications to users over WebSocket.
7. **WebSocketController**: Handles WebSocket messages from clients.

### Authentication Flow

1. The client connects to the WebSocket endpoint with a JWT token in the headers.
2. The `AuthChannelInterceptor` validates the token and authenticates the user.
3. The user's connection is registered in the `WebSocketSessionRegistry`.
4. When the user disconnects, their session is removed from the registry.

### Notification Flow

1. When an event occurs (e.g., leave approval):
   - A notification is created and stored in the database
   - The system checks if the target user is currently connected
   - If connected, the notification is sent in real-time via WebSocket
   - If not connected, the notification is only stored in the database

2. When users log in:
   - They can retrieve their notifications via REST API or WebSocket
   - They receive new notifications in real-time while connected

## Client-Side Implementation

### Components

1. **NotificationService.js**: Manages WebSocket connections and notifications.
2. **NotificationsComponent.jsx**: React component to display and interact with notifications.

### Connection Flow

1. User logs in and receives an authentication token.
2. The client establishes a WebSocket connection with the token.
3. The client subscribes to personal notification channels.
4. When notifications arrive, they are displayed to the user.

### Interacting with Notifications

1. **Retrieving Notifications**: The client sends a message to `/app/notifications` to get all notifications.
2. **Marking as Read**: The client sends a message to `/app/notifications/mark-read` with the notification ID.
3. **Marking All as Read**: The client sends a message to `/app/notifications/mark-all-read`.

## Integration Points

The notification system is integrated with the leave management system at the following points:

1. **Leave Request Submission**: Manager is notified when an employee submits a leave request.
2. **Leave Request Approval**: Employee is notified when their leave request is approved.
3. **Leave Request Rejection**: Employee is notified when their leave request is rejected.
4. **Leave Request Cancellation**: Manager is notified when an employee cancels a leave request.

## Testing the System

To test the WebSocket notification system:

1. Start the server application
2. Use the included client example code to connect to the WebSocket
3. Perform actions like submitting or approving leave requests
4. Observe real-time notifications appearing in the client

## Security Considerations

- All WebSocket connections and messages are authenticated
- User-specific notifications are sent only to the intended recipient
- WebSocket connections use the same JWT tokens as the REST API
- Message destinations are secured based on user roles

## Troubleshooting

- Check browser console for WebSocket connection errors
- Verify that the JWT token is valid and included in the connection headers
- Ensure the user has the correct permissions for the requested destinations
- Review server logs for authentication and message routing issues 