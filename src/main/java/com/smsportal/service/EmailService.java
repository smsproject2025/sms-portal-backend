package com.smsportal.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    @Qualifier("godaddyMailSender")
    private final JavaMailSender godaddyMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${app.name:SMSPortal}")
    private String appName;

    @Async
    public void sendPasswordResetEmail(String toEmail, String userName, String resetToken) {
        try {
            String resetLink = frontendUrl + "/auth/reset-password?token=" + resetToken;

            MimeMessage message = godaddyMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, appName);
            helper.setTo(toEmail);
            helper.setSubject("Reset your " + appName + " password");
            helper.setText(buildResetEmailHtml(userName, resetLink, frontendUrl, appName), true);

            godaddyMailSender.send(message);
            log.info("Password reset email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendPasswordChangedEmail(String toEmail, String userName) {
        try {
            MimeMessage message = godaddyMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, appName);
            helper.setTo(toEmail);
            helper.setSubject("Your " + appName + " password was changed");
            helper.setText(buildPasswordChangedHtml(userName, appName, frontendUrl), true);

            godaddyMailSender.send(message);
            log.info("Password changed email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send password changed email to {}: {}", toEmail, e.getMessage());
        }
    }

    // ── HTML Templates ────────────────────────────────────────────────

    private String buildResetEmailHtml(String name, String resetLink,
                                        String frontendUrl, String appName) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin:0;padding:0;background:#0a0c10;font-family:'DM Sans',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#0a0c10;padding:40px 0;">
                <tr><td align="center">
                  <table width="520" cellpadding="0" cellspacing="0"
                    style="background:#1c2029;border:1px solid #2a2f3d;border-radius:16px;overflow:hidden;">

                    <!-- Header -->
                    <tr>
                      <td style="background:linear-gradient(135deg,#0d1117,#0a1929);
                                 padding:32px;text-align:center;border-bottom:1px solid #2a2f3d;">
                        <div style="font-size:28px;margin-bottom:8px;">⚡</div>
                        <div style="font-family:Arial,sans-serif;font-size:22px;font-weight:800;
                                    color:#e8eaf0;letter-spacing:-0.5px;">%s</div>
                      </td>
                    </tr>

                    <!-- Body -->
                    <tr>
                      <td style="padding:36px 40px;">
                        <p style="margin:0 0 12px;font-size:24px;font-weight:700;color:#e8eaf0;">
                          Hi %s,
                        </p>
                        <p style="margin:0 0 24px;font-size:15px;color:#8892a4;line-height:1.7;">
                          We received a request to reset the password for your %s account.
                          Click the button below to choose a new password. This link expires in
                          <strong style="color:#e8eaf0;">1 hour</strong>.
                        </p>

                        <!-- CTA Button -->
                        <table width="100%%" cellpadding="0" cellspacing="0" style="margin:0 0 28px;">
                          <tr><td align="center">
                            <a href="%s"
                               style="display:inline-block;background:#00e5ff;color:#000;
                                      font-size:15px;font-weight:700;padding:14px 36px;
                                      border-radius:10px;text-decoration:none;
                                      letter-spacing:0.02em;">
                              Reset My Password
                            </a>
                          </td></tr>
                        </table>

                        <!-- Fallback link -->
                        <p style="margin:0 0 8px;font-size:13px;color:#555e72;">
                          Or copy this link into your browser:
                        </p>
                        <p style="margin:0 0 28px;font-size:12px;color:#00e5ff;
                                  word-break:break-all;background:#111318;
                                  padding:10px 14px;border-radius:8px;
                                  border:1px solid #2a2f3d;">
                          %s
                        </p>

                        <!-- Warning -->
                        <div style="background:#1a1209;border:1px solid rgba(255,215,64,0.2);
                                    border-left:3px solid #ffd740;border-radius:8px;padding:14px 16px;">
                          <p style="margin:0;font-size:13px;color:#ffd740;">
                            ⚠️ If you didn't request a password reset, you can safely ignore this email.
                            Your password will not change.
                          </p>
                        </div>
                      </td>
                    </tr>

                    <!-- Footer -->
                    <tr>
                      <td style="padding:20px 40px;border-top:1px solid #2a2f3d;text-align:center;">
                        <p style="margin:0;font-size:12px;color:#555e72;">
                          © %s · <a href="%s" style="color:#8892a4;text-decoration:none;">Visit site</a>
                        </p>
                      </td>
                    </tr>

                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(appName, name, appName, resetLink, resetLink, appName, frontendUrl);
    }

    private String buildPasswordChangedHtml(String name, String appName, String frontendUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0;padding:0;background:#0a0c10;font-family:Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#0a0c10;padding:40px 0;">
                <tr><td align="center">
                  <table width="520" cellpadding="0" cellspacing="0"
                    style="background:#1c2029;border:1px solid #2a2f3d;border-radius:16px;overflow:hidden;">

                    <tr>
                      <td style="background:linear-gradient(135deg,#0d1117,#0a1929);
                                 padding:32px;text-align:center;border-bottom:1px solid #2a2f3d;">
                        <div style="font-size:28px;margin-bottom:8px;">⚡</div>
                        <div style="font-size:22px;font-weight:800;color:#e8eaf0;">%s</div>
                      </td>
                    </tr>

                    <tr>
                      <td style="padding:36px 40px;">
                        <p style="margin:0 0 12px;font-size:22px;font-weight:700;color:#e8eaf0;">
                          Password changed ✓
                        </p>
                        <p style="margin:0 0 24px;font-size:15px;color:#8892a4;line-height:1.7;">
                          Hi <strong style="color:#e8eaf0;">%s</strong>, your %s account password was
                          successfully changed.
                        </p>
                        <div style="background:#0d1a0d;border:1px solid rgba(0,230,118,0.2);
                                    border-left:3px solid #00e676;border-radius:8px;padding:14px 16px;">
                          <p style="margin:0;font-size:13px;color:#00e676;">
                            ✓ If you made this change, no further action is needed.
                          </p>
                        </div>
                        <p style="margin:24px 0 0;font-size:13px;color:#555e72;">
                          If you did not make this change, please
                          <a href="%s/auth/forgot-password" style="color:#00e5ff;">
                            reset your password immediately
                          </a>.
                        </p>
                      </td>
                    </tr>

                    <tr>
                      <td style="padding:20px 40px;border-top:1px solid #2a2f3d;text-align:center;">
                        <p style="margin:0;font-size:12px;color:#555e72;">© %s</p>
                      </td>
                    </tr>

                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(appName, name, appName, frontendUrl, appName);
    }
}
