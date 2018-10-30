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
FLAG_WHITELIST = "whitelist"
FLAG_GREYLIST = "greylist"
FLAG_BLACKLIST = "blacklist"
FLAG_GREYLIST_MAX_O = "greylist-max-o"

# List of all known flags.
FLAGS = [
    FLAG_WHITELIST,
    FLAG_GREYLIST,
    FLAG_BLACKLIST,
    FLAG_GREYLIST_MAX_O,
]
FLAGS_SET = set(FLAGS)

# Suffix used in command line args to express that only known and
# otherwise unassigned entries should be assign the given flag.
# For example, the P dark greylist is checked in as it was in P,
# but signatures have changes since then. The flag instructs this
# script to skip any entries which do not exist any more.
FLAG_IGNORE_CONFLICTS_SUFFIX = "-ignore-conflicts"

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

# Predicates to be used with filter_apis.
IS_UNASSIGNED = lambda api, flags: not flags
IS_SERIALIZATION = lambda api, flags: SERIALIZATION_REGEX.match(api)

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

    for flag in FLAGS:
        ignore_conflicts_flag = flag + FLAG_IGNORE_CONFLICTS_SUFFIX
        parser.add_argument('--' + flag, dest=flag, nargs='*', default=[], metavar='TXT_FILE',
            help='lists of entries with flag "' + flag + '"')
        parser.add_argument('--' + ignore_conflicts_flag, dest=ignore_conflicts_flag, nargs='*',
            default=[], metavar='TXT_FILE',
            help='lists of entries with flag "' + flag +
                 '". skip entry if missing or flag conflict.')

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
        f.writelines(map(lambda line: line + '\n', lines))

class FlagsDict:
    def __init__(self, public_api, private_api):
        # Bootstrap the entries dictionary.
        # (1) Load all public entries and assign them to the whitelist.
        public = { x: set([ FLAG_WHITELIST ]) for x in public_api }
        # (2) Load all private entries and leave them with no flags.
        private = { x: set() for x in private_api }
        # (3) Merge into a single dictionary.
        self.data = self._merge_two_disjoint_dicts(public, private)
        self.data_keyset = set(self.data.keys())

    def _merge_two_disjoint_dicts(self, x, y):
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

    def _check_entries_set(self, keys_subset, source):
        """Checks whether a set of keys is a subset of keys in a dictionary.

        Args:
            keys_subset (set/list): Presumed subset of keys of `entries_dict`.
            source (string): Source that `keys_subset` comes from. Used for error message.

        Throws:
            AssertionError if check fails.

        Returns:
            None.
        """
        assert isinstance(keys_subset, set)
        assert keys_subset.issubset(self.data_keyset), (
            "Error processing: {}\n"
            "The following entries were not found:\n"
            "{}"
            "Please visit go/hiddenapi for more information.").format(
                source, "".join(map(lambda x: "  " + str(x), keys_subset - self.data_keyset)))

    def _check_flags_set(self, flags_subset, source):
        """Checks whether a set of flags is a subset of all the known flags.

        Args:
            flags_subset (set/list): Presumed subset of FLAGS.
            source (string): Source that `flags_subset` comes from. Used for error message.

        Throws:
            AssertionError if check fails.

        Returns:
            None.
        """
        assert isinstance(flags_subset, set)
        assert flags_subset.issubset(FLAGS_SET), (
            "Error processing: {}\n"
            "The following flags were not recognized: \n"
            "{}\n"
            "Please visit go/hiddenapi for more information.").format(
                source, "\n".join(flags_subset - FLAGS_SET))

    def filter_apis(self, filter_fn):
        """Returns keys of dict which match a given predicate.

        This is a helper function which allows to filter on both keys and values.
        The built-in filter() invokes the lambda only with dict's keys.

        Args:
            filter_fn : Function which takes two arguments (api/flags) and returns a boolean.

        Returns:
            A set of dict's keys which match the predicate.
        """
        return set(filter(lambda x: filter_fn(x, self.data[x]), self.data))

    def get_valid_subset_of_unassigned_apis(self, entries_subset):
        """Sanitizes a key set input to only include keys which exist in the dictionary
        and have not been assigned any flags.

        Args:
            entries_subset (set/list): Key set to be sanitized.

        Returns:
            Sanitized key set.
        """
        return set(entries_subset).intersection(self.filter_apis(IS_UNASSIGNED))

    def generate_csv(self):
        """Constructs CSV entries from a dictionary.

        Returns:
            List of lines comprising a CSV file.
        """
        return sorted(map(lambda api: ",".join([api] + sorted(self.data[api])), self.data))

    def parse_and_merge_csv(self, csv_lines, source = "<unknown>"):
        """Parses CSV entries and merges them into a given dictionary.

        The expected CSV format is:
            <api signature>,<flag1>,<flag2>,...,<flagN>

        Args:
            csv_lines (list of strings): Lines read from a CSV file.
            source (string): Origin of `csv_lines`. Will be printed in error messages.

        Throws:
            AssertionError if parsed API signatures cannot be found in `entries_dict`,
            or if parsed flags are unknown.

        Returns:
            None. Parsed entries are inserted into `entries_dict`.
        """
        # Split CSV lines into arrays of values.
        csv_values = [ line.split(',') for line in csv_lines ]

        # Check that all entries exist in the dict.
        csv_keys = set([ csv[0] for csv in csv_values ])
        self._check_entries_set(csv_keys, source)

        # Check that all flags are known.
        csv_flags = set(reduce(lambda x, y: set(x).union(y), [ csv[1:] for csv in csv_values ], []))
        self._check_flags_set(csv_flags, source)

        # Iterate over all CSV lines, find entry in dict and append flags to it.
        for csv in csv_values:
            self.data[csv[0]].update(csv[1:])

    def assign_flag(self, flag, entries, source="<unknown>"):
        """Assigns a flag to given subset of entries.

        Args:
            flag (string): One of FLAGS.
            entries_subset (list/set): Subset of dict's keys to recieve the flag.
            source (string): Origin of `entries_subset`. Will be printed in error messages.

        Throws:
            AssertionError if parsed API signatures cannot be found in `entries_dict`,
            or if flag is unknown.
        """
        # Check that all entries exist in the dict.
        self._check_entries_set(set(entries), source)

        # Check that the flag is known.
        self._check_flags_set(set([ flag ]), source)

        # Iterate over the entries subset, find each entry in dict and assign the flag to it.
        for api in entries:
            self.data[api].add(flag)

def main(argv):
    # Parse arguments.
    args = vars(get_args())

    flags = FlagsDict(read_lines(args["public"]), read_lines(args["private"]))

    # Combine inputs which do not require any particular order.
    # (1) Assign serialization API to whitelist.
    flags.assign_flag(FLAG_WHITELIST, flags.filter_apis(IS_SERIALIZATION))
    # (2) Merge input CSV files into the dictionary.
    for filename in args["csv"]:
        flags.parse_and_merge_csv(read_lines(filename), filename)
    # (3) Merge text files with a known flag into the dictionary.
    for flag in FLAGS:
        for filename in args[flag]:
            flags.assign_flag(flag, read_lines(filename), filename)

    # Merge text files where conflicts should be ignored.
    # This will only assign the given flag if:
    # (a) the entry exists, and
    # (b) it has not been assigned any other flag.
    # Because of (b), this must run after all strict assignments have been performed.
    for flag in FLAGS:
        for filename in args[flag + FLAG_IGNORE_CONFLICTS_SUFFIX]:
            valid_entries = flags.get_valid_subset_of_unassigned_apis(read_lines(filename))
            flags.assign_flag(flag, valid_entries, filename)

    # Assign all remaining entries to the blacklist.
    flags.assign_flag(FLAG_BLACKLIST, flags.filter_apis(IS_UNASSIGNED))

    # Write output.
    write_lines(args["output"], flags.generate_csv())

if __name__ == "__main__":
    main(sys.argv)
