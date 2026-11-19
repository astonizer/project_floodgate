#!/usr/bin/env python3
"""
Payout screening load generator.

Simulates 6 hours of payout activity (compressed to wall-clock seconds).
Seeds 3 "mule" beneficiaries that each receive payments from 6+ distinct
payers within a 30-minute window, ensuring BENEFICIARY_FAN_IN fires.

Usage:
    pip install requests
    python generate.py [--url http://localhost:8080] [--total 1000]

Outputs:
    - Per-status counts
    - Per-reason-code counts
    - p50 / p95 / p99 latency (target: p95 < 100 ms)
"""

import argparse
import json
import random
import string
import time
from datetime import datetime, timedelta, timezone
from typing import NamedTuple

import requests

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

MULE_BENEFICIARIES = [
    {"account": "MULE000000001", "ifsc": "HDFC0MUL001", "name": "Mule Account Alpha"},
    {"account": "MULE000000002", "ifsc": "ICIC0MUL002", "name": "Mule Account Beta"},
    {"account": "MULE000000003", "ifsc": "SBIN0MUL003", "name": "Mule Account Gamma"},
]

# Each mule gets 6 distinct payers within 30 minutes (triggers FAN_IN at threshold=5)
MULE_PAYER_COUNT = 6

# High-amount senders for HIGH_AMOUNT rule (amount >= 200000)
HIGH_AMOUNT_SENDERS = [f"HIGHAMT{i:06d}" for i in range(1, 6)]

# New beneficiary senders for NEW_BENEFICIARY rule (amount >= 50000, no prior payout)
NEW_BENE_SENDERS = [f"NEWBENE{i:06d}" for i in range(1, 4)]

CHANNELS = ["IMPS", "NEFT", "RTGS"]


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def random_account(prefix: str = "") -> str:
    suffix = "".join(random.choices(string.digits, k=10))
    return (prefix + suffix)[:16]


def random_pan() -> str:
    letters = string.ascii_uppercase
    return (
        "".join(random.choices(letters, k=5))
        + "".join(random.choices(string.digits, k=4))
        + random.choice(letters)
    )


def random_ifsc() -> str:
    bank = "".join(random.choices(string.ascii_uppercase, k=4))
    branch = "".join(random.choices(string.ascii_uppercase + string.digits, k=6))
    return f"{bank}0{branch}"


def payout_id() -> str:
    return "PAY" + "".join(random.choices(string.ascii_uppercase + string.digits, k=12))


# ---------------------------------------------------------------------------
# Payload builders
# ---------------------------------------------------------------------------

def normal_payload(sim_time: datetime) -> dict:
    """Regular payout — expected to be AUTO_RELEASED."""
    return {
        "payoutId":           payout_id(),
        "sourceAccount":      random_account("SRC"),
        "beneficiaryAccount": random_account("BEN"),
        "beneficiaryIfsc":    random_ifsc(),
        "beneficiaryName":    "Normal Beneficiary",
        "pan":                random_pan(),
        "amount":             str(random.randint(1000, 49000)),
        "channel":            random.choice(CHANNELS),
        "currency":           "INR",
        "createdAt":          sim_time.isoformat(),
    }


def mule_payload(mule: dict, source_account: str, sim_time: datetime) -> dict:
    """Payout to a mule from a specific source account."""
    return {
        "payoutId":           payout_id(),
        "sourceAccount":      source_account,
        "beneficiaryAccount": mule["account"],
        "beneficiaryIfsc":    mule["ifsc"],
        "beneficiaryName":    mule["name"],
        "pan":                random_pan(),
        "amount":             str(random.randint(10000, 49000)),
        "channel":            random.choice(CHANNELS),
        "currency":           "INR",
        "createdAt":          sim_time.isoformat(),
    }


def high_amount_payload(source: str, sim_time: datetime) -> dict:
    return {
        "payoutId":           payout_id(),
        "sourceAccount":      source,
        "beneficiaryAccount": random_account("BEN"),
        "beneficiaryIfsc":    random_ifsc(),
        "beneficiaryName":    "High Value Recipient",
        "pan":                random_pan(),
        "amount":             str(random.randint(200000, 500000)),
        "channel":            "RTGS",
        "currency":           "INR",
        "createdAt":          sim_time.isoformat(),
    }


def new_bene_payload(source: str, sim_time: datetime) -> dict:
    return {
        "payoutId":           payout_id(),
        "sourceAccount":      source,
        "beneficiaryAccount": random_account("NEWB"),
        "beneficiaryIfsc":    random_ifsc(),
        "beneficiaryName":    "New Beneficiary",
        "pan":                random_pan(),
        "amount":             str(random.randint(50000, 199000)),
        "channel":            "NEFT",
        "currency":           "INR",
        "createdAt":          sim_time.isoformat(),
    }


# ---------------------------------------------------------------------------
# Simulation schedule
# ---------------------------------------------------------------------------

def build_schedule(total: int, sim_start: datetime, sim_hours: int = 6) -> list[dict]:
    """
    Build an ordered list of payloads spread over sim_hours.
    Mule fan-in events are clustered in 30-minute windows early in the run.
    """
    payloads: list[dict] = []
    sim_end = sim_start + timedelta(hours=sim_hours)
    sim_duration = (sim_end - sim_start).total_seconds()

    # --- Seed mule fan-in events (each within a 30-min window) ---
    for mule in MULE_BENEFICIARIES:
        # Pick a random 30-min window in the first 2 hours
        window_start = sim_start + timedelta(
            minutes=random.randint(0, 90)
        )
        for i in range(MULE_PAYER_COUNT):
            source = f"FANSRC{mule['account'][-3:]}{i:02d}"
            offset = timedelta(minutes=random.randint(0, 29))
            payloads.append(mule_payload(mule, source, window_start + offset))

    # --- Seed HIGH_AMOUNT events ---
    for src in HIGH_AMOUNT_SENDERS:
        offset = timedelta(seconds=random.uniform(0, sim_duration))
        payloads.append(high_amount_payload(src, sim_start + offset))

    # --- Seed NEW_BENEFICIARY events ---
    for src in NEW_BENE_SENDERS:
        offset = timedelta(seconds=random.uniform(0, sim_duration))
        payloads.append(new_bene_payload(src, sim_start + offset))

    # --- Fill the rest with normal payloads ---
    seeded = len(payloads)
    for _ in range(total - seeded):
        offset = timedelta(seconds=random.uniform(0, sim_duration))
        payloads.append(normal_payload(sim_start + offset))

    # Sort by simulated createdAt
    payloads.sort(key=lambda p: p["createdAt"])
    return payloads


# ---------------------------------------------------------------------------
# Runner
# ---------------------------------------------------------------------------

def run(url: str, total: int) -> None:
    endpoint = f"{url}/api/payouts"
    sim_start = datetime.now(timezone.utc) - timedelta(hours=6)

    print(f"Building schedule: {total} payouts over 6 simulated hours...")
    schedule = build_schedule(total, sim_start)
    print(f"Schedule built: {len(schedule)} payouts. Starting submission...\n")

    status_counts: dict[str, int] = {}
    reason_counts: dict[str, int] = {}
    latencies_ms: list[float] = []
    errors = 0

    for i, payload in enumerate(schedule, 1):
        t0 = time.perf_counter()
        try:
            resp = requests.post(endpoint, json=payload, timeout=5)
            elapsed_ms = (time.perf_counter() - t0) * 1000
            latencies_ms.append(elapsed_ms)

            if resp.status_code == 200:
                body = resp.json()
                status = body.get("status", "UNKNOWN")
                status_counts[status] = status_counts.get(status, 0) + 1
                for rc in body.get("reasons", []):
                    reason_counts[rc] = reason_counts.get(rc, 0) + 1
            else:
                errors += 1
                if errors <= 5:
                    print(f"  [ERROR] {resp.status_code}: {resp.text[:200]}")
        except Exception as exc:
            errors += 1
            latencies_ms.append(5000.0)
            if errors <= 5:
                print(f"  [EXCEPTION] {exc}")

        if i % 100 == 0 or i == len(schedule):
            pct = i / len(schedule) * 100
            avg = sum(latencies_ms[-100:]) / min(100, len(latencies_ms))
            print(f"  Progress: {i}/{len(schedule)} ({pct:.0f}%)  "
                  f"avg last 100: {avg:.1f} ms")

    # ---------------------------------------------------------------------------
    # Results
    # ---------------------------------------------------------------------------
    latencies_ms.sort()
    n = len(latencies_ms)

    def percentile(p: float) -> float:
        idx = int(n * p / 100)
        return latencies_ms[min(idx, n - 1)]

    print("\n" + "=" * 60)
    print("RESULTS")
    print("=" * 60)

    print("\nStatus counts:")
    for status, count in sorted(status_counts.items()):
        print(f"  {status:20s}: {count}")

    print("\nReason code counts:")
    if reason_counts:
        for rc, count in sorted(reason_counts.items()):
            print(f"  {rc:30s}: {count}")
    else:
        print("  (none)")

    print(f"\nTotal payouts : {n}")
    print(f"Errors        : {errors}")
    print(f"\nLatency (ms):")
    print(f"  p50  : {percentile(50):.1f} ms")
    print(f"  p95  : {percentile(95):.1f} ms  {'✓ OK' if percentile(95) < 100 else '✗ EXCEEDS 100ms TARGET'}")
    print(f"  p99  : {percentile(99):.1f} ms")
    print(f"  max  : {latencies_ms[-1]:.1f} ms")
    print("=" * 60)


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Payout screening load generator")
    parser.add_argument("--url",   default="http://localhost:8080", help="Base URL")
    parser.add_argument("--total", type=int, default=1000,          help="Total payouts to send")
    args = parser.parse_args()

    run(args.url, args.total)
