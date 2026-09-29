#!/usr/bin/env python3
"""避難所シードの生成（#180）。

神戸市オープンデータのスナップショット（source.json）と訳語辞書（glossary.json）から
Flyway のシードマイグレーションを組み立てる。

手で 423 件 × 4 言語の INSERT を書くとレビューできないため、
「元データ + 辞書 + 組み立て規則」の 3 つに分けている。元データが更新されたら
snapshot を差し替えて再生成する。

    python3 scripts/shelters/build_seed.py            # 生成
    python3 scripts/shelters/build_seed.py --report   # 辞書に足りない語を出す

施設名は「語幹 + 接尾語」に分解して組み立てる（本庄小学校グラウンド →
本庄 / 小学校 / グラウンド）。括弧付きの別名など規則に乗らないものは
glossary の names で個別に上書きする。
"""

from __future__ import annotations

import argparse
import json
import re
from collections import Counter
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent
SOURCE = HERE / "source.json"
GLOSSARY = HERE / "glossary.json"
OUTPUT = REPO / "src/main/resources/db/migration/V19__seed_shelter_open_data.sql"

LANGUAGES = ("ja", "en", "ko", "zh")

# 施設名の接尾語。長いものから順に剥がす（「特別支援学校」が「学校」より先）。
SUFFIXES = [
    "特別支援学校", "高等学校", "小学校", "中学校", "高校", "大学",
    "公民館", "公会堂", "体育館", "会館", "センター", "こども園", "保育園", "幼稚園",
    "公園", "広場", "グラウンド", "運動場", "住宅", "ホール", "病院", "神社", "寺",
    "学校", "別館", "分校",
]

# 屋外の緊急避難場所は名称の末尾に【屋外】が付く。siting 列で表すため名称からは外す。
OUTDOOR_MARKER = "【屋外】"

# オープンデータの ○ △ × － 空 を、そのまま 5 値では持たず 4 値に寄せる。
# 空と － はどちらも「その災害では対象外」。△ は ○ に丸めない（条件付きを使えると誤解させる）。
SUITABILITY = {
    "○": "suitable",
    "△": "conditional",
    "×": "unsuitable",
    "－": "not-applicable",
    "-": "not-applicable",
    "": "not-applicable",
}

PET = {"○": "accepted", "×": "not-accepted", "調整中": "under-consideration"}

DISASTER_COLUMNS = {
    "土砂災害": "suitability_landslide",
    "洪水": "suitability_flood",
    "津波": "suitability_tsunami",
    "大火事": "suitability_large_fire",
}


def load_glossary() -> dict:
    if not GLOSSARY.exists():
        return {k: {} for k in ("wards", "towns", "stems", "suffixes", "names", "notes")}
    data = json.loads(GLOSSARY.read_text(encoding="utf-8"))
    for key in ("wards", "towns", "stems", "suffixes", "names", "notes"):
        data.setdefault(key, {})
    return data


def split_name(raw: str) -> tuple[str, list[str], bool]:
    """施設名を (語幹, 接尾語の並び, 屋外か) に分解する。"""
    outdoor = OUTDOOR_MARKER in raw
    name = raw.replace(OUTDOOR_MARKER, "").strip()
    tail: list[str] = []
    stem = name
    while True:
        for suffix in SUFFIXES:
            if stem.endswith(suffix) and len(stem) > len(suffix):
                tail.insert(0, suffix)
                stem = stem[: -len(suffix)]
                break
        else:
            break
    return stem, tail, outdoor


def split_address(raw: str) -> tuple[str, str, str]:
    """住所を (区, 町名, 番地以降) に分解する。"""
    match = re.match(r"^神戸市(.{1,6}?区)(.*)$", raw)
    if not match:
        raise ValueError(f"想定外の住所: {raw}")
    ward, rest = match.group(1), match.group(2)
    number_match = re.search(r"[0-9０-９].*$", rest)
    if number_match:
        town = rest[: number_match.start()]
        number = number_match.group(0)
    else:
        town, number = rest, ""
    return ward, town, number.replace("‐", "-").replace("　他", " and others")


def translate(term: str, table: dict, lang: str) -> str:
    """辞書を引く。未登録なら日本語のまま返す（生成を止めない）。"""
    entry = table.get(term)
    if not entry:
        return term
    return entry.get(lang) or term


def compose_name(raw: str, glossary: dict, lang: str) -> str:
    if lang == "ja":
        return raw.replace(OUTDOOR_MARKER, "").strip()
    override = glossary["names"].get(raw)
    if override and override.get(lang):
        return override[lang]

    stem, tail, _ = split_name(raw)
    parts = [translate(stem, glossary["stems"], lang)] if stem else []
    parts += [translate(s, glossary["suffixes"], lang) for s in tail]
    # 中国語は分かち書きしない。英語・韓国語は空白で繋ぐ。
    separator = "" if lang == "zh" else " "
    return separator.join(p for p in parts if p).strip()


def compose_address(raw: str, glossary: dict, lang: str) -> str:
    if lang == "ja":
        return raw
    ward, town, number = split_address(raw)
    ward_t = translate(ward, glossary["wards"], lang)
    town_t = translate(town, glossary["towns"], lang)
    if lang == "en":
        # 英語は小さい単位から並べる（番地, 町名, 区, 市）。
        head = f"{number} " if number else ""
        return f"{head}{town_t}, {ward_t}, Kobe".strip()
    # 韓国語・中国語は日本語と同じ大→小の順。
    return f"{'고베시' if lang == 'ko' else '神户市'}{ward_t}{town_t}{number}"


def sql_escape(value: str) -> str:
    return value.replace("'", "''")


def shelter_id(record: dict) -> str:
    """オープンデータの行 id に紐づけた安定 id。再配信で並びが変わると変化する。"""
    return f"kobe-{record['_id']:03d}"


def build_rows(records: list[dict], glossary: dict) -> tuple[list[str], list[str]]:
    shelters, localizations = [], []
    for record in records:
        sid = shelter_id(record)
        outdoor = OUTDOOR_MARKER in record["施設名称"] or record["分類"] == "屋外の緊急避難場所"
        # 全件が指定緊急避難場所。「避難所としての利用」が ○ なら指定避難所も兼ねる。
        shelter_type = "both" if record["避難所としての利用"] == "○" else "emergency"
        suitability = {
            column: SUITABILITY[record[field].strip()]
            for field, column in DISASTER_COLUMNS.items()
        }
        pet = PET[record["ペット同行避難"].strip()]
        phone = record["電話番号"].strip()
        shelters.append(
            "("
            + ", ".join(
                [
                    f"'{sid}'",
                    f"{record['緯度']}",
                    f"{record['経度']}",
                    f"'{shelter_type}'",
                    f"'{'outdoor' if outdoor else 'indoor'}'",
                    f"'{suitability['suitability_landslide']}'",
                    f"'{suitability['suitability_flood']}'",
                    f"'{suitability['suitability_tsunami']}'",
                    f"'{suitability['suitability_large_fire']}'",
                    f"'{pet}'",
                    f"'{sql_escape(phone)}'" if phone else "NULL",
                ]
            )
            + ")"
        )
        note_raw = record["備考"].strip()
        for lang in LANGUAGES:
            name = compose_name(record["施設名称"], glossary, lang)
            address = compose_address(record["住所"], glossary, lang)
            if not note_raw:
                note = "NULL"
            elif lang == "ja":
                note = f"'{sql_escape(note_raw)}'"
            else:
                note = f"'{sql_escape(translate(note_raw, glossary['notes'], lang))}'"
            localizations.append(
                f"('{sid}', '{lang}', '{sql_escape(name)}', '{sql_escape(address)}', {note})"
            )
    return shelters, localizations


def missing_terms(records: list[dict], glossary: dict) -> dict[str, list[tuple[str, int]]]:
    """辞書に無い語を、出現回数の多い順に返す。"""
    counters = {"wards": Counter(), "towns": Counter(), "stems": Counter(), "suffixes": Counter(), "notes": Counter()}
    for record in records:
        ward, town, _ = split_address(record["住所"])
        counters["wards"][ward] += 1
        counters["towns"][town] += 1
        stem, tail, _ = split_name(record["施設名称"])
        if stem:
            counters["stems"][stem] += 1
        for suffix in tail:
            counters["suffixes"][suffix] += 1
        if record["備考"].strip():
            counters["notes"][record["備考"].strip()] += 1

    missing = {}
    for key, counter in counters.items():
        table = glossary[key]
        missing[key] = [
            (term, count)
            for term, count in counter.most_common()
            if term not in table or not all(table[term].get(lang) for lang in ("en", "ko", "zh"))
        ]
    return missing


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--report", action="store_true", help="辞書に足りない語を出力する")
    args = parser.parse_args()

    source = json.loads(SOURCE.read_text(encoding="utf-8"))
    records = source["records"]
    glossary = load_glossary()

    if args.report:
        missing = missing_terms(records, glossary)
        for key, terms in missing.items():
            print(f"=== {key}: 未登録 {len(terms)} 語 ===")
            for term, count in terms:
                print(f"  ({count}) {term}")
        total = sum(len(v) for v in missing.values())
        print(f"\n合計 未登録 {total} 語")
        return

    shelters, localizations = build_rows(records, glossary)
    header = f"""-- 避難所シード（#180）。**このファイルは生成物。手で編集しない。**
--
--   生成:   python3 scripts/shelters/build_seed.py
--   元データ: scripts/shelters/source.json（神戸市オープンデータのスナップショット）
--   訳語:   scripts/shelters/glossary.json
--
-- 神戸市オープンデータカタログ / 指定緊急避難場所・指定避難所
-- resource_id 74d6b85b-17a6-4ba8-b7f6-af3ea15f9283（{source['total']} 件）
--
-- 列の意味と 5 値 → 4 値の寄せ方は V18 のコメントを参照。

"""
    body = [
        "INSERT INTO shelter (id, latitude, longitude, type, siting,",
        "    suitability_landslide, suitability_flood, suitability_tsunami, suitability_large_fire,",
        "    pet_acceptance, phone_number) VALUES",
        ",\n".join(shelters) + ";",
        "",
        "INSERT INTO shelter_localization (shelter_id, language, name, address, note) VALUES",
        ",\n".join(localizations) + ";",
        "",
    ]
    OUTPUT.write_text(header + "\n".join(body), encoding="utf-8")
    print(f"{OUTPUT.relative_to(REPO)} を生成: 避難所 {len(shelters)} 件 / ローカライズ {len(localizations)} 行")


if __name__ == "__main__":
    main()
