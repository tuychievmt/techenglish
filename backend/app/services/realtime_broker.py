from __future__ import annotations

import asyncio
import json
import logging
import time
from typing import Any, Callable, Dict, Optional

from fastapi import Depends

from app.redis_client import get_redis_client

logger = logging.getLogger("techenglish.realtime")

PVP_WAITING_QUEUE_KEY = "techenglish:pvp:waiting_queue"
ROOM_CHANNEL_PREFIX = "techenglish:room:"
PVP_CHANNEL_PREFIX = "techenglish:pvp:"


class RedisRealtimeBroker:
    """
    Stateless Redis-backed realtime broker (SP2-02 / BUG-07).
    Replaces process-local WebSocket connection maps and PvP matchmaking lists so
    multiple uvicorn/gunicorn workers share queues and pub/sub broadcasts seamlessly.
    """

    def __init__(self, redis_client: Any) -> None:
        self._redis = redis_client

    @staticmethod
    def room_channel(room_slug: str) -> str:
        return f"{ROOM_CHANNEL_PREFIX}{room_slug}"

    @staticmethod
    def pvp_channel(match_id: str) -> str:
        return f"{PVP_CHANNEL_PREFIX}{match_id}"

    # ---- PvP Matchmaking Queue (Redis List) ----

    def enqueue_pvp_match(self, match_id: str) -> None:
        self._redis.rpush(PVP_WAITING_QUEUE_KEY, match_id)

    def dequeue_pvp_match(self) -> Optional[str]:
        raw = self._redis.lpop(PVP_WAITING_QUEUE_KEY)
        if raw is None:
            return None
        return raw.decode("utf-8") if isinstance(raw, bytes) else str(raw)

    def remove_pvp_match(self, match_id: str) -> None:
        self._redis.lrem(PVP_WAITING_QUEUE_KEY, 0, match_id)

    def clear_pvp_queue(self) -> None:
        self._redis.delete(PVP_WAITING_QUEUE_KEY)

    # ---- Pub/Sub Publish & Subscribe ----

    def publish_room_event(self, room_slug: str, payload: Dict[str, Any]) -> int:
        encoded = json.dumps(payload, ensure_ascii=False)
        return int(self._redis.publish(self.room_channel(room_slug), encoded))

    def publish_pvp_event(self, match_id: str, payload: Dict[str, Any]) -> int:
        encoded = json.dumps(payload, ensure_ascii=False)
        return int(self._redis.publish(self.pvp_channel(match_id), encoded))

    def subscribe_room(self, room_slug: str) -> Any:
        pubsub = self._redis.pubsub()
        pubsub.subscribe(self.room_channel(room_slug))
        return pubsub

    def subscribe_pvp(self, match_id: str) -> Any:
        pubsub = self._redis.pubsub()
        pubsub.subscribe(self.pvp_channel(match_id))
        return pubsub

    @staticmethod
    def _decode_message(msg: Optional[Dict[str, Any]]) -> Optional[Dict[str, Any]]:
        if not msg or msg.get("type") != "message":
            return None
        data = msg.get("data")
        if isinstance(data, bytes):
            data = data.decode("utf-8")
        if not isinstance(data, str):
            return None
        try:
            parsed = json.loads(data)
            return parsed if isinstance(parsed, dict) else None
        except Exception:
            return None

    def read_next_event(self, pubsub: Any, timeout_seconds: float = 0.05) -> Optional[Dict[str, Any]]:
        deadline = time.monotonic() + max(0.0, timeout_seconds)
        while True:
            remaining = max(0.0, deadline - time.monotonic())
            msg = pubsub.get_message(ignore_subscribe_messages=False, timeout=remaining)
            if msg is None:
                return None
            parsed = self._decode_message(msg)
            if parsed is not None:
                return parsed
            if time.monotonic() >= deadline:
                return None

    async def forward_pubsub_to_websocket(
        self,
        pubsub: Any,
        websocket: Any,
        stop_event: asyncio.Event,
        predicate: Optional[Callable[[Dict[str, Any]], bool]] = None,
        poll_timeout_seconds: float = 0.02,
    ) -> None:
        """Continuously relays Redis Pub/Sub JSON events to a connected WebSocket."""
        while not stop_event.is_set():
            try:
                event = await asyncio.to_thread(
                    self.read_next_event,
                    pubsub,
                    poll_timeout_seconds,
                )
            except asyncio.CancelledError:
                break
            except Exception as exc:
                logger.debug("Redis pubsub read stopped: %s", exc)
                break

            if event is None:
                await asyncio.sleep(0.005)
                continue

            if predicate is not None and not predicate(event):
                continue

            try:
                await websocket.send_json(event)
            except Exception:
                break


def get_realtime_broker(redis_client: Any = Depends(get_redis_client)) -> RedisRealtimeBroker:
    return RedisRealtimeBroker(redis_client)
