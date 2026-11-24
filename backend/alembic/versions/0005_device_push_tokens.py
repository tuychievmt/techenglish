"""add device_push_tokens table for FCM push notifications (SP2-10)

Revision ID: 0005_device_push_tokens
Revises: 0004_oauth_identities
Create Date: 2026-11-23 13:38:00.000000
"""
from __future__ import annotations

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa

revision: str = "0005_device_push_tokens"
down_revision: Union[str, None] = "0004_oauth_identities"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "device_push_tokens",
        sa.Column("id", sa.Integer(), autoincrement=True, nullable=False),
        sa.Column("user_id", sa.String(length=64), nullable=False),
        sa.Column("platform", sa.String(length=24), nullable=False),
        sa.Column("token", sa.String(length=512), nullable=False),
        sa.Column("created_at_ms", sa.BigInteger(), nullable=False),
        sa.Column("updated_at_ms", sa.BigInteger(), nullable=False),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("token"),
    )
    with op.batch_alter_table("device_push_tokens", schema=None) as batch_op:
        batch_op.create_index(batch_op.f("ix_device_push_tokens_user_id"), ["user_id"], unique=False)


def downgrade() -> None:
    with op.batch_alter_table("device_push_tokens", schema=None) as batch_op:
        batch_op.drop_index(batch_op.f("ix_device_push_tokens_user_id"))
    op.drop_table("device_push_tokens")
