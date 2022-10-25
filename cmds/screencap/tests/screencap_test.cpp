#include "gmock/gmock.h"
#include "gtest/gtest.h"
#include "../screencap.h"

TEST(Align, InvalideDisplayArgument) {
  char* argv[]  = {"screencap", "-d", "0", "misc/x.png", nullptr};
  int status = screencap(4, argv);
  ASSERT_EQ(0, 1);
}