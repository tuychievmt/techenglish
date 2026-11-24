from __future__ import annotations

import os

# Preserve any externally configured DATABASE_URL (e.g. PostgreSQL service in CI)
# before individual legacy test modules set local SQLite defaults.
_initial_db_url = os.environ.get("DATABASE_URL")
if _initial_db_url and not os.environ.get("ALEMBIC_DATABASE_URL"):
    os.environ["ALEMBIC_DATABASE_URL"] = _initial_db_url
