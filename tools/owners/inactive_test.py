#!/usr/bin/python

import inactive
import unittest

TEST_INPUT = """foo@google.com
# Standalone comment
bar@google.com # Trailing comment
per-file Red.java = foo@google.com
per-file Blue.java = foo@google.com, bar@google.com,baz@google.com
baz@google.com
foo@example.net
foobar@google.com"""

class InactiveTest(unittest.TestCase):
    def test_unchanged(self):
        self.assertEqual(TEST_INPUT, inactive.filter_inactive(TEST_INPUT, ["foo","bar","baz","foobar"]))

    def test_foo(self):
        self.assertEqual("""# Standalone comment
bar@google.com # Trailing comment
per-file Blue.java = bar@google.com,baz@google.com
baz@google.com
foo@example.net
foobar@google.com""", inactive.filter_inactive(TEST_INPUT, ["bar","baz","foobar"]))

    def test_bar(self):
        self.assertEqual("""foo@google.com
# Standalone comment
per-file Red.java = foo@google.com
per-file Blue.java = foo@google.com, baz@google.com
baz@google.com
foo@example.net
foobar@google.com""", inactive.filter_inactive(TEST_INPUT, ["foo","baz","foobar"]))

    def test_baz(self):
        self.assertEqual("""foo@google.com
# Standalone comment
bar@google.com # Trailing comment
per-file Red.java = foo@google.com
per-file Blue.java = foo@google.com, bar@google.com
foo@example.net
foobar@google.com""", inactive.filter_inactive(TEST_INPUT, ["foo","bar","foobar"]))

    def test_foo_bar_baz(self):
        self.assertEqual("""# Standalone comment
foo@example.net
foobar@google.com""", inactive.filter_inactive(TEST_INPUT, ["foobar"]))

    def test_foo_bar_baz_nobody(self):
        self.assertEqual("""# Standalone comment
foo@example.net""", inactive.filter_inactive(TEST_INPUT, []))

if __name__ == "__main__":
    unittest.main(verbosity=2)
