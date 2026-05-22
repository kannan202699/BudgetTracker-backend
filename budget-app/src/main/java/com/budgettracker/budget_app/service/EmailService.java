package com.budgettracker.budget_app.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Sends HTML emails for OTP verification and password reset; silently skips if the mail sender is not configured.
 */
@Service
@Slf4j
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:5050}")
    private String frontendUrl;

    @Value("${app.name:BudgetPro}")
    private String appName;

    /**
     * Returns true if the mail sender and from-email are both configured.
     */
    public boolean isConfigured() {
        return mailSender != null && fromEmail != null && !fromEmail.isBlank();
    }

    // ── Email Verification OTP ──────────────────────────────────────────────

    /**
     * Sends a styled 6-digit OTP email; logs a warning and returns early if the mail service is unconfigured.
     */
    public void sendEmailVerificationOtp(String toEmail, String otp) {
        if (!isConfigured()) {
            log.warn("Email service not configured — OTP email skipped for: {}", toEmail);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(appName + " — Your verification code");
            helper.setText(buildOtpEmailHtml(otp), true);
            mailSender.send(message);
            log.info("OTP email sent to: {}", toEmail);
        } catch (MessagingException | RuntimeException e) {
            log.error("Failed to send OTP email to: {}", toEmail, e);
        }
    }

    private String buildOtpEmailHtml(String otp) {
        String[] digits = otp.split("");
        StringBuilder digitBoxes = new StringBuilder();
        for (String d : digits) {
            digitBoxes.append("""
                    <td style="width:44px;height:54px;background:#1e1e3a;border:2px solid #667eea;
                               border-radius:10px;text-align:center;vertical-align:middle;
                               font-size:26px;font-weight:800;color:#fff;padding:0;font-family:monospace;">
                      %s
                    </td>
                    <td style="width:8px;"></td>
                    """.formatted(d));
        }
        return """
                <!DOCTYPE html>
                <html lang="en">
                <body style="margin:0;padding:0;font-family:Arial,sans-serif;background:#f0f2f5;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="padding:40px 20px;">
                    <tr><td align="center">
                      <table width="560" cellpadding="0" cellspacing="0"
                             style="background:#1a1a2e;border-radius:16px;overflow:hidden;
                                    box-shadow:0 8px 32px rgba(0,0,0,0.3);max-width:560px;width:100%%;">
                        <tr>
                          <td style="background:linear-gradient(135deg,#667eea,#764ba2);padding:32px;text-align:center;">
                            <h1 style="color:#fff;margin:0;font-size:24px;letter-spacing:-0.5px;">💰 %s</h1>
                            <p style="color:rgba(255,255,255,0.75);margin:6px 0 0;font-size:13px;">Email Verification</p>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:36px 36px 28px;color:#e0e0e0;text-align:center;">
                            <p style="font-size:15px;color:#b0b0c0;margin:0 0 28px;line-height:1.6;">
                              Use the code below to verify your email address.<br>
                              This code expires in <strong style="color:#a78bfa;">10 minutes</strong>.
                            </p>
                            <table cellpadding="0" cellspacing="0" style="margin:0 auto 28px;">
                              <tr>%s</tr>
                            </table>
                            <p style="font-size:13px;color:#555570;margin:0;">
                              If you didn't request this, you can safely ignore this email.
                            </p>
                          </td>
                        </tr>
                        <tr>
                          <td style="background:#111124;padding:18px 36px;text-align:center;
                                     border-top:1px solid rgba(255,255,255,0.06);">
                            <p style="color:#444460;font-size:12px;margin:0;">
                              &copy; 2025 %s &middot; Do not reply to this email
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(appName, digitBoxes, appName);
    }

    // ── Password Reset ──────────────────────────────────────────────────────

    /**
     * Sends a styled password-reset link email; logs a warning and returns early if unconfigured.
     */
    public void sendPasswordResetEmail(String toEmail, String username, String resetToken) {
        if (!isConfigured()) {
            log.warn("Email service not configured — password reset email skipped for: {}", toEmail);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(appName + " — Password Reset Request");
            String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
            helper.setText(buildResetEmailHtml(username, resetLink), true);
            mailSender.send(message);
            log.info("Password reset email sent to: {}", toEmail);
        } catch (MessagingException | RuntimeException e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
        }
    }

    private String buildResetEmailHtml(String username, String resetLink) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <body style="margin:0;padding:0;font-family:Arial,sans-serif;background:#f0f2f5;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="padding:40px 20px;">
                    <tr><td align="center">
                      <table width="600" cellpadding="0" cellspacing="0"
                             style="background:#1a1a2e;border-radius:16px;overflow:hidden;box-shadow:0 8px 32px rgba(0,0,0,0.3);max-width:600px;width:100%%;">
                        <tr>
                          <td style="background:linear-gradient(135deg,#667eea,#764ba2);padding:36px;text-align:center;">
                            <h1 style="color:#fff;margin:0;font-size:26px;letter-spacing:-0.5px;">💰 %s</h1>
                            <p style="color:rgba(255,255,255,0.75);margin:8px 0 0;font-size:14px;">Password Reset Request</p>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:40px 36px;color:#e0e0e0;">
                            <p style="font-size:16px;margin:0 0 12px;">Hi <strong>%s</strong>,</p>
                            <p style="font-size:14px;line-height:1.7;color:#b0b0c0;margin:0 0 28px;">
                              We received a request to reset your BudgetPro password. Click the button below
                              — this link is valid for <strong style="color:#a78bfa;">15 minutes</strong> and can only be used once.
                            </p>
                            <div style="text-align:center;margin:0 0 28px;">
                              <a href="%s"
                                 style="display:inline-block;background:linear-gradient(135deg,#667eea,#764ba2);
                                        color:#fff;padding:14px 36px;border-radius:10px;text-decoration:none;
                                        font-weight:700;font-size:15px;letter-spacing:0.3px;">
                                Reset My Password
                              </a>
                            </div>
                            <p style="font-size:13px;color:#7a7a8c;line-height:1.7;margin:0 0 16px;">
                              If you did not request this reset, please ignore this email.
                            </p>
                            <p style="font-size:12px;color:#555570;word-break:break-all;margin:0;">
                              Or copy this link:<br>
                              <a href="%s" style="color:#667eea;">%s</a>
                            </p>
                          </td>
                        </tr>
                        <tr>
                          <td style="background:#111124;padding:20px 36px;text-align:center;border-top:1px solid rgba(255,255,255,0.06);">
                            <p style="color:#444460;font-size:12px;margin:0;">
                              &copy; 2025 %s &middot; Automated email &mdash; please do not reply
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(appName, username, resetLink, resetLink, resetLink, appName);
    }
}
