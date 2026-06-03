package com.mtnrs.revenuesync.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final Resend resend;
    private final String fromEmail;

    public EmailService(
            @Value("${resend.api-key}") String apiKey,
            @Value("${resend.from-email}") String fromEmail
    ) {
        this.resend    = new Resend(apiKey);
        this.fromEmail = fromEmail;
    }

    public void sendVerificationEmail(String toEmail, String name, String token) {
        try {
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(toEmail)
                    .subject("Verify your RevenueSync account")
                    .html(buildVerificationHtml(name, token))
                    .build();

            resend.emails().send(params);
            log.info("Verification email sent to={}", toEmail);

        } catch (ResendException e) {
            log.error("Failed to send verification email to={} error={}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    private String buildVerificationHtml(String name, String token) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: monospace; background: #080c14; color: #e2e8f0; padding: 40px;">
                  <div style="max-width: 480px; margin: 0 auto;">
                    <h2 style="color: #00d4aa;">revenue<b>Sync</b></h2>
                    <p>Hello, <strong>%s</strong>.</p>
                    <p>Your verification code is:</p>
                    <div style="font-size: 36px; font-weight: bold; letter-spacing: 12px;
                                color: #00d4aa; padding: 24px; border: 1px solid #1e2d3d;
                                text-align: center; margin: 24px 0;">
                      %s
                    </div>
                    <p style="color: #64748b;">This code expires in 24 hours.</p>
                    <p style="color: #64748b;">If you did not create an account, ignore this email.</p>
                  </div>
                </body>
                </html>
                """.formatted(name, token);
    }
}
