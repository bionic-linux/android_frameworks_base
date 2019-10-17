commit 761092fbbde60061f6e9d1f1c7d73db3a89856dc
Author: Brandon Chen <brandon.chen@imgtec.com>
Date:   Thu Oct 17 14:58:38 2019 +0800

    Ensure wallpaper size is smaller than max texture size
    
    If users manually push large size wallpaper to the directory, it
    could exceed the capability of gpu queried from GL_MAX_TEXTURE_SIZE.
    This causes device fail to boot and stay in boot animation screen.
    
    Change-Id: I9f0d8a67f80bda368ae484679155c1e640d4c813
    Signed-off-by: Brandon Chen <brandon.chen@imgtec.com>

 .../server/wallpaper/WallpaperManagerService.java  | 110 ++++++++++++++++++++-
 1 file changed, 108 insertions(+), 2 deletions(-)

commit 0d5792e9822fb26a2e3c9fee67f46a02f2a84941
Author: wilsonshih <wilsonshih@google.com>
Date:   Thu Jul 4 11:23:54 2019 +0800

    Clear the calling identity in isUsableDisplay.
    
    The INTERNAL_SYSTEM_WINDOW permission is checked when
    WindowManagerService#shouldShowSystemDecors is called, we should leave
    this permission check because it is public method.
    Clear the calling identity in WallpaperManagerService#isUsableDisplay
    as this method should be used only internally.
    
    Fix: 136447676
    Test: atest VrDisplayTests MultiDisplaySystemDecorationTests
    Change-Id: I97a93cdd0253933527010f97049694b59d5a4e2a

 .../android/server/wallpaper/WallpaperManagerService.java    | 12 ++++++++++--
 1 file changed, 10 insertions(+), 2 deletions(-)

commit c45a9903302e91dc027418c501d4aa52f4a4e9f6
Author: Amith Yamasani <yamasani@google.com>
Date:   Fri Apr 5 16:29:30 2019 -0700

    Allow wallpapers and IMEs to get location if needed
    
    Wallpapers and Input Methods are bound by the system
    and are only brought up to BFGS, which is insufficient
    for getting foreground location. Add the required
    flag to the bindService call to allow the bound process
    to reach FGSL when visible.
    
    Same for VoiceInteractionServices
    
    Bug: 117494189
    Test: Manually enable a wallpaper that needs location and
         verify it gets location.
          atest CtsAppTestCases:ActivityManagerProcessStateTest
    
    Change-Id: I6767e1f480e5b3d6e33864dabd9cb167df4846f5

 .../java/com/android/server/wallpaper/WallpaperManagerService.java     | 3 ++-
 1 file changed, 2 insertions(+), 1 deletion(-)

commit e3d2feaa1f5acb011403925c81d58b589edbfa7c
Author: Keun young Park <keunyoung@google.com>
Date:   Fri Mar 29 17:42:32 2019 -0700

    Fix wallpaper NPE caused by user switching before IWallpaperService connection
    
    - car side is doing quick user switching during boot-up and that leads into
      wallpaper user switching before the service connection.
      user 0 start, user 0 unlock, user switch to user 10, boot complete, user 10 unlock
    
    Bug: 129569939
    Test: Try user switching flow in car env and confirm no crash
    Change-Id: I764f68377ddf8b581f8386a236aa5c24a96368dd

 .../java/com/android/server/wallpaper/WallpaperManagerService.java   | 5 ++++-
 1 file changed, 4 insertions(+), 1 deletion(-)

commit 96dbbb9d2e5c543bcf1e5b3a45229e479dd7f8f2
Author: Ahan Wu <ahanwu@google.com>
Date:   Fri Mar 29 15:26:22 2019 +0800

    Notify callbacks to update in case wallpaper is waiting for unlock.
    
    If the wallpaper isn't direct-boot aware, the fallback wallpaper will be
    bound first until we unlock from lock screen and then the desired one
    will be bound.
    
    However, the attributes will not be updated after the desired wallpaper
    is bound, this might cause some bugs so we also notify callbacks in this
    case.
    
    Bug: 127810511
    Test: Install 'Friendly Bugs Free L.Wallpaper' from play store.
    Test: Use 'Friendly Bugs Free' as wallpaper.
    Test: Press power key to enter AOD, observe the behavior.
    Test: Restart device, unlock, press power key, observe behavior.
    Change-Id: I7b8f44a0bcf452dcd757786dd12efa9eef22bae1

 .../core/java/com/android/server/wallpaper/WallpaperManagerService.java  | 1 +
 1 file changed, 1 insertion(+)

commit b17c4244af14dad1bf26a3004b72e7e176afb176
Merge: dc81949 b72ff9c
Author: TreeHugger Robot <treehugger-gerrit@google.com>
Date:   Thu Mar 28 01:13:08 2019 +0000

    Merge "Fix WallpaperEngine object leaked during stress test."

commit b72ff9c60f1b7067a09d41407e77a5eeb2fe6a8c
Author: wilsonshih <wilsonshih@google.com>
Date:   Thu Mar 21 17:27:02 2019 +0800

    Fix WallpaperEngine object leaked during stress test.
    
    The wallpaper engine object may not have released due to someone continuously
    change wallpaper with the same component, such as image wallpaper.
    In this case, server side may receive attached engine to a connection, however,
    it could already detached.
    
    Example sequence:
    bind wallpaper (A) connection (C1)
      detachWallpaperLocked(lastwallpaper)
      lastwallpaper = A + C1
    bind wallpaper (A) connection (C2)
      detachWallpaperLocked(lastwallpaper = A + C1)
      last = A + C2
      attachEngine to C1
    Then the connection C1 will never be released because it was be detached.
    
    We can fix this by notify that WallpaperService is detached.
    When it was detached, it shall destroy its engine if there is, and doesn't attach
    back to WallpaperManagerService anymore.
    
    Bug: 128974839
    Test: run atest WallpaperManagerTest, then dump
    adb shell dumpsys activity service com.android.systemui/com.android.systemui.ImageWallpaper
    to make sure there is only one WallpaperEngine object.
    
    Change-Id: Ifea201fe8860af11376717d344fee77182b38e54

 .../java/com/android/server/wallpaper/WallpaperManagerService.java   | 5 +++++
 1 file changed, 5 insertions(+)

commit 859856d8d69ebcbe2f32d27a2884a58ed4cae802
Author: Jeff Sharkey <jsharkey@android.com>
Date:   Mon Mar 25 11:44:11 2019 -0600

    Unify media permissions enforcement in framework.
    
    This opens the door to the same consistent logic being shared by
    anyone across the OS who wants to enforce storage permissions.
    
    Bug: 126788266
    Test: atest --test-mapping packages/apps/MediaProvider
    Exempted-From-Owner-Approval: Trivial permissions refactoring
    Change-Id: I3107425f8dafa6ba05918bb67c3c0cb5d3899657

 .../server/wallpaper/WallpaperManagerService.java     | 19 +++----------------
 1 file changed, 3 insertions(+), 16 deletions(-)

commit fa24e4f6676ffe68bc614ea66ed2e58ec80cd397
Author: wilsonshih <wilsonshih@google.com>
Date:   Fri Feb 22 10:29:19 2019 +0800

    Refine extract default image wallpaper colors method.
    
    Refine extract default image colors method, also cache the default
    image wallpaper colors when we first read the image file.
    
    Fix: 123490371
    Test: change to default image wallpaper and can change theme color.
    
    Change-Id: Iacb7280b0bae7f7075684a14522b95d3adbfe2c7

 .../server/wallpaper/WallpaperManagerService.java  | 76 +++++++++++-----------
 1 file changed, 38 insertions(+), 38 deletions(-)

commit 643bf13f602373a34271371088bf4d39373a099a
Author: wilsonshih <wilsonshih@google.com>
Date:   Wed Feb 27 12:49:19 2019 +0800

    Notify the WMS when the display content is ready.
    
    We only add wallpaper to the display that can support the display
    system decoration attribute. However, when receiving a display
    add callback does not mean that the display content has already
    been created, we could not add wallpaper to the display in this
    case.
    To prevent above condition, we can notify the wallpaper that the
    content is ready to add wallpaper instead of listening a display
    was added.
    
    Bug: 125007644
    Bug: 124073468
    Test: atest ActivityManagerMultiDisplayTests#testWallpaperShowOnSecondaryDisplays
    
    Change-Id: I67062ce62cdf185884ab77a5741e5374e5987ef6

 .../server/wallpaper/WallpaperManagerService.java  | 55 +++++++++++++---------
 1 file changed, 34 insertions(+), 21 deletions(-)

commit dd98961be67a7024dcbdedbcba5e903416d633ef
Author: Andrii Kulian <akulian@google.com>
Date:   Thu Feb 21 12:13:28 2019 -0800

    Read system decor support status from WM
    
    There is no way to add FLAG_SHOULD_SHOW_SYSTEM_DECORATIONS to
    hardware or simulated displays, so wallpaper, IME and nav bar were
    not showing up even when desktop mode developer option was enabled.
    
    Bug: 123199549
    Bug: 125007644
    Bug: 124073468
    Bug: 124073384
    Test: Enable force desktop mode, reboot, create simulated display
    Change-Id: Id6f2f3746407467f20c1f26e735b84653e34625d

 .../android/server/wallpaper/WallpaperManagerService.java   | 13 ++++++++++---
 1 file changed, 10 insertions(+), 3 deletions(-)

commit b4924521663f0c1f90100d43d6c8b7b66a3c85b4
Author: Ahan Wu <ahanwu@google.com>
Date:   Wed Feb 20 19:15:04 2019 +0800

    Remove DrawableEngine and related logics
    
    We will always render image wallpaper with GLEngine so the
    DrawableEnigne is no longer necessary.
    
    Remove DrawableEngine and related code including tests makes the code
    more clean.
    
    Bug: 123617158
    Test: Manually set wallpaper by Photos and rotate home
    Test: runtest systemui
    
    Change-Id: I630112e755b74217e44518ec93273c99fb173f24

 .../java/com/android/server/wallpaper/WallpaperManagerService.java   | 5 -----
 1 file changed, 5 deletions(-)

commit 67e7f1054fd313e7298765d63efbdcf28aaffc87
Author: Ahan Wu <ahanwu@google.com>
Date:   Mon Jan 14 20:38:14 2019 +0800

    Render ImageWallpaper with OpenGL ES and apply visual effects. (Fix bug)
    
    We have to render image wallpaper with OpenGL ES to apply some amazing
    visual effects.
    
    Bug: 122803209
    Bug: 124073420
    Bug: 123616712
    Bug: 123615467
    Test: Manually.
    Change-Id: I0123d4ba2acb5a84b709c0468910e006c8e49563

 .../java/com/android/server/wallpaper/WallpaperManagerService.java | 7 ++-----
 1 file changed, 2 insertions(+), 5 deletions(-)

commit a42845b0254e63b018208cdb25b96eed0950bc89
Merge: 6e6583e 98897ce
Author: TreeHugger Robot <treehugger-gerrit@google.com>
Date:   Thu Feb 7 02:02:34 2019 +0000

    Merge "Revert "Render ImageWallpaper with OpenGL ES and apply visual effects.""

commit 98897ce6c5f0971f66e2ab875638daa74be8841f
Author: Lucas Dupin <dupin@google.com>
Date:   Wed Feb 6 20:43:36 2019 +0000

    Revert "Render ImageWallpaper with OpenGL ES and apply visual effects."
    
    This reverts commit 9a8e260af132b7d0846ffc39c435bcb604a53f93.
    Bug: 118658627
    
    Reason for revert:
    I've received a few bug reports indicating that images were getting stretched, animations were gone and sometimes nothing would be rendered.
    
    I'll revert the CL to have something for stable for Beta 1. We can then work on the fixes after the Taiwan team is back from vacation.
    
    Change-Id: Id09ba3d7f372af9153c056a12e676c0227d19939

 .../java/com/android/server/wallpaper/WallpaperManagerService.java | 7 +++++--
 1 file changed, 5 insertions(+), 2 deletions(-)

commit 1f4459783c10af7bc3fd0638a06665ba8149802c
Merge: b3cf932 9a8e260
Author: Selim Cinek <cinek@google.com>
Date:   Wed Jan 30 20:56:12 2019 +0000

    Merge "Render ImageWallpaper with OpenGL ES and apply visual effects."

commit 9a8e260af132b7d0846ffc39c435bcb604a53f93
Author: Ahan Wu <ahanwu@google.com>
Date:   Mon Jan 14 20:38:14 2019 +0800

    Render ImageWallpaper with OpenGL ES and apply visual effects.
    
    We have to render image wallpaper with OpenGL ES to apply some amazing
    visual effects.
    
    Bug: 122803209
    Test: Manually.
    Change-Id: I8d702a59211de58f912f2a18cb54b6e807c6e457

 .../java/com/android/server/wallpaper/WallpaperManagerService.java | 7 ++-----
 1 file changed, 2 insertions(+), 5 deletions(-)

commit 8e519294b0091cb55ac70a698c428e4c15294b7d
Merge: 9b634f1 ff5c8ba
Author: wilsonshih <wilsonshih@google.com>
Date:   Sun Jan 27 19:15:09 2019 -0800

    Merge "Fix theme cannot change due to there is no wallpaper colors." into pi-dev am: dee98726bf
    am: ff5c8bac7c
    
    Change-Id: Id789abf9c9f96236385ccbab987393e34de7300c

commit 507ada5c563b02ab163ee3ef25c6f1b48fc9bc06
Author: wilsonshih <wilsonshih@google.com>
Date:   Sat Jan 19 11:22:09 2019 +0800

    Prevent unnecessary connector been created in fallback connection.
    
    Separate the && condition to prevent create unnecessary connector in
    fallback connection.
    
    Fix: 123104622
    Test: connect on secondary display and change wallpapers, check log and
    use dumpsys to ensure there is no unnecessary ImageWallpaper been
    created.
    
    Change-Id: I6ffd833b9e544db5ccb3f68aba28f6bfc4ac15fd

 .../server/wallpaper/WallpaperManagerService.java       | 17 +++++++++--------
 1 file changed, 9 insertions(+), 8 deletions(-)

commit 31d70a1d5a992b4f67cb74b76fc6c38888a28224
Author: wilsonshih <wilsonshih@google.com>
Date:   Mon Jan 14 12:42:35 2019 +0800

    Fix theme cannot change due to there is no wallpaper colors.
    
    Device theme cannot changed because there is no wallpaper colors when
    first boot with default image wallpaper. We can still calculate it
    if there is no cropFile with the wallpaper component is ImageWallpaper.
    
    Fix: 122660786
    Test: manual test.
    Test: atest WallpaperColorsTest WallpaperManagerTest WallpaperServiceTest
    
    Change-Id: I26cc1d751dadd28185c650eff6e6ef60d5986aaa

 .../server/wallpaper/WallpaperManagerService.java  | 22 ++++++++++++++++++++++
 1 file changed, 22 insertions(+)

commit 1153b780335071540b64a00d07d2a898b9c29af6
Merge: 71839b7 723a80e
Author: Wu Ahan <ahanwu@google.com>
Date:   Fri Jan 11 08:21:59 2019 +0000

    Merge "Enable AOD image wallpaper and apply aod mask view."

commit 3c8657e0963cd2968eb9c090208cfccb44b45769
Merge: e6707b8 3047bb1
Author: Iavor-Valentin Iftime <valiiftime@google.com>
Date:   Wed Jan 9 13:26:03 2019 +0000

    Merge "Crop wallpaper image to preserve screen aspect ratio"

commit 723a80e4fd70ddfb37a881f023c4ced4ad03f775
Author: Ahan Wu <ahanwu@google.com>
Date:   Wed Nov 7 20:39:32 2018 +0800

    Enable AOD image wallpaper and apply aod mask view.
    
    1. Enables image wallpaper in AOD.
    2. Enables a mask with 70% black scrim and vignette effects.
    3. Add feature flag in developer options which is default disabled.
    
    Bug: 111861907
    Bug: 118470430
    Test: Manually test the flow
    Test: runtest systemui
    Test: atest ImageWallpaperTransformerTest
    Test: atest AodMaskViewTest
    
    Change-Id: Iff2642d52264e88012f4759842a59aaf5bc45b38

 .../android/server/wallpaper/WallpaperManagerService.java   | 13 +++++++++++--
 1 file changed, 11 insertions(+), 2 deletions(-)

commit 3047bb1114c3235276d2b6661b6b8123ca716e90
Author: Valentin Iftime <valiiftime@google.com>
Date:   Fri Dec 28 17:02:19 2018 +0100

    Crop wallpaper image to preserve screen aspect ratio
    
    Bug: 121119842
    Test: see bug description
    Change-Id: Ic7161864e738ab50ce068d5b9e89e4fca62c1bfa

 .../android/server/wallpaper/WallpaperManagerService.java | 15 +++++++++++++++
 1 file changed, 15 insertions(+)

commit 36597d4bd7dc074635e9ecb4e4d355cdef0b4ca5
Author: wilsonshih <wilsonshih@google.com>
Date:   Wed Dec 5 18:56:39 2018 +0800

    Support wallpaper on secondary displays.(7/N)
    
    Expand the wallpaper color listener for multiple displays. This will
    allow the launcher on the secondary display can also receive the
    correct wallpaper color.
    
    Bug: 115486823
    Test: atest WallpaperManagerTest
    Test: atest ActivityManagerMultiDisplayTests
    Change-Id: I3d893537a8c606170b5641c9eb4683d09743d80c

 .../server/wallpaper/WallpaperManagerService.java  | 178 ++++++++++++++++-----
 1 file changed, 135 insertions(+), 43 deletions(-)

commit 674a4a0f38d9a860022c656fc66f935aaecc7c68
Author: wilsonshih <wilsonshih@google.com>
Date:   Wed Dec 19 11:47:25 2018 +0800

    Check if the wallpaper service exists before use.
    
    Add a null pointer check before calling the methods of
    WallpaperConnection.mService.
    Under normal circumstances, if the service is disconnected, we only
    need to wait until receive WallpaperConnection#onServiceConnected.
    
    Fix: 121181553
    Test: Manually test on multi display environments by kill
    wallpaper services, including SystemUI and selected wallpapers.
    
    Change-Id: Ic679ba5393f658f3071b017551ee845035d0dfd6

 .../server/wallpaper/WallpaperManagerService.java  | 27 +++++++++++++---------
 1 file changed, 16 insertions(+), 11 deletions(-)

commit 78268c3b322dbb2d266507a5206575bf11fd4249
Author: wilsonshih <wilsonshih@google.com>
Date:   Tue Dec 18 20:48:28 2018 +0800

    A quick fix for fallback wallpaper connection NPE.
    
    This is a quick fix for system service crash in WallpaperManagerService
    due to I didn't consider what to do if SystemUI crash or bind
    ImageWallpaper failed.
    
    Bug: 121181553
    Test: Mojave boot up.
    Change-Id: Ie47d9a8fdcc4140a218c5c37f566e3c2a53fb691

 .../com/android/server/wallpaper/WallpaperManagerService.java    | 9 +++++++++
 1 file changed, 9 insertions(+)

commit a282bf7e5c1e6860466795d01238a93b3741d45d
Author: wilsonshih <wilsonshih@google.com>
Date:   Fri Nov 30 12:48:05 2018 +0800

    Support wallpaper on secondary displays.(5/N)
    
    1. Pull DisplayData from WallpaperData, so the rendering size will only
    related to the display.
    2. In the previous CL, we provided a new property "supportsMultipleDisplays"
    that lets the WallpaperManagerService or wallpaper selector know that the
    component can be rendered on each display at the same time. In this CL we
    provide a fallback mechanism. If the selected wallpaper service does not
    support multiple displays, we will display the default image wallpaper on
    the secondary display.
    3. Add private access for some fields and methods.(by IDE reminder)
    
    Bug: 115486823
    Test: atest WallpaperManagerTest
    Test: atest WmTests
    Test: atest ActivityManagerMultiDisplayTests
    Change-Id: I2fa6ad5dc39a84e317680399e70178ca258a22ae

 .../server/wallpaper/WallpaperManagerService.java  | 326 ++++++++++++++-------
 1 file changed, 214 insertions(+), 112 deletions(-)

commit d9173df6765bc263f7e7eafe601009c4547b44b7
Author: wilsonshih <wilsonshih@google.com>
Date:   Thu Nov 29 11:52:15 2018 +0800

    Fix wallpaper size changed to -1*-1 on first boot.
    
    In previous version we have ensured the DisplaySize won’t be invalided
    when getting it from getDisplayDataOrCreate, The problem occurs when
    there is no wallpaper (such as first boot or xml error), it will reset
    the size to -1 then tries to restore an image wallpaper. Then it will
    go wrong because the bitmap size is set to -1.
    
    Change-Id: I71faa85cbbb5dcbb13414a15b1c9e3ad2a37887b
    Fix: 120114212
    Test: Manually test back/restore wallpaper.
    Test: atest WallpaperManagerTest
    Test: atest WmTests

 .../java/com/android/server/wallpaper/WallpaperManagerService.java     | 3 +--
 1 file changed, 1 insertion(+), 2 deletions(-)

commit 4c8c3274dae088923fe3f7db60c5ab6ac25285b6
Author: Lucas Dupin <dupin@google.com>
Date:   Tue Nov 6 17:47:48 2018 -0800

    Ambient wallpaper API feedback
    
    Making this api a @SystemApi, protecting it with a permission and
    changing boolean animation parameter to a long.
    
    Change-Id: Ife6aac2806a5590288a801751f22d85c3cfd4622
    Fixes: 116117810
    Test: atest DozeWallpaperStateTest
    Test: atest WallpaperServiceTest
    Test: set image wallpaper
    Test: set AOD wallpaper that holds permission
    Test: set AOD wallpaper that doesn't hold permission

 .../server/wallpaper/WallpaperManagerService.java  | 34 ++++++++++++++++++----
 1 file changed, 28 insertions(+), 6 deletions(-)

commit 81e10a706836c3e89cc4673fc78050ed4d86729b
Author: wilsonshih <wilsonshih@google.com>
Date:   Thu Nov 15 10:54:21 2018 +0800

    Support wallpaper on secondary displays.(3/N)
    
    The launchers which running on secondary displays can set the desired
    wallpaper dimensions and paddings on correct display.
    Preset reasonable wallpaper size for newly added displays.
    Add some error handling between the binder calls.
    
    Note: We still only save/load wallpaper info for default display, as most of
    time secondary displays are not fixed.
    
    Bug: 115486823
    Test: atest WallpaperManagerTest
    Test: atest WmTests
    Test: atest ActivityManagerMultiDisplayTests
    
    Change-Id: Ifd1a96fa185f2d75825c6fe8d3db69466b31c5c8

 .../server/wallpaper/WallpaperManagerService.java  | 325 ++++++++++++++-------
 1 file changed, 212 insertions(+), 113 deletions(-)

commit de93f49ae4bfbfdfdcf7b3b796adb30149896f54
Author: wilsonshih <wilsonshih@google.com>
Date:   Thu Nov 1 21:23:40 2018 +0800

    Support wallpaper on secondary displays.(1/N)
    
    Extends WallpaperConnection:mEngine, mapping an engine to a display.
    Handling display change events for add or remove wallpaper on secondary
    display.
    Only attach wallpaper on accessible display, usually WallpaperService is
    third party app and cannot access private display.
    
    Bug: 115486823
    Test: atest ActivityManagerMultiDisplayTests
    Test: atest WallpaperManagerTest
    
    Change-Id: Idb6063c3cf4c8c5b854676666615e3df4e6d65f4

 .../server/wallpaper/WallpaperManagerService.java  | 302 ++++++++++++++++-----
 1 file changed, 230 insertions(+), 72 deletions(-)

commit 0285aad24f04101581ebfa5c4a0436d3220eef83
Author: Valentin Iftime <valiiftime@google.com>
Date:   Mon Sep 10 11:01:27 2018 +0200

    Allow low resolution wallpaper crop
    
    If launcher sets desired dimensions smaller than display size,
    don't expand them to match display.
    Wallpaper will be scaled to fill display (ag/4867989).
    Old bugs: b/11332853 and b/11606952
    
    Bug: 113651690
    Bug: 74517029
    Test: Needs launcher that sets desired dimensions lower than display size (eg. new TVLauncher).
    Flash & wipe, after first boot /data/system/users/0/wallpaper
    and /data/system/users/0/wallpaper_orig should be 1x1px black bitmaps.
    CTS WallpaperManagerTest
    
    Change-Id: Ic54c3cc5986c37cef8eefc4be99a951c1f952460

 .../android/server/wallpaper/WallpaperManagerService.java | 15 ---------------
 1 file changed, 15 deletions(-)

commit 8f09a5057eac1d3fe328fbfcfa7442984270fb69
Author: Lucas Dupin <dupin@google.com>
Date:   Fri Jul 27 16:47:45 2018 +0800

    Expose Ambient Wallpaper API
    
    In P we introduced a private concept of AOD wallpapers, in Q
    we're making it a public surface.
    
    Bug: 111395593
    Test: make
    Change-Id: I4c406386f0ee15dc8734a24b040482b6cb807126

 .../java/com/android/server/wallpaper/WallpaperManagerService.java    | 4 ++--
 1 file changed, 2 insertions(+), 2 deletions(-)

commit 4b4c530ced67aa90df812aa7d19d1aabedcf99ff
Author: Lucas Dupin <dupin@google.com>
Date:   Sun Jun 24 18:22:10 2018 -0700

    Revert "Add theme mode design let user can choose theme self"
    
    The CL was intended only for P as a temporary solution.
    This reverts commit f0800fa3bd6d5279df3a183cc35ca5489112d73e.
    
    Test: switch wallpapers, observe theme
    Bug: 110758454
    Change-Id: If10e4d87b6ddac10063b2671abd99e0baccdf92e

 .../server/wallpaper/WallpaperManagerService.java  | 148 +--------------------
 1 file changed, 1 insertion(+), 147 deletions(-)

commit cf2c35dca6e20ffc87b4980b3b5d576a080db80a
Merge: 56cc567 b5e5053
Author: TreeHugger Robot <treehugger-gerrit@google.com>
Date:   Thu May 24 20:00:27 2018 +0000

    Merge "Revert "WallpaperColors hint computation"" into pi-dev

commit b5e5053ebc442ced1ad702f551919bc533bee164
Author: Lucas Dupin <dupin@google.com>
Date:   Thu May 24 16:33:14 2018 +0000

    Revert "WallpaperColors hint computation"
    
    This reverts commit c50f47d970b474371938f33e46b13ae2dd040df0.
    
    Fixes: 79465234
    Reason for revert: Google still does it using private APIs and apps were relying on this behavior, not good for the ecosystem.
    
    Change-Id: I62e2b4cd1e6e562fcdd89c97e599bcdade83381a

 .../core/java/com/android/server/wallpaper/WallpaperManagerService.java | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit f0800fa3bd6d5279df3a183cc35ca5489112d73e
Author: Tony Huang <tonyychuang@google.com>
Date:   Wed May 2 10:53:52 2018 +0800

    Add theme mode design let user can choose theme self
    
    Add a ContentObserver on WallpaperManagerService to
    listen THEME_MODE value change. When changed, call
    notifyWallpaperColorsChanged and set WallpaperColors
    color hint by the current mode.
    
    Bug: 63903361
    Test: manual
    Change-Id: I4f7aa7b184565b1bb23c7f4f1f07fc310dac7546

 .../server/wallpaper/WallpaperManagerService.java  | 148 ++++++++++++++++++++-
 1 file changed, 147 insertions(+), 1 deletion(-)

commit a47fcc974afece721a661617a24b9e2abbf5949a
Author: wilsonshih <wilsonshih@google.com>
Date:   Thu Apr 26 14:27:38 2018 +0800

    Reload wallpaper after user unlock.
    
    Live wallpaper may not support direct-boot aware, however, those kind of wallpapers should be showed after user unlock.
    It may not suit to call switchUser after onUnlockUser since there is no user switch happen.
    
    Bug: 78539162
    Test: Manual test, reboot and switch users.
    Change-Id: I99f8ad99c913ac05bc51c38c3da5aa51c0ba98fd

 .../java/com/android/server/wallpaper/WallpaperManagerService.java  | 6 ++++--
 1 file changed, 4 insertions(+), 2 deletions(-)

commit ad7d90fbbce8b61264432200ef6f9358dc56df31
Author: Jaekyun Seok <jaekyun@google.com>
Date:   Wed Apr 4 01:57:18 2018 +0900

    Switch user only if the user is changed
    
    Additionally this CL will initialize WallpaperManagerService on
    systemReady() instead of creator.
    
    Bug: 77356490
    Test: succeeded building and tested with a partner device
    Change-Id: I782f6c65ed2b9a37ef5ced68fda8a25d0542302b

 .../android/server/wallpaper/WallpaperManagerService.java | 15 +++++++++++----
 1 file changed, 11 insertions(+), 4 deletions(-)

commit 105540da419d7ec1da86f3c0ad6e5e2173deeb8e
Author: Christopher Tate <ctate@google.com>
Date:   Wed Mar 21 13:03:09 2018 -0700

    Regularize some wallpaper APIs
    
    Specifically:
    
      + support clearing wallpapers, with similar permissions to setting
      + support adjusting padding, with similar permissions as defining
        wallpaper dimension hints (which behave somewhat similarly)
    
    Bug: 62343054
    Test: atest CtsPermissionTestCases:NoWallpaperPermissionsTest
    Change-Id: Ia25f2791a20564c58096a25e6e189aa3b06b411c

 .../core/java/com/android/server/wallpaper/WallpaperManagerService.java | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit c50f47d970b474371938f33e46b13ae2dd040df0
Author: Lucas Dupin <dupin@google.com>
Date:   Fri Mar 2 13:10:42 2018 -0800

    WallpaperColors hint computation
    
    Public WallpaperColors surface should not compute hints, malicious apps
    might use theme inversion to slow down the system.
    
    Change-Id: I9d14259e433b1047d2512b3d994524dca5e8531a
    Fixes: 69532159
    Test: set white wallpaper, black wallpaper, observe theme changing
    Test: runtest -x services/tests/servicestests/src/com/android/server/wallpaper/WallpaperServiceTests.java

 .../core/java/com/android/server/wallpaper/WallpaperManagerService.java | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit 7d21b286568881373b4f5188db10f739ee85a296
Merge: 06db5f7 c5d4747
Author: Chris Tate <ctate@android.com>
Date:   Thu Feb 22 00:34:45 2018 +0000

    Merge "Explicitly restart the killed wallpaper service" am: 0780b18a9f am: 9298af376a
    am: c5d4747494
    
    Change-Id: I6f9d38268320095ba69bfa490fcdf4dadd173fe8

commit 06919216e48d3e8521c50bd9a6e2c7e363e70495
Author: Tetsutoki Shiozawa <tetsutoki.shiozawa@sony.com>
Date:   Fri Feb 2 14:18:43 2018 +0900

    Explicitly restart the killed wallpaper service
    
    Symptom:
    Live wallpaper setting was reset when a wallpaper service was killed
    by Lmk. The wallpaper fell back to the default image wallpaper. It's
    a black bitmap.
    
    Root cause:
    When restarting wallpaper service takes more than 10 seconds, it's
    treated as a bad wallpaper. The wallpaper setting is reset to remove
    the bad wallpaper.
    
    This is not a suitable solution under the low memory situation.
    Multiple processes are killed by Lmk at one time. Killed services are
    automatically restarted by AMS with a few seconds interval.
    The restart interval is increased by a factor of the number of killed
    services. Sometimes, it takes more than 1 minute.
    
    Solution:
    When a wallpaper service is killed, WallpaperManagerService requests
    restarting the service immediately. It can ignore the restart interval.
    
    Bug: 73071020
    Change-Id: Id5bb1cf121029a513f8773597f296b47667d1e21

 .../server/wallpaper/WallpaperManagerService.java  | 25 ++++++++++++++++------
 1 file changed, 18 insertions(+), 7 deletions(-)

commit 660d573e438c4b1a044fa399bb99272a0bcc9f22
Author: Lucas Dupin <dupin@google.com>
Date:   Tue Dec 19 10:05:19 2017 -0800

    Let wallpaper know when to animate AoD transition
    
    Sometimes the screen will blank, and sometime the
    wallpaper has the opportunity to animate the
    transition.
    
    Bug: 64155983
    Test: atest tests/Internal/src/android/service/wallpaper/WallpaperServiceTest.java
    Test: atest packages/SystemUI/tests/src/com/android/systemui/doze/DozeWallpaperStateTest.java
    Change-Id: Ia92c00edb98eeeba42da33bdc7bec3feb961a658

 .../java/com/android/server/wallpaper/WallpaperManagerService.java  | 6 +++---
 1 file changed, 3 insertions(+), 3 deletions(-)

commit 7517b5dcce8dde3a22177857b8fff6439fd98d82
Author: Lucas Dupin <dupin@google.com>
Date:   Tue Aug 22 12:51:25 2017 -0700

    Support wallpapers in AoD
    
    Such wallpaper has to define supportsAmbientMode,
    and set it to true on its android.service.wallpaper
    meta data.
    
    Also introduces WallpaperService.Engine#onAmbientModeChanged
    to notify a live wallpaper that the display state has changed.
    
    Change-Id: I49e846069a698b3cc3bb6e7cda98172920eaae4c
    Bug: 64155983
    Test: runtest -x frameworks/base/packages/SystemUI/tests/src/com/android/systemui/doze/DozeWallpaperStateTest.java
    Test: runtest -x frameworks/base/tests/Internal/src/android/app/WallpaperInfoTest.java
    Test: runtest -x frameworks/base/tests/Internal/src/android/service/wallpaper/WallpaperServiceTest.java
    Test: set AoD wallpaper, go to aod, lock screen, launcher
    Test: set regular wallpaper, go to aod, lock screen, launcher

 .../server/wallpaper/WallpaperManagerService.java  | 30 ++++++++++++++++++++++
 1 file changed, 30 insertions(+)

commit 4bbf8524ed84524616c1af940700c247e51c1776
Author: Daichi Hirono <hirono@google.com>
Date:   Wed Dec 6 10:34:18 2017 +0900

    Add config resource specifying WallpaperManagerService
    
    The config resource can be used by a vendor to override
    WallpaperManagerService.
    
    Bug: 69299523
    Test: WallpaperServiceTests, WallpaperManagerTest, WallpaperColorTest,
          WallpaperInfoTest.
    
    Change-Id: I9a38117c5b6fdc01aabb8293cde75485023970cd

 .../server/wallpaper/WallpaperManagerService.java  | 41 ++++++++++++++++------
 1 file changed, 31 insertions(+), 10 deletions(-)

commit 4f65a3571345f38c199dcd50ab5369e070877c96
Merge: dd3b6c8 9272d45
Author: Lucas Dupin <dupin@google.com>
Date:   Fri Sep 15 23:17:43 2017 +0000

    Merge "Color extraction should not block switchUser" into oc-mr1-dev

commit 9272d45da90cc1206ea81d7100724142dca35222
Author: Lucas Dupin <dupin@google.com>
Date:   Thu Sep 14 14:15:42 2017 -0700

    Color extraction should not block switchUser
    
    switchUser is called from the main thread. This would block it
    until color extraction ends, possibly causing a timeout in
    ActivityManager.
    
    We're now offloading color extraction to another thread.
    
    Test: Change wallpapers, switch users.
    Change-Id: I570c3ce5a562b56106d614c8febc12134e151afc
    Fixes: 65146279

 .../server/wallpaper/WallpaperManagerService.java     | 19 +++++++++++--------
 1 file changed, 11 insertions(+), 8 deletions(-)

commit f717b9350926205f85063ce2d178f88c97215f7b
Author: Christopher Tate <ctate@google.com>
Date:   Mon Sep 11 15:52:54 2017 -0700

    Make sure that updated wallpaper id is immediate
    
    As soon as setBitmap() or equivalent returns, the new wallpaper id needs
    to be immediately observable.  This was being subverted by inconsistent
    and racy "initialize from persisted state" handling in the set-wallpaper
    case: a load triggered by rebinding the static image display service
    could in some cases wind up overriding the new state calculated while
    new wallpaper imagery was being processed.
    
    The fix is to clarify the semantics of when load-from-persisted happens:
    it is now done *only* when the user is first spun up (or at boot, for
    the system user), and a firm guarantee provided about the up-front
    availability of the associated bookkeeping.  That, in turn, means not
    having to futz with lazy init when some client wants to read the current
    wallpaper imagery, and eliminating that gets rid of the races.
    
    And in a strictly-cosmetic fix, corrected the descriptive text for one
    of the permission enforcement calls.  Copypasta strikes again!
    
    Bug: 65016846
    Test: cts-tradefed run cts-dev -m CtsAppTestCases -t android.app.cts.WallpaperManagerTest\#setBitmapTest
    Change-Id: I73da48a58cca1849f073b8aea72019916dc2272b

 .../server/wallpaper/WallpaperManagerService.java  | 26 +++++++++++++---------
 1 file changed, 15 insertions(+), 11 deletions(-)

commit 8efbe0d7ef86ea54566dd89429ebd137cce1033d
Author: Christopher Tate <ctate@google.com>
Date:   Tue Aug 29 16:50:13 2017 -0700

    Setting a wallpaper must be synchronous
    
    Observable state needs to be fully established by the time the
    caller is allowed to proceed, otherwise they might read stale
    information.  In particular, we hadn't yet committed changes to
    the backing store by the time observers were released, so they
    could then read a stale generation number as the "current" state.
    
    With this fix, the flaky CTS test is now reliably passing.
    
    Test: cts-tradefed run cts-dev -m CtsAppTestCases -t android.app.cts.WallpaperManagerTest\#setBitmapTest
    Bug: 65016846
    Change-Id: I93fc690caedfbcd455a91625a3c67fff9168483b

 .../server/wallpaper/WallpaperManagerService.java      | 18 ++++++++++--------
 1 file changed, 10 insertions(+), 8 deletions(-)

commit 8a71c48d1cf159fe9da0bc60db68cc2d3a5243dc
Author: Christopher Tate <ctate@google.com>
Date:   Mon Aug 14 16:45:03 2017 -0700

    Add privileged permission for reading the current wallpaper image
    
    OEMs can therefore arrange factory-default access to have access prior
    to setup or user interaction.
    
    Bug: 30770233
    Bug: 64029450
    Test: manual
    
    Change-Id: I2cb30721f2a64ef50275b711ca10ca571248504a

 .../com/android/server/wallpaper/WallpaperManagerService.java     | 8 ++++++--
 1 file changed, 6 insertions(+), 2 deletions(-)

commit 4512145475c98c30e66bba25966a50c296401ab5
Merge: d05ac9c 33e014b
Author: Lucas Dupin <dupin@google.com>
Date:   Wed Aug 9 19:37:56 2017 +0000

    Merge "Fix race condition in binder thread" into oc-dr1-dev
    am: 33e014b9bc
    
    Change-Id: I5e98f321a23a5c99b321c10c59bbbc55b308bbf9

commit fb3ab2ac48ceb4aea40d3f9ddf1bf2db7034ecdc
Author: Lucas Dupin <dupin@google.com>
Date:   Tue Aug 8 14:17:30 2017 -0400

    Fix race condition in binder thread
    
    Changing wallpapers too quickly could cause a race condition where
    RemoteCallbackList#beginBroadcast would be called twice without
    the corresponding call to end the broadcast.
    
    Change-Id: I2e63df69ff4ffde76a0b91a2ad46f03d044d95b7
    Test: Set breakpoint to pause thread and cause race condition.
    Bug: 64391687

 .../server/wallpaper/WallpaperManagerService.java  | 52 +++++++++++-----------
 1 file changed, 26 insertions(+), 26 deletions(-)

commit 4204f1658c46fb5302ecae61ec73139890a07a76
Merge: 57d0423e91 9f22443
Author: TreeHugger Robot <treehugger-gerrit@google.com>
Date:   Thu Aug 3 23:07:53 2017 +0000

    Merge "Use normal API for legacy wallpaper restore" into oc-mr1-dev

commit 9f22443b4e3ac1ca1763a25a186177809ec1c3cb
Author: Christopher Tate <ctate@google.com>
Date:   Tue Aug 1 16:32:49 2017 -0700

    Use normal API for legacy wallpaper restore
    
    No longer do we play crazy rename-into-place tricks!  Just take
    the restored image and treat it as an ordinary new system wallpaper,
    and let the system machinery do all its proper work.  While we're at
    it, we get rid of the now-spurious shenanigans about backing up the
    wallpaper under the 'android' package, an overdue and welcome bit
    of thinning.
    
    In addition, we now fully migrate a legacy wallpaper image to have
    both a source and a display crop in place.  We were previously
    not generating the equivalent source image, which made N+ backups
    unable to store the image properly.
    
    Fix 64269208
    Fix 62809439
    Test: manual (set wallpaper under M, backup, flash to ToT,
          force restore)
    
    Change-Id: If9b26b777f0fda95cf37c3c790a3fa4e82ff0340

 .../server/wallpaper/WallpaperManagerService.java  | 46 ++++++++++++++++------
 1 file changed, 33 insertions(+), 13 deletions(-)

commit 50ba991655555dfde90149489f6485a0529ba0ac
Author: Lucas Dupin <dupin@google.com>
Date:   Fri Jul 14 11:55:05 2017 -0700

    Multi-user and WallpaperColors
    
    Test: set different wallpapers for different users and switch between them.
    Test: re-ran cts tests at cts/tests/app/src/android/app/cts/WallpaperManagerTest.java
    Change-Id: Ic06d1dc6db26869a2948590863ca9b8ac81c630e
    Fixes: 63513694

 .../server/wallpaper/WallpaperManagerService.java  | 183 ++++++++++++++-------
 1 file changed, 128 insertions(+), 55 deletions(-)

commit 26fb43c034168896b674427463fc40a52ad750cd
Author: Lucas Dupin <dupin@google.com>
Date:   Fri Jul 14 11:55:05 2017 -0700

    Multi-user and WallpaperColors
    
    Test: set different wallpapers for different users and switch between them.
    Test: re-ran cts tests at cts/tests/app/src/android/app/cts/WallpaperManagerTest.java
    Change-Id: Ic06d1dc6db26869a2948590863ca9b8ac81c630e
    Merged-In: Ic06d1dc6db26869a2948590863ca9b8ac81c630e
    Fixes: 63513694

 .../server/wallpaper/WallpaperManagerService.java  | 183 ++++++++++++++-------
 1 file changed, 128 insertions(+), 55 deletions(-)

commit be1066266ab0bf2910c4a27905c746f81abaaa56
Merge: ab48e51 3f8a360
Author: Lucas Dupin <dupin@google.com>
Date:   Fri Jul 7 17:45:21 2017 +0000

    Merge "WallpaperColors caching and synchronization" into oc-dr1-dev am: bd17b31310
    am: 3f8a36052b
    
    Change-Id: Ie66ba8d4fbc26bb256239d5c1b064210c19eb8ab

commit 0fa54f4f9e85afbd47fd7a70501c7686c67fa198
Author: Bryan Mawhinney <bryanmawhinney@google.com>
Date:   Thu Jul 6 17:09:37 2017 +0100

    Include value of allowBackup in "dumpsys wallpaper"
    
    Bug: 63381635
    Test: manual
    Change-Id: I22e0b6e0ecefc28979d81332707da8c0c5dd331b

 .../core/java/com/android/server/wallpaper/WallpaperManagerService.java | 2 ++
 1 file changed, 2 insertions(+)

commit 75ec37906799d0a0103b73a55a573dd39559c1f3
Author: Lucas Dupin <dupin@google.com>
Date:   Thu Jun 29 14:07:18 2017 -0700

    WallpaperColors caching and synchronization
    
    Making sure that colors are being cached in
    WallpaperManagerService and that sysui won't
    force a new color extraction.
    
    Fixes: 62958267
    Test: manual, reboot, look at systrace
    Test: runtest -x cts/tests/app/src/android/app/cts/WallpaperManagerTest.java
    Change-Id: Ic079a8e3d4d4ad65947b718dcc544f795c16f152

 .../server/wallpaper/WallpaperManagerService.java  | 64 +++++++++++++---------
 1 file changed, 38 insertions(+), 26 deletions(-)

commit 93252de33e148448c1555012ff9203e0f550d33b
Author: Christopher Tate <ctate@google.com>
Date:   Thu Jun 15 14:48:41 2017 -0700

    Require READ_EXTERNAL_STORAGE to read wallpaper imagery
    
    We now require the READ_EXTERNAL_STORAGE permission for an app to
    be allowed to read the system's wallpaper imagery.  This is a
    logged-but-benign no-op failure for apps targeting API levels up
    through Android O, but a crashing failure (SecurityException) for
    apps that target API levels newer than O.
    
    Also, marked the permission-requiring parts of the formal API
    with appropriate @RequiresPermission annotations.
    
    Bug 30770233
    Test: bit CtsPermissionTestCases:.NoWallpaperPermissionsTest
    
    Change-Id: Id75181f05d05e5ca32f5fefcbe15dc81de264dff

 .../server/wallpaper/WallpaperManagerService.java   | 21 +++++++++++++++++++--
 1 file changed, 19 insertions(+), 2 deletions(-)

commit 284836b0574dcffa19162d6b3f68c0b090bba611
Author: Lucas Dupin <dupin@google.com>
Date:   Fri Jun 23 15:28:41 2017 -0700

    Do not extract colors of live wallpapers.
    
    Not extracting colors from live wallpaper thumbanils
    since they might not represent the actual colors
    being displayed.
    
    Bug: 62019730
    Test: Manual. Set live wallpaper, scrim is grey.
    Change-Id: Ida652cab069beb1ee5fe36eb7862cc21e8edbc2e

 .../server/wallpaper/WallpaperManagerService.java  | 53 ++++------------------
 1 file changed, 10 insertions(+), 43 deletions(-)

commit 84b89d9d59797483a7e4a1bf82f3819d81e696e9
Author: Lucas Dupin <dupin@google.com>
Date:   Tue May 9 12:16:19 2017 -0700

    WallpaperColors refactor
    
    Hiding color extraction into WallpaperColors.
    This enables us to create WallpaperColors from a a Bitmap
    or Drawable.
    
    Fixes: 62197187
    Fixes: 62490115
    Test: runtest --path cts/tests/app/src/android/app/cts/WallpaperColorsTest.java
    Change-Id: I614cfa205e02b551a141642eac6de21251c3bff6

 .../server/wallpaper/WallpaperManagerService.java  | 82 ++++++++++------------
 1 file changed, 37 insertions(+), 45 deletions(-)

commit 698bb9da1768a9b75a06ff5eb947560bca2e777d
Merge: fd8ad94 2496ac8
Author: Christopher Tate <ctate@google.com>
Date:   Fri May 12 08:30:16 2017 +0000

    Merge "Accommodate service disconnect / package update race" into oc-dev am: a3301df508
    am: 2496ac8e94
    
    Change-Id: I0358ae03a55fd79f992066aea6ef514f37069142

commit c349e59f3394d7586bd45044fd6872cabfc33b79
Author: Christopher Tate <ctate@google.com>
Date:   Fri May 5 17:37:43 2017 -0700

    Accommodate service disconnect / package update race
    
    Ordering of delivery of package monitoring messages and service
    disconnect callbacks is indeterminate, but we need to make decisions
    about service disconnect based on the package update state.  We
    address the race by explicitly deferring our reset/timeout logic by
    a short time to allow the package monitor to process any pending
    updates on the same looper thread before we take action.
    
    Bug 34356215
    Test: manual
    
    Change-Id: Icffedbaf19b8eac8830c22fc05f299eb96b54acd

 .../server/wallpaper/WallpaperManagerService.java  | 43 ++++++++++++++++++----
 1 file changed, 35 insertions(+), 8 deletions(-)

commit bcae58519457a6ef93e1556264d484770d49761b
Author: Lucas Dupin <dupin@google.com>
Date:   Wed May 3 12:42:58 2017 -0700

    Get wallpaper colors from thumbnail
    
    Live wallpapers now have a default color extraciton implementation.
    We'll try to pick colors from their thumbnails.
    
    Bug: 37952518
    Test: manual
    Change-Id: I2915cbab673e39cd5e4686fe8e271ac2d4587436

 .../server/wallpaper/WallpaperManagerService.java  | 104 ++++++++++++++++-----
 1 file changed, 79 insertions(+), 25 deletions(-)

commit 41f6942429ca439779905a9af9c773fad9bc087e
Author: Lucas Dupin <dupin@google.com>
Date:   Wed May 3 15:26:22 2017 -0700

    Fix problem where wrong colors would be picked
    
    getWallpaperSafeLocked doesn't do what we need, it creates a new
    WallpaperData if a lock screen wallpaper isn't set.
    The desired behavior would be to get the current system wallpaper colors instead.
    
    There was also a synchronization problem in there.
    
    Test: set live wallpaper and look at lock screen colors
    Change-Id: I12a2acb6956ba31bb1d3c0843e04b8e4566e4f12

 .../android/server/wallpaper/WallpaperManagerService.java   | 13 +++++++++++--
 1 file changed, 11 insertions(+), 2 deletions(-)

commit ea1fb1e077e9fa8ce6a8a9e8caaf7423dc09cc9d
Author: Lucas Dupin <dupin@google.com>
Date:   Wed Apr 5 17:39:44 2017 -0700

    Wallpaper color extraction
    
    Now it's possible to listen to changes on wallpaper colors by
    registering a listener on WallpaperManager. It's also possible
    to know the current wallpaper colors and if it's light or dark.
    
    Test: runtest --path cts/tests/app/src/android/app/cts/WallpaperColorsTest.java && \
          runtest --path cts/tests/app/src/android/app/cts/WallpaperManagerTest.java
    Bug: 36856508
    Change-Id: Ia6b317b710e721d26f0fe41c847b9426e61d8d8b

 .../server/wallpaper/WallpaperManagerService.java  | 239 ++++++++++++++++++++-
 1 file changed, 238 insertions(+), 1 deletion(-)

commit fe9a53bc45fd0124a876dc0a49680aaf86641d3e
Author: Jeff Sharkey <jsharkey@android.com>
Date:   Fri Mar 31 14:08:23 2017 -0600

    Consistent dump() permission checking.
    
    This change introduces new methods on DumpUtils that can check if the
    caller has DUMP and/or PACKAGE_USAGE_STATS access.  It then moves all
    existing dump() methods to use these checks so that we emit
    consistent error messages.
    
    Test: cts-tradefed run commandAndExit cts-dev -m CtsSecurityTestCases -t android.security.cts.ServicePermissionsTest
    Bug: 32806790
    Change-Id: Iaff6b9506818ee082b1e169c89ebe1001b3bfeca

 .../com/android/server/wallpaper/WallpaperManagerService.java  | 10 ++--------
 1 file changed, 2 insertions(+), 8 deletions(-)

commit 2a6c55f01dd3b97a0c23ee19d9931d1c4e656760
Author: Christopher Tate <ctate@google.com>
Date:   Fri Mar 31 12:28:30 2017 -0700

    Be more lenient about live wallpaper unavailability
    
    - Give the app 5 more seconds to restart following a crash
    - Presume that system-default wallpapers are robust, and don't fall back
      to showing nothing at all if timing or update problems arise
    - Improve logging about static imagery handling during boot
    - For now, turn on a bit more logging about wallpaper activity
    
    Bug 36573894
    Bug 34979999
    Test: manual
    
    Change-Id: I787a351f1251b83451d55bb98a4e0de9ce17836a

 .../server/wallpaper/WallpaperManagerService.java  | 63 ++++++++++++++++------
 1 file changed, 48 insertions(+), 15 deletions(-)

commit d66b7e827002e6465c9513f17a7d6d8b6f9403d3
Merge: ec46371 9ee86cb
Author: Christopher Tate <ctate@google.com>
Date:   Tue Dec 20 01:17:58 2016 +0000

    Migrate system+lock wallpaper to lock-only when setting live wp am: 7cd0010df6
    am: 9ee86cb987
    
    Change-Id: I7d293d3d3cc0f60b0a3ce88ce9400db9d46fa20a

commit 7cd0010df6314397939dac2cb54caa41fdd49d3c
Author: Christopher Tate <ctate@google.com>
Date:   Mon Dec 19 14:38:44 2016 -0800

    Migrate system+lock wallpaper to lock-only when setting live wp
    
    If the static imagery is shared between system+lock, preserve the lock
    presentation when setting a different system-surface live wallpaper.  If
    the caller knows it wants to achieve system+lock display of the new live
    wallpaper, it follows up setWallpaperComponent() with an explicit clear
    of the lock wallpaper state.
    
    Previously, setting a new system live wallpaper would necessarily override
    a desired static lock image in the case of system+lock shared imagery.
    Now it doesn't.
    
    Bug 32664317
    
    Change-Id: I7ef2cded534f9e6e91899db4c37bd24efaf09fee

 .../android/server/wallpaper/WallpaperManagerService.java   | 13 +++++++++++++
 1 file changed, 13 insertions(+)

commit dc589ac82b5fe2063f4cfd94c8ae26d43d5420a0
Author: Sudheer Shanka <sudheersai@google.com>
Date:   Thu Nov 10 15:30:17 2016 -0800

    Update usage of ActivityManagerNative.
    
    - Remove references to ActivityManagerProxy.
    - Add isSystemReady to ActivityManager.
    
    Bug: 30977067
    Test: cts/hostsidetests/services/activityandwindowmanager/util/run-test android.server.cts
          adb shell am instrument -e class com.android.server.am.ActivityManagerTest,com.android.server.am.TaskStackChangedListenerTest \
              -w com.android.frameworks.servicestests/android.support.test.runner.AndroidJUnitRunner
    Change-Id: I07390b6124fb1515821f5c0b37baf6ae74adc8fa

 .../java/com/android/server/wallpaper/WallpaperManagerService.java     | 3 +--
 1 file changed, 1 insertion(+), 2 deletions(-)

commit ac2561e8206ac42921bb6ddbb0a5972fb360e394
Author: Wale Ogunwale <ogunwale@google.com>
Date:   Tue Nov 1 15:43:46 2016 -0700

    Make window token add/remove APIs require displayId
    
    Window tokens can now only be on one display, so we now require clients
    that want to add/remove window tokens to specify the display they would
    like the token to be created on. This simplifies the token handling code
    in WM and will be useful moving forward for clients that want to add
    windows to external displays.
    
    Test: Existing tests pass
    Change-Id: I6b2d8d58a913b3624f1a9a7bebbb99315613f103

 .../com/android/server/wallpaper/WallpaperManagerService.java    | 9 +++++----
 1 file changed, 5 insertions(+), 4 deletions(-)

commit 329fed87b9b401baa48ab840192a61847d44ed71
Merge: bd2fdfa 8371d28
Author: Christopher Tate <ctate@google.com>
Date:   Thu Oct 13 19:24:45 2016 +0000

    Retain allowBackup annotation when system+lock wallpaper becomes lock only am: edd8dc83d5 am: 4cb8e2de6f
    am: 8371d28717
    
    Change-Id: I13cbbeabab75413ab66b5c64f021ccba2330d830

commit edd8dc83d515ed46d555949cf3ffa515c8a19ebe
Author: Christopher Tate <ctate@google.com>
Date:   Wed Oct 12 15:17:58 2016 -0700

    Retain allowBackup annotation when system+lock wallpaper becomes lock only
    
    Bug 32069467
    
    Change-Id: I5d0b91c40e1a2896f2b11efeef7bff84503cf2c0

 .../core/java/com/android/server/wallpaper/WallpaperManagerService.java | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit 6abac1c08e999434d64a1a3bfecba36531d867ea
Merge: 017f1dc 69f0cb5
Author: Sudheer Shanka <sudheersai@google.com>
Date:   Tue Oct 11 17:47:28 2016 +0000

    Merge "Remove internal user-specific state after user removal."

commit 619cbf95aa3ddb9d05f3b6e176f5bac2763a8afb
Merge: 0e09afb d054741
Author: Christopher Tate <ctate@google.com>
Date:   Tue Oct 11 03:30:19 2016 +0000

    Ignore wallpaper relaunch timeout during shutdown am: 762dfd1566 am: 660ad176e9
    am: d0547414b1
    
    Change-Id: I5032141cbb4d59061712d5ae8c297a97930057cd

commit 762dfd156619072e8f74dafaf055b6f0bf7ab2d5
Author: Christopher Tate <ctate@google.com>
Date:   Mon Oct 10 17:44:48 2016 -0700

    Ignore wallpaper relaunch timeout during shutdown
    
    Because of course during system shutdown, the wallpaper service
    won't actually get relaunched.  Sometimes shutdown can take long
    enough that this timeout kicks, so we need to avoid clearing the
    live wallpaper state spuriously.
    
    For simplicity we just check at "would have timed out now" time
    rather than try to distinguish between the shutdown case and a
    genuine crash that raced with the shutdown broadcast.
    
    Bug 32020355
    
    Change-Id: I9335b2c0214b4c750ef950fed157d186aa670176

 .../server/wallpaper/WallpaperManagerService.java  | 25 ++++++++++++++++++++++
 1 file changed, 25 insertions(+)

commit 69f0cb5c7811427b1aa969d097fb67bfc5357180
Author: Sudheer Shanka <sudheersai@google.com>
Date:   Thu Oct 6 17:33:20 2016 -0700

    Remove internal user-specific state after user removal.
    
    Fixes: 32001177
    Test: Verified that user state is removed with some logging.
    Change-Id: I4b3d10c43e401db4d8d593c24d052e337ca33e41

 .../core/java/com/android/server/wallpaper/WallpaperManagerService.java  | 1 +
 1 file changed, 1 insertion(+)

commit 42ed4974161990d2ffab15f670a723b4c32e3f82
Merge: d9eb6ce 2c4522c
Author: Sudheer Shanka <sudheersai@google.com>
Date:   Sat Sep 24 01:11:57 2016 +0000

    Merge "Add usermanager related perf tests - part2"

commit 6313e936ae6d50dc36bb3d25b2a780d9246bdbf5
Merge: bb85654 aba83f2d
Author: Adrian Roos <roosa@google.com>
Date:   Wed Sep 21 07:03:45 2016 +0000

    Fix black wallpaper after repeated crashes am: c3f915e482 am: 77f60f3809
    am: aba83f2d5a
    
    Change-Id: Ic84ee60efe638774bc4577d137aba2f2d817f595

commit c3f915e4824d24b653098ac4410e488e2f646ed9
Author: Adrian Roos <roosa@google.com>
Date:   Tue Sep 6 11:40:53 2016 -0700

    Fix black wallpaper after repeated crashes
    
    Adds logic to recover from a repeatedly crashing wallpaper. Before,
    a wallpaper that crashed twice but not within 10s would not trigger
    the recovery logic in WallpaperManagerService, but would also not be
    restarted by ActivityManager (because two crashes will effectively
    disable a service connection), thus resulting in a black wallpaper.
    
    Change-Id: Ie4f7862dc07a89d13f5e2b56c825a3371ea21114
    Fixes: 30250003

 .../server/wallpaper/WallpaperManagerService.java  | 29 ++++++++++++++++++++++
 1 file changed, 29 insertions(+)

commit 2c4522cc1bf3a3d0178688427a33b860ddfe4bba
Author: Sudheer Shanka <sudheersai@google.com>
Date:   Sat Aug 27 20:53:28 2016 -0700

    Add usermanager related perf tests - part2
    
    - Add onLockedBootComplete callback to UserSwitchObserver.
    
    Bug: 30948225
    Test: adb shell am instrument -e class android.multiuser.UserLifecycleTest -w com.android.perftests.multiuser/android.support.test.runner.AndroidJUnitRunner
    Change-Id: I87533094805a81fbd85d4f492e637b3304ecd5e2

 .../android/server/wallpaper/WallpaperManagerService.java   | 13 ++-----------
 1 file changed, 2 insertions(+), 11 deletions(-)

commit cb4374954e3f2c5168291a7a3a106cf13559c6ef
Merge: 24f8deb 20fce0a
Author: Adrian Roos <roosa@google.com>
Date:   Thu Sep 1 03:04:09 2016 +0000

    Wallpaper: Fix letterboxing if wallpaper is scaled but not cropped am: 5c97ff23a3 am: 865f437e99 am: 7634e5e52c
    am: 20fce0ac80
    
    Change-Id: I8d1870bf8e0049fab7f1ece4974388f04cad974e

commit 5c97ff23a37d7b3588dced019ee09731a5e9b9cd
Author: Adrian Roos <roosa@google.com>
Date:   Wed Aug 31 10:25:38 2016 -0700

    Wallpaper: Fix letterboxing if wallpaper is scaled but not cropped
    
    Fixes a bug where the wrong part of an image was decoded if
    the image needed to be scaled but not cropped.
    
    Change-Id: I011e59d85b526097ef1daabd63805c7cdc74c17b
    Fixes: 31112957

 .../com/android/server/wallpaper/WallpaperManagerService.java  | 10 +++++++++-
 1 file changed, 9 insertions(+), 1 deletion(-)

commit 6ace777436c0f22e6bed43184bd3657af2a6b573
Merge: cc7ec07 e4af99d
Author: Jorim Jaggi <jjaggi@google.com>
Date:   Tue Aug 23 18:04:57 2016 +0000

    Change retail mode wallpaper (1/2) am: 6c902d0453 am: 9d0879c6ac am: 3d19454ec9
    am: e4af99d6b7
    
    Change-Id: Ia74d06c44aa2b6d8bd75fa3db4fa5f5772c218d3

commit d57bcbb6c7454f578055a6570a0b6a1503097b82
Merge: d663cc1 47b1465
Author: Christopher Tate <ctate@google.com>
Date:   Tue Aug 23 17:56:19 2016 +0000

    Fix wallpaper backup eligibility test am: f7d1b5de6e am: 100b610066 am: 6939bf0b05
    am: 47b146517f
    
    Change-Id: I66425f3f057434c2fbd47ca076c5bba53ad80179

commit 9d0879c6ac474d18359793df3427544dbe01ea0e
Merge: 1b192da 6c902d0
Author: Jorim Jaggi <jjaggi@google.com>
Date:   Tue Aug 23 00:17:20 2016 +0000

    Change retail mode wallpaper (1/2)
    am: 6c902d0453
    
    Change-Id: I1ca69b6cc7450d7f7056e69e3cf7fab1ad2df624

commit c7136b3b3c5ed8d688e87d8b26c4a10cb313d698
Merge: 081e393 6c902d0
Author: TreeHugger Robot <treehugger-gerrit@google.com>
Date:   Mon Aug 22 22:55:12 2016 +0000

    Merge "Change retail mode wallpaper (1/2)" into nyc-dr1-dev

commit f7d1b5de6eb769e9fb727dd27922a761d2960360
Author: Christopher Tate <ctate@google.com>
Date:   Fri Aug 19 11:21:07 2016 -0700

    Fix wallpaper backup eligibility test
    
    Bug 30970354
    Bug 30938483
    
    Change-Id: I3c933a80505238897ceb8e89c228ed48ee5a9d0e

 .../java/com/android/server/wallpaper/WallpaperManagerService.java    | 4 ++--
 1 file changed, 2 insertions(+), 2 deletions(-)

commit 6c902d04532838f7549506b1c62349e321ac9485
Author: Jorim Jaggi <jjaggi@google.com>
Date:   Thu Aug 18 10:44:54 2016 -0700

    Change retail mode wallpaper (1/2)
    
    Change-Id: I7ce5711e57bc39edf10fc7151f26168c6183d71e
    Fixes: 30830249

 .../java/com/android/server/wallpaper/WallpaperManagerService.java  | 6 +++---
 1 file changed, 3 insertions(+), 3 deletions(-)

commit bcffe9fde26859cb25617dc2de542b8e81176430
Merge: 2610072 544a8bd
Author: Christopher Tate <ctate@google.com>
Date:   Sun Aug 14 00:45:17 2016 +0000

    Remember all wallpapers' backup-allow state am: c613c63096 am: 67c0d08a46
    am: 544a8bd01e
    
    Change-Id: I2a8784957187b426ea1cc788f47acb60026d9eef

commit c613c63096d9c3e3da8c081210661007d2acf8fb
Author: Christopher Tate <ctate@google.com>
Date:   Fri Aug 12 14:13:02 2016 -0700

    Remember all wallpapers' backup-allow state
    
    ...not just system wallpapers.
    
    Bug 30751829
    
    Change-Id: If9f5df33e587e31080a7e65b1cbcae03f9a39f3c

 .../java/com/android/server/wallpaper/WallpaperManagerService.java    | 4 +---
 1 file changed, 1 insertion(+), 3 deletions(-)

commit 328439d0a28abb2489c18b91f6bbff772cae13ee
Merge: 534376f 9649aec
Author: Christopher Tate <ctate@google.com>
Date:   Thu Aug 11 20:43:44 2016 +0000

    Check system & lock wallpaper backup eligibility independently am: 6172266154 am: 611afaf4ae
    am: 9649aecab6
    
    Change-Id: I8c4dd55fa63f0a9ffbcd7992ee29040f79ae1780

commit 6172266154e9071abba2c0aab9ffb31e0ec8c239
Author: Christopher Tate <ctate@google.com>
Date:   Wed Aug 10 16:13:14 2016 -0700

    Check system & lock wallpaper backup eligibility independently
    
    Bug 30751829
    
    Change-Id: Ic94689dd63238449222d1aea07231d9fd29fc76d

 .../java/com/android/server/wallpaper/WallpaperManagerService.java  | 6 ++++--
 1 file changed, 4 insertions(+), 2 deletions(-)

commit 2329e7b20c9981599674db283c15d1e69f3370ce
Merge: d4ac560 ac2f0b2
Author: Christopher Tate <ctate@google.com>
Date:   Wed Jul 27 01:36:17 2016 +0000

    Fix wallpaper restore from pre-N devices am: ebadfb17e7
    am: ac2f0b2c51
    
    Change-Id: I7b8c8eb3e880389d1f70fd15f9d82bd815f1be16

commit ebadfb17e77c5e297635376dbdbcf1615192620f
Author: Christopher Tate <ctate@google.com>
Date:   Mon Jul 25 14:50:08 2016 -0700

    Fix wallpaper restore from pre-N devices
    
    The previous path "worked" but left the wallpaper bookkeeping with
    incorrect SELinux labeling, which led to cascade failures later on
    when apps tried to set new system wallpaper imagery or read the
    current image for the picker UI.
    
    We now (a) explicitly label wallpaper files in all change cases,
    (b) let the restored imagery flow through the full crop path, as
    should have been done in the first place, and as a result (c) lift
    the size restrictions on the source image, because now we are doing
    a device-appropriate scaling operation on the image.
    
    The "when to crop/scale" test has been slightly relaxed such that
    a restored image of exactly the right size now longer triggers a
    superfluous factor-1.0 scaling operation.
    
    Bug 30361282
    
    Change-Id: I9a305eea2227952493f64ab78862648cafb816ff

 .../java/com/android/server/wallpaper/WallpaperManagerService.java  | 6 +++---
 1 file changed, 3 insertions(+), 3 deletions(-)

commit 81b8b82d7ab5713414d108d347c920b122386d33
Merge: 984d575 7ce8cfb
Author: Chris Tate <ctate@android.com>
Date:   Wed Jul 20 23:04:29 2016 +0000

    Merge \\"Run the SELinux wallpaper restorecon operations on all users\\" into nyc-mr1-dev am: bf9fa8280e
    am: 7ce8cfbec4
    
    Change-Id: I8e5e20a2a9b21ec6a704aa9c7abb3a0ef1878d12

commit 38a5dc33a9140a7bfd90cfa58185d357ab3df8f9
Author: Christopher Tate <ctate@google.com>
Date:   Wed Jul 20 15:10:18 2016 -0700

    Run the SELinux wallpaper restorecon operations on all users
    
    And only run them once per device boot lifetime.
    
    Bug 30229410
    
    Change-Id: Ia6f7f7eea7f8985c83b531dfa16e96d08235b901

 .../server/wallpaper/WallpaperManagerService.java  | 33 +++++++++++++---------
 1 file changed, 20 insertions(+), 13 deletions(-)

commit 5810a1c2523f8dca02e81b41c8e03ab15b27fc2b
Merge: 4274dc6 119840e
Author: Adrian Roos <roosa@google.com>
Date:   Fri Jul 15 23:25:19 2016 +0000

    Merge \\"Add way to set live wallpaper across users\\" into nyc-mr1-dev am: 9a7c3bd577
    am: 119840eb91
    
    Change-Id: I16436af5b7ada58113d846b9836d302061730177

commit 9a7c3bd577ba1d3ea43082814529ea3d6379d024
Merge: d5b4385 40ea083
Author: TreeHugger Robot <treehugger-gerrit@google.com>
Date:   Fri Jul 15 21:07:30 2016 +0000

    Merge "Add way to set live wallpaper across users" into nyc-mr1-dev

commit 40ea083b59c05beec2eb2c064927086aa55173f3
Author: Adrian Roos <roosa@google.com>
Date:   Thu Jul 14 14:19:55 2016 -0700

    Add way to set live wallpaper across users
    
    Also adds an entry point for vendor specific services
    to SystemUI.
    
    Bug: 30038484
    Change-Id: I8f335c1f7de15d619f2c688a8ac95372f166595f

 .../android/server/wallpaper/WallpaperManagerService.java  | 14 +++++++++++---
 1 file changed, 11 insertions(+), 3 deletions(-)

commit 674e0f0375b6a5272cb0a00fd155f04219da0d59
Merge: 62005aa 6e78e3a
Author: Chris Tate <ctate@android.com>
Date:   Tue Jul 12 19:15:45 2016 +0000

    Merge \\"Fix up mangled SELinux labeling of wallpaper files\\" into nyc-mr1-dev am: 87488ac89b
    am: 6e78e3a955
    
    Change-Id: Ided20fd0096205b2e93f6729408ac3c455f76e9f

commit 359c48c805a902ec0990884ea8cbe7f875ce8154
Merge: 807cf86 0483184
Author: Selim Cinek <cinek@google.com>
Date:   Mon Jul 11 16:59:03 2016 -0700

    resolve merge conflicts of 0483184 to master
    
    Change-Id: I8466c8d56b5685705591dad7cfb7d439b6dee3d7

commit 190e853608011f82a35e2f578ed2105a46d6fb1d
Author: Christopher Tate <ctate@google.com>
Date:   Mon Jul 11 11:35:34 2016 -0700

    Fix up mangled SELinux labeling of wallpaper files
    
    We do this lazily at user unlock, in the background to avoid
    impacting unlock time.
    
    Bug 29469965
    
    Change-Id: Ic08e38399da486b40d8967e4a2e7828b094e7ba6

 .../server/wallpaper/WallpaperManagerService.java  | 25 ++++++++++++++++++----
 1 file changed, 21 insertions(+), 4 deletions(-)

commit e31f6b8024d1a0cfa71894e9a8ce1b39a9f85b2f
Author: Jorim Jaggi <jjaggi@google.com>
Date:   Fri Jul 1 16:15:09 2016 -0700

    Preparations for different scrim depending on wallpaper
    
    - Allow wallpaper info to be queried by user
    - Refactor ScrimController for extensibility
    
    Bug: 28936996
    Change-Id: Ib019331a844110f1f24f35d225d2648626545233

 .../java/com/android/server/wallpaper/WallpaperManagerService.java   | 5 +++--
 1 file changed, 3 insertions(+), 2 deletions(-)

commit 98ca2bc94b451669dd4d62343eaf5d5a0537019e
Merge: 17b9680 cb9705d
Author: Christopher Tate <ctate@google.com>
Date:   Fri Jul 1 00:41:23 2016 +0000

    Make sure SELinux labels are correct after move-to operations am: fa7d97fa15 am: 4b96358fcb
    am: cb9705d833
    
    Change-Id: Id12b564eab7dc5faad090e7fa076f0ab3d15d480

commit cb9705d833f596905edb5e9e6ac21dd4d1efca86
Merge: 7258aac 4b96358
Author: Christopher Tate <ctate@google.com>
Date:   Fri Jul 1 00:36:30 2016 +0000

    Make sure SELinux labels are correct after move-to operations am: fa7d97fa15
    am: 4b96358fcb
    
    Change-Id: I45bb5260b1b1fbfa1393d57cf9f0761e0df608ce

commit 9ecf5010d3be549eac3db1659eed2cc6edbbd0d9
Merge: 8aa4a84 fa7d97f
Author: Christopher Tate <ctate@google.com>
Date:   Fri Jul 1 00:29:33 2016 +0000

    Make sure SELinux labels are correct after move-to operations
    am: fa7d97fa15
    
    Change-Id: I913e54c6269f75b10b51030d0b2921007107281d

commit fa7d97fa15700b62b01d0b7dd42fcaf12c57b9f5
Author: Christopher Tate <ctate@google.com>
Date:   Thu Jun 30 12:21:57 2016 -0700

    Make sure SELinux labels are correct after move-to operations
    
    In some circumstances wallpaper-related files are moved into position,
    and must then take proper effect.  Make sure that they have the
    correct SELinux labels afterwards to avoid preventing some valid
    accesses.
    
    Bug 29469965
    
    Change-Id: I6d7c86be63d568fa0ad8841d109a7ff2149fdd54

 .../java/com/android/server/wallpaper/WallpaperManagerService.java  | 6 +++++-
 1 file changed, 5 insertions(+), 1 deletion(-)

commit 1d84a7ffef52c95b0e80e65bf9a7b8f7deffbd65
Merge: be007c2 a5b7b96
Author: Fyodor Kupolov <fkupolov@google.com>
Date:   Thu Jun 23 22:11:49 2016 +0000

    Merge \"Merge \\"Print names of observers causing timeout\\" into nyc-mr1-dev am: 38d7897690\" into nyc-mr1-dev-plus-aosp
    am: a5b7b96500
    
    Change-Id: I04decfb9d3988e5080ab3ff5c88032836dd1306a

commit 0b77ef9f5199b7cd0956f2bfe049cbd699ca03b4
Author: Fyodor Kupolov <fkupolov@google.com>
Date:   Mon Jun 20 17:16:52 2016 -0700

    Print names of observers causing timeout
    
    Pass and store name in registerUserSwitchObserver and later print names of
    observers causing timeout.
    
    Bug: 29039588
    Change-Id: I09c4bcc986168a07f5e20ad0f38667b783332288

 .../java/com/android/server/wallpaper/WallpaperManagerService.java   | 5 ++---
 1 file changed, 2 insertions(+), 3 deletions(-)

commit 1ec2fd72cea31d46cca6e8f6686115fabe885664
Merge: e9061cf 032dcff
Author: The Android Automerger <android-build@android.com>
Date:   Thu May 19 23:51:32 2016 +0000

    stephenli@ manually merge many commits up to '032dcff'
    
    * commit '032dcff': (22 commits)
      Remove outdated google services links.
      Fix misc-macro-parentheses warnings in services jni.
      Fix misc-macro-parentheses warnings in hwui and graphic jni.
      Fix misc-macro-parentheses warnings in aapt and androidfw.
      docs: Update to column widths for Complications table
      Fix a11y crash when window layer isn't unique.
      Never set resized while not drag resizing for pinned stack.
      While turning OFF do not honor ON requests.
      Fix GATT autoConnect race condition
      Fix GATT autoConnect race condition
      Fix RTL issue in delete dialog.
      Incorporate feedback on new wallpaper-related APIs
      Mapping up/down of legacy Gps vs. Gnss Status
      Fixed a bug where the chronometer was invisible
      Fixed a bug where the chronometer wasn't updating the time
      Update BlockedNumberContract javadocs.
      [RenderScript] Fix ScriptIntrinsicBlur documentation.
      Update documentation about copyTo and copyFrom.
      DO NOT MERGE Cherry pick libpng usage fixes
      Start the Wear Time System Service with SystemServer
      ...

commit 98d609ce3f98585a21f3be31a318bd4e1396a562
Author: Christopher Tate <ctate@google.com>
Date:   Wed May 18 17:31:58 2016 -0700

    Incorporate feedback on new wallpaper-related APIs
    
    - Documentation
    - Method naming
    - Throwing exceptions rather than returning zero/null/false on input failures
    
    Bug 28773334
    
    Change-Id: Ia41c1e31c76b7114f3ffeb16033384cac5a1031d

 .../android/server/wallpaper/WallpaperManagerService.java | 15 ++++++++-------
 1 file changed, 8 insertions(+), 7 deletions(-)

commit 10b889de7d0c9271e3fb47c87a8ed140bee9c271
Merge: 2173c75 68fffa5
Author: The Android Automerger <android-build-merger@google.com>
Date:   Thu May 12 02:10:33 2016 +0000

    stephenli Manually merge commit '68fffa5'
    
    * commit '68fffa5': (23 commits)
      Fix smallest width configuration calculation
      docs: DoDS, wearable reference docs
      Switch the default text selection handles to Material style.
      docs: Noted minor API changes in release notes
      docs: added "billions" doc in Distribute>Essentials
      Remove wear design pages redirecting to design/wear
      correct the support library redirects to redirect whole path
      Stop saving ActionMenuItemView state.
      Fix iterator double-advance in ContentObserverController
      TIF: Remove the uniqueness check for track ID from notifyTracksChanged
      Update and add attributes to the JavaDoc for VectorDrawable
      Use Q=100 JPEG instead of PNG for wallpaper display
      Fix issue #28400000: Settings memory UI still showing z-ram...
      docs: Updated support library revision history for 23.4.0
      docs: Updates to notifications for DP3
      docs: Added emoji section to api overview.
      Fixed a bug where the QS was animating wrong when closing
      Fix KeyguardManager.isSecure() to observe work profile
      cherrypick from mnc-docs docs: Updated APK Signature Scheme v2 doc.
      Docs: Added new Whitelist feature to Data Saver for DP3
      ...

commit c484f5497c7263ab7012bad600caea9e493f465c
Author: Christopher Tate <ctate@google.com>
Date:   Wed May 11 14:31:34 2016 -0700

    Use Q=100 JPEG instead of PNG for wallpaper display
    
    The quality difference is minimal, but the time to process the
    wallpaper is an order of magnitude lower for JPEG.  The source
    imagery is still being preserved in its original format and
    resolution; this affects only the scaled/cropped version used
    on-screen.
    
    Bug 28672266
    
    Change-Id: I193854348ffb7eeb9e45dc08b8ef7173ea75c240

 .../core/java/com/android/server/wallpaper/WallpaperManagerService.java | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit c298b1695d6b22409ea3cdbe0f863d9ed06b3bdd
Author: Joe LaPenna <jlapenna@google.com>
Date:   Mon May 2 15:25:50 2016 -0700

    Downgrade WallpaperManagerService error
    
    BUG: 28530212
    
    Change-Id: I35f0f1b1e7241939cb3a2f27dbb5da7f231c0b35

 .../core/java/com/android/server/wallpaper/WallpaperManagerService.java | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit 8347b631883d073a2919517179c1e1378a8671e6
Author: Christopher Tate <ctate@google.com>
Date:   Fri Apr 29 18:59:18 2016 -0700

    Preserve shared {system+lock} wallpaper when setting system only
    
    We migrate the existing (shared) system wallpaper to be the lock-only
    wallpaper when setting a new system-only wallpaper.  The end result
    is that if you say "set the system wallpaper only" the lock wallpaper
    won't change.
    
    (The migration is via a rename of the underlying imagery files, not
    via copying, because copying all that data would be silly.)
    
    Bug 27599080
    
    Change-Id: I03ecf23c943fe88af58d5ac26f05587a15e2d0a9

 .../server/wallpaper/WallpaperManagerService.java  | 76 +++++++++++++++++++---
 1 file changed, 68 insertions(+), 8 deletions(-)

commit ad2e4bf9f36cf612db6c397feca8effb125ee541
Author: Amith Yamasani <yamasani@google.com>
Date:   Tue Apr 26 14:35:54 2016 -0700

    Stop user faster and clear stale broadcasts
    
    Moved several USER_STOPPING registered receivers to listen
    to USER_STOPPED, since they don't need to be blocking the
    shutdown of the user.
    
    Clear all stale broadcasts when stopping a user, so that we
    don't unnecessarily start up processes and deliver stale
    broadcasts. This was causing code to run when the user was
    already stopped and resulted in crashes when other providers
    and services couldn't be started anymore. Hopefully this fixes
    many of those races.
    
    Bug: 28371487
    Change-Id: Ic35a7a23fa8fe009a53f8bf7545d4dad5fa34134

 .../java/com/android/server/wallpaper/WallpaperManagerService.java | 7 -------
 1 file changed, 7 deletions(-)

commit d7faf53605838487cace9979e577005cc7c8cabc
Author: Christopher Tate <ctate@google.com>
Date:   Thu Feb 25 12:43:38 2016 -0800

    Don't back up wallpapers that we've been told not to
    
    In addition, now that the full uncropped wallpaper image is being
    backed up, we now handle that via the full-data backup path instead
    of key/value.  Restore still knows about legacy data that gets
    delivered via the older key/value mechanism.
    
    This change also has the effect of removing the size limitations
    around wallpaper restore acceptance.  Any size source imagery is
    valid, as crop & scale are rerun in a device-appropriate way
    after the restore.
    
    Bug 25453848
    Bug 25727875
    
    Change-Id: Idc64a2eaab97a8ecc9d2b8ca5dc011f29cab324d

 .../server/wallpaper/WallpaperManagerService.java  | 78 +++++++++++++++-------
 1 file changed, 55 insertions(+), 23 deletions(-)

commit c28e3a93ff01a809bcfa5bb78e3358f926a09879
Author: Adrian Roos <roosa@google.com>
Date:   Thu Apr 14 10:47:52 2016 -0700

    Ensure sane WallpaperData
    
    Also adds dumpsys output for lock wallpapers.
    
    Fixes: 28192320
    Change-Id: I66ccf8afad7412ae346e7cc02d030c2fdda97753

 .../server/wallpaper/WallpaperManagerService.java  | 29 ++++++++++++++++++++--
 1 file changed, 27 insertions(+), 2 deletions(-)

commit 383f9fedd6c3423d7a8abed21151a89808fa0fab
Author: Jeff Sharkey <jsharkey@android.com>
Date:   Wed Apr 13 16:08:15 2016 -0600

    Bind wallpaper at later boot phase.
    
    We need to wait until third-party apps can start before we try
    binding to the wallpaper.
    
    Bug: 28166684
    Change-Id: I6ef559a667104e830e97da68b437ff592816f6f3

 .../java/com/android/server/wallpaper/WallpaperManagerService.java   | 5 ++---
 1 file changed, 2 insertions(+), 3 deletions(-)

commit 1cab76af8537a275d1af38d25f5692a68e48eed6
Author: Jeff Sharkey <jsharkey@android.com>
Date:   Tue Apr 12 18:23:31 2016 -0600

    Make wallpapers direct-boot aware.
    
    If the user's wallpaper isn't direct-boot aware, wait around for
    the user to be unlocked, instead of clearing the wallpaper.
    
    Also switch a few classes to using SystemService lifecycle, since
    events are dispatched faster than through broadcasts.  Fix bug where
    ContentService.systemReady() was never called, and make sure
    EntropyMixer doesn't risk being GC'ed.
    
    Bug: 26280055
    Change-Id: I9fff468a439b868baa68cf11bb6ee9f7d52b7b5a

 .../server/wallpaper/WallpaperManagerService.java  | 132 +++++++++++++++------
 1 file changed, 94 insertions(+), 38 deletions(-)

commit 1a96b63b6c8c98c9d3f04ff2d5b7616e5707aedd
Author: Christopher Tate <ctate@google.com>
Date:   Tue Mar 22 15:25:42 2016 -0700

    Scale wallpaper crop to device-suitable size
    
    No need to carry around enormous images just to aggressively
    downsample them at draw time.
    
    Bug 26863365
    
    Change-Id: Id233d39bf61f45d44ac0329ea406f9c702c36f97

 .../server/wallpaper/WallpaperManagerService.java  | 157 +++++++++++++++------
 1 file changed, 111 insertions(+), 46 deletions(-)

commit 2c7238e832680dafc75fe8ec3a35469a590d4128
Merge: ee66ab9 0572e18
Author: Sunny Goyal <sunnygoyal@google.com>
Date:   Thu Mar 31 21:59:35 2016 +0000

    Merge "Sending WALLPAPER_CHANGED broadcast when live wallpaper changes" into nyc-dev

commit 0572e1847aa20799cf1e87bc0bfeb1278d2cf423
Author: Sunny Goyal <sunnygoyal@google.com>
Date:   Thu Mar 31 11:05:51 2016 -0700

    Sending WALLPAPER_CHANGED broadcast when live wallpaper changes
    
    Bug: 27947401
    Change-Id: Ib4fb3310e124e01d8fb7d7045d565ca3b9456050

 .../core/java/com/android/server/wallpaper/WallpaperManagerService.java  | 1 +
 1 file changed, 1 insertion(+)

commit edf7d04003890d3b673ab804f5b739e143f4faca
Author: Christopher Tate <ctate@google.com>
Date:   Tue Mar 29 18:24:25 2016 -0700

    API review: WallpaperManager
    
    - Rename FLAG_SET_* to simply FLAG_*
    - Improve documentation
    
    Bug 27365822
    
    Change-Id: I10e85aeaa462a8ae092938c0ccd55d171f02c20a

 .../server/wallpaper/WallpaperManagerService.java  | 44 +++++++++++-----------
 1 file changed, 22 insertions(+), 22 deletions(-)

commit d57d17ca969756e6b9792199e221df475a7537f9
Author: Christopher Tate <ctate@google.com>
Date:   Fri Mar 25 13:41:46 2016 -0700

    Assign a new wallpaper ID when a live wallpaper is set
    
    Bug 27851191
    
    Change-Id: I5fc490e38d80e1b357ef44cdcabbad41c8bd9a93

 .../java/com/android/server/wallpaper/WallpaperManagerService.java  | 6 +++++-
 1 file changed, 5 insertions(+), 1 deletion(-)

commit e409f0e46243e943af2a223c00bc7098dc7e5c88
Author: Christopher Tate <ctate@google.com>
Date:   Mon Mar 21 14:53:15 2016 -0700

    Add API to read the current wallpaper's ID
    
    Also regularize cross-user call handling throughout.
    
    Bug 27743435
    
    Change-Id: Ibc530d0576d657723a613888357a6ea71b482763

 .../server/wallpaper/WallpaperManagerService.java  | 38 +++++++++++++++-------
 1 file changed, 26 insertions(+), 12 deletions(-)

commit 2cdd3f2b62188ae5d688553ac9ddfcf8b5dfa322
Author: Christopher Tate <ctate@google.com>
Date:   Mon Mar 14 17:36:16 2016 -0700

    Don't stomp live wallpapers when tidying up imagery
    
    Tidying up the wallpaper imagery bookkeeping mustn't accidentally
    overwrite any user-selected live wallpaper usage.  Only do the full
    reset when it's needful *and* we expect to need it.
    
    Bug 27537903
    
    Change-Id: Iaacc750a24ef36ee4e4cc436b62055b51b49b235

 .../server/wallpaper/WallpaperManagerService.java  | 28 ++++++++++++++++------
 1 file changed, 21 insertions(+), 7 deletions(-)

commit 41297ff82c94d28fe19a6dd8f6214aee7c0af8e2
Author: Christopher Tate <ctate@google.com>
Date:   Thu Mar 10 16:46:15 2016 -0800

    Once restored, the wallpaper needs to actually draw
    
    We now wait until both the wallpaper imagery and the metadata have
    been restored [if present], and then explicitly regenerate the crop
    from the source based on that.
    
    Bug 27423845
    
    Change-Id: I986efd13b6b73d25b5ab1215af516ccea3a5c609

 .../server/wallpaper/WallpaperManagerService.java  | 22 +++++++++++++---------
 1 file changed, 13 insertions(+), 9 deletions(-)

commit d5896630f6a2f21da107031cab216dc93bdcd851
Author: Jeff Sharkey <jsharkey@android.com>
Date:   Fri Mar 4 16:16:00 2016 -0700

    Move more PM calls to ParceledListSlice.
    
    Since the data returned by these calls can grow unbounded based on
    various GET flags, we need to switch 'em over.
    
    Bug: 27391893
    Change-Id: Ie849ca30dbaaa91158da4c83675657715629a0ee

 .../core/java/com/android/server/wallpaper/WallpaperManagerService.java | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit 79a2457e198cf40b2b80e7fb6bd1656a9d78f246
Author: Christopher Tate <ctate@google.com>
Date:   Wed Mar 2 14:42:44 2016 -0800

    Add API to clear a specific wallpaper
    
    There was previously no public API for clearing the keyguard wallpaper
    versus the system wallpaper, or both.  Now there is.
    
    Bug 27400185
    
    Change-Id: If1789dd430040acdf16d77413c0e4b46bf3789f3

 .../com/android/server/wallpaper/WallpaperManagerService.java     | 8 ++++++++
 1 file changed, 8 insertions(+)

commit db27b844f4605fc968931a5e102f03867898b9a6
Author: Christopher Tate <ctate@google.com>
Date:   Thu Feb 25 14:39:17 2016 -0800

    Tidy up stale lock wallpaper state in set-both operation
    
    Also fix bug that was failing to remember the lock-only wallpaper, and
    along the way make the disk write a single large block operation instead
    of a number of small writes.
    
    Bug 27353079
    
    Change-Id: Ib1351e509af95905dced41e69c6e13dcce839511

 .../server/wallpaper/WallpaperManagerService.java  | 112 ++++++++++++---------
 1 file changed, 67 insertions(+), 45 deletions(-)

commit 7e1693af7f367d7fe7851e46693e4c0067305a50
Author: Joe Onorato <joeo@google.com>
Date:   Mon Feb 1 17:45:03 2016 -0800

    Turn down the logging a little bit.
    
    Change-Id: I922d6f3895d80292fbf613b90e502cad462bb9ef

 .../core/java/com/android/server/wallpaper/WallpaperManagerService.java | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit 8cde0798edc9dc316509078736e5c4b4235c3b3f
Author: Vadim Tryshev <vadimt@google.com>
Date:   Fri Feb 19 17:02:15 2016 -0800

    Add preloading wallpaper to the getWallpaper call.
    
    Otherwise, there is no way to get a wallpaper for
    the user that was never loaded.
    
    Bug: 25185253
    Change-Id: I88df266f6af7ca78ebc30d78e82e1df3ab09a3c5

 .../server/wallpaper/WallpaperManagerService.java      | 18 ++++++++----------
 1 file changed, 8 insertions(+), 10 deletions(-)

commit dcd93cc21a437ae86dc55622a66a948d6870af74
Author: Yorke Lee <yorkelee@google.com>
Date:   Fri Jan 8 14:12:55 2016 -0800

    Add Wallpaper.getBitmapAsUser()
    
    Add a new API to allow a wallpaper bitmap to be obtained for
    a specific user.
    
    Without this API, using only getWallpaperFile(..., userId), one
    can't get a default wallpaper bitmap if the wallpaper is not set.
    
    Bug: 25185253
    Change-Id: Ibe1e9a49d22bee08fd4bed415573c1ee28526aea

 .../core/java/com/android/server/wallpaper/WallpaperManagerService.java  | 1 +
 1 file changed, 1 insertion(+)

commit ea6724a3c3b8917d51d32ebf4fe0eb69828b7054
Author: Christopher Tate <ctate@google.com>
Date:   Thu Feb 18 18:39:19 2016 -0800

    Don't try to register (and invoke!) intentional null callbacks
    
    Change-Id: Iccb9df242a4d6c3aea03be9132f83afb70be0397

 .../java/com/android/server/wallpaper/WallpaperManagerService.java    | 4 +++-
 1 file changed, 3 insertions(+), 1 deletion(-)

commit be132e6ea494023d4b8c37658a34efa8b705dce9
Author: Christopher Tate <ctate@google.com>
Date:   Wed Feb 10 12:59:49 2016 -0800

    Keyguard wallpaper
    
    Clients can now set a lock-only wallpaper that Keyguard can
    observe and choose to draw as appropriate.
    
    Bug 25454162
    
    Change-Id: I3fc30e02919e814b55dfded2a1a36ad9d2e55299

 .../server/wallpaper/WallpaperManagerService.java  | 397 +++++++++++++++------
 1 file changed, 294 insertions(+), 103 deletions(-)

commit f2519814cc7136773a115b770d20cf4c92945952
Author: Oleksandr Peletskyi <peletskyi@google.com>
Date:   Tue Jan 26 20:16:06 2016 +0100

    Added restriction that disallows ability to set wallpaper.
    BUG: 24890474
    
    Change-Id: I424aa80d914e3b6f3f9eba8ccb4802bad6f54907

 .../server/wallpaper/WallpaperManagerService.java  | 43 ++++++++++++++++------
 1 file changed, 31 insertions(+), 12 deletions(-)

commit 1e1e2e013fbd2b77ecf3bb87f58ba9d4849d172a
Author: Christopher Tate <ctate@google.com>
Date:   Mon Jan 25 15:34:36 2016 -0800

    Extract crop hint rect from source wallpaper image
    
    Setting the wallpaper is still synchronous: the caller blocks until any
    backend cropping/manipulation has completed.  There is a timeout (currently
    30 seconds) on that to avoid wedging the caller arbitrarily.
    
    Bug 25454501
    
    Change-Id: Idca2fe1b10e4fa34d6d54865903d9a1b9e305e3c

 .../server/wallpaper/WallpaperManagerService.java  | 198 +++++++++++++++++++--
 1 file changed, 181 insertions(+), 17 deletions(-)

commit ad3c2592a06be34c32f45a9c19429065f0107daa
Author: Christopher Tate <ctate@google.com>
Date:   Wed Jan 20 18:13:17 2016 -0800

    Expanded wallpaper APIs for keyguard and change detection
    
    * There is a nonzero integer ID associated with the currently-set
      wallpaper image, and this changes every time any caller sets
      the wallpaper.  This is to support external change detection.
    
    * The API now permits a caller to independently set imagery as
      the new general system wallpaper or keyguard-specific wallpaper.
      The backing implementation is not yet plumbed through, but the
      API is now in place for clients to safely write code against.
    
    Bug 25454501
    Bug 25454162
    
    Change-Id: I4934f102d5630485bf2870d83664145ae68a3695

 .../server/wallpaper/WallpaperManagerService.java  | 97 +++++++++++++++-------
 1 file changed, 69 insertions(+), 28 deletions(-)

commit 233d94c0df13a7e54f738f442457cebc62294384
Author: Xiaohui Chen <xiaohuic@google.com>
Date:   Thu Jul 30 15:08:00 2015 -0700

    clean up UserHandle.USER_OWNER reference
    
    cleaning up a few in framework/base/services.
    
    Bug: 19913735
    Change-Id: I1af5f24d9b39d6712ad3a665effa6166e32ce3d3

 .../java/com/android/server/wallpaper/WallpaperManagerService.java  | 6 +++---
 1 file changed, 3 insertions(+), 3 deletions(-)

commit 55a302402be3240c9e3289351c01e1cd2e853bc8
Merge: 3b7bc56 ac53194
Author: Xiaohui Chen <xiaohuic@google.com>
Date:   Thu May 14 21:05:35 2015 +0000

    Merge "wallpaper: avoid exception when user is not initialized" into mnc-dev

commit 9e9e2e73c6ec7bece20268196dc89ad0c8bafad4
Author: Wojciech Staszkiewicz <staszkiewicz@google.com>
Date:   Fri May 8 14:58:46 2015 +0100

    Pass charset to XmlPullParser.setInput instead of null
    
    Passing null to XmlPullParser.setInput forces it to do additional
    work, which can be easily avoided if we know the charset beforehand.
    
    bug: b/20849543
    
    Change-Id: Iaff97be9df2d0f99d7af8f19f65934439c9658e2

 .../java/com/android/server/wallpaper/WallpaperManagerService.java   | 5 +++--
 1 file changed, 3 insertions(+), 2 deletions(-)

commit ac531941fb9259057b86c08fb95b82cd67f6d8c0
Author: Xiaohui Chen <xiaohuic@google.com>
Date:   Wed May 13 13:20:52 2015 -0700

    wallpaper: avoid exception when user is not initialized
    
    Bug: 21087887
    Change-Id: I56b9bf08c6e7100589f0df116e9d20f2f77d50e8

 .../server/wallpaper/WallpaperManagerService.java  | 38 ++++++++++++----------
 1 file changed, 20 insertions(+), 18 deletions(-)

commit d69e4c1460017062e7c36be55801cb434ad19d97
Author: Dianne Hackborn <hackbod@google.com>
Date:   Fri Apr 24 09:54:54 2015 -0700

    Update use of procstate for services.
    
    Now that we have a separate foreground service proc state
    (above a sleeping top app), update various system services
    to put their bindings into this state when appropriate.
    
    There are two new bind flags for this -- one that just always
    makes it a foreground service, another that only does it when
    the device is awake (useful for things like the wallpaper).
    
    And with all of that, tweak network policy manager to only
    include apps that are at least foreground service state when
    in power save and device idle modes.  This will allow us to
    further reduce the set of apps that have network access
    (in particular not giving access to the current top app when
    the screen is off), hopefully leading to even better battery
    life.
    
    Change-Id: I91d85a5c5ed64e856149e9a5d94a634a7925ec7f

 .../java/com/android/server/wallpaper/WallpaperManagerService.java     | 3 ++-
 1 file changed, 2 insertions(+), 1 deletion(-)

commit 429796226a8831af63a6303a58329f6b68f7b100
Author: Kenny Guy <kennyguy@google.com>
Date:   Mon Apr 13 18:03:05 2015 +0000

    Add foreground profile changed to user switch observer.
    
    Called when the focused activity changes from one profile
    to another to allow sys ui to vary display based on the
    current profile.
    This reverts commit 735a3f90598be31bca5d551d781280a205a5f27f.
    Resubmitting with fix for build break in another package.
    
    Bug: 19531782
    Change-Id: I98cd2968ade3b1d23feb90d98057f306695d569e

 .../java/com/android/server/wallpaper/WallpaperManagerService.java   | 5 +++++
 1 file changed, 5 insertions(+)

commit 735a3f90598be31bca5d551d781280a205a5f27f
Author: Kenny Guy <kennyguy@google.com>
Date:   Mon Apr 13 16:12:55 2015 +0000

    Revert "Add foreground profile changed to user switch observer."
    
    This reverts commit 99b9030a3ececd9b88e5011c98be0a5b9499c776.
    
    Change-Id: If152904f298ecd9e6fd5d038797d061a5c85eec8

 .../java/com/android/server/wallpaper/WallpaperManagerService.java   | 5 -----
 1 file changed, 5 deletions(-)

commit 99b9030a3ececd9b88e5011c98be0a5b9499c776
Author: Kenny Guy <kennyguy@google.com>
Date:   Wed Apr 1 18:27:34 2015 +0100

    Add foreground profile changed to user switch observer.
    
    Called when the focused activity changes from one profile
    to another to allow sys ui to vary display based on the
    current profile.
    
    Bug: 19531782
    Change-Id: I1f98398f4b37ce77077394546906ef4dff06cd47

 .../java/com/android/server/wallpaper/WallpaperManagerService.java   | 5 +++++
 1 file changed, 5 insertions(+)

commit f3ece36535d4999cf2bfd2175a33da6c3cdf298e
Author: Benjamin Franz <bfranz@google.com>
Date:   Wed Feb 11 10:51:10 2015 +0000

    Block setting wallpapers from managed profiles.
    
    Silently fail when a managed profile app tries to change the
    wallpaper and return default values for getters in that case.
    This is implemented through a new AppOp that is controlled by
    a new user restriction that will be set during provisioning.
    
    Bug: 18725052
    Change-Id: I1601852617e738be86560f054daf3435dd9f5a9f

 .../server/wallpaper/WallpaperManagerService.java  | 67 +++++++++++++++++++---
 1 file changed, 59 insertions(+), 8 deletions(-)

commit a526a1e78c5b02096f596097ece4611a6fcc9e27
Author: Kenny Guy <kennyguy@google.com>
Date:   Wed Jan 21 16:52:01 2015 +0000

    Flush and sync before calling JournaledFile.commit
    
    JournaledFile.commit on ext4 isn't atomic you need
    to flush / sync before closing the file otherwise
    you might end up with an empty file.
    
    Bug: 19091744
    Change-Id: Ia04784084053cc8ce338a13a61dc6d397610a006

 .../core/java/com/android/server/wallpaper/WallpaperManagerService.java | 2 ++
 1 file changed, 2 insertions(+)

commit 52d750c5c24969a6a25f058bf6c724016f319e2b
Author: Amith Yamasani <yamasani@google.com>
Date:   Wed Dec 10 14:09:05 2014 -0800

    Check which file changed before requesting backup
    
    Make sure that the changed file is one of the wallpaper files
    before requesting backup or informing listeners.
    
    Bug: 18694053
    Change-Id: Iaa8fe9d3c97634b3cc6a9ccd67c36cf394d17ca0

 .../server/wallpaper/WallpaperManagerService.java        | 16 ++++++++++------
 1 file changed, 10 insertions(+), 6 deletions(-)

commit 5a589430b3be99a6fdaf03e621162d08c8b1ad8a
Author: Filip Gruszczynski <gruszczy@google.com>
Date:   Tue Oct 14 12:06:06 2014 -0700

    Prevent index out of bounds when using String.substring.
    
    Change-Id: Ib30f9646e9895b6a2f065a0b2e51cf53e821b087

 .../java/com/android/server/wallpaper/WallpaperManagerService.java   | 5 +++--
 1 file changed, 3 insertions(+), 2 deletions(-)

commit 5dcc3aca42255616c48388e012d1b43b2ac450f9
Author: Filip Gruszczynski <gruszczy@google.com>
Date:   Mon Oct 13 15:51:39 2014 -0700

    Log wallpaper crashes.
    
    Change-Id: I40d9876dad2a8ec98135e3df127950dc3797040b

 .../java/com/android/server/wallpaper/WallpaperManagerService.java  | 6 ++++++
 1 file changed, 6 insertions(+)

commit da058e260d1c5ac8039034b38db8c697c16017bb
Author: Christopher Tate <ctate@google.com>
Date:   Wed Oct 8 14:51:09 2014 -0700

    Also monitor MOVED_TO events for wallpaper updates
    
    Restore uses moveTo(), not open/write/close, so we need
    to watch for that as well.  Now the wallpaper service sees
    and regenerates the wallpaper image immediately upon restore.
    
    Bug 17909454
    
    Change-Id: I0db224c3d507bdc40399d49bb4bea01899f76ad1

 .../com/android/server/wallpaper/WallpaperManagerService.java     | 8 +++++---
 1 file changed, 5 insertions(+), 3 deletions(-)

commit 90f86baebdb53f2edb5e2bd2b3f6f43bc802f9a9
Author: Christopher Tate <ctate@google.com>
Date:   Thu Sep 11 12:37:19 2014 -0700

    Write new wallpaper files from scratch...
    
    ...rather than overwriting the existing wallpaper bitmap file "in
    place."  If the new bitmap is smaller than the previous one, we wind
    up with the previous image's contents as spurious trailing file
    contents.  Also, it means that if any wallpaper image is particularly
    large on disk, then we'll forever be backing up that high-water-mark
    amount of data every time the wallpaper is changed.
    
    The fix is to open the "write the new bitmap to disk" fd with
    MODE_TRUNCATE.
    
    Bug 17285333
    
    Change-Id: I3d8708d72e316834b7ecec20386153a703efddd9

 .../core/java/com/android/server/wallpaper/WallpaperManagerService.java | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit 067e5f68b9216b233df1c6529db182ff9b2887ab
Author: Dianne Hackborn <hackbod@google.com>
Date:   Sun Sep 7 23:14:30 2014 -0700

    Add new wallpaper features for insets and offsets.
    
    Issue #17394151: WallpaperService / Engines need to get notified
    of WindowInsets
    
    Issue #17394203 Wallpapers need a system API to be shifted in order
    to support burn in protection
    
    Adds a new API on WallpaperManager to set additional offsets to
    make wallpapers extend beyond the display size.
    
    Insets are now reported to wallpapers, to use as they may.  This
    includes information about the above offsets, so they can place
    their content within the visible area.  And to help with this, also
    expose the stable offsets APIs in WindowInsets which is also very
    useful information for the wallpaper.
    
    Another new API on WallpaperManager to set a raw offset to apply
    to the wallpaper window, forcing it to move on the screen regardless
    of what the wallpaper is drawing.
    
    Fix wallpapers when used with overscan enabled, so they still extend
    out across the entire screen.  Conveniently, the above new window
    insets information is very useful for this case as well!
    
    And a new wallpaper test app for all this stuff.
    
    Change-Id: I287ee36581283dd34607609fcd3170d99d120d8e

 .../server/wallpaper/WallpaperManagerService.java  | 86 ++++++++++++++++++++--
 1 file changed, 78 insertions(+), 8 deletions(-)

commit ecd827ac0d7a62ea81b8123120be1fe6424c2338
Author: Christopher Tate <ctate@google.com>
Date:   Fri Sep 5 17:42:34 2014 -0700

    Fix binder identity use when clearing wallpapers
    
    Don't restore it too soon, because the rarely-needed fallback path
    will need to be executed as system, too.
    
    Bug 17394246
    
    Change-Id: Ic5e662d4eae331b016fc91ffd08647bd8d4d6ff3

 .../server/wallpaper/WallpaperManagerService.java  | 46 +++++++++++-----------
 1 file changed, 24 insertions(+), 22 deletions(-)

commit 29c30161281341ec851817534c24f17f91625e52
Author: Justin Koh <justinkoh@google.com>
Date:   Fri Sep 5 17:10:10 2014 -0700

    Make the image wallpaper component overlayable
    
    This is necessary for devices that want to have wallpaper but don't have
    SystemUi.
    
    Bug: 17394246
    Change-Id: I75c2a3a2120fd6600274d44059b3f85569b9a187

 .../server/wallpaper/WallpaperManagerService.java  | 29 ++++++++++++----------
 1 file changed, 16 insertions(+), 13 deletions(-)

commit 28f0877073e0ebc59f1eeeb6e0d54b614b9d3fa5
Author: Jeff Sharkey <jsharkey@android.com>
Date:   Wed Apr 16 09:41:58 2014 -0700

    Allow custom wallpaper and boot animation.
    
    Add new "ro.config.wallpaper" and "ro.config.wallpaper_component"
    properties which may be defined outside of the bundled framework
    resources.  Falls back to bundled resources when properties are
    undefined.
    
    Also look for boot animation under OEM partition.
    
    Bug: 13340779
    Change-Id: Ibdc9935dbdaae3319bf63b40573de0503d82ae67

 .../com/android/server/wallpaper/WallpaperManagerService.java    | 9 ++-------
 1 file changed, 2 insertions(+), 7 deletions(-)

commit ebebadb56d6b3eab6e11dae9d48d639f0af4946d
Author: Selim Cinek <cinek@google.com>
Date:   Wed Mar 5 22:17:26 2014 +0100

    Fixed a bug where the current wallpaper could be reset on an update
    
    There is a race condition which caused the wallpaper to be reset
    on an app update since the broadcast notifying the service is async.
    This CL corrects this behaviour by enforcing that we only reset the
    wallpaper if its service was shut down twice in a certain timeframe.
    Before, the condition falsely was checking whether the service was
    started in the last couple of millis instead of killed.
    
    Bug: 11901821
    Change-Id: Icfbc7a5df63215079a83805c5187a3182b192757

 .../server/wallpaper/WallpaperManagerService.java    | 20 ++++++++++++++------
 1 file changed, 14 insertions(+), 6 deletions(-)

commit 9158825f9c41869689d6b1786d7c7aa8bdd524ce
Author: Amith Yamasani <yamasani@google.com>
Date:   Fri Nov 22 08:25:26 2013 -0800

    Move some system services to separate directories
    
    Refactored the directory structure so that services can be optionally
    excluded. This is step 1. Will be followed by another change that makes
    it possible to remove services from the build.
    
    Change-Id: Ideacedfd34b5e213217ad3ff4ebb21c4a8e73f85

 .../server/wallpaper/WallpaperManagerService.java  | 1353 ++++++++++++++++++++
 1 file changed, 1353 insertions(+)
