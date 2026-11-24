from __future__ import annotations

import csv
import sqlite3
import sys
from pathlib import Path

from fastapi.testclient import TestClient

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "backend"))
sys.path.insert(0, str(ROOT / "scripts"))
sys.path.insert(0, str(ROOT / "tools" / "vocabulary_import"))

import import_verified_tg  # noqa: E402
import vocabulary_importer  # noqa: E402
import vocabulary_validator  # noqa: E402
from app.main import app  # noqa: E402


def _create_v6_academic_terms_db(db_path: Path) -> None:
    conn = sqlite3.connect(str(db_path))
    try:
        conn.execute(
            """
            CREATE TABLE academic_terms (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                term TEXT NOT NULL,
                normalized_term TEXT NOT NULL UNIQUE,
                phonetic_transcription TEXT NOT NULL DEFAULT '',
                part_of_speech TEXT NOT NULL,
                translation_ru TEXT NOT NULL,
                translation_tg TEXT NOT NULL DEFAULT '',
                definition_en TEXT NOT NULL,
                definition_ru TEXT NOT NULL,
                academic_explanation_en TEXT NOT NULL,
                academic_explanation_ru TEXT NOT NULL,
                practical_explanation TEXT NOT NULL,
                example_en TEXT NOT NULL,
                example_ru TEXT NOT NULL,
                major_category TEXT NOT NULL,
                category_id INTEGER NOT NULL,
                subcategory_id INTEGER NOT NULL,
                domain TEXT NOT NULL,
                subdomain TEXT NOT NULL,
                difficulty_level TEXT NOT NULL,
                cefr_level TEXT NOT NULL,
                synonyms TEXT NOT NULL DEFAULT '',
                related_terms TEXT NOT NULL DEFAULT '',
                abbreviations TEXT NOT NULL DEFAULT '',
                source_reference TEXT NOT NULL DEFAULT '',
                source_id INTEGER,
                verification_status TEXT NOT NULL DEFAULT 'verified',
                is_favorite INTEGER NOT NULL DEFAULT 0,
                is_in_srs INTEGER NOT NULL DEFAULT 0,
                repetitions INTEGER NOT NULL DEFAULT 0,
                ease_factor REAL NOT NULL DEFAULT 2.5,
                interval_days INTEGER NOT NULL DEFAULT 0,
                next_review_at INTEGER NOT NULL DEFAULT 0,
                last_studied_at INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL DEFAULT 0,
                ipa_source_url TEXT NOT NULL DEFAULT '',
                ipa_license TEXT NOT NULL DEFAULT '',
                normalized_search TEXT NOT NULL DEFAULT ''
            );
            """
        )
        conn.execute(
            """
            INSERT INTO academic_terms (
                term, normalized_term, part_of_speech, translation_ru, translation_tg,
                definition_en, definition_ru, academic_explanation_en, academic_explanation_ru,
                practical_explanation, example_en, example_ru, major_category, category_id,
                subcategory_id, domain, subdomain, difficulty_level, cefr_level,
                source_reference, verification_status, is_favorite, repetitions, normalized_search
            ) VALUES (
                'Idempotency', 'idempotency', 'noun', 'идемпотентность', '',
                'Property of operations that yield the same result when repeated.',
                'Свойство операций давать одинаковый результат при повторении.',
                'Academic definition EN.', 'Академическое определение RU.',
                'Используется в REST API.', 'PUT is idempotent.', 'PUT идемпотентен.',
                'Distributed Systems & Cloud Computing', 106, 1,
                'Distributed Systems', 'API', 'Advanced', 'C1',
                'RFC 9110', 'verified', 1, 4, 'idempotency идемпотентность'
            );
            """
        )
        conn.commit()
    finally:
        conn.close()


def test_import_verified_tg_persists_csv_and_rebuilds_normalized_search(tmp_path: Path):
    db_path = tmp_path / "test_v6.db"
    _create_v6_academic_terms_db(db_path)

    input_csv = tmp_path / "terms_tg_draft.csv"
    with input_csv.open("w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=["normalized_term", "translation_tg_draft", "status"])
        w.writeheader()
        w.writerow({"normalized_term": "idempotency", "translation_tg_draft": "тағйирнопазирӣ", "status": "verified"})
        w.writerow({"normalized_term": "deadlock", "translation_tg_draft": "драфт", "status": "pending_review"})

    verified_csv = tmp_path / "verified_tg_translations.csv"
    updated_count, persisted_count = import_verified_tg.import_verified_translations(
        input_csv=input_csv,
        db_path=db_path,
        verified_csv_path=verified_csv,
    )
    assert updated_count == 1
    assert persisted_count == 1
    assert verified_csv.exists()

    with verified_csv.open("r", encoding="utf-8", newline="") as f:
        rows = list(csv.DictReader(f))
    assert rows == [{"normalized_term": "idempotency", "translation_tg": "тағйирнопазирӣ"}]

    conn = sqlite3.connect(str(db_path))
    try:
        tg, norm_search, is_fav, reps = conn.execute(
            "SELECT translation_tg, normalized_search, is_favorite, repetitions FROM academic_terms WHERE normalized_term = 'idempotency'"
        ).fetchone()
    finally:
        conn.close()

    assert tg == "тағйирнопазирӣ"
    assert "тағйирнопазирӣ" in norm_search
    assert is_fav == 1 and reps == 4


def test_validator_and_importer_support_36_domains_ipa_provenance_and_v6_normalized_search(tmp_path: Path):
    # 1. Record with IPA but missing ipa_source_url/ipa_license must fail validation
    bad_ipa_record = {
        "id": 1,
        "term": "Shader Pipeline",
        "normalized_term": "shader pipeline",
        "phonetic_transcription": "/ˈʃeɪ.dər ˈpaɪp.laɪn/",
        "ipa_source_url": "",
        "ipa_license": "",
        "part_of_speech": "noun phrase",
        "translation_ru": "шейдерный конвейер",
        "translation_tg": "",
        "definition_en": "Programmable GPU stages transforming vertices and fragments into screen pixels.",
        "definition_ru": "Программируемые этапы GPU для преобразования вершин и фрагментов в пиксели.",
        "academic_explanation_en": "GPU rendering pipeline.",
        "academic_explanation_ru": "Конвейер рендеринга GPU.",
        "practical_explanation": "Используется в Vulkan и DirectX.",
        "example_en": "Compile the shader pipeline ahead of time.",
        "example_ru": "Скомпилируйте шейдерный конвейер заранее.",
        "major_category": "Software Engineering & Compiler Technology",
        "category_id": 108,
        "subcategory_id": 1,
        "domain": "GameDev",
        "subdomain": "Graphics",
        "difficulty_level": "Advanced",
        "cefr_level": "C1",
        "source_reference": "Khronos Vulkan Spec",
        "source_id": 1,
        "verification_status": "verified",
        "created_at": 1764000000,
        "updated_at": 1764000000,
    }
    bad_res = vocabulary_validator.validate_records([bad_ipa_record])
    assert not bad_res.is_valid
    assert any("ipa" in err.lower() for err in bad_res.errors)

    # 2. Valid record in one of the 6 new v3 domains (36 total) with IPA provenance passes & upserts with normalized_search
    good_record = dict(
        bad_ipa_record,
        ipa_source_url="https://github.com/open-dict-data/ipa-dict",
        ipa_license="MIT / CC-BY-SA-3.0",
    )
    good_res = vocabulary_validator.validate_records([good_record])
    assert good_res.is_valid, good_res.errors

    db_path = tmp_path / "importer_v6.db"
    _create_v6_academic_terms_db(db_path)
    inserted, updated, skipped = vocabulary_importer.upsert_terms_into_sqlite(db_path, [good_record])
    assert (inserted, updated, skipped) == (1, 0, 0)

    conn = sqlite3.connect(str(db_path))
    try:
        row = conn.execute(
            "SELECT ipa_source_url, ipa_license, normalized_search FROM academic_terms WHERE normalized_term = 'shader pipeline'"
        ).fetchone()
    finally:
        conn.close()

    assert row is not None
    assert row[0] == "https://github.com/open-dict-data/ipa-dict"
    assert row[1] == "MIT / CC-BY-SA-3.0"
    assert "шейдерный конвейер" in row[2] and "shader pipeline" in row[2]


def test_ota_manifest_supports_download_url_and_multi_type_item_count():
    client = TestClient(app)
    resp = client.get("/v1/ota/manifest")
    assert resp.status_code == 200
    packs = resp.json()["packs"]
    assert len(packs) >= 1
    assert "download_url" in packs[0]
