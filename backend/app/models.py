from __future__ import annotations

from sqlalchemy import (
    BigInteger,
    Boolean,
    Column,
    Float,
    ForeignKey,
    Integer,
    String,
    Text,
    UniqueConstraint,
    create_engine,
)
from sqlalchemy.orm import DeclarativeBase, sessionmaker

from app.config import is_prod_env, settings


class Base(DeclarativeBase):
    pass


class UserRow(Base):
    __tablename__ = "users"

    id = Column(String(64), primary_key=True)
    username = Column(String(32), unique=True, nullable=False, index=True)
    email = Column(String(255), unique=True, nullable=True, index=True)
    password_hash = Column(String(255), nullable=True)
    provider = Column(String(32), nullable=False, default="EMAIL")
    role = Column(String(24), nullable=False, default="USER")
    is_email_verified = Column(Boolean, nullable=False, default=False)
    email_verify_token = Column(String(128), nullable=True)
    email_verify_expires_at_ms = Column(BigInteger, nullable=True)
    password_reset_token = Column(String(128), nullable=True)
    password_reset_expires_at_ms = Column(BigInteger, nullable=True)
    password_reset_attempts = Column(Integer, nullable=False, default=0)
    display_name = Column(String(120), nullable=False, default="Разработчик")
    avatar_key = Column(String(512), nullable=False, default="preset_terminal")
    status_text = Column(String(200), nullable=False, default="Studying Technical English")
    tech_stack_csv = Column(String(255), nullable=False, default="Kotlin,Python,SQL")
    github_url = Column(String(255), nullable=False, default="")
    telegram_handle = Column(String(120), nullable=False, default="")
    league = Column(String(32), nullable=False, default="BRONZE")
    total_xp = Column(Integer, nullable=False, default=0)
    squad_bonus_xp = Column(Integer, nullable=False, default=0)
    streak_days = Column(Integer, nullable=False, default=0)
    accepts_pings = Column(Boolean, nullable=False, default=True)
    confirmed_reports = Column(Integer, nullable=False, default=0)
    is_contributor = Column(Boolean, nullable=False, default=False)
    created_at_ms = Column(BigInteger, nullable=False)


class OAuthIdentityRow(Base):
    __tablename__ = "oauth_identities"
    __table_args__ = (UniqueConstraint("provider", "provider_sub", name="uq_oauth_provider_sub"),)

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(String(64), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    provider = Column(String(32), nullable=False)
    provider_sub = Column(String(255), nullable=False)
    provider_email = Column(String(255), nullable=True)
    created_at_ms = Column(BigInteger, nullable=False)


class SyncEventRow(Base):
    __tablename__ = "sync_events"

    event_id = Column(String(64), primary_key=True)
    user_id = Column(String(64), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    event_type = Column(String(64), nullable=False)
    payload_json = Column(Text, nullable=False, default="{}")
    awarded_xp = Column(Integer, nullable=False, default=0)
    client_timestamp_ms = Column(BigInteger, nullable=False)
    server_received_ms = Column(BigInteger, nullable=False, index=True)
    week_start_utc5_ms = Column(BigInteger, nullable=False, index=True)


class Sm2StateRow(Base):
    __tablename__ = "sm2_states"
    __table_args__ = (UniqueConstraint("user_id", "normalized_term", name="uq_sm2_user_term"),)

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(String(64), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    normalized_term = Column(String(200), nullable=False)
    repetitions = Column(Integer, nullable=False, default=0)
    ease_factor = Column(Float, nullable=False, default=2.5)
    interval_days = Column(Integer, nullable=False, default=0)
    next_review_at_ms = Column(BigInteger, nullable=False, default=0)
    client_timestamp_ms = Column(BigInteger, nullable=False, default=0)
    server_received_ms = Column(BigInteger, nullable=False, default=0)


class UserFavoriteRow(Base):
    __tablename__ = "user_favorites"
    __table_args__ = (UniqueConstraint("user_id", "normalized_term", name="uq_fav_user_term"),)

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(String(64), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    normalized_term = Column(String(200), nullable=False)


class UserCustomWordRow(Base):
    __tablename__ = "user_custom_words"
    __table_args__ = (UniqueConstraint("user_id", "normalized_term", name="uq_custom_user_term"),)

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(String(64), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    normalized_term = Column(String(200), nullable=False)
    translation = Column(String(300), nullable=False, default="")


class FriendshipRow(Base):
    __tablename__ = "friendships"
    __table_args__ = (UniqueConstraint("user_id", "friend_id", name="uq_friendship_pair"),)

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(String(64), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    friend_id = Column(String(64), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    created_at_ms = Column(BigInteger, nullable=False)


class FriendPingRow(Base):
    __tablename__ = "friend_pings"

    id = Column(Integer, primary_key=True, autoincrement=True)
    sender_id = Column(String(64), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    receiver_id = Column(String(64), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    sent_at_ms = Column(BigInteger, nullable=False, index=True)


class ActivityFeedRow(Base):
    __tablename__ = "activity_feed"

    id = Column(String(64), primary_key=True)
    actor_user_id = Column(String(64), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    summary_ru = Column(String(300), nullable=False)
    reaction_count = Column(Integer, nullable=False, default=0)
    created_at_ms = Column(BigInteger, nullable=False, index=True)


class UserBlockRow(Base):
    __tablename__ = "user_blocks"
    __table_args__ = (UniqueConstraint("blocker_id", "blocked_id", name="uq_user_block"),)

    id = Column(Integer, primary_key=True, autoincrement=True)
    blocker_id = Column(String(64), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    blocked_id = Column(String(64), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    created_at_ms = Column(BigInteger, nullable=False)


class ModerationReportRow(Base):
    __tablename__ = "moderation_reports"

    id = Column(String(64), primary_key=True)
    reporter_id = Column(String(64), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    target_type = Column(String(32), nullable=False)
    target_id = Column(String(128), nullable=False)
    reason = Column(String(300), nullable=False)
    status = Column(String(32), nullable=False, default="OPEN")
    created_at_ms = Column(BigInteger, nullable=False)


# =========================================================================
# Stage 4A ORM Tables (ФТ-13.1 Comments/Mnemonics & ФТ-13.2 Community Decks)
# =========================================================================


class CommentRow(Base):
    __tablename__ = "comments"

    id = Column(String(64), primary_key=True)
    target_type = Column(String(16), nullable=False, index=True)  # TERM | SNIPPET
    target_id = Column(Integer, nullable=False, index=True)
    author_user_id = Column(String(64), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    author_username = Column(String(32), nullable=False)
    author_rank_title = Column(String(64), nullable=False, default="Intern Developer")
    body = Column(Text, nullable=False)
    upvotes = Column(Integer, nullable=False, default=0)
    downvotes = Column(Integer, nullable=False, default=0)
    is_hidden = Column(Boolean, nullable=False, default=False)
    created_at_ms = Column(BigInteger, nullable=False, index=True)


class CommentVoteRow(Base):
    __tablename__ = "comment_votes"
    __table_args__ = (UniqueConstraint("comment_id", "user_id", name="uq_comment_vote_user"),)

    id = Column(Integer, primary_key=True, autoincrement=True)
    comment_id = Column(String(64), ForeignKey("comments.id", ondelete="CASCADE"), nullable=False, index=True)
    user_id = Column(String(64), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    vote = Column(Integer, nullable=False)  # -1 or +1


class CommunityDeckRow(Base):
    __tablename__ = "community_decks"

    id = Column(String(64), primary_key=True)
    title = Column(String(100), nullable=False)
    description = Column(String(500), nullable=False, default="")
    tags_csv = Column(String(255), nullable=False, default="")
    author_user_id = Column(String(64), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    author_username = Column(String(32), nullable=False)
    rating = Column(Float, nullable=False, default=0.0)
    downloads_count = Column(Integer, nullable=False, default=0)
    term_count = Column(Integer, nullable=False, default=0)
    is_hidden = Column(Boolean, nullable=False, default=False)
    created_at_ms = Column(BigInteger, nullable=False, index=True)


class CommunityDeckTermRow(Base):
    __tablename__ = "community_deck_terms"

    id = Column(Integer, primary_key=True, autoincrement=True)
    deck_id = Column(String(64), ForeignKey("community_decks.id", ondelete="CASCADE"), nullable=False, index=True)
    term = Column(String(200), nullable=False)
    transcription = Column(String(200), nullable=False, default="")
    translation = Column(String(300), nullable=False)
    example = Column(Text, nullable=False, default="")
    explanation_ru = Column(Text, nullable=False, default="")


class CommunityDeckRatingRow(Base):
    __tablename__ = "community_deck_ratings"
    __table_args__ = (UniqueConstraint("deck_id", "user_id", name="uq_deck_rating_user"),)

    id = Column(Integer, primary_key=True, autoincrement=True)
    deck_id = Column(String(64), ForeignKey("community_decks.id", ondelete="CASCADE"), nullable=False, index=True)
    user_id = Column(String(64), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    stars = Column(Integer, nullable=False)


# =========================================================================
# Stage 4B ORM Tables (AI Proxy Quota, Squads ФТ-14.4, Crowdsourcing ФТ-14.3)
# =========================================================================


class AiUsageLogRow(Base):
    __tablename__ = "ai_usage_logs"

    id = Column(String(64), primary_key=True)
    user_id = Column(String(64), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    feature = Column(String(32), nullable=False)  # INTERVIEW | CODE_REVIEW
    utc_day = Column(String(16), nullable=False, index=True)  # YYYY-MM-DD
    created_at_ms = Column(BigInteger, nullable=False)


class SquadRow(Base):
    __tablename__ = "squads"

    id = Column(String(64), primary_key=True)
    name = Column(String(80), nullable=False)
    invite_code = Column(String(24), unique=True, nullable=False, index=True)
    creator_user_id = Column(String(64), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    weekly_goal_xp = Column(Integer, nullable=False, default=1000)
    created_at_ms = Column(BigInteger, nullable=False)


class SquadMemberRow(Base):
    __tablename__ = "squad_members"
    __table_args__ = (UniqueConstraint("squad_id", "user_id", name="uq_squad_member"),)

    id = Column(Integer, primary_key=True, autoincrement=True)
    squad_id = Column(String(64), ForeignKey("squads.id", ondelete="CASCADE"), nullable=False, index=True)
    user_id = Column(String(64), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    joined_at_ms = Column(BigInteger, nullable=False)


class DictionaryPrRow(Base):
    __tablename__ = "dictionary_prs"

    id = Column(String(64), primary_key=True)
    author_user_id = Column(String(64), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    author_username = Column(String(32), nullable=False)
    term = Column(String(120), nullable=False)
    proposed_translation_ru = Column(String(300), nullable=False, default="")
    proposed_translation_tg = Column(String(300), nullable=False, default="")
    proposed_definition_ru = Column(Text, nullable=False, default="")
    source_reference = Column(String(300), nullable=False)
    license_accepted = Column(Boolean, nullable=False, default=True)
    status = Column(String(24), nullable=False, default="OPEN")  # OPEN | APPROVED | REJECTED
    upvotes = Column(Integer, nullable=False, default=0)
    downvotes = Column(Integer, nullable=False, default=0)
    created_at_ms = Column(BigInteger, nullable=False, index=True)


class DictionaryPrVoteRow(Base):
    __tablename__ = "dictionary_pr_votes"
    __table_args__ = (UniqueConstraint("pr_id", "user_id", name="uq_dict_pr_vote"),)

    id = Column(Integer, primary_key=True, autoincrement=True)
    pr_id = Column(String(64), ForeignKey("dictionary_prs.id", ondelete="CASCADE"), nullable=False, index=True)
    user_id = Column(String(64), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    vote = Column(Integer, nullable=False)


# =========================================================================
# Stage 4C ORM Tables (Rooms ФТ-13.3, PvP Duels ФТ-14.1, Peer Review ФТ-14.2)
# =========================================================================


class RoomMessageRow(Base):
    __tablename__ = "room_messages"

    id = Column(String(64), primary_key=True)
    room_slug = Column(String(48), nullable=False, index=True)
    author_user_id = Column(String(64), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    author_username = Column(String(32), nullable=False)
    author_rank_title = Column(String(64), nullable=False, default="Intern Developer")
    body = Column(Text, nullable=False)
    is_hidden = Column(Boolean, nullable=False, default=False)
    created_at_ms = Column(BigInteger, nullable=False, index=True)


class PvpMatchRow(Base):
    __tablename__ = "pvp_matches"

    id = Column(String(64), primary_key=True)
    player1_id = Column(String(64), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    player2_id = Column(String(64), nullable=True, index=True)
    mode = Column(String(32), nullable=False, default="TRAINING_FALLBACK")  # RANKED_1V1 | TRAINING_FALLBACK
    state = Column(String(32), nullable=False, default="IN_PROGRESS")
    questions_json = Column(Text, nullable=False, default="[]")
    player1_score = Column(Integer, nullable=False, default=0)
    player2_score = Column(Integer, nullable=False, default=0)
    ranked_xp_awarded = Column(Integer, nullable=False, default=0)
    created_at_ms = Column(BigInteger, nullable=False, index=True)
    finished_at_ms = Column(BigInteger, nullable=False, default=0)


class PeerReviewSubmissionRow(Base):
    __tablename__ = "peer_review_submissions"

    id = Column(String(64), primary_key=True)
    case_id = Column(String(64), nullable=False, index=True)
    author_user_id = Column(String(64), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    author_username = Column(String(32), nullable=False)
    author_rank_title = Column(String(64), nullable=False, default="Intern Developer")
    text_response = Column(Text, nullable=False, default="")
    voice_object_key = Column(String(255), nullable=True)
    voice_duration_seconds = Column(Integer, nullable=False, default=0)
    voice_expires_at_ms = Column(BigInteger, nullable=False, default=0)
    is_hidden = Column(Boolean, nullable=False, default=False)
    created_at_ms = Column(BigInteger, nullable=False, index=True)


class PeerReviewFeedbackRow(Base):
    __tablename__ = "peer_review_feedback"

    id = Column(String(64), primary_key=True)
    submission_id = Column(String(64), ForeignKey("peer_review_submissions.id", ondelete="CASCADE"), nullable=False, index=True)
    reviewer_user_id = Column(String(64), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    reviewer_username = Column(String(32), nullable=False)
    reviewer_rank_title = Column(String(64), nullable=False, default="Intern Developer")
    rating_stars = Column(Integer, nullable=False, default=5)
    comment_text = Column(Text, nullable=False)
    created_at_ms = Column(BigInteger, nullable=False)


class DevicePushTokenRow(Base):
    __tablename__ = "device_push_tokens"

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(String(64), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    platform = Column(String(24), nullable=False, default="ANDROID")
    token = Column(String(512), unique=True, nullable=False)
    created_at_ms = Column(BigInteger, nullable=False)
    updated_at_ms = Column(BigInteger, nullable=False)


class ActivityFeedReactionRow(Base):
    __tablename__ = "activity_feed_reactions"
    __table_args__ = (UniqueConstraint("event_id", "user_id", name="uq_feed_reaction_event_user"),)

    id = Column(Integer, primary_key=True, autoincrement=True)
    event_id = Column(String(64), ForeignKey("activity_feed.id", ondelete="CASCADE"), nullable=False, index=True)
    user_id = Column(String(64), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    created_at_ms = Column(BigInteger, nullable=False)


def normalize_database_url(database_url: str) -> str:
    url = (database_url or "").strip()
    if url.startswith("postgres://"):
        return "postgresql+psycopg://" + url[len("postgres://"):]
    if url.startswith("postgresql://"):
        return "postgresql+psycopg://" + url[len("postgresql://"):]
    return url


def build_engine_kwargs(database_url: str = settings.database_url) -> dict:
    normalized = normalize_database_url(database_url)
    if normalized.startswith("sqlite"):
        return {"connect_args": {"check_same_thread": False}, "future": True}
    return {
        "future": True,
        "pool_pre_ping": True,
        "pool_size": 3,
        "max_overflow": 4,
        "pool_recycle": 280,
    }


def build_engine(database_url: str = settings.database_url):
    normalized = normalize_database_url(database_url)
    return create_engine(normalized, **build_engine_kwargs(normalized))


engine = build_engine()
SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False, future=True)


def init_db(target_engine=engine) -> None:
    from app.config import is_deployed_env

    if is_deployed_env():
        return
    Base.metadata.create_all(bind=target_engine)
