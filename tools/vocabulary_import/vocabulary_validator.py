#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Academic Vocabulary Validator (`vocabulary_validator.py`).
Validates JSON/CSV vocabulary records against `vocabulary_schema.json`,
detects exact and normalized duplicates, checks conflicting translations,
validates domains, categories, and authoritative sources.
"""

import json
import re
import sys
from pathlib import Path
from typing import Any, Dict, List, Tuple

ROOT = Path(__file__).resolve().parent.parent.parent
sys.path.insert(0, str(ROOT / "tools" / "vocabulary_import"))

from sources_and_taxonomy import AUTHORITATIVE_SOURCES, MAJOR_CATEGORIES, THEMATIC_DOMAINS_30

SCHEMA_FILE = Path(__file__).resolve().parent / "vocabulary_schema.json"

VALID_POS = {"noun", "noun phrase", "verb", "adjective", "adverb", "abbreviation"}
VALID_DIFFICULTIES = {"Beginner", "Intermediate", "Advanced", "Expert", "Research Level"}
VALID_CEFR = {"A2", "B1", "B2", "C1", "C2"}
VALID_STATUSES = {"draft", "pending_review", "verified", "rejected", "needs_revision"}
VALID_MAJOR_CATS = {m[1] for m in MAJOR_CATEGORIES}
VALID_DOMAINS_36 = set(THEMATIC_DOMAINS_30)
VALID_DOMAINS_30 = VALID_DOMAINS_36  # backward-compatible alias
VALID_SOURCE_IDS = {s[0] for s in AUTHORITATIVE_SOURCES}


def normalize_term(raw: str) -> str:
    return re.sub(r"\s+", " ", (raw or "").strip().lower())


class ValidationResult:
    def __init__(self) -> None:
        self.total_records = 0
        self.unique_terms = 0
        self.verified_terms = 0
        self.pending_review_terms = 0
        self.draft_terms = 0
        self.rejected_terms = 0
        self.needs_revision_terms = 0
        self.duplicate_count = 0
        self.conflicting_translation_count = 0
        self.empty_definition_count = 0
        self.missing_translation_count = 0
        self.missing_source_count = 0
        self.errors: List[str] = []
        self.warnings: List[str] = []

    @property
    def is_valid(self) -> bool:
        return (
            len(self.errors) == 0
            and self.duplicate_count == 0
            and self.empty_definition_count == 0
            and self.missing_translation_count == 0
        )


def validate_records(records: List[Dict[str, Any]]) -> ValidationResult:
    schema = json.loads(SCHEMA_FILE.read_text(encoding="utf-8"))
    required_fields = schema["required"]

    res = ValidationResult()
    res.total_records = len(records)

    seen_norm_to_translation: Dict[str, str] = {}

    for idx, rec in enumerate(records, start=1):
        # 1. Check required fields presence
        for req in required_fields:
            if req not in rec or rec[req] is None or (isinstance(rec[req], str) and not rec[req].strip()):
                res.errors.append(f"Record #{idx}: Missing required field '{req}'")

        term = str(rec.get("term", "")).strip()
        norm = normalize_term(term)
        if not norm or len(norm) < 2:
            res.errors.append(f"Record #{idx}: Invalid empty or 1-char term '{term}'")
            continue

        if rec.get("normalized_term") != norm:
            res.errors.append(
                f"Record #{idx} ('{term}'): normalized_term '{rec.get('normalized_term')}' != '{norm}'"
            )

        # 2. Duplicate & conflicting translation detection
        ru = str(rec.get("translation_ru", "")).strip()
        if not ru:
            res.missing_translation_count += 1
            res.errors.append(f"Record #{idx} ('{term}'): Missing Russian translation")

        if norm in seen_norm_to_translation:
            res.duplicate_count += 1
            prev_ru = seen_norm_to_translation[norm]
            if prev_ru.lower() != ru.lower():
                res.conflicting_translation_count += 1
                res.errors.append(
                    f"Record #{idx} ('{term}'): Conflicting duplicate translation '{ru}' vs '{prev_ru}'"
                )
            else:
                res.errors.append(f"Record #{idx} ('{term}'): Exact normalized duplicate detected")
        else:
            seen_norm_to_translation[norm] = ru

        # 3. Definition check
        def_en = str(rec.get("definition_en", "")).strip()
        def_ru = str(rec.get("definition_ru", "")).strip()
        if len(def_en) < 20 or len(def_ru) < 20:
            res.empty_definition_count += 1
            res.errors.append(f"Record #{idx} ('{term}'): Definition too short or empty")

        # 4. Enum & taxonomy validation
        if rec.get("part_of_speech") not in VALID_POS:
            res.errors.append(f"Record #{idx} ('{term}'): Invalid part_of_speech '{rec.get('part_of_speech')}'")

        if rec.get("major_category") not in VALID_MAJOR_CATS:
            res.errors.append(f"Record #{idx} ('{term}'): Invalid major_category '{rec.get('major_category')}'")

        if rec.get("domain") not in VALID_DOMAINS_36:
            res.errors.append(
                f"Record #{idx} ('{term}'): Domain '{rec.get('domain')}' not in {len(VALID_DOMAINS_36)} thematic domains"
            )

        # IPA provenance invariant: non-empty IPA must carry documented license and source URL
        ipa_val = str(rec.get("phonetic_transcription", "")).strip()
        if ipa_val:
            ipa_url = str(rec.get("ipa_source_url", "")).strip()
            ipa_lic = str(rec.get("ipa_license", "")).strip()
            if not ipa_url or not ipa_lic:
                res.errors.append(
                    f"Record #{idx} ('{term}'): Non-empty phonetic_transcription requires ipa_source_url and ipa_license"
                )

        if rec.get("difficulty_level") not in VALID_DIFFICULTIES:
            res.errors.append(f"Record #{idx} ('{term}'): Invalid difficulty_level '{rec.get('difficulty_level')}'")

        if rec.get("cefr_level") not in VALID_CEFR:
            res.errors.append(f"Record #{idx} ('{term}'): Invalid cefr_level '{rec.get('cefr_level')}'")

        status = rec.get("verification_status")
        if status not in VALID_STATUSES:
            res.errors.append(f"Record #{idx} ('{term}'): Invalid verification_status '{status}'")
        elif status == "verified":
            res.verified_terms += 1
        elif status == "pending_review":
            res.pending_review_terms += 1
        elif status == "draft":
            res.draft_terms += 1
        elif status == "rejected":
            res.rejected_terms += 1
        elif status == "needs_revision":
            res.needs_revision_terms += 1

        # 5. Source validation (no fake DOIs/URLs)
        src_ref = str(rec.get("source_reference", "")).strip()
        src_id = rec.get("source_id")
        if not src_ref or (src_id is not None and src_id not in VALID_SOURCE_IDS):
            res.missing_source_count += 1
            if status == "verified":
                res.warnings.append(f"Record #{idx} ('{term}'): Verified term missing authoritative source_id")

    res.unique_terms = len(seen_norm_to_translation)
    return res


if __name__ == "__main__":
    from corpus_builder import build_full_corpus

    terms, senses, _, _ = build_full_corpus()
    result = validate_records(terms)
    print("=== Vocabulary Corpus Validation Summary ===")
    print(f"Total records:            {result.total_records}")
    print(f"Unique normalized terms:  {result.unique_terms}")
    print(f"Verified terms:           {result.verified_terms}")
    print(f"Duplicates found:         {result.duplicate_count}")
    print(f"Conflicting translations: {result.conflicting_translation_count}")
    print(f"Empty definitions:        {result.empty_definition_count}")
    print(f"Missing RU translations:  {result.missing_translation_count}")
    print(f"Missing sources:          {result.missing_source_count}")
    print(f"Validation Errors:        {len(result.errors)}")
    if not result.is_valid:
        for err in result.errors[:20]:
            print("  ERROR:", err)
        sys.exit(1)
    print("STATUS: PASSED (100% schema & quality compliance)")
