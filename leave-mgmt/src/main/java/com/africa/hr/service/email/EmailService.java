package com.africa.hr.service.email;

import com.africa.hr.model.LeaveRequest;
import com.africa.hr.model.LeaveRequestStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

        private final JavaMailSender mailSender;

        public void sendLeaveRequestNotification(LeaveRequest leaveRequest) {
                log.info("Sending leave request notification for request: {}", leaveRequest.getId());

                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(leaveRequest.getEmployee().getManager().getEmail());
                message.setSubject("New Leave Request Notification");
                message.setText(String.format(
                                "A new leave request has been submitted by %s.\n\n" +
                                                "Leave Type: %s\n" +
                                                "Start Date: %s\n" +
                                                "End Date: %s\n" +
                                                "Reason: %s\n\n" +
                                                "Please review and take appropriate action.",
                                leaveRequest.getEmployee().getFullName(),
                                leaveRequest.getLeaveType().getName(),
                                leaveRequest.getStartDate(),
                                leaveRequest.getEndDate(),
                                leaveRequest.getLeaveRequestReason()));

                mailSender.send(message);
        }

        public void sendLeaveRequestStatusNotification(LeaveRequest leaveRequest) {
                log.info("Sending leave request status notification for request: {}", leaveRequest.getId());

                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(leaveRequest.getEmployee().getEmail());
                message.setSubject("Leave Request Status Update");

                String statusMessage = leaveRequest.getStatus() == LeaveRequestStatus.APPROVED
                                ? "Your leave request has been approved."
                                : String.format("Your leave request has been rejected.\nReason: %s",
                                                leaveRequest.getRejectionReason());

                message.setText(String.format(
                                "Dear %s,\n\n" +
                                                "%s\n\n" +
                                                "Leave Type: %s\n" +
                                                "Start Date: %s\n" +
                                                "End Date: %s\n" +
                                                "Approved By: %s\n" +
                                                "Approved At: %s",
                                leaveRequest.getEmployee().getFullName(),
                                statusMessage,
                                leaveRequest.getLeaveType().getName(),
                                leaveRequest.getStartDate(),
                                leaveRequest.getEndDate(),
                                leaveRequest.getManager().getFullName(),
                                leaveRequest.getApprovedAt()));

                mailSender.send(message);
        }
}