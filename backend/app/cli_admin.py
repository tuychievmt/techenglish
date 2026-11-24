from __future__ import annotations

import argparse
import os
import sys
import time
import uuid
from typing import Optional

from sqlalchemy.orm import Session

from app.models import SessionLocal, UserRow
from app.security import hash_password

VALID_ROLES = {"USER", "MODERATOR", "ADMIN"}


def promote_user_role(db: Session, email: str, role: str) -> UserRow:
    """Promotes an existing user to USER, MODERATOR, or ADMIN (CLI/ops only, never exposed via HTTP)."""
    normalized_role = role.strip().upper()
    if normalized_role not in VALID_ROLES:
        raise ValueError(f"Invalid role '{role}'. Allowed: {sorted(VALID_ROLES)}")
    clean_email = email.strip().lower()
    user = db.query(UserRow).filter(UserRow.email == clean_email).first()
    if not user:
        raise LookupError(f"User with email '{clean_email}' not found")
    user.role = normalized_role
    db.commit()
    db.refresh(user)
    return user


def bootstrap_admin_from_env(db: Session) -> Optional[UserRow]:
    """
    Bootstraps or promotes the first ADMIN account strictly via environment variables
    (BOOTSTRAP_ADMIN_EMAIL, optional BOOTSTRAP_ADMIN_PASSWORD / BOOTSTRAP_ADMIN_USERNAME).
    """
    admin_email = os.getenv("BOOTSTRAP_ADMIN_EMAIL", "").strip().lower()
    if not admin_email:
        return None
    user = db.query(UserRow).filter(UserRow.email == admin_email).first()
    if user:
        if user.role != "ADMIN":
            user.role = "ADMIN"
            db.commit()
            db.refresh(user)
        return user

    admin_password = os.getenv("BOOTSTRAP_ADMIN_PASSWORD", "").strip()
    if not admin_password:
        return None
    admin_username = os.getenv("BOOTSTRAP_ADMIN_USERNAME", "root_admin").strip()
    user = UserRow(
        id=str(uuid.uuid4()),
        username=admin_username,
        email=admin_email,
        password_hash=hash_password(admin_password),
        provider="EMAIL",
        role="ADMIN",
        is_email_verified=True,
        display_name="Administrator",
        created_at_ms=int(time.time() * 1000),
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    return user


def main(argv: Optional[list[str]] = None) -> int:
    parser = argparse.ArgumentParser(description="TechEnglish RBAC admin CLI")
    sub = parser.add_subparsers(dest="cmd", required=True)

    p_promote = sub.add_parser("promote", help="Promote user by email to USER/MODERATOR/ADMIN")
    p_promote.add_argument("--email", required=True)
    p_promote.add_argument("--role", required=True, choices=sorted(VALID_ROLES))

    sub.add_parser("bootstrap-env", help="Bootstrap ADMIN from BOOTSTRAP_ADMIN_EMAIL env var")

    args = parser.parse_args(argv)
    with SessionLocal() as db:
        if args.cmd == "promote":
            u = promote_user_role(db, args.email, args.role)
            print(f"Updated {u.email} ({u.username}) -> role={u.role}")
            return 0
        if args.cmd == "bootstrap-env":
            u = bootstrap_admin_from_env(db)
            if u is None:
                print("No BOOTSTRAP_ADMIN_EMAIL configured or user not found.", file=sys.stderr)
                return 1
            print(f"Bootstrapped admin {u.email} ({u.username}) -> role={u.role}")
            return 0
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
