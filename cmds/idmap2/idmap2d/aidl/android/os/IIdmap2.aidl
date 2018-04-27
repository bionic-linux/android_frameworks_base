package android.os;

/**
 * @hide
 */
interface IIdmap2 {
  @utf8InCpp String getIdmapPath(@utf8InCpp String overlayApkPath, int userId);
  boolean removeIdmap(@utf8InCpp String overlayApkPath, int userId);
  /* FIXME: consider changing to FileDescriptors instead: idmap2 would still
   * need permission to open apk_data_file objects, but idmap2d can get away
   * with system_server opening the files on its behalf */
  @nullable @utf8InCpp String createIdmap(@utf8InCpp String targetApkPath,
                                          @utf8InCpp String overlayApkPath, int userId);
}
