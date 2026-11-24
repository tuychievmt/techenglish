"""add activity_feed_reactions table for per-user reaction tracking (SP2-08 / BUG-25)

Revision ID: 0006_activity_feed_reactions
Revises: 0005_device_push_tokens
Create Date: 2026-11-23 14:23:00.000000
"""
from __future__ import annotations

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa

revision: str = "0006_activity_feed_reactions"
down_revision: Union[str, None] = "0005_device_push_tokens"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "activity_feed_reactions",
        sa.Column("id", sa.Integer(), autoincrement=True, nullable=False),
        sa.Column("event_id", sa.String(length=64), nullable=False),
        sa.Column("user_id", sa.String(length=64), nullable=False),
        sa.Column("created_at_ms", sa.BigInteger(), nullable=False),
        sa.ForeignKeyConstraint(["event_id"], ["activity_feed.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("event_id", "user_id", name="uq_feed_reaction_event_user"),
    )
    with op.batch_alter_table("activity_feed_reactions", schema=None) as batch_op:
        batch_op.create_index(batch_op.f("ix_activity_feed_reactions_event_id"), ["event_id"], unique=False)
        batch_op.create_index(batch_op.f("ix_activity_feed_reactions_user_id"), ["user_id"], unique=False)


def downgrade() -> None:
    with op.batch_alter_table("activity_feed_reactions", schema=None) as batch_op:
        batch_op.drop_index(batch_op.f("ix_activity_feed_reactions_user_id"))
        batch_op.drop_index(batch_op.f("ix_activity_feed_reactions_event_id"))
    op.drop_table("activity_feed_reactions")
