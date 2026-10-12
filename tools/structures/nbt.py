"""Tiny writer for Minecraft's NBT format, enough to save structure templates (.nbt)."""
import gzip
import struct


class Byte(int):
    pass


class Short(int):
    pass


class Long(int):
    pass


class Float(float):
    pass


class Double(float):
    pass


class IntArray(list):
    pass


TAG_END, TAG_BYTE, TAG_SHORT, TAG_INT, TAG_LONG, TAG_FLOAT, TAG_DOUBLE = 0, 1, 2, 3, 4, 5, 6
TAG_BYTE_ARRAY, TAG_STRING, TAG_LIST, TAG_COMPOUND, TAG_INT_ARRAY = 7, 8, 9, 10, 11


def tag_type(value):
    if isinstance(value, bool):
        return TAG_BYTE
    if isinstance(value, Byte):
        return TAG_BYTE
    if isinstance(value, Short):
        return TAG_SHORT
    if isinstance(value, Long):
        return TAG_LONG
    if isinstance(value, int):
        return TAG_INT
    if isinstance(value, Double):
        return TAG_DOUBLE
    if isinstance(value, float):
        return TAG_FLOAT
    if isinstance(value, str):
        return TAG_STRING
    if isinstance(value, IntArray):
        return TAG_INT_ARRAY
    if isinstance(value, list):
        return TAG_LIST
    if isinstance(value, dict):
        return TAG_COMPOUND
    raise TypeError(f"can't store {value!r} in NBT")


def _string(out, text):
    data = text.encode("utf-8")
    out.append(struct.pack(">H", len(data)))
    out.append(data)


def _payload(out, value):
    t = tag_type(value)
    if t == TAG_BYTE:
        out.append(struct.pack(">b", int(value)))
    elif t == TAG_SHORT:
        out.append(struct.pack(">h", value))
    elif t == TAG_INT:
        out.append(struct.pack(">i", value))
    elif t == TAG_LONG:
        out.append(struct.pack(">q", value))
    elif t == TAG_FLOAT:
        out.append(struct.pack(">f", value))
    elif t == TAG_DOUBLE:
        out.append(struct.pack(">d", value))
    elif t == TAG_STRING:
        _string(out, value)
    elif t == TAG_INT_ARRAY:
        out.append(struct.pack(">i", len(value)))
        out.append(struct.pack(f">{len(value)}i", *value))
    elif t == TAG_LIST:
        inner = tag_type(value[0]) if value else TAG_END
        for item in value:
            if tag_type(item) != inner:
                raise TypeError(f"mixed list {value!r}")
        out.append(struct.pack(">bi", inner, len(value)))
        for item in value:
            _payload(out, item)
    elif t == TAG_COMPOUND:
        for key, item in value.items():
            out.append(struct.pack(">b", tag_type(item)))
            _string(out, key)
            _payload(out, item)
        out.append(b"\x00")


def write(path, root):
    out = [struct.pack(">b", TAG_COMPOUND)]
    _string(out, "")
    _payload(out, root)
    with gzip.open(path, "wb", compresslevel=9) as f:
        f.write(b"".join(out))


def read(path):
    """Reads a file back, used by the self check."""
    data = gzip.open(path).read()
    pos = 0

    def take(n):
        nonlocal pos
        chunk = data[pos:pos + n]
        pos += n
        return chunk

    def string():
        (n,) = struct.unpack(">H", take(2))
        return take(n).decode("utf-8")

    def payload(t):
        if t == TAG_BYTE:
            return struct.unpack(">b", take(1))[0]
        if t == TAG_SHORT:
            return struct.unpack(">h", take(2))[0]
        if t == TAG_INT:
            return struct.unpack(">i", take(4))[0]
        if t == TAG_LONG:
            return struct.unpack(">q", take(8))[0]
        if t == TAG_FLOAT:
            return struct.unpack(">f", take(4))[0]
        if t == TAG_DOUBLE:
            return struct.unpack(">d", take(8))[0]
        if t == TAG_STRING:
            return string()
        if t == TAG_INT_ARRAY:
            (n,) = struct.unpack(">i", take(4))
            return list(struct.unpack(f">{n}i", take(4 * n)))
        if t == TAG_LIST:
            inner, n = struct.unpack(">bi", take(5))
            return [payload(inner) for _ in range(n)]
        if t == TAG_COMPOUND:
            result = {}
            while True:
                (inner,) = struct.unpack(">b", take(1))
                if inner == TAG_END:
                    return result
                key = string()
                result[key] = payload(inner)
        raise ValueError(f"unknown tag {t}")

    (t,) = struct.unpack(">b", take(1))
    string()
    return payload(t)
