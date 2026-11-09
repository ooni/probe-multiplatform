#!/usr/bin/env python3
"""
Push the translated text of the bootstrap descriptors in
`composeApp/src/<flavor>/composeResources/files/assets/descriptors.json`
(`ooniMain` and `dwMain`) to the OONI Run v2 API (`PUT /api/v2/oonirun/links/{oonirun_link_id}`).

Only the text fields are updated: `name`, `short_description`, `description`
and their `*_intl` maps. Every other field (nettests, icon, color, author,
expiration date...) is taken from the live link, so the nettest revision does
not change.

Run `generate_descriptors_json.py` first so descriptors.json is up to date.
Languages not in `SUPPORTED_LANGUAGES` (generate_descriptors_json.py) are
dropped from the `*_intl` maps before sending.

Usage:
    python3 scripts/update_descriptors_online.py --token <JWT> [--dry-run]
    python3 scripts/update_descriptors_online.py --token <JWT> --base-url https://api.dev.ooni.io
"""

import argparse
import json
import sys
import urllib.error
import urllib.request
from pathlib import Path

sys.dont_write_bytecode = True  # no scripts/__pycache__ from the import below
from generate_descriptors_json import SUPPORTED_LANGUAGES  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
FLAVORS = ("ooniMain", "dwMain")


def descriptors_json(flavor):
    return ROOT / f"composeApp/src/{flavor}/composeResources/files/assets/descriptors.json"


TEXT_FIELDS = (
    "name",
    "short_description",
    "description",
    "name_intl",
    "short_description_intl",
    "description_intl",
)
# Fields accepted by OONIRunLinkCreateEdit, other than the text fields.
KEPT_FIELDS = ("author", "icon", "color", "expiration_date", "nettests")
NETTEST_FIELDS = (
    "test_name",
    "inputs",
    "options",
    "is_background_run_enabled_default",
    "is_manual_run_enabled_default",
    "targets_name",
    "inputs_extra",
)


def request(method, url, token=None, body=None):
    headers = {"Content-Type": "application/json", "Accept": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=60) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        detail = e.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"{method} {url} failed: HTTP {e.code} {detail}") from None


def build_payload(live, local, publish_email):
    payload = {field: live.get(field) for field in KEPT_FIELDS}
    payload["nettests"] = [
        {k: nt[k] for k in NETTEST_FIELDS if k in nt} for nt in live.get("nettests", [])
    ]
    payload["publish_email"] = live.get("publish_email", publish_email)
    for field in TEXT_FIELDS:
        value = local.get(field)
        if field.endswith("_intl") and value:
            value = {lang: text for lang, text in value.items() if lang in SUPPORTED_LANGUAGES}
        payload[field] = value
    return payload


def summarize_changes(live, payload):
    changes = []
    for field in TEXT_FIELDS:
        old, new = live.get(field), payload.get(field)
        if old == new:
            continue
        if isinstance(new, dict):
            old = old or {}
            added = sorted(set(new) - set(old))
            removed = sorted(set(old) - set(new))
            changed = sorted(k for k in set(new) & set(old) if new[k] != old[k])
            changes.append(
                f"  {field}: +{len(added)} -{len(removed)} ~{len(changed)} languages"
                + (f" (added: {', '.join(added)})" if added else "")
            )
        else:
            changes.append(f"  {field} changed")
    return changes


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--token", required=True, help="OONI account JWT (admin, or the link author)")
    p.add_argument("--base-url", default="https://api.ooni.org", help="OONI API base URL")
    p.add_argument(
        "--flavor",
        nargs="+",
        choices=FLAVORS,
        default=list(FLAVORS),
        help="flavors whose descriptors to update (default: all)",
    )
    p.add_argument("--ids", nargs="*", help="only update these oonirun_link_id values")
    p.add_argument(
        "--publish-email",
        action=argparse.BooleanOptionalAction,
        default=True,
        help="publish_email value if the live link does not report one (default: true)",
    )
    p.add_argument("--dry-run", action="store_true", help="show changes without sending PUT requests")
    opt = p.parse_args()

    base_url = opt.base_url.rstrip("/")
    descriptors = [
        descriptor
        for flavor in opt.flavor
        for descriptor in json.loads(descriptors_json(flavor).read_text(encoding="utf-8"))
    ]
    if opt.ids:
        descriptors = [d for d in descriptors if d["oonirun_link_id"] in opt.ids]

    failures = 0
    for local in descriptors:
        link_id = local["oonirun_link_id"]
        url = f"{base_url}/api/v2/oonirun/links/{link_id}"
        try:
            live = request("GET", url, token=opt.token)
            payload = build_payload(live, local, opt.publish_email)
            changes = summarize_changes(live, payload)
            if not changes:
                print(f"{link_id} {local['name']}: up to date")
                continue
            print(f"{link_id} {local['name']}:")
            print("\n".join(changes))
            if opt.dry_run:
                continue
            updated = request("PUT", url, token=opt.token, body=payload)
            print(f"  updated (revision {updated.get('revision')}, date_updated {updated.get('date_updated')})")
        except RuntimeError as e:
            failures += 1
            print(f"{link_id}: {e}", file=sys.stderr)

    if failures:
        sys.exit(1)


if __name__ == "__main__":
    main()
