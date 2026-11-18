#!/usr/bin/env python3
"""Check Gson's device-package DTO constructors in the actual minified APK."""
import argparse
import struct
import zipfile

REQUIRED = {'Lcom/xh/hotme/me/setmeal/bean/' + name + ';' for name in (
    'UserPackageCountBean', 'UserPackageCountBean$UserPackageData',
    'SetMealListPackageBean', 'SetMealListPackageBean$DataBean',
    'PackageListBean', 'PackageListBean$DataBean',
    'SetMealHistoryBean', 'SetMealHistoryBean$DataBean', 'SetMealHistoryBean$DataBean$RowsBean',
    'SetMealHistoryPackageBean', 'SetMealHistoryPackageBean$DataBean',
    'SetMealHistoryPackageBean$DataBean$PackageSnapshotBean',
    'UsageRecordBean', 'UsageRecordBean$DataBean', 'UsageRecordBean$DataBean$RowsBean',
)}


def uleb(data, offset):
    value, shift = 0, 0
    while True:
        byte = data[offset]
        offset += 1
        value |= (byte & 127) << shift
        if byte < 128:
            return value, offset
        shift += 7


def models(data):
    if not data.startswith(b'dex\n'):
        raise ValueError('Expected standard DEX')
    strings = []
    count, offset = struct.unpack_from('<II', data, 56)
    for index in range(count):
        start = struct.unpack_from('<I', data, offset + index * 4)[0]
        _, start = uleb(data, start)
        strings.append(data[start:data.index(b'\0', start)].decode('utf-8', errors='replace'))
    count, offset = struct.unpack_from('<II', data, 64)
    types = [strings[struct.unpack_from('<I', data, offset + index * 4)[0]] for index in range(count)]
    proto_offset = struct.unpack_from('<I', data, 76)[0]
    method_offset = struct.unpack_from('<I', data, 92)[0]
    count, offset = struct.unpack_from('<II', data, 96)
    for index in range(count):
        cls, flags, _, _, _, _, class_data, _ = struct.unpack_from('<8I', data, offset + index * 32)
        name = types[cls]
        if name not in REQUIRED:
            continue
        constructor = False
        if class_data:
            cursor = class_data
            sizes = []
            for _ in range(4):
                size, cursor = uleb(data, cursor)
                sizes.append(size)
            for _ in range(sizes[0] + sizes[1]):
                _, cursor = uleb(data, cursor)
                _, cursor = uleb(data, cursor)
            method = 0
            for _ in range(sizes[2]):
                delta, cursor = uleb(data, cursor)
                access, cursor = uleb(data, cursor)
                _, cursor = uleb(data, cursor)
                method += delta
                _, proto, string = struct.unpack_from('<HHI', data, method_offset + method * 8)
                parameters = struct.unpack_from('<I', data, proto_offset + proto * 12 + 8)[0]
                if strings[string] == '<init>' and not parameters and access & 1:
                    constructor = True
        yield name, flags, constructor


def check_apk(path):
    definitions = {}
    with zipfile.ZipFile(path) as apk:
        for name in apk.namelist():
            if name.endswith('.dex'):
                definitions.update((name, (flags, ctor)) for name, flags, ctor in models(apk.read(name)))
    errors = [f'Missing DTO: {name}' for name in sorted(REQUIRED - definitions.keys())]
    for name, (flags, ctor) in definitions.items():
        if flags & 0x600:
            errors.append(f'Non-instantiable DTO: {name}')
        if not ctor:
            errors.append(f'Missing public no-args constructor: {name}')
    if errors:
        raise ValueError('\n'.join(errors))
    print(f'PASS: {len(definitions)} release package DTOs retain concrete classes and public constructors')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk')
    args = parser.parse_args()
    try:
        check_apk(args.apk)
    except (ValueError, zipfile.BadZipFile) as error:
        parser.exit(1, f'FAIL: {error}\n')
