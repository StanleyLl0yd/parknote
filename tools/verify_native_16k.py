#!/usr/bin/env python3
"""Verify packaged Android ELF LOAD segments and uncompressed APK ZIP alignment.

Uses only Python stdlib. APK native libraries must be aligned to 16 KiB in the
ZIP for direct mapping. An AAB is an input to bundletool, not a final installable
APK; its ELF LOAD alignment is checked but ZIP entry alignment is not required.
"""
import argparse
import struct
import sys
import zipfile
from pathlib import Path

PAGE = 16 * 1024
ZIP_LOCAL_SIGNATURE = 0x04034B50


def check_elf(data: bytes, name: str) -> int:
    if len(data) < 64 or data[:4] != b"\x7fELF":
        raise ValueError(f"{name}: not a valid ELF shared library")
    kind, endian_byte = data[4], data[5]
    if endian_byte not in (1, 2) or kind not in (1, 2):
        raise ValueError(f"{name}: unsupported ELF class or byte order")
    endian = "<" if endian_byte == 1 else ">"
    if kind == 2:
        phoff = struct.unpack_from(endian + "Q", data, 32)[0]
        phentsize, count = struct.unpack_from(endian + "HH", data, 54)
        minimum_entry, align_offset = 56, 48
    else:
        phoff = struct.unpack_from(endian + "I", data, 28)[0]
        phentsize, count = struct.unpack_from(endian + "HH", data, 42)
        minimum_entry, align_offset = 32, 28
    if phentsize < minimum_entry or not count or phoff + count * phentsize > len(data):
        raise ValueError(f"{name}: invalid ELF program header table")
    loads = 0
    for i in range(count):
        offset = phoff + i * phentsize
        p_type = struct.unpack_from(endian + "I", data, offset)[0]
        if p_type != 1:
            continue
        loads += 1
        fmt = "Q" if kind == 2 else "I"
        alignment = struct.unpack_from(endian + fmt, data, offset + align_offset)[0]
        if alignment < PAGE or alignment % PAGE:
            raise ValueError(f"{name}: PT_LOAD alignment {alignment:#x} is below 16 KiB")
    if not loads:
        raise ValueError(f"{name}: no PT_LOAD program headers")
    return loads


def check_package(path: Path) -> tuple[int, int]:
    if not path.is_file():
        raise ValueError(f"Missing build output: {path}")
    apk = path.suffix.lower() == ".apk"
    if not apk and path.suffix.lower() != ".aab":
        raise ValueError(f"Unsupported package extension: {path}")
    libs = 0
    segments = 0
    with zipfile.ZipFile(path) as package, path.open("rb") as disk:
        for entry in package.infolist():
            if not entry.filename.endswith(".so"):
                continue
            libs += 1
            segments += check_elf(package.read(entry), entry.filename)
            if apk and entry.compress_type == zipfile.ZIP_STORED:
                disk.seek(entry.header_offset)
                header = disk.read(30)
                if len(header) < 30:
                    raise ValueError(f"{entry.filename}: truncated ZIP local header")
                values = struct.unpack("<IHHHHHIIIHH", header)
                if values[0] != ZIP_LOCAL_SIGNATURE:
                    raise ValueError(f"{entry.filename}: corrupt ZIP local header")
                filename_size, extra_size = values[-2:]
                data_offset = entry.header_offset + 30 + filename_size + extra_size
                if data_offset % PAGE:
                    raise ValueError(f"{entry.filename}: ZIP data offset {data_offset} is not 16 KiB aligned")
    return libs, segments


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("packages", nargs="+", type=Path)
    args = parser.parse_args()
    try:
        for path in args.packages:
            libs, loads = check_package(path)
            print(f"PASS {path}: {libs} native libraries, {loads} aligned PT_LOAD segments")
    except (OSError, ValueError, struct.error, zipfile.BadZipFile) as exc:
        print(f"FAIL: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
