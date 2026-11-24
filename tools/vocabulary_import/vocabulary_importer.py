#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Academic Vocabulary Importer & Exporter (`vocabulary_importer.py`).
Supports importing from JSON and CSV files, normalizing terms, detecting
duplicates and conflicting translations, and upserting into the SQLite database
without losing existing user SRS progress or favorites.
"""

import argparse
import csv
import json
import re
import sqlite3
import sys
import time
from pathlib import Path
from typing import Any, Dict, List, Tuple

ROOT = Path(__file__).resolve().parent.parent.parent
sys.path.insert(0, str(ROOT / "tools" / "vocabulary_import"))

from vocabulary_validator import normalize_term, validate_records


def _build_normalized_search(*parts: str) -> str:
    joined = " ".join(p.strip() for p in parts if p and p.strip())
    return re.sub(r"\s+", " ", joined.lower()).strip()


def load_records_from_json(json_path: Path) -> List[Dict[str, Any]]:
    payload = json.loads(json_path.read_text(encoding="utf-8"))
    if isinstance(payload, dict) and "terms" in payload:
        return list(payload["terms"])
    if isinstance(payload, list):
        return payload
    raise ValueError(f"Unsupported JSON vocabulary format in {json_path}")


def load_records_from_csv(csv_path: Path) -> List[Dict[str, Any]]:
    records: List[Dict[str, Any]] = []
    now_ts = int(time.time())
    with csv_path.open("r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for idx, row in enumerate(reader, start=1):
            term = (row.get("term") or "").strip()
            records.append({
                "id": int(row.get("id") or idx),
                "term": term,
                "normalized_term": normalize_term(row.get("normalized_term") or term),
                "phonetic_transcription": (row.get("phonetic_transcription") or "").strip(),
                "ipa_source_url": (row.get("ipa_source_url") or "").strip(),
                "ipa_license": (row.get("ipa_license") or "").strip(),
                "part_of_speech": (row.get("part_of_speech") or "noun phrase").strip(),
                "translation_ru": (row.get("translation_ru") or "").strip(),
                "translation_tg": (row.get("translation_tg") or "").strip(),
                "definition_en": (row.get("definition_en") or "").strip(),
                "definition_ru": (row.get("definition_ru") or "").strip(),
                "academic_explanation_en": (row.get("academic_explanation_en") or row.get("definition_en") or "").strip(),
                "academic_explanation_ru": (row.get("academic_explanation_ru") or row.get("definition_ru") or "").strip(),
                "practical_explanation": (row.get("practical_explanation") or row.get("definition_ru") or "").strip(),
                "example_en": (row.get("example_en") or "").strip(),
                "example_ru": (row.get("example_ru") or "").strip(),
                "major_category": (row.get("major_category") or "Software Engineering & Compiler Technology").strip(),
                "category_id": int(row.get("category_id") or 108),
                "subcategory_id": int(row.get("subcategory_id") or 1),
                "domain": (row.get("domain") or "Software Engineering").strip(),
                "subdomain": (row.get("subdomain") or "Software Architecture").strip(),
                "difficulty_level": (row.get("difficulty_level") or "Intermediate").strip(),
                "cefr_level": (row.get("cefr_level") or "B2").strip(),
                "synonyms": (row.get("synonyms") or "").strip(),
                "related_terms": (row.get("related_terms") or "").strip(),
                "abbreviations": (row.get("abbreviations") or "").strip(),
                "source_reference": (row.get("source_reference") or "").strip(),
                "source_id": int(row["source_id"]) if row.get("source_id") else None,
                "verification_status": (row.get("verification_status") or "pending_review").strip(),
                "created_at": int(row.get("created_at") or now_ts),
                "updated_at": int(row.get("updated_at") or now_ts),
            })
    return records


def export_records_to_csv(records: List[Dict[str, Any]], csv_path: Path) -> None:
    if not records:
        return
    fieldnames = [
        "id", "term", "normalized_term", "phonetic_transcription", "ipa_source_url", "ipa_license",
        "part_of_speech",
        "translation_ru", "translation_tg", "definition_en", "definition_ru",
        "academic_explanation_en", "academic_explanation_ru", "practical_explanation",
        "example_en", "example_ru", "major_category", "category_id", "subcategory_id",
        "domain", "subdomain", "difficulty_level", "cefr_level", "synonyms",
        "related_terms", "abbreviations", "source_reference", "source_id",
        "verification_status", "created_at", "updated_at", "normalized_search"
    ]
    csv_path.parent.mkdir(parents=True, exist_ok=True)
    with csv_path.open("w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        for r in records:
            writer.writerow({k: r.get(k, "") for k in fieldnames})


def upsert_terms_into_sqlite(
    db_path: Path,
    records: List[Dict[str, Any]],
    allow_unverified: bool = False
) -> Tuple[int, int, int]:
    """
    Upserts validated records into `academic_terms` in SQLite without overwriting
    user progress columns (`is_favorite`, `is_in_srs`, `repetitions`, `ease_factor`,
    `interval_days`, `next_review_at`, `last_studied_at`).
    Returns (inserted_count, updated_count, skipped_count).
    """
    validation = validate_records(records)
    if not validation.is_valid:
        raise ValueError(
            f"Vocabulary validation failed with {len(validation.errors)} errors: {validation.errors[:5]}"
        )

    conn = sqlite3.connect(str(db_path))
    inserted = updated = skipped = 0
    try:
        cur = conn.cursor()
        existing = {
            row[0]: row[1]
            for row in cur.execute("SELECT normalized_term, id FROM academic_terms;").fetchall()
        }

        for r in records:
            if not allow_unverified and r["verification_status"] != "verified":
                skipped += 1
                continue
            norm = r["normalized_term"]
            norm_search = r.get("normalized_search") or _build_normalized_search(
                r["term"],
                r.get("abbreviations", ""),
                r["translation_ru"],
                r.get("translation_tg", ""),
                r.get("synonyms", ""),
                r["definition_en"],
                r["definition_ru"],
            )
            if norm in existing:
                term_id = existing[norm]
                cur.execute(
                    """
                    UPDATE academic_terms
                    SET term = ?,
                        phonetic_transcription = ?,
                        ipa_source_url = ?,
                        ipa_license = ?,
                        part_of_speech = ?,
                        translation_ru = ?,
                        translation_tg = ?,
                        definition_en = ?,
                        definition_ru = ?,
                        academic_explanation_en = ?,
                        academic_explanation_ru = ?,
                        practical_explanation = ?,
                        example_en = ?,
                        example_ru = ?,
                        major_category = ?,
                        category_id = ?,
                        subcategory_id = ?,
                        domain = ?,
                        subdomain = ?,
                        difficulty_level = ?,
                        cefr_level = ?,
                        synonyms = ?,
                        related_terms = ?,
                        abbreviations = ?,
                        source_reference = ?,
                        source_id = ?,
                        verification_status = ?,
                        updated_at = ?,
                        normalized_search = ?
                    WHERE id = ?;
                    """,
                    (
                        r["term"], r.get("phonetic_transcription", ""),
                        r.get("ipa_source_url", ""), r.get("ipa_license", ""),
                        r["part_of_speech"],
                        r["translation_ru"], r.get("translation_tg", ""),
                        r["definition_en"], r["definition_ru"],
                        r["academic_explanation_en"], r["academic_explanation_ru"],
                        r["practical_explanation"], r["example_en"], r["example_ru"],
                        r["major_category"], r.get("category_id", 108), r.get("subcategory_id", 1),
                        r["domain"], r["subdomain"], r["difficulty_level"], r["cefr_level"],
                        r.get("synonyms", ""), r.get("related_terms", ""), r.get("abbreviations", ""),
                        r.get("source_reference", ""), r.get("source_id"),
                        r["verification_status"], r["updated_at"], norm_search,
                        term_id,
                    ),
                )
                updated += 1
            else:
                cur.execute(
                    """
                    INSERT INTO academic_terms (
                        term, normalized_term, phonetic_transcription, ipa_source_url, ipa_license,
                        part_of_speech,
                        translation_ru, translation_tg, definition_en, definition_ru,
                        academic_explanation_en, academic_explanation_ru, practical_explanation,
                        example_en, example_ru, major_category, category_id, subcategory_id,
                        domain, subdomain, difficulty_level, cefr_level,
                        synonyms, related_terms, abbreviations, source_reference, source_id,
                        verification_status, is_favorite, is_in_srs, repetitions, ease_factor,
                        interval_days, next_review_at, last_studied_at, created_at, updated_at,
                        normalized_search
                    ) VALUES (
                        ?, ?, ?, ?, ?,
                        ?,
                        ?, ?, ?, ?,
                        ?, ?, ?,
                        ?, ?, ?, ?, ?,
                        ?, ?, ?, ?,
                        ?, ?, ?, ?, ?,
                        ?, 0, 0, 0, 2.5,
                        0, 0, 0, ?, ?,
                        ?
                    );
                    """,
                    (
                        r["term"], norm, r.get("phonetic_transcription", ""),
                        r.get("ipa_source_url", ""), r.get("ipa_license", ""),
                        r["part_of_speech"],
                        r["translation_ru"], r.get("translation_tg", ""),
                        r["definition_en"], r["definition_ru"],
                        r["academic_explanation_en"], r["academic_explanation_ru"],
                        r["practical_explanation"], r["example_en"], r["example_ru"],
                        r["major_category"], r.get("category_id", 108), r.get("subcategory_id", 1),
                        r["domain"], r["subdomain"], r["difficulty_level"], r["cefr_level"],
                        r.get("synonyms", ""), r.get("related_terms", ""), r.get("abbreviations", ""),
                        r.get("source_reference", ""), r.get("source_id"),
                        r["verification_status"], r["created_at"], r["updated_at"], norm_search,
                    ),
                )
                existing[norm] = cur.lastrowid
                inserted += 1

        conn.commit()
    finally:
        conn.close()

    return inserted, updated, skipped


def main() -> None:
    parser = argparse.ArgumentParser(description="TechEnglish Academic Vocabulary Importer (JSON/CSV)")
    parser.add_argument("--input", required=True, help="Path to input .json or .csv vocabulary file")
    parser.add_argument("--db", help="Optional path to SQLite database to upsert into")
    parser.add_argument("--export-csv", help="Optional path to export validated CSV")
    args = parser.parse_args()

    in_path = Path(args.input)
    if in_path.suffix.lower() == ".json":
        records = load_records_from_json(in_path)
    elif in_path.suffix.lower() == ".csv":
        records = load_records_from_csv(in_path)
    else:
        raise ValueError("Input file must be .json or .csv")

    val = validate_records(records)
    print(f"Loaded {val.total_records} records ({val.unique_terms} unique, {val.verified_terms} verified).")
    if not val.is_valid:
        for e in val.errors[:15]:
            print("ERROR:", e)
        sys.exit(1)

    if args.export_csv:
        export_records_to_csv(records, Path(args.export_csv))
        print(f"Exported CSV to {args.export_csv}")

    if args.db:
        ins, upd, skp = upsert_terms_into_sqlite(Path(args.db), records)
        print(f"SQLite Upsert Complete -> Inserted: {ins}, Updated: {upd}, Skipped: {skp}")


if __name__ == "__main__":
    main()
