from __future__ import annotations

import logging
import os
import smtplib
from email.message import EmailMessage
from typing import Dict, List, Protocol

logger = logging.getLogger("techenglish.email")


class EmailSender(Protocol):
    is_fake: bool

    def send_verification_email(self, to_email: str, token: str) -> None: ...

    def send_password_reset_email(self, to_email: str, token: str) -> None: ...


class SmtpEmailSender:
    """Production SMTP EmailSender implementation (configured via SMTP_* env vars)."""

    is_fake: bool = False

    def __init__(
        self,
        host: str,
        port: int = 587,
        username: str = "",
        password: str = "",
        from_addr: str = "noreply@techenglish.app",
        use_tls: bool = True,
    ) -> None:
        self.host = host
        self.port = port
        self.username = username
        self.password = password
        self.from_addr = from_addr
        self.use_tls = use_tls

    def _send(self, to_email: str, subject: str, body: str) -> None:
        msg = EmailMessage()
        msg["Subject"] = subject
        msg["From"] = self.from_addr
        msg["To"] = to_email
        msg.set_content(body)
        with smtplib.SMTP(self.host, self.port, timeout=10) as smtp:
            if self.use_tls:
                smtp.starttls()
            if self.username:
                smtp.login(self.username, self.password)
            smtp.send_message(msg)

    def send_verification_email(self, to_email: str, token: str) -> None:
        self._send(
            to_email,
            "TechEnglish — Подтверждение email",
            f"Ваш код подтверждения email (действует 15 минут): {token}",
        )

    def send_password_reset_email(self, to_email: str, token: str) -> None:
        self._send(
            to_email,
            "TechEnglish — Сброс пароля",
            f"Ваш код сброса пароля (действует 15 минут): {token}",
        )


class ConsoleEmailSender:
    """Dev/test fake EmailSender storing messages in memory instead of leaking tokens in HTTP."""

    is_fake: bool = True

    def __init__(self) -> None:
        self.outbox: List[Dict[str, str]] = []

    def send_verification_email(self, to_email: str, token: str) -> None:
        self.outbox.append({"kind": "verify", "to": to_email, "token": token})
        logger.info("[DEV EMAIL] Verification email queued for %s", to_email)

    def send_password_reset_email(self, to_email: str, token: str) -> None:
        self.outbox.append({"kind": "reset", "to": to_email, "token": token})
        logger.info("[DEV EMAIL] Password reset email queued for %s", to_email)


default_email_sender = ConsoleEmailSender()


def get_email_sender() -> EmailSender:
    smtp_host = os.getenv("SMTP_HOST", "").strip()
    if smtp_host:
        return SmtpEmailSender(
            host=smtp_host,
            port=int(os.getenv("SMTP_PORT", "587")),
            username=os.getenv("SMTP_USER", ""),
            password=os.getenv("SMTP_PASSWORD", ""),
            from_addr=os.getenv("SMTP_FROM", "noreply@techenglish.app"),
            use_tls=os.getenv("SMTP_USE_TLS", "true").strip().lower() != "false",
        )
    return default_email_sender
