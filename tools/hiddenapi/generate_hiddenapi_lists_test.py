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
"""Unit tests for Hidden API list generation."""
import unittest
from generate_hiddenapi_lists import *

class TestHiddenapiListGeneration(unittest.TestCase):
    def test_check_entries_set(self):
        check_entries_set([ 'A', 'B' ], { 'A' : 1, 'B' : 2, 'C' : 3 }, 'source')

    def test_check_entries_set__fail(self):
        with self.assertRaises(AssertionError):
            check_entries_set([ 'A', 'B', 'D' ], { 'A' : 1, 'B' : 2, 'C' : 3 }, 'source')

    def test_check_tags_set(self):
        check_tags_set([ TAG_WHITELIST, TAG_GREYLIST ], 'source')

    def test_check_tags_set__fail(self):
        with self.assertRaises(AssertionError):
            check_tags_set([ TAG_WHITELIST, TAG_GREYLIST, 'foo' ], 'source')

    def test_filter_dict_keys(self):
        self.assertEqual(
            filter_dict_keys((lambda key, val: key + val < 10), { 1 : 2, 3 : 4, 5 : 6 }),
            [ 1, 3 ])

    def test_merge_two_disjoint_dicts(self):
        self.assertEquals(merge_two_disjoint_dicts({}, {}), {})
        self.assertEquals(merge_two_disjoint_dicts({ 'A' : 1 }, {}), { 'A' : 1 })
        self.assertEquals(merge_two_disjoint_dicts({}, { 'B' : 2 }), { 'B' : 2 })
        self.assertEquals(
            merge_two_disjoint_dicts({ 'A' : 1 }, { 'B' : 2 }),
            { 'A' : 1 , 'B' : 2 })
        self.assertEquals(
            merge_two_disjoint_dicts({ 'A' : 1 }, { 2 : 'B' }),
            { 'A' : 1 , 2 : 'B' })

    def test_merge_two_disjoint_dicts__fail(self):
        with self.assertRaises(AssertionError):
            merge_two_disjoint_dicts({ 'A' : 1 }, { 'A' : 2 })

    def test_get_valid_subset_of_unassigned_keys(self):
        d = { 'A' : set(), 'B' : set(TAG_WHITELIST), 'C' : set(TAG_GREYLIST) }
        e = set([ 'A', 'B', 'D' ])
        self.assertEqual(get_valid_subset_of_unassigned_keys(e, d), set([ 'A' ]))

    def test_parse_csv(self):
        d = { 'A' : set(), 'B' : set([TAG_WHITELIST]) }
        # Test empty CSV entry.
        parse_csv([ "A" ], d)
        self.assertEqual(d, { 'A' : set(), 'B' : set([TAG_WHITELIST]) })
        # Test assigning an already assigned tag.
        parse_csv([ "B," + TAG_WHITELIST ], d)
        self.assertEqual(d, { 'A' : set(), 'B' : set([TAG_WHITELIST]) })
        # Test new additions.
        parse_csv([ "A," + TAG_GREYLIST, "B," + TAG_BLACKLIST + "," + TAG_BLACKLIST_MAX_O ], d)
        self.assertEqual(d,
            { 'A' : set([ TAG_GREYLIST ]),
              'B' : set([ TAG_WHITELIST, TAG_BLACKLIST, TAG_BLACKLIST_MAX_O ]) })

    def test_parse_csv__fail_entry(self):
        d = { 'A' : set(), 'B' : set() }
        with self.assertRaises(AssertionError):
            parse_csv([ 'C' ], d)

    def test_parse_csv__fail_tag(self):
        d = { 'A' : set(), 'B' : set() }
        with self.assertRaises(AssertionError):
            parse_csv([ 'A,foo' ], d)

    def test_assign_tag(self):
        d = { 'A' : set(), 'B' : set([TAG_WHITELIST]) }
        # Test assigning an already assigned tag.
        assign_tag(TAG_WHITELIST, [ 'B' ], d)
        self.assertEqual(d, { 'A' : set(), 'B' : set([TAG_WHITELIST]) })
        # Test new additions.
        assign_tag(TAG_GREYLIST, [ 'A', 'B' ], d)
        self.assertEqual(d,
            { 'A' : set([ TAG_GREYLIST ]),
              'B' : set([ TAG_WHITELIST, TAG_GREYLIST ]) })

    def test_assign_tag__fail_entry(self):
        d = { 'A' : set(), 'B' : set() }
        with self.assertRaises(AssertionError):
            assign_tag(TAG_WHITELIST, [ 'C' ], d)

    def test_assign_tag__fail_tag(self):
        d = { 'A' : set(), 'B' : set() }
        with self.assertRaises(AssertionError):
            assign_tag('foo', [ 'A' ], d)

    def test_generate_csv(self):
        self.assertEqual(
            generate_csv({ 'A' : set(),
                           'B' : set([ TAG_WHITELIST ]),
                           'C' : set([ TAG_GREYLIST, TAG_BLACKLIST ]) }),
            [ 'A\n',
              'B,' + TAG_WHITELIST + '\n',
              'C,' + TAG_BLACKLIST + ',' + TAG_GREYLIST + '\n' ])

if __name__ == '__main__':
    unittest.main()
