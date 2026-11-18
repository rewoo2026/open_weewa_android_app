#!/usr/bin/env python3
"""Reject APKs referencing List removal APIs missing below Android 15.

Kotlin calls to MutableList.removeFirst/removeLast can compile to Android 15
platform methods with compileSdk >= 35. A completed camera file then crashes
on Android 14 and older with NoSuchMethodError.
"""
import argparse
import struct
import zipfile


def unsupported_methods(data):
    if not data.startswith(b'dex\n'):
        raise ValueError('Expected a standard DEX file')
    string_count, string_offset = struct.unpack_from('<II', data, 56)
    type_count, type_offset = struct.unpack_from('<II', data, 64)
    method_count, method_offset = struct.unpack_from('<II', data, 88)
    strings = []
    for index in range(string_count):
        offset = struct.unpack_from('<I', data, string_offset + index * 4)[0]
        while data[offset] & 0x80:
            offset += 1
        offset += 1
        strings.append(data[offset:data.index(b'\0', offset)].decode('utf-8', errors='replace'))
    types = [strings[struct.unpack_from('<I', data, type_offset + index * 4)[0]]
             for index in range(type_count)]
    for index in range(method_count):
        owner, _, name = struct.unpack_from('<HHI', data, method_offset + index * 8)
        if types[owner] in {'Ljava/util/List;', 'Ljava/util/ArrayList;',
                            'Ljava/util/concurrent/CopyOnWriteArrayList;'}:
            if strings[name] in {'removeFirst', 'removeLast'}:
                yield types[owner] + '->' + strings[name]


def check_apk(path):
    failures = []
    with zipfile.ZipFile(path) as apk:
        dex_files = [name for name in apk.namelist() if name.endswith('.dex')]
        if not dex_files:
            raise ValueError('APK contains no DEX files')
        for name in dex_files:
            failures.extend(name + ': ' + method for method in unsupported_methods(apk.read(name)))
    if failures:
        raise ValueError('APIs unavailable on Android 14 and older:\n' + '\n'.join(failures))
    print(f'PASS: no Android-15-only List removal APIs across {len(dex_files)} DEX files')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk')
    args = parser.parse_args()
    try:
        check_apk(args.apk)
    except (ValueError, zipfile.BadZipFile) as error:
        parser.exit(1, f'FAIL: {error}\n')
