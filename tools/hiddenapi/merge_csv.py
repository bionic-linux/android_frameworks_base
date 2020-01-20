#!/usr/bin/env python
#
# Copyright (C) 2018 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""
Merge multiple CSV files, possibly with different columns.
"""

import argparse
import csv
import os

args_parser = argparse.ArgumentParser(description='Merge given CSV files into a single one.')
args_parser.add_argument('--header', help='Comma separated field names; '
                                          'if missing determines the header from input files.')
args_parser.add_argument('--csv_name', help='Find CSV file in an input directory.')
args_parser.add_argument('--output', help='Output file for merged CSV.', default='-',
                         type=argparse.FileType('w'))
args_parser.add_argument('files', nargs=argparse.REMAINDER)
args = args_parser.parse_args()

csv_readers = []
for file in args.files:
    if os.path.isdir(file):
        file = os.path.join(file, args.csv_name)
    # Check the file actually exists
    if os.path.isfile(file):
        csv_readers.append(csv.DictReader(open(file, 'r'), delimiter=',', quotechar='|'))

# Build union of all columns from source files:
headers = set()
for reader in csv_readers:
    headers = headers.union(reader.fieldnames)
if args.header:
    fieldnames = args.header.split(',')
    assert headers == set(fieldnames), "Header mismatch."
else:
    fieldnames = sorted(headers)

# Concatenate all files to output:
writer = csv.DictWriter(args.output, delimiter=',', quotechar='|', quoting=csv.QUOTE_MINIMAL,
                        dialect='unix', fieldnames=fieldnames)
writer.writeheader()
for reader in csv_readers:
    for row in reader:
        writer.writerow(row)
