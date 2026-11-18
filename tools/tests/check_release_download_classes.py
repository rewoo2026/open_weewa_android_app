#!/usr/bin/env python3
"""Reject APKs whose DEX files reference stripped FileDownloader classes.

Usage: python3 tools/tests/check_release_download_classes.py path/to/app.apk
Checks class definitions, not the mere presence of strings or type references.
"""
import argparse
import struct
import zipfile


def dex_types_and_definitions(data):
    if not data.startswith(b'dex\n'):
        raise ValueError('Expected a standard DEX file')
    string_count, string_offset = struct.unpack_from('<II', data, 56)
    type_count, type_offset = struct.unpack_from('<II', data, 64)
    class_count, class_offset = struct.unpack_from('<II', data, 96)
    strings = []
    for index in range(string_count):
        offset = struct.unpack_from('<I', data, string_offset + index * 4)[0]
        while data[offset] & 0x80:
            offset += 1
        offset += 1
        strings.append(data[offset:data.index(b'\0', offset)].decode('utf-8', errors='replace'))
    types = [strings[struct.unpack_from('<I', data, type_offset + index * 4)[0]]
             for index in range(type_count)]
    definitions = {types[struct.unpack_from('<I', data, class_offset + index * 32)[0]]
                   for index in range(class_count)}
    return {name.lstrip('[') for name in types}, definitions


def check_apk(path):
    references, definitions = set(), set()
    with zipfile.ZipFile(path) as apk:
        dex_files = [name for name in apk.namelist() if name.endswith('.dex')]
        if not dex_files:
            raise ValueError('APK contains no DEX files')
        for name in dex_files:
            types, classes = dex_types_and_definitions(apk.read(name))
            references.update(types)
            definitions.update(classes)
    missing = sorted(name for name in references - definitions
                     if name.startswith('Lcom/liulishuo/filedownloader/'))
    if missing:
        raise ValueError('Missing FileDownloader definitions:\n' + '\n'.join(missing))
    print(f'PASS: no missing FileDownloader class definitions across {len(dex_files)} DEX files')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk')
    args = parser.parse_args()
    try:
        check_apk(args.apk)
    except (ValueError, zipfile.BadZipFile) as error:
        parser.exit(1, f'FAIL: {error}\n')
