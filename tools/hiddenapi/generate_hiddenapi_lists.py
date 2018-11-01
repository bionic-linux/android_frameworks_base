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
Generate API lists for non-SDK API enforcement.
"""
import argparse
import os
import sys
import re

# Names of flags recognized by the `hiddenapi` tool.
TAG_WHITELIST = "whitelist"
TAG_GREYLIST = "greylist"
TAG_BLACKLIST = "blacklist"
TAG_GREYLIST_MAX_O = "greylist-max-o"
TAG_GREYLIST_MAX_P = "greylist-max-p"

# List of all known tags.
TAGS = [
    TAG_WHITELIST,
    TAG_GREYLIST,
    TAG_BLACKLIST,
    TAG_GREYLIST_MAX_O,
    TAG_GREYLIST_MAX_P,
]

# Suffix used in command line args to express that only known and
# otherwise unassigned entries should be assign the given flag.
# For example, the P dark greylist is checked in as it was in P,
# but signatures have changes since then. The flag instructs this
# script to skip any entries which do not exist any more.
TAG_IGNORE_CONFLICTS_SUFFIX = "-ignore-conflicts"

# Regex patterns of fields/methods used in serialization. These are
# considered public API despite being hidden.
SERIALIZATION_PATTERNS = [
    r'readObject\(Ljava/io/ObjectInputStream;\)V',
    r'readObjectNoData\(\)V',
    r'readResolve\(\)Ljava/lang/Object;',
    r'serialVersionUID:J',
    r'serialPersistentFields:\[Ljava/io/ObjectStreamField;',
    r'writeObject\(Ljava/io/ObjectOutputStream;\)V',
    r'writeReplace\(\)Ljava/lang/Object;',
]

# Single regex used to match serialization API. It combines all the
# SERIALIZATION_PATTERNS into a single regular expression.
SERIALIZATION_REGEX = re.compile(r'.*->(' + '|'.join(SERIALIZATION_PATTERNS) + r')$')

# Predicates to be used with filter_dict_keys.
IS_UNASSIGNED = lambda api, tags: not tags
IS_SERIALIZATION = lambda api, tags: SERIALIZATION_REGEX.match(api)

def get_args():
    """Parses command line arguments.

    Returns:
        Namespace: dictionary of parsed arguments
    """
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', required=True)
    parser.add_argument('--public', required=True, help='list of all public entries')
    parser.add_argument('--private', required=True, help='list of all private entries')
    parser.add_argument('--csv', nargs='*', default=[], metavar='CSV_FILE',
        help='CSV files to be merged into output')

    for tag in TAGS:
        ignore_conflicts_tag = tag + TAG_IGNORE_CONFLICTS_SUFFIX
        parser.add_argument('--' + tag, dest=tag, nargs='*', default=[], metavar='TXT_FILE',
            help='lists of entries with tag "' + tag + '"')
        parser.add_argument('--' + ignore_conflicts_tag, dest=ignore_conflicts_tag, nargs='*',
            default=[], metavar='TXT_FILE',
            help='lists of entries with tag "' + tag + '". skip entry if missing or tag conflict.')

    return parser.parse_args()

def read_lines(filename):
    """Reads entire file and return it as a list of lines.

    Lines which begin with a hash are ignored.

    Args:
        filename (string): Path to the file to read from.

    Returns:
        list: Lines of the loaded file as a list of strings.
    """
    with open(filename, 'r') as f:
        return map(lambda line: line.strip(),
                   filter(lambda line: not line.startswith('#'),f.readlines()))

def write_lines(filename, lines):
    """Writes list of lines into a file, overwriting the file it it exists.

    Args:
        filename (string): Path to the file to be writting into.
        lines (list): List of strings to write into the file.
    """
    with open(filename, 'w') as f:
        f.writelines(lines)

def check_entries_set(keys_subset, entries_dict, source):
    """Checks whether a set of keys is a subset of keys in a dictionary.

    Args:
        keys_subset (set/list): Presumed subset of keys of `entries_dict`.
        entries_dict (dict): Dictionary that `keys_subset` should be checked against.
        source (string): Source that `keys_subset` comes from. Used for error message.

    Throws:
        AssertionError if check fails.

    Returns:
        None.
    """
    keys_all = set(entries_dict.keys())
    assert keys_all.issuperset(keys_subset), (
        "Error processing: {}\n"
        "The following entries were not found:\n"
        "{}"
        "Please visit go/hiddenapi for more information.").format(
            source, "".join(map(lambda x: "  " + str(x), set(keys_subset).difference(keys_all))))

def check_tags_set(tags_subset, source):
    """Checks whether a set of tags is a subset of all the known tags.

    Args:
        tags_subset (set/list): Presumed subset of TAGS.
        source (string): Source that `tags_subset` comes from. Used for error message.

    Throws:
        AssertionError if check fails.

    Returns:
        None.
    """
    assert set(TAGS).issuperset(tags_subset), (
        "Error processing: {}\n"
        "The following tags were not recognized: \n"
        "{}\n"
        "Please visit go/hiddenapi for more information.").format(
            source, "\n".join(set(tags_subset).difference(TAGS)))

def filter_dict_keys(filter_fn, entries_dict):
    """Returns keys of dict which match a given predicate.

    This is a helper function which allows to filter on both keys and values.
    The built-in filter() invokes the lambda only with dict's keys.

    Args:
        filter_fn (function): Function which takes two arguments (key/value) and returns a boolean.
        entries_dict (dict): Dictionary to be filtered.

    Returns:
        A list of dict's keys which match the predicate.
    """
    return filter(lambda x: filter_fn(x, entries_dict[x]), entries_dict)

def merge_two_disjoint_dicts(x, y):
    """Merges key/value pairs from two dictionaries assuming no collisions.

    Args:
        x,y (dict): Two dictionaries.

    Throws:
        AssertionError if dicts' key sets are not disjoint.

    Returns:
        A new dict combining key/value pairs of dicts `x` and `y`.
    """
    assert set(x.keys()).isdisjoint(set(y.keys()))
    z = x.copy()
    z.update(y)
    return z

def get_valid_subset_of_unassigned_keys(entries_subset, entries_dict):
    """Sanitizes a key set input to only include keys which exist in the dictionary and have not
    been assigned any tags.

    Args:
        entries_subset (set/list): Key set to be sanitized.
        entries_dict (dict): Dictionary with all known api signatures (keys) and their tags.

    Returns:
        Sanitized key set.
    """
    unassigned_keys = filter_dict_keys(IS_UNASSIGNED, entries_dict)
    return set(entries_subset).intersection(unassigned_keys)

def generate_csv(entries_dict):
    """Constructs CSV entries from a dictionary.

    Args:
        entries_dict (dict): Dictionary with all known api signatures (keys) and their tags.

    Returns:
        List of lines comprising a CSV file.
    """
    return sorted(map(lambda api: ",".join([api] + sorted(entries_dict[api])) + "\n", entries_dict))

def parse_csv(csv_lines, entries_dict, source = "<unknown>"):
    """Parses CSV entries and merges them into a given dictionary.

    The expected CSV format is:
        <api signature>,<tag1>,<tag2>,...,<tagN>

    Args:
        csv_lines (list of strings): Lines read from a CSV file.
        entries_dict (dict): Dictionary that parsed entries will be inserted into.
        source (string): Origin of `csv_lines`. Will be printed in error messages.

    Throws:
        AssertionError if parsed API signatures cannot be found in `entries_dict`,
        or if parsed tags are unknown.

    Returns:
        None. Parsed entries are inserted into `entries_dict`.
    """
    # Split CSV lines into arrays of values.
    csv_values = map(lambda line: line.split(','), csv_lines)

    # Check that all entries exist in the dict.
    csv_keys = map(lambda csv: csv[0], csv_values)
    check_entries_set(csv_keys, entries_dict, source)

    # Check that all tags are known.
    csv_tags = reduce(lambda x, y: set(x).union(y), map(lambda csv: csv[1:], csv_values), [])
    check_tags_set(csv_tags, source)

    # Iterate over all CSV lines, find entry in dict and append tags to it.
    map(lambda csv : entries_dict[csv[0]].update(csv[1:]), csv_values)

def assign_tag(tag, entries_subset, entries_dict, source="<unknown>"):
    """Assigns a tag to given subset of entries.

    Args:
        tag (string): One of TAGS.
        entries_subset (list/set): Subset of dict's keys to recieve the tag.
        entries_dict (dict): Dictionary with all known api signatures (keys) and their tags.
        source (string): Origin of `entries_subset`. Will be printed in error messages.

    Throws:
        AssertionError if parsed API signatures cannot be found in `entries_dict`,
        or if tag is unknown.

    Returns:
        None. Values in `entries_dict` are modified.
    """
    # Check that all entries exist in the dict.
    check_entries_set(entries_subset, entries_dict, source)

    # Check that the tag is known.
    check_tags_set([ tag ], source)

    # Iterate over the entries subset, find each entry in dict and assign the tag to it.
    map(lambda api: entries_dict[api].add(tag), entries_subset)

def main(argv):
    # Parse arguments.
    args = vars(get_args())

    # Bootstrap the entries dictionary.
    # (1) Load all public entries and assign them to the whitelist.
    public = { x: set([ TAG_WHITELIST ]) for x in read_lines(args["public"]) }
    # (2) Load all private entries and leave them with no tags.
    private = { x: set() for x in read_lines(args["private"]) }
    # (3) Merge into a single dictionary.
    entries_dict = merge_two_disjoint_dicts(public, private)

    # Combine inputs which do not require any particular order.
    # (1) Assign serialization API to whitelist.
    assign_tag(TAG_WHITELIST, filter_dict_keys(IS_SERIALIZATION, entries_dict), entries_dict)
    # (2) Merge input CSV files into the dictionary.
    for filename in args["csv"]:
        parse_csv(read_lines(filename), entries_dict, filename)
    # (3) Merge text files with a known tag into the dictionary.
    for tag in TAGS:
        for filename in args[tag]:
            assign_tag(tag, read_lines(filename), entries_dict, filename)

    # Merge text files where conflicts should be ignored.
    # This will only assign the given tag if:
    # (a) the entry exists, and
    # (b) it has not been assigned any other tag.
    # Because of (b), this must run after all strict assignments have been performed.
    for tag in TAGS:
        for filename in args[tag + TAG_IGNORE_CONFLICTS_SUFFIX]:
            valid_entries = get_valid_subset_of_unassigned_keys(read_lines(filename), entries_dict)
            assign_tag(tag, valid_entries, entries_dict, filename)

    # Assign all remaining entries to the blacklist.
    assign_tag(TAG_BLACKLIST, filter_dict_keys(IS_UNASSIGNED, entries_dict), entries_dict)

    # Write output.
    write_lines(args["output"], generate_csv(entries_dict))

if __name__ == "__main__":
    main(sys.argv)
