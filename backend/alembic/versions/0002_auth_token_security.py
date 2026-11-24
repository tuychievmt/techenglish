"""add email verify and password reset ttl and attempts columns

Revision ID: 0002_auth_token_security
Revises: 0001_initial_v3_0_0
Create Date: 2026-11-23 01:15:00.000000
"""
from __future__ import annotations

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa

revision: str = "0002_auth_token_security"
down_revision: Union[str, None] = "0001_initial_v3_0_0"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    with op.batch_alter_table("users", schema=None) as batch_op:
        batch_op.add_column(sa.Column("email_verify_expires_at_ms", sa.BigInteger(), nullable=True))
        batch_op.add_column(sa.Column("password_reset_expires_at_ms", sa.BigInteger(), nullable=True))
        batch_op.add_column(
            sa.Column("password_reset_attempts", sa.Integer(), nullable=False, server_default="0")
        )


def downgrade() -> None:
    with op.batch_alter_table("users", schema=None) as batch_op:
        batch_op.drop_column("password_reset_attempts")
        batch_op.drop_column("password_reset_expires_at_ms")
        batch_op.drop_column("email_verify_expires_at_ms")
