#!/usr/bin/env python3
"""
SP3-03 / R7 / DATA-01: Strict importer for human-verified Tajik translations.
Rejects every record whose status != 'verified' so machine drafts (`pending_review`)
can never enter the release database without human sign-off.
Also persists verified translations into `tools/vocabulary_import/verified_tg_translations.csv`
(picked up by `corpus_builder.py`) and recalculates `normalized_search` in SQLite (Room v6 / FTS4).
"""
from __future__ import annotations

import argparse
import csv
import re
import sqlite3
from pathlib import Path
from typing import Dict, List, Tuple

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_VERIFIED_CSV = ROOT / "tools" / "vocabulary_import" / "verified_tg_translations.csv"


def _build_normalized_search(*parts: str) -> str:
    joined = " ".join(p.strip() for p in parts if p and p.strip())
    return re.sub(r"\s+", " ", joined.lower()).strip()


def load_verified_rows(csv_path: Path) -> Tuple[List[Tuple[str, str]], int]:
    verified: List[Tuple[str, str]] = []
    skipped = 0
    with csv_path.open("r", encoding="utf-8", newline="") as f:
        reader = csv.DictReader(f)
        for row in reader:
            status = (row.get("status") or "").strip().lower()
            term = (row.get("normalized_term") or "").strip().lower()
            tg = (row.get("translation_tg_draft") or row.get("translation_tg") or "").strip()
            if status == "verified" and term and tg:
                verified.append((tg, term))
            else:
                skipped += 1
    return verified, skipped


def persist_verified_csv(verified: List[Tuple[str, str]], verified_csv_path: Path) -> int:
    merged: Dict[str, str] = {}
    if verified_csv_path.exists():
        with verified_csv_path.open("r", encoding="utf-8", newline="") as f:
            for row in csv.DictReader(f):
                norm = (row.get("normalized_term") or "").strip().lower()
                tg = (row.get("translation_tg") or "").strip()
                if norm and tg:
                    merged[norm] = tg
    for tg, norm in verified:
        merged[norm] = tg

    verified_csv_path.parent.mkdir(parents=True, exist_ok=True)
    with verified_csv_path.open("w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=["normalized_term", "translation_tg"])
        writer.writeheader()
        for norm in sorted(merged.keys()):
            writer.writerow({"normalized_term": norm, "translation_tg": merged[norm]})
    return len(merged)


def import_verified_translations(
    input_csv: Path,
    db_path: Path,
    verified_csv_path: Path = DEFAULT_VERIFIED_CSV,
    dry_run: bool = False,
) -> Tuple[int, int]:
    verified, skipped = load_verified_rows(input_csv)
    print(f"Verified rows ready to import: {len(verified)}; skipped (pending_review/invalid): {skipped}")
    if dry_run or not verified:
        return 0, 0

    persisted_count = persist_verified_csv(verified, verified_csv_path)

    updated = 0
    conn = sqlite3.connect(str(db_path))
    try:
        cur = conn.cursor()
        cols = {row[1] for row in cur.execute("PRAGMA table_info(academic_terms);").fetchall()}
        has_norm_search = "normalized_search" in cols

        for tg, norm in verified:
            row = cur.execute(
                """
                SELECT id, term, abbreviations, translation_ru, synonyms, definition_en, definition_ru
                FROM academic_terms
                WHERE normalized_term = ?;
                """,
                (norm,),
            ).fetchone()
            if not row:
                continue
            row_id, term, abbr, tr_ru, syns, def_en, def_ru = row
            if has_norm_search:
                norm_search = _build_normalized_search(term, abbr, tr_ru, tg, syns, def_en, def_ru)
                cur.execute(
                    "UPDATE academic_terms SET translation_tg = ?, normalized_search = ? WHERE id = ?;",
                    (tg, norm_search, row_id),
                )
            else:
                cur.execute(
                    "UPDATE academic_terms SET translation_tg = ? WHERE id = ?;",
                    (tg, row_id),
                )
            updated += 1
        conn.commit()
    finally:
        conn.close()

    return updated, persisted_count


def main() -> None:
    parser = argparse.ArgumentParser(description="Import strictly human-verified Tajik term translations (R7)")
    parser.add_argument("--input", type=Path, default=Path("docs/tg_review/terms_tg_draft.csv"))
    parser.add_argument("--db", type=Path, default=Path("content_pack/src/main/assets/databases/tech_english.db"))
    parser.add_argument("--verified-csv", type=Path, default=DEFAULT_VERIFIED_CSV)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    updated, persisted = import_verified_translations(
        input_csv=args.input,
        db_path=args.db,
        verified_csv_path=args.verified_csv,
        dry_run=args.dry_run,
    )
    print(f"Updated {updated} rows in {args.db}; persisted {persisted} verified entries in {args.verified_csv}")


if __name__ == "__main__":
    main()
