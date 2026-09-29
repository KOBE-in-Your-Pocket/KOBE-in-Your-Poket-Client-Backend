#!/usr/bin/env python3
"""ローマ字読みから韓国語表記を導く（#180）。

423 件 × 4 言語ぶんのハングルを人が書くと、確認もできないし揺れる。
文化体育観光部「外来語表記法」の日本語表記規則を実装し、**人が与えるのは読み
（ローマ字）だけ**にしている。誤りが出るとしても規則的な誤りになり、
glossary.json の読みを直せば全体が直る。

規則の要点:
  - か行 / た行 は語頭で平音（가기구게고 / 다지쓰데도）、語中で激音（카키쿠케코 / 타치쓰테토）
  - が行 / ざ行 / だ行 / ば行 は常に平音
  - ん は前の音節の終声 ㄴ
  - っ（子音重複）は前の音節の終声 ㅅ
  - 長音は表記しない（ō → 오）
"""

from __future__ import annotations

# 語頭 / 語中で表記が変わる行のみ 2 つ持つ。それ以外は同じ値を並べる。
SYLLABLES: dict[str, tuple[str, str]] = {
    "a": ("아", "아"), "i": ("이", "이"), "u": ("우", "우"), "e": ("에", "에"), "o": ("오", "오"),
    "ka": ("가", "카"), "ki": ("기", "키"), "ku": ("구", "쿠"), "ke": ("게", "케"), "ko": ("고", "코"),
    "kya": ("갸", "캬"), "kyu": ("규", "큐"), "kyo": ("교", "쿄"),
    "sa": ("사", "사"), "shi": ("시", "시"), "su": ("스", "스"), "se": ("세", "세"), "so": ("소", "소"),
    "sha": ("샤", "샤"), "shu": ("슈", "슈"), "sho": ("쇼", "쇼"),
    "ta": ("다", "타"), "chi": ("지", "치"), "tsu": ("쓰", "쓰"), "te": ("데", "테"), "to": ("도", "토"),
    "cha": ("자", "차"), "chu": ("주", "추"), "cho": ("조", "초"),
    "na": ("나", "나"), "ni": ("니", "니"), "nu": ("누", "누"), "ne": ("네", "네"), "no": ("노", "노"),
    "nya": ("냐", "냐"), "nyu": ("뉴", "뉴"), "nyo": ("뇨", "뇨"),
    "ha": ("하", "하"), "hi": ("히", "히"), "fu": ("후", "후"), "he": ("헤", "헤"), "ho": ("호", "호"),
    "hya": ("하", "하"), "hyu": ("휴", "휴"), "hyo": ("효", "효"),
    "ma": ("마", "마"), "mi": ("미", "미"), "mu": ("무", "무"), "me": ("메", "메"), "mo": ("모", "모"),
    "mya": ("먀", "먀"), "myu": ("뮤", "뮤"), "myo": ("묘", "묘"),
    "ya": ("야", "야"), "yu": ("유", "유"), "yo": ("요", "요"),
    "ra": ("라", "라"), "ri": ("리", "리"), "ru": ("루", "루"), "re": ("레", "레"), "ro": ("로", "로"),
    "rya": ("랴", "랴"), "ryu": ("류", "류"), "ryo": ("료", "료"),
    "wa": ("와", "와"),
    "ga": ("가", "가"), "gi": ("기", "기"), "gu": ("구", "구"), "ge": ("게", "게"), "go": ("고", "고"),
    "gya": ("갸", "갸"), "gyu": ("규", "규"), "gyo": ("교", "교"),
    "za": ("자", "자"), "ji": ("지", "지"), "zu": ("즈", "즈"), "ze": ("제", "제"), "zo": ("조", "조"),
    "ja": ("자", "자"), "ju": ("주", "주"), "jo": ("조", "조"),
    "da": ("다", "다"), "de": ("데", "데"), "do": ("도", "도"),
    "ba": ("바", "바"), "bi": ("비", "비"), "bu": ("부", "부"), "be": ("베", "베"), "bo": ("보", "보"),
    "bya": ("뱌", "뱌"), "byu": ("뷰", "뷰"), "byo": ("뵤", "뵤"),
    "pa": ("파", "파"), "pi": ("피", "피"), "pu": ("푸", "푸"), "pe": ("페", "페"), "po": ("포", "포"),
    "pya": ("퍄", "퍄"), "pyu": ("퓨", "퓨"), "pyo": ("표", "표"),
}

# 長い綴りから試す（"shi" を "s" + "hi" と読まないため）。
ORDERED = sorted(SYLLABLES, key=len, reverse=True)

# 終声を足すための組み立て表（ハングルの合成規則）。
INITIALS = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
MEDIALS = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ"
FINALS = "ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ"


def with_final(syllable: str, final: str) -> str:
    """ハングル 1 文字に終声を付ける。"""
    code = ord(syllable) - 0xAC00
    if not 0 <= code < 11172:
        return syllable
    initial, medial = divmod(code, 588)
    medial, existing_final = divmod(medial, 28)
    if existing_final:
        return syllable
    return chr(0xAC00 + initial * 588 + medial * 28 + FINALS.index(final) + 1)


def to_hangul(romaji: str) -> str:
    """ヘボン式ローマ字（長音記号なし）をハングルへ転写する。"""
    result: list[str] = []
    for word in romaji.replace("-", " ").split():
        result.append(_word_to_hangul(word.lower()))
    return " ".join(result)


def _word_to_hangul(word: str) -> str:
    out: list[str] = []
    index = 0
    pending_final: str | None = None
    while index < len(word):
        # ん: 後ろが母音でない n（ヘボン式では母音前は n' と書く取り決め）
        if word[index] == "n" and (index + 1 >= len(word) or word[index + 1] in "bcdfghjkmnpqrstvwxyz'"):
            pending_final = "ㄴ"
            index += 1 if word[index : index + 2] != "n'" else 2
            continue
        # っ: 同じ子音が 2 つ続く
        if index + 1 < len(word) and word[index] == word[index + 1] and word[index] not in "aiueon":
            pending_final = "ㅅ"
            index += 1
            continue
        for spelling in ORDERED:
            if word.startswith(spelling, index):
                at_head = not out and pending_final is None
                syllable = SYLLABLES[spelling][0 if at_head else 1]
                if pending_final is not None and out:
                    out[-1] = with_final(out[-1], pending_final)
                    pending_final = None
                elif pending_final is not None:
                    pending_final = None
                out.append(syllable)
                index += len(spelling)
                break
        else:
            # 読みに想定外の綴りがあれば黙って落とさず知らせる。
            raise ValueError(f"ローマ字として解釈できない綴り: {word!r} の {index} 文字目 ({word[index:]!r})")
    if pending_final is not None and out:
        out[-1] = with_final(out[-1], pending_final)
    return "".join(out)


if __name__ == "__main__":
    for sample in ("Higashinada", "Fukaekitamachi", "Rokko", "Sanda", "Myodani", "Hazetani", "Itayado"):
        print(f"{sample:16s} {to_hangul(sample)}")
