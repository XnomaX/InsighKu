"""Unit tests for the CORD-v2 token parser. Run: `python -m pytest test_parse_cord.py`
or `python test_parse_cord.py` (no pytest needed)."""

from cord_parser import parse_cord

SAMPLE = (
    "<s><s_cord_receipt><s_items>"
    "<s_item><s_itemname>ES KOPI SUSU SASTRA</s_itemname><s_qty>1</s_qty><s_price>22000</s_price></s_item>"
    "<s_item><s_itemname>GORENAN</s_itemname><s_qty>2</s_qty><s_price>10000</s_price></s_item>"
    "</s_items><s_total>32000</s_total></s>"
)


def test_parses_items_and_total():
    out = parse_cord(SAMPLE)
    assert out["total"] == "32000"
    assert len(out["items"]) == 2
    assert out["items"][0] == {"name": "ES KOPI SUSU SASTRA", "qty": "1", "price": "22000"}
    assert out["items"][1] == {"name": "GORENAN", "qty": "2", "price": "10000"}


def test_empty_sequence():
    out = parse_cord("")
    assert out == {"items": [], "total": ""}


def test_missing_total():
    seq = "<s_item><s_itemname>X</s_itemname></s_item>"
    out = parse_cord(seq)
    assert out["total"] == ""
    assert out["items"] == [{"name": "X", "qty": "", "price": ""}]


def test_missing_fields_in_item():
    seq = "<s_item><s_itemname>Y</s_itemname></s_item><s_total>9</s_total>"
    out = parse_cord(seq)
    assert out["total"] == "9"
    assert out["items"] == [{"name": "Y", "qty": "", "price": ""}]


if __name__ == "__main__":
    test_parses_items_and_total()
    test_empty_sequence()
    test_missing_total()
    test_missing_fields_in_item()
    print("all parse_cord tests passed")
