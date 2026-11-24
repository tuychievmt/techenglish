"""add oauth_identities table for server-side OAuth (provider, sub) binding

Revision ID: 0004_oauth_identities
Revises: 0003_user_role_rbac
Create Date: 2026-11-23 01:28:00.000000
"""
from __future__ import annotations

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa

revision: str = "0004_oauth_identities"
down_revision: Union[str, None] = "0003_user_role_rbac"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "oauth_identities",
        sa.Column("id", sa.Integer(), autoincrement=True, nullable=False),
        sa.Column("user_id", sa.String(length=64), nullable=False),
        sa.Column("provider", sa.String(length=32), nullable=False),
        sa.Column("provider_sub", sa.String(length=255), nullable=False),
        sa.Column("provider_email", sa.String(length=255), nullable=True),
        sa.Column("created_at_ms", sa.BigInteger(), nullable=False),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("provider", "provider_sub", name="uq_oauth_provider_sub"),
    )
    with op.batch_alter_table("oauth_identities", schema=None) as batch_op:
        batch_op.create_index(batch_op.f("ix_oauth_identities_user_id"), ["user_id"], unique=False)


def downgrade() -> None:
    with op.batch_alter_table("oauth_identities", schema=None) as batch_op:
        batch_op.drop_index(batch_op.f("ix_oauth_identities_user_id"))
    op.drop_table("oauth_identities")
