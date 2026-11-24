"""add role column to users for RBAC

Revision ID: 0003_user_role_rbac
Revises: 0002_auth_token_security
Create Date: 2026-11-23 01:20:00.000000
"""
from __future__ import annotations

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa

revision: str = "0003_user_role_rbac"
down_revision: Union[str, None] = "0002_auth_token_security"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    with op.batch_alter_table("users", schema=None) as batch_op:
        batch_op.add_column(sa.Column("role", sa.String(length=24), nullable=False, server_default="USER"))


def downgrade() -> None:
    with op.batch_alter_table("users", schema=None) as batch_op:
        batch_op.drop_column("role")
