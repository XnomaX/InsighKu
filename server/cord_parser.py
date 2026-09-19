"""Pure CORD-v2 token parser. No torch/transformers/Flask imports, so it can be
unit-tested anywhere. `app.py` imports `parse_cord` from here."""

from __future__ import annotations

import re

_ITEM_RE = re.compile(r"<s_item>(.*?)</s_item>", re.DOTALL)
_TOTAL_RE = re.compile(r"<s_total>(.*?)</s_total>", re.DOTALL)

# Matches any CORD/Donut special token: <s>, </s>, <s_cord_receipt>,
# </s_cord_receipt>, <s_itemname>, </s_itemname>, <s_total_price>, etc.
# The custom fine-tune's CORD tokens are often NOT flagged as "special" by the
# tokenizer, so skip_special_tokens=True at decode time leaves them in the text.
# This strips them generically, wherever they appear.
_TOKEN_RE = re.compile(r"</?s[a-z_]*>")


def _clean(value: str) -> str:
    """Strip every CORD/Donut special token from a value."""
    return _TOKEN_RE.sub("", value).strip()


def strip_tokens(seq: str) -> str:
    """Remove all CORD/Donut special tokens from a raw token sequence."""
    return _TOKEN_RE.sub("", seq).strip()


def _field(block: str, token: str) -> str:
    m = re.search(rf"<{token}>(.*?)</{token}>", block, re.DOTALL)
    return _clean(m.group(1)) if m else ""


def parse_cord(seq: str) -> dict:
    """Parse a decoded CORD-v2 token sequence into structured data.

    Robust to nesting drift: extracts item blocks and the total directly from
    the token stream. `merchant` is always None — CORD-v2 has no merchant field.
    All extracted values are scrubbed of stray CORD special tokens so nothing
    like `<s_itemname>` leaks into the form.
    """
    items = []
    for block in _ITEM_RE.findall(seq):
        items.append(
            {
                "name": _field(block, "s_itemname"),
                "qty": _field(block, "s_qty"),
                "price": _field(block, "s_price"),
            }
        )
    total = _TOTAL_RE.findall(seq)
    total = _clean(total[0]) if total else ""
    return {"items": items, "total": total}
