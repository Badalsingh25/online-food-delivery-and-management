package com.hungerexpress.menu;

import com.hungerexpress.restaurant.Restaurant;
import com.hungerexpress.user.User;
import com.hungerexpress.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationService {
    
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    
    @Value("${app.admin.email:badalkusingh8@gmail.com}")
    private String adminEmail;
    
    public void notifyNewItemSubmitted(MenuItem item, Restaurant restaurant) {
        try {
            // Get admin user
            Optional<User> adminUser = userRepository.findByEmail(adminEmail);
            if (adminUser.isEmpty()) {
                log.warn("Admin user not found with email: {}", adminEmail);
                return;
            }
            
            String subject = "🍽️ New Menu Item Awaiting Approval";
            String body = String.format("""
                Dear Admin,
                
                A new menu item has been submitted for approval:
                
                📝 Item Details:
                - Name: %s
                - Restaurant: %s
                - Price: ₹%.2f
                - Description: %s
                - Category: %s
                
                ⏰ Submitted At: %s
                
                Please review and approve/reject this item in the admin panel:
                http://localhost:4200/admin/menu-approvals
                
                This item will not be visible to customers until you approve it.
                
                Best regards,
                HungerExpress System
                """,
                item.getName(),
                restaurant.getName(),
                item.getPrice(),
                item.getDescription() != null ? item.getDescription() : "N/A",
                item.getCategory() != null ? item.getCategory().getName() : "N/A",
                item.getSubmittedAt()
            );
            
            sendEmail(adminEmail, subject, body);
            log.info("Notification sent to admin for new item: {}", item.getName());
            
        } catch (Exception e) {
            log.error("Failed to send notification to admin", e);
        }
    }
    
    public void notifyItemUpdated(MenuItem item, Restaurant restaurant) {
        try {
            String subject = "🔄 Menu Item Updated - Re-approval Required";
            String body = String.format("""
                Dear Admin,
                
                A previously approved menu item has been updated and requires re-approval:
                
                📝 Updated Item Details:
                - Name: %s
                - Restaurant: %s
                - Price: ₹%.2f
                - Description: %s
                
                ⏰ Updated At: %s
                
                Please review the changes and approve/reject:
                http://localhost:4200/admin/menu-approvals
                
                This item has been temporarily hidden from customers until re-approved.
                
                Best regards,
                HungerExpress System
                """,
                item.getName(),
                restaurant.getName(),
                item.getPrice(),
                item.getDescription() != null ? item.getDescription() : "N/A",
                item.getSubmittedAt()
            );
            
            sendEmail(adminEmail, subject, body);
            log.info("Re-approval notification sent to admin for item: {}", item.getName());
            
        } catch (Exception e) {
            log.error("Failed to send update notification to admin", e);
        }
    }
    
    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("noreply@hungerexpress.com");
            
            mailSender.send(message);
        } catch (Exception e) {
            // If email fails, just log it - don't break the flow
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
