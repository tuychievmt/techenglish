#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Deterministic offline SQLite seed database builder for TechEnglish v2.0.
Reads Room's KSP-exported v2 schema JSON (`2.json`) so table DDL, indexes,
foreign keys, PRAGMA user_version = 2, and room_master_table identity_hash
match 100% exactly, and populates both the v1 core modules and the 10,308-term
Academic Technical Dictionary.
"""

import json
import re
import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "scripts"))
sys.path.insert(0, str(ROOT / "tools" / "vocabulary_import"))

from seed_data.languages_data import CATEGORIES, LANGUAGES
from seed_data.vocabulary_data import WORDS
from seed_data.snippets_part1 import SNIPPETS_PART1
from seed_data.snippets_part2 import SNIPPETS_PART2
from seed_data.docs_data import DOC_ARTICLES
from seed_data.scenarios_data import SCENARIOS, WORK_PHRASES
from sources_and_taxonomy import AUTHORITATIVE_SOURCES
from corpus_builder import build_full_corpus
from vocabulary_validator import validate_records

SCHEMA_PATH = (
    ROOT
    / "app"
    / "schemas"
    / "com.techenglish.app.core.database.TechEnglishDatabase"
    / "6.json"
)
OUTPUT_DB = ROOT / "content_pack" / "src" / "main" / "assets" / "databases" / "tech_english.db"

_WS_RE = re.compile(r"\s+")


def _build_normalized_search(*parts: str) -> str:
    joined = " ".join(p.strip() for p in parts if p and p.strip())
    return _WS_RE.sub(" ", joined.lower()).strip()


def build_database() -> None:
    if not SCHEMA_PATH.exists():
        raise FileNotFoundError(
            f"Exported Room v3 schema not found at {SCHEMA_PATH}. Run :app:kspDebugKotlin first."
        )

    schema = json.loads(SCHEMA_PATH.read_text(encoding="utf-8"))
    db_meta = schema["database"]
    version = int(db_meta["version"])
    identity_hash = db_meta["identityHash"]

    # Build and validate the 20,600+ Academic Technical Vocabulary corpus
    academic_terms, word_senses, subcategories, word_relations = build_full_corpus()
    val = validate_records(academic_terms)
    assert val.is_valid, f"Academic corpus validation failed: {val.errors[:5]}"
    assert val.unique_terms >= 20000, f"Expected >= 20,000 unique terms, got {val.unique_terms}"

    OUTPUT_DB.parent.mkdir(parents=True, exist_ok=True)
    if OUTPUT_DB.exists():
        OUTPUT_DB.unlink()

    conn = sqlite3.connect(str(OUTPUT_DB))
    try:
        cur = conn.cursor()
        cur.execute("PRAGMA foreign_keys = ON;")

        # 1. Execute exact Room v2 DDL for all tables and indices
        for entity in db_meta["entities"]:
            table_name = entity["tableName"]
            create_sql = entity["createSql"].replace("${TABLE_NAME}", table_name)
            cur.execute(create_sql)
            for idx in entity.get("indices", []):
                idx_sql = idx["createSql"].replace("${TABLE_NAME}", table_name)
                cur.execute(idx_sql)

        # 2. Room master table + exact v2 identity_hash
        for setup_sql in db_meta.get("setupQueries", []):
            cur.execute(setup_sql)
        cur.execute(f"PRAGMA user_version = {version};")

        # 3. Insert categories (11 core categories)
        cur.executemany(
            "INSERT INTO categories (id, name, icon, is_system) VALUES (?, ?, ?, ?);",
            CATEGORIES,
        )

        # 4. Insert 19 programming languages
        assert len(LANGUAGES) == 19
        cur.executemany(
            """
            INSERT INTO programming_languages (
                id, slug, name, description_en, description_ru,
                history_en, history_ru, paradigms, domains, frameworks,
                typing_system, memory_management, difficulty,
                year_created, creator, accent_color_hex
            ) VALUES (
                :id, :slug, :name, :description_en, :description_ru,
                :history_en, :history_ru, :paradigms, :domains, :frameworks,
                :typing_system, :memory_management, :difficulty,
                :year_created, :creator, :accent_color_hex
            );
            """,
            LANGUAGES,
        )

        # 5. Insert 169 code snippets
        all_snippets = SNIPPETS_PART1 + SNIPPETS_PART2
        cur.executemany(
            """
            INSERT INTO code_snippets (
                language_id, task_key, task_title_en, task_title_ru,
                code, explanation_en, explanation_ru,
                line_explanations_json, keywords_json
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
            """,
            [
                (
                    s["language_id"],
                    s["task_key"],
                    s["task_title_en"],
                    s["task_title_ru"],
                    s["code"],
                    s["explanation_en"],
                    s["explanation_ru"],
                    json.dumps(s["line_explanations"], ensure_ascii=False),
                    json.dumps(s["keywords"], ensure_ascii=False),
                )
                for s in all_snippets
            ],
        )

        # 6. Insert 255 core IT vocabulary words (with Unicode-lowercased normalized_search)
        cur.executemany(
            """
            INSERT INTO words (
                category_id, language_id, term, transcription, translation,
                example, explanation_ru, is_favorite, is_custom,
                repetitions, ease_factor, interval_days, next_review_at,
                normalized_search
            ) VALUES (?, ?, ?, ?, ?, ?, ?, 0, 0, 0, 2.5, 0, 0, ?);
            """,
            [
                (
                    *row,
                    _build_normalized_search(str(row[2]), str(row[4]), str(row[6])),
                )
                for row in WORDS
            ],
        )

        # 7. Insert 156+ documentation articles & error logs (with source_url & license)
        assert len(DOC_ARTICLES) >= 150, f"Expected >= 150 doc articles, got {len(DOC_ARTICLES)}"
        for a in DOC_ARTICLES:
            assert a.get("source_url") and a.get("license"), f"Doc article #{a['id']} missing source_url or license"
        cur.executemany(
            """
            INSERT INTO doc_articles (
                id, title, tech_tag, content, explanation_ru,
                level, reading_progress, is_completed, glossary_json,
                source_url, license
            ) VALUES (?, ?, ?, ?, ?, ?, 0.0, 0, ?, ?, ?);
            """,
            [
                (
                    a["id"],
                    a["title"],
                    a["tech_tag"],
                    a["content"],
                    a["explanation_ru"],
                    a["level"],
                    json.dumps(a["glossary"], ensure_ascii=False),
                    a["source_url"],
                    a["license"],
                )
                for a in DOC_ARTICLES
            ],
        )

        # 8. Insert 156+ work scenarios across 12 contexts (>= 12 per context)
        assert len(SCENARIOS) >= 150, f"Expected >= 150 scenarios, got {len(SCENARIOS)}"
        cur.executemany(
            """
            INSERT INTO scenarios (
                id, type, exercise_type, title, prompt, prompt_ru,
                solution, options_json, explanation_ru,
                useful_expressions_json, difficulty
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
            """,
            [
                (
                    sc["id"],
                    sc["type"],
                    sc["exercise_type"],
                    sc["title"],
                    sc["prompt"],
                    sc["prompt_ru"],
                    sc["solution"],
                    json.dumps(sc["options"], ensure_ascii=False),
                    sc["explanation_ru"],
                    json.dumps(sc["useful_expressions"], ensure_ascii=False),
                    sc["difficulty"],
                )
                for sc in SCENARIOS
            ],
        )

        # 9. Insert 312+ professional workplace phrases
        assert len(WORK_PHRASES) >= 300, f"Expected >= 300 work phrases, got {len(WORK_PHRASES)}"
        cur.executemany(
            """
            INSERT INTO work_phrases (
                id, category, phrase_en, translation_ru, context_ru, difficulty
            ) VALUES (?, ?, ?, ?, ?, ?);
            """,
            WORK_PHRASES,
        )

        # 10. Insert v3 Authoritative Sources (`word_sources` with license)
        cur.executemany(
            """
            INSERT INTO word_sources (id, title, url, source_type, publisher, license)
            VALUES (?, ?, ?, ?, ?, ?);
            """,
            AUTHORITATIVE_SOURCES,
        )

        # 11. Insert Subcategories (`subcategories`)
        cur.executemany(
            """
            INSERT INTO subcategories (id, category_id, name, slug, domain)
            VALUES (?, ?, ?, ?, ?);
            """,
            subcategories,
        )

        # 12. Insert 20,658+ Verified Academic Technical Terms (`academic_terms` with normalized_search)
        for rec in academic_terms:
            rec["normalized_search"] = _build_normalized_search(
                rec.get("term", ""),
                rec.get("abbreviations", ""),
                rec.get("translation_ru", ""),
                rec.get("translation_tg", ""),
                rec.get("synonyms", ""),
                rec.get("definition_en", ""),
                rec.get("definition_ru", ""),
            )
        cur.executemany(
            """
            INSERT INTO academic_terms (
                id, term, normalized_term, phonetic_transcription, part_of_speech,
                translation_ru, translation_tg, definition_en, definition_ru,
                academic_explanation_en, academic_explanation_ru, practical_explanation,
                example_en, example_ru, major_category, category_id, subcategory_id,
                domain, subdomain, difficulty_level, cefr_level,
                synonyms, related_terms, abbreviations, source_reference, source_id,
                verification_status, is_favorite, is_in_srs, repetitions, ease_factor,
                interval_days, next_review_at, last_studied_at, created_at, updated_at,
                ipa_source_url, ipa_license, normalized_search
            ) VALUES (
                :id, :term, :normalized_term, :phonetic_transcription, :part_of_speech,
                :translation_ru, :translation_tg, :definition_en, :definition_ru,
                :academic_explanation_en, :academic_explanation_ru, :practical_explanation,
                :example_en, :example_ru, :major_category, :category_id, :subcategory_id,
                :domain, :subdomain, :difficulty_level, :cefr_level,
                :synonyms, :related_terms, :abbreviations, :source_reference, :source_id,
                :verification_status, 0, 0, 0, 2.5,
                0, 0, 0, :created_at, :updated_at,
                :ipa_source_url, :ipa_license, :normalized_search
            );
            """,
            academic_terms,
        )

        # 12b. Install Room @Fts4 contentSyncTriggers and rebuild prebuilt FTS4 index (R12)
        for entity in db_meta["entities"]:
            for trigger_sql in entity.get("contentSyncTriggers", []):
                cur.execute(trigger_sql)
        cur.execute("INSERT INTO `academic_terms_fts`(`academic_terms_fts`) VALUES('rebuild');")

        # 13. Insert Polysemous Senses (`word_senses`)
        cur.executemany(
            """
            INSERT INTO word_senses (
                id, term_id, sense_number, sense_label, domain,
                definition_en, definition_ru, example_en, example_ru
            ) VALUES (
                :id, :term_id, :sense_number, :sense_label, :domain,
                :definition_en, :definition_ru, :example_en, :example_ru
            );
            """,
            word_senses,
        )

        # 14. Insert Term Relations (`word_relations`)
        cur.executemany(
            """
            INSERT INTO word_relations (id, term_id, related_term, relation_type)
            VALUES (:id, :term_id, :related_term, :relation_type);
            """,
            word_relations,
        )

        conn.commit()

        # 15. Strict Integrity & Requirement Assertions
        fk_errors = cur.execute("PRAGMA foreign_key_check;").fetchall()
        assert not fk_errors, f"Foreign key check failed: {fk_errors}"
        integrity = cur.execute("PRAGMA integrity_check;").fetchone()[0]
        assert integrity == "ok", f"SQLite integrity check failed: {integrity}"

        idx_names = {
            row[0]
            for row in cur.execute("SELECT name FROM sqlite_master WHERE type='index';").fetchall()
        }
        required_indexes = {
            # V1 mandatory indexes
            "idx_words_next_review",
            "idx_words_category",
            "idx_words_language",
            "idx_snippets_lang_task",
            "idx_words_term_lang",
            "idx_logs_timestamp",
            "idx_logs_word",
            "idx_logs_scenario",
            "idx_logs_article",
            "idx_logs_snippet",
            # V2 academic dictionary mandatory indexes
            "idx_academic_normalized_term",
            "idx_academic_category_id",
            "idx_academic_subcategory_id",
            "idx_academic_difficulty",
            "idx_academic_cefr",
            "idx_academic_next_review",
            "idx_academic_source_id",
            "idx_academic_domain",
            "idx_academic_status",
            "idx_academic_srs_due",
            "idx_senses_term_id",
            "idx_relations_term_id",
            # V3 stage 2 indexes
            "idx_custom_docs_created",
            "idx_time_attack_score",
            # V4 stage 3 indexes (sync, leaderboards, activity feed)
            "idx_sync_queue_status_created",
            "idx_leaderboard_tab_rank",
            "idx_activity_feed_created",
            # V5 stage 4A community indexes
            "idx_comments_target",
            "idx_community_decks_rating",
            "idx_deck_terms_deck_id",
            "idx_room_messages_room_created",
        }
        missing = required_indexes - idx_names
        assert not missing, f"Missing required SQLite indexes: {missing}"

        cur.execute("ANALYZE;")
        cur.execute("VACUUM;")
        conn.commit()

        print("=== TechEnglish v3.0 Seed Database Built Successfully ===")
        print(f"Output:          {OUTPUT_DB}")
        print(f"Size:            {OUTPUT_DB.stat().st_size / (1024 * 1024):.2f} MB")
        print(f"Room Version:    {version}")
        print(f"Identity Hash:   {identity_hash}")
        print(f"Categories:      {len(CATEGORIES)}")
        print(f"Languages:       {len(LANGUAGES)}")
        print(f"Code Snippets:   {len(all_snippets)}")
        print(f"Core IT Words:   {len(WORDS)}")
        print(f"Academic Terms:  {len(academic_terms):,}")
        print(f"Word Senses:     {len(word_senses)}")
        print(f"Word Sources:    {len(AUTHORITATIVE_SOURCES)}")
        print(f"Subcategories:   {len(subcategories)}")
        print(f"Word Relations:  {len(word_relations):,}")
        print(f"Doc Articles:    {len(DOC_ARTICLES)}")
        print(f"Work Scenarios:  {len(SCENARIOS)}")
        print(f"Work Phrases:    {len(WORK_PHRASES)}")
        print(f"Verified Idx:    {len(required_indexes)}/{len(required_indexes)}")
    finally:
        conn.close()


if __name__ == "__main__":
    build_database()
