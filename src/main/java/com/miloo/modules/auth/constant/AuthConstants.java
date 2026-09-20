package com.miloo.modules.auth.constant;

public final class AuthConstants {

    private AuthConstants() {
        // Utility constant class - prevent instantiation
    }

    /**
     * HTML page returned when user opens the account activation link in a web browser.
     */
    public static final String ACTIVATION_SUCCESS_HTML = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Account Activated - Miloo</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                        margin: 0;
                        background: #f9fafb;
                    }
                    .card {
                        background: #ffffff;
                        padding: 48px 40px;
                        border-radius: 20px;
                        box-shadow: 0 10px 30px rgba(0, 0, 0, 0.06);
                        text-align: center;
                        max-width: 440px;
                        margin: 20px;
                    }
                    .icon {
                        width: 72px;
                        height: 72px;
                        background: #E94057;
                        border-radius: 50%;
                        display: inline-flex;
                        align-items: center;
                        justify-content: center;
                        color: #ffffff;
                        font-size: 36px;
                        margin-bottom: 24px;
                    }
                    h1 {
                        color: #111827;
                        font-size: 26px;
                        font-weight: 700;
                        margin: 0 0 12px;
                    }
                    p {
                        color: #6b7280;
                        font-size: 16px;
                        line-height: 1.6;
                        margin: 0 0 24px;
                    }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="icon">&#10003;</div>
                    <h1>Account Activated!</h1>
                    <p>Your Miloo account has been successfully verified and activated. You can now open the app and log in.</p>
                </div>
            </body>
            </html>
            """;

    /**
     * Generates responsive HTML email content for welcome and activation emails.
     */
    public static String buildWelcomeActivationEmail(String activationLink, int ttlHours) {
        return """
                <div style="font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; max-width: 540px; margin: 0 auto; padding: 32px; border: 1px solid #eaeaea; border-radius: 12px; background-color: #ffffff;">
                    <h2 style="color: #E94057; text-align: center; margin-bottom: 24px;">Welcome to Miloo!</h2>
                    <p style="font-size: 16px; color: #333333; line-height: 1.6;">Hello,</p>
                    <p style="font-size: 16px; color: #333333; line-height: 1.6;">Thank you for joining Miloo. To start connecting and discovering amazing people, please activate your account by clicking the button below:</p>
                    <div style="text-align: center; margin: 32px 0;">
                        <a href="%s" style="display: inline-block; background: #E94057; color: #ffffff; text-decoration: none; padding: 14px 28px; font-size: 16px; font-weight: bold; border-radius: 8px; box-shadow: 0 4px 12px rgba(233,64,87,0.25);">Activate My Account</a>
                    </div>
                    <p style="font-size: 14px; color: #666666; line-height: 1.5;">If the button doesn't work, copy and paste this link into your browser:</p>
                    <p style="font-size: 13px; color: #E94057; word-break: break-all;"><a href="%s" style="color: #E94057;">%s</a></p>
                    <p style="font-size: 14px; color: #888888; margin-top: 24px;">This activation link is valid for %d hours. If you did not create this account, please ignore this email.</p>
                    <hr style="border: none; border-top: 1px solid #f0f0f0; margin: 28px 0;">
                    <p style="font-size: 12px; color: #aaaaaa; text-align: center;">&copy; 2026 Miloo App. All rights reserved.</p>
                </div>
                """.formatted(activationLink, activationLink, activationLink, ttlHours);
    }

    /**
     * Generates a styled HTML error page when activation link is expired, already used, or invalid.
     */
    public static String buildActivationErrorHtml(String errorMessage) {
        String title;
        String description;
        String iconBg;
        String iconColor;
        String iconSymbol;

        if (errorMessage != null && errorMessage.toLowerCase().contains("already been used")) {
            title = "Link Already Used";
            description = "This account activation link has already been used. Your account is active and you can open the Miloo app to log in.";
            iconBg = "#FDF1F3";
            iconColor = "#E94057";
            iconSymbol = "&#10003;";
        } else if (errorMessage != null && errorMessage.toLowerCase().contains("expired")) {
            title = "Activation Link Expired";
            description = "This activation link has expired (links are valid for 24 hours). Please log in to your account to request a new link.";
            iconBg = "#FEF3C7";
            iconColor = "#D97706";
            iconSymbol = "&#9888;";
        } else {
            title = "Activation Failed";
            description = (errorMessage != null && !errorMessage.isBlank())
                    ? errorMessage
                    : "The activation link is invalid or incomplete. Please check your email and try again.";
            iconBg = "#FEE2E2";
            iconColor = "#DC2626";
            iconSymbol = "&#10007;";
        }

        return String.format("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s - Miloo</title>
                    <style>
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            min-height: 100vh;
                            margin: 0;
                            background: #f9fafb;
                        }
                        .card {
                            background: #ffffff;
                            padding: 48px 40px;
                            border-radius: 20px;
                            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.06);
                            text-align: center;
                            max-width: 440px;
                            margin: 20px;
                        }
                        .icon {
                            width: 72px;
                            height: 72px;
                            background: %s;
                            color: %s;
                            border-radius: 50%%;
                            display: inline-flex;
                            align-items: center;
                            justify-content: center;
                            font-size: 32px;
                            margin-bottom: 24px;
                        }
                        h1 {
                            color: #111827;
                            font-size: 24px;
                            font-weight: 700;
                            margin: 0 0 12px;
                        }
                        p {
                            color: #6b7280;
                            font-size: 15px;
                            line-height: 1.6;
                            margin: 0;
                        }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <div class="icon">%s</div>
                        <h1>%s</h1>
                        <p>%s</p>
                    </div>
                </body>
                </html>
                """, title, iconBg, iconColor, iconSymbol, title, description);
    }
}
