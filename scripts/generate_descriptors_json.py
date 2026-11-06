#!/usr/bin/env python3
"""
Regenerate the translated text of the bootstrap descriptors in
`composeApp/src/<flavor>/composeResources/files/assets/descriptors.json`
from the `strings-descriptors.xml` translation files of the same flavor
(`ooniMain` for OONI Probe, `dwMain` for News Media Scan).

For every descriptor the script rewrites `name`, `short_description`,
`description` and their `*_intl` maps. All other fields (link id, nettests,
icon, color, dates, revision...) are kept as they are.

Translation keys come from the group of each oonirun_link_id (see GROUPS):
    <Group> -> Test_<Group>_Fullname
               Dashboard_<Group>_Card_Description
               Dashboard_<Group>_Overview_Paragraph

Usage:
    python3 scripts/generate_descriptors_json.py [--flavor ooniMain dwMain] [--check]
"""

import argparse
import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC_DIR = ROOT / "composeApp/src"
FLAVORS = ("ooniMain", "dwMain")
STRINGS_FILE = "strings-descriptors.xml"
DEFAULT_LANG = "en"

# Injected into `Dashboard_Experimental_Overview_Paragraph` in place of `%1$s`.
EXPERIMENTAL_TEST_LIST = "\n".join(
    [
        "* [STUN Reachability](https://github.com/ooni/spec/blob/master/nettests/ts-025-stun-reachability.md)",
        "* [OpenVPN](https://github.com/ooni/spec/blob/master/nettests/ts-040-openvpn.md)",
        "* [ECH Check](https://github.com/ooni/spec/blob/master/nettests/ts-039-echcheck.md)",
        "* [DNS Check](https://github.com/ooni/spec/blob/master/nettests/ts-028-dnscheck.md)",
        "* [Tor Snowflake](https://ooni.org/nettest/tor-snowflake/) (Long running test)",
        "* [Vanilla Tor](https://github.com/ooni/spec/blob/master/nettests/ts-016-vanilla-tor.md) (Long running test)",
    ]
)

# oonirun_link_id -> group used in the translation keys.
GROUPS = {
    # OONI Probe (ooniMain)
    "00104": "Websites",
    "00105": "InstantMessaging",
    "00106": "Circumvention",
    "00107": "Performance",
    "00108": "Experimental",
    # News Media Scan (dwMain)
    "10004": "TrustedInternationalMedia",
    "10005": "SelectedMedia",
    "10006": "GlobalMedia",
}

ANDROID_ESCAPES = re.compile(r"\\(u[0-9a-fA-F]{4}|.)", re.DOTALL)


def unescape_android(text):
    """Turn Android/Compose string resource escapes into plain text."""

    def replace(match):
        seq = match.group(1)
        if seq.startswith("u") and len(seq) == 5:
            return chr(int(seq[1:], 16))
        return {"n": "\n", "t": "\t"}.get(seq, seq)  # \' \" \\ \@ \? -> literal

    return ANDROID_ESCAPES.sub(replace, text)


def resource_dir_to_lang(dir_name):
    """values -> en, values-fr -> fr, values-zh-rCN -> zh_CN, values-pt-rBR -> pt_BR."""
    if dir_name == "values":
        return DEFAULT_LANG
    qualifier = dir_name[len("values-"):]
    if "-r" in qualifier:
        lang, region = qualifier.split("-r", 1)
        return f"{lang}_{region}"
    return qualifier


def load_translations(res_dir):
    """Return {lang: {key: text}} for every values*/strings-descriptors.xml."""
    translations = {}
    for path in sorted(res_dir.glob(f"values*/{STRINGS_FILE}")):
        lang = resource_dir_to_lang(path.parent.name)
        root = ET.parse(path).getroot()
        translations[lang] = {
            s.get("name"): unescape_android(s.text or "") for s in root.findall("string")
        }
    if DEFAULT_LANG not in translations:
        sys.exit(f"Missing default file {res_dir / 'values' / STRINGS_FILE}")
    return translations


def keys_for(descriptor):
    group = GROUPS.get(descriptor.get("oonirun_link_id"))
    if group is None:
        return None
    return {
        "name": f"Test_{group}_Fullname",
        "short_description": f"Dashboard_{group}_Card_Description",
        "description": f"Dashboard_{group}_Overview_Paragraph",
    }


def format_text(text):
    return text.replace("%1$s", EXPERIMENTAL_TEST_LIST).strip()


def update_descriptor(descriptor, translations):
    keys = keys_for(descriptor)
    if keys is None:
        print(f"skip {descriptor.get('oonirun_link_id')}: not in GROUPS")
        return

    english = translations[DEFAULT_LANG]
    for field, key in keys.items():
        if key not in english:
            sys.exit(f"Key {key} missing from {DEFAULT_LANG} translations")
        default_text = format_text(english[key])
        descriptor[field] = default_text

        intl = {}
        for lang, strings in translations.items():
            text = strings.get(key)
            if lang == DEFAULT_LANG or not text:
                continue
            text = format_text(text)
            # Keep text equal to English too: dropping pt_BR would make the app
            # fall back to another `pt*` entry (see LocalizationString.getCurrent).
            # The API rejects intl values shorter than 2 characters.
            if len(text) >= 2:
                intl[lang] = text
        descriptor[f"{field}_intl"] = intl


def generate(flavor, check):
    """Regenerate one flavor. Returns False if --check found it out of date."""
    res_dir = SRC_DIR / flavor / "composeResources"
    descriptors_json = res_dir / "files/assets/descriptors.json"
    translations = load_translations(res_dir)
    descriptors = json.loads(descriptors_json.read_text(encoding="utf-8"))
    for descriptor in descriptors:
        update_descriptor(descriptor, translations)

    output = json.dumps(descriptors, indent=4, ensure_ascii=False) + "\n"
    path = descriptors_json.relative_to(ROOT)
    if check:
        if descriptors_json.read_text(encoding="utf-8") != output:
            print(f"{path} is out of date")
            return False
        print(f"{path} is up to date")
        return True

    descriptors_json.write_text(output, encoding="utf-8")
    print(f"wrote {path}: {len(descriptors)} descriptors, {len(translations) - 1} languages")
    return True


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument(
        "--flavor",
        nargs="+",
        choices=FLAVORS,
        default=list(FLAVORS),
        help="flavors to regenerate (default: all)",
    )
    p.add_argument(
        "--check",
        action="store_true",
        help="do not write; exit 1 if a descriptors.json is out of date",
    )
    opt = p.parse_args()

    results = [generate(flavor, opt.check) for flavor in opt.flavor]
    if not all(results):
        sys.exit(1)


if __name__ == "__main__":
    main()
