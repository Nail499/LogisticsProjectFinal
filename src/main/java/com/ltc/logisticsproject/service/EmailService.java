package com.ltc.logisticsproject.service;

import com.ltc.logisticsproject.entity.VerificationPurpose;
import jakarta.mail.internet.MimeMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

// Brendləşdirilmiş HTML email göndərən wrapper (spring-boot-starter-mail +
// Gmail SMTP, bax application.properties#spring.mail.*). Saytın rəng
// palitrası ilə üst-üstə düşür (tünd header #111827, narıncı vurğu
// #fe8704, bax frontend/src/pages/public/Login.jsx-dəki eyni rənglər) və
// loqonu (frontend/src/assets/fleetra-mark.svg-dən PNG-yə çevrilib,
// resources/mail/fleetra-mark.png) "cid:" inline şəkil kimi header-ə
// yerləşdirir. İki cür email göndərir: (1) təsdiq kodu (6 rəqəmli qutu —
// qeydiyyat/şifrə bərpası, bax sendVerificationCode) və (2) ümumi bildiriş
// (ikon + mesaj + "Bax" düyməsi — xoş gəlmisiniz/sifariş/reys statusu,
// bax sendNotificationEmail, istifadə olunduğu yer: NotificationService).
// Hər iki forma eyni header/footer-i paylaşır ki, bütün email-lər eyni
// brend görünüşündə olsun.
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailService {

    static final String NAVY = "#111827";
    static final String ORANGE = "#fe8704";

    final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    String fromAddress;

    public void sendVerificationCode(String toEmail, String code, VerificationPurpose purpose) {
        String subject;
        String title;
        String intro;
        String note;
        switch (purpose) {
            case REGISTER -> {
                subject = "Fleetra — email təsdiq kodu";
                title = "Email ünvanınızı təsdiqləyin";
                intro = "Fleetra hesabınızı aktivləşdirmək üçün aşağıdakı kodu qeydiyyat pəncərəsinə daxil edin:";
                note = "Bu kod 15 dəqiqə ərzində etibarlıdır. Əgər bu qeydiyyatı siz etməmisinizsə, bu email-i nəzərə almayın.";
            }
            case EMAIL_CHANGE -> {
                subject = "Fleetra — email dəyişmə kodu";
                title = "Yeni email ünvanınızı təsdiqləyin";
                intro = "Profilinizdə email ünvanını dəyişmək üçün aşağıdakı kodu daxil edin:";
                note = "Bu kod 15 dəqiqə ərzində etibarlıdır. Əgər bu tələbi siz etməmisinizsə, bu email-i sadəcə nəzərə almayın.";
            }
            default -> {
                subject = "Fleetra — şifrə bərpası kodu";
                title = "Şifrənizi bərpa edin";
                intro = "Şifrənizi yeniləmək üçün aşağıdakı kodu daxil edin:";
                note = "Bu kod 15 dəqiqə ərzində etibarlıdır. Əgər bu tələbi siz etməmisinizsə, şifrəniz təhlükəsizdir — bu email-i sadəcə nəzərə almayın.";
            }
        }

        String body =
                "<h1 style=\"margin:0 0 12px;font-size:19px;color:" + NAVY + ";font-family:Arial,Helvetica,sans-serif;\">" + title + "</h1>"
                        + "<p style=\"margin:0 0 24px;font-size:14px;line-height:1.6;color:#4b5563;font-family:Arial,Helvetica,sans-serif;\">" + intro + "</p>"
                        + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:0 auto 24px;background:#fff7ed;border:1.5px solid #fed7aa;border-radius:12px;\">"
                        + "<tr><td style=\"padding:16px 34px;font-size:30px;font-weight:800;letter-spacing:9px;color:" + ORANGE + ";font-family:'Courier New',monospace;text-align:center;\">" + code + "</td></tr>"
                        + "</table>"
                        + "<p style=\"margin:0;font-size:12.5px;line-height:1.6;color:#9ca3af;font-family:Arial,Helvetica,sans-serif;\">" + note + "</p>";

        send(toEmail, subject, wrapBody(body));
    }

    // Ümumi bildiriş email-i (xoş gəlmisiniz, sifariş qəbul edildi, yük
    // götürüldü, çatdırıldı və s.). ctaLabel/ctaLink null ola bilər — o
    // zaman sadəcə düymə göstərilmir.
    public void sendNotificationEmail(String toEmail, String subject, String title, String message, String ctaLabel, String ctaLink) {
        StringBuilder body = new StringBuilder();
        body.append("<h1 style=\"margin:0 0 12px;font-size:19px;color:").append(NAVY).append(";font-family:Arial,Helvetica,sans-serif;\">").append(title).append("</h1>");
        body.append("<p style=\"margin:0 0 ").append(ctaLabel != null ? "24" : "4").append("px;font-size:14px;line-height:1.6;color:#4b5563;font-family:Arial,Helvetica,sans-serif;\">").append(message).append("</p>");
        if (ctaLabel != null && ctaLink != null) {
            body.append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:0 0 4px;\"><tr><td style=\"border-radius:10px;background:").append(ORANGE).append(";\">")
                    .append("<a href=\"").append(ctaLink).append("\" style=\"display:inline-block;padding:12px 26px;font-size:14px;font-weight:700;color:#ffffff;text-decoration:none;font-family:Arial,Helvetica,sans-serif;\">")
                    .append(ctaLabel).append("</a></td></tr></table>");
        }
        send(toEmail, subject, wrapBody(body.toString()));
    }

    private void send(String toEmail, String subject, String html) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromAddress, "Fleetra");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            helper.addInline("fleetraLogo", new ClassPathResource("mail/fleetra-mark.png"));
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            throw new RuntimeException("Email göndərilə bilmədi: " + e.getMessage(), e);
        }
    }

    // Header (loqo + "Fleetra" wordmark) və footer bütün email-lər üçün
    // ortaqdır — yalnız ortadakı "body" hissəsi (kod qutusu və ya
    // bildiriş mətni/düyməsi) fərqlənir.
    private String wrapBody(String bodyHtml) {
        return "<!DOCTYPE html>"
                + "<html><body style=\"margin:0;padding:0;background:#f3f4f6;font-family:Arial,Helvetica,sans-serif;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f3f4f6;padding:32px 0;\">"
                + "<tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"480\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:480px;width:100%;background:#ffffff;border-radius:16px;overflow:hidden;\">"

                // Header
                + "<tr><td style=\"background:" + NAVY + ";padding:30px 24px;text-align:center;\">"
                + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:0 auto;\"><tr>"
                + "<td style=\"padding-right:10px;\"><img src=\"cid:fleetraLogo\" width=\"40\" height=\"40\" style=\"display:block;border-radius:10px;\" alt=\"Fleetra\"/></td>"
                + "<td style=\"font-size:22px;font-weight:800;color:#ffffff;font-family:Arial,Helvetica,sans-serif;\">Fleet<span style=\"color:" + ORANGE + ";\">ra</span></td>"
                + "</tr></table>"
                + "</td></tr>"

                // Body (dinamik hissə) — mətn sola düzülür (default), yalnız
                // kod qutusu/düymə öz table-larında margin:0 auto ilə
                // mərkəzləşir (bax sendVerificationCode/sendNotificationEmail).
                + "<tr><td style=\"padding:36px 32px 28px;text-align:left;\">" + bodyHtml + "</td></tr>"

                // Footer
                + "<tr><td style=\"padding:18px 32px;background:#f9fafb;text-align:center;border-top:1px solid #e5e7eb;\">"
                + "<p style=\"margin:0;font-size:12px;color:#9ca3af;font-family:Arial,Helvetica,sans-serif;\">Fleetra komandası &middot; Bu avtomatik göndərilən mesajdır</p>"
                + "</td></tr>"

                + "</table></td></tr></table>"
                + "</body></html>";
    }
}
