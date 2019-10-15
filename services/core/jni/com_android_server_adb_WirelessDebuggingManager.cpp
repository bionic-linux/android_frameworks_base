/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "WirelessDebuggingManager-JNI"

#define LOG_NDEBUG 0

#include <algorithm>
#include <condition_variable>
#include <mutex>
#include <optional>
#include <random>
#include <string>
#include <vector>

#include <adbwifi/crypto/device_identifier.h>
#include <adbwifi/crypto/key_store.h>
#include <adbwifi/pairing/pairing_server.h>
#include <android-base/properties.h>
#include <utils/Log.h>

#include <nativehelper/JNIHelp.h>
#include "jni.h"

namespace android {

using adbwifi::crypto::DeviceIdentifier;
using adbwifi::crypto::KeyStore;
using adbwifi::pairing::PairingServer;

// ----------------------------------------------------------------------------
namespace {

template <class T, class N>
class JSmartWrapper {
public:
    JSmartWrapper(JNIEnv* env, T* jData) :
        mEnv(env),
        mJData(jData) {
    }

    virtual ~JSmartWrapper() = default;

    const N* data() const {
        return mRawData;
    }

    jsize length() const {
        return mLength;
    }

protected:
    N* mRawData = nullptr;
    JNIEnv* mEnv = nullptr;
    T* mJData = nullptr;
    jsize mLength = 0;
};  // JSmartWrapper

class JStringUTFWrapper : public JSmartWrapper<jstring, const char> {
public:
    explicit JStringUTFWrapper(JNIEnv* env, jstring* str)
        : JSmartWrapper(env, str) {
        mRawData = env->GetStringUTFChars(*str, NULL);
        mLength = env->GetStringUTFLength(*str);
    }

    virtual ~JStringUTFWrapper() {
        if (data()) {
            mEnv->ReleaseStringUTFChars(*mJData, mRawData);
        }
    }
}; // JStringUTFWrapper

class JByteArrayWrapper : public JSmartWrapper<jbyteArray, jbyte> {
public:
    explicit JByteArrayWrapper(JNIEnv* env, jbyteArray* arr)
        : JSmartWrapper(env, arr) {
        mRawData = env->GetByteArrayElements(*arr, NULL);
        mLength = env->GetArrayLength(*arr);
    }

    virtual ~JByteArrayWrapper() {
        if (data()) {
            mEnv->ReleaseByteArrayElements(*mJData, mRawData, JNI_ABORT);
        }
    }
}; // JByteArrayWrapper

std::string randomAlphaNumString(size_t len) {
    std::string ret;
    std::random_device rd;
    std::mt19937 mt(rd());
    // Generate values starting with zero and then up to enough to cover numeric
    // digits, small letters and capital letters (26 each).
    std::uniform_int_distribution<uint8_t> dist(0, 61);
    for (size_t i = 0; i < len; ++i) {
        uint8_t val = dist(mt);
        if (val < 10) {
            ret += '0' + val;
        } else if (val < 36) {
            ret += 'A' + (val - 10);
        } else {
            ret += 'a' + (val - 36);
        }
    }
    return ret;
}

std::string generateDeviceGuid() {
    // The format is adb-<serial_no>-<six-random-alphanum>
    std::string guid = "adb-";

    std::string serial = android::base::GetProperty("ro.serialno", "");
    if (serial.empty()) {
        // Generate 16-bytes of random alphanum string
        serial = randomAlphaNumString(16);
    }
    guid += serial + '-';
    // Random six-char suffix
    guid += randomAlphaNumString(6);
    return guid;
}

const char kKeystorePath[] = "/data/misc/adb";

std::unique_ptr<PairingServer> sServer;
}  // namespace

static jboolean native_keystore_init(JNIEnv* env,
                                     jclass /* clazz */) {
    // Create the device guid if one doesn't exist yet.
    DeviceIdentifier device_id(kKeystorePath);
    if (device_id.getUniqueDeviceId().empty()) {
        device_id.resetUniqueDeviceId(generateDeviceGuid());
    }
    // Create a KeyStore object to generate the device id and keys if none
    // exists.
    auto key_store = KeyStore::create(kKeystorePath);
    if (key_store == nullptr) {
        ALOGE("Unable to create adbwifi keystore");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

static jboolean native_keystore_remove_guid(JNIEnv* env,
                                            jclass /* clazz */,
                                            jstring guid) {
    auto key_store = KeyStore::create(kKeystorePath);
    if (key_store == nullptr) {
        ALOGE("Unable to create adbwifi keystore");
        return JNI_FALSE;
    }
    JStringUTFWrapper guidWrapper(env, &guid);
    return key_store->removePeerInfo(std::string(guidWrapper.data(), guidWrapper.length())) ? JNI_TRUE
                                                                                            : JNI_FALSE;
}

static std::vector<uint8_t> stringToUint8(const std::string& str) {
    auto* p8 = reinterpret_cast<const uint8_t*>(str.data());
    return std::vector<uint8_t>(p8, p8 + str.length());
}

static jboolean native_pairing_start(JNIEnv* env,
                                     jobject thiz,
                                     jstring password) {
    // Get the system certificate and private key to initialize the pairing
    // server.
    auto key_store = KeyStore::create(kKeystorePath);
    if (key_store == nullptr) {
        ALOGE("Unable to create adbwifi keystore for pairing");
        return JNI_FALSE;
    }

    // Setup the arguments to pass to the pairing server
    auto opt_device_info = key_store->getDeviceInfo();
    if (!opt_device_info.has_value()) {
        ALOGE("Unable to retrieve device information for pairing");
        return JNI_FALSE;
    }
    auto [guid, name, cert, priv_key] = *opt_device_info;

    adbwifi::pairing::PeerInfo system_info = {};
    memcpy(system_info.guid, guid.data(), std::min(adbwifi::pairing::kPeerGuidLength, guid.size()));
    memcpy(system_info.name, name.data(), std::min(adbwifi::pairing::kPeerNameLength, name.size()));

    JStringUTFWrapper passwordWrapper(env, &password);
    auto pswd8 = stringToUint8(std::string(passwordWrapper.data(), passwordWrapper.length()));
    auto cert8 = stringToUint8(cert);
    auto priv8 = stringToUint8(priv_key);

    // Create the pairing server
    sServer = PairingServer::create(pswd8,
                                    system_info,
                                    cert8,
                                    priv8,
                                    adbwifi::pairing::kDefaultPairingPort);
    if (sServer == nullptr) {
        ALOGE("Unable to create pairing server");
        return JNI_FALSE;
    }

    // This is a blocking call. It will wait until we either get a signal to
    // stop, or we get a valid pairing.
    std::mutex mutex;
    std::condition_variable cv;
    std::optional<bool> got_pairing;
    std::unique_lock<std::mutex> lock(mutex);
    std::string peer_name, peer_guid;
    auto callback = [&](const adbwifi::pairing::PeerInfo* peer_info,
                        const adbwifi::pairing::PairingConnection::Data* cert,
                        void* /* opaque */) {
        if (peer_info != nullptr && cert != nullptr) {
            // Save in the keystore
            auto key_info = std::make_tuple(std::string(peer_info->guid),
                                            std::string(peer_info->name),
                                            std::string(reinterpret_cast<const char*>(cert->data())));
            if (!key_store->storePeerInfo(std::move(key_info))) {
                ALOGE("Unable to store peer information into the keystore.");
                {
                    std::lock_guard<std::mutex> lock(mutex);
                    got_pairing = false;
                }
                cv.notify_one();
                return;
            }

            // We could set the java strings directly, but you would need to
            // setup JNI AttachCurrentThread/DetachCurrentThread to set it.
            peer_name = peer_info->name;
            peer_guid = peer_info->guid;
            {
                std::lock_guard<std::mutex> lock(mutex);
                got_pairing = true;
            }
            cv.notify_one();
            return;
        }
        {
            std::lock_guard<std::mutex> lock(mutex);
            got_pairing = false;
        }
        cv.notify_one();
    };
    if (!sServer->start(callback, nullptr)) {
        ALOGE("Unable to start pairing server");
        return JNI_FALSE;
    }

    ALOGI("Waiting for pairing server to complete");
    cv.wait(lock, [&]() { return got_pairing.has_value(); });

    // Write to PairingThread's member variables
    jclass clazz = env->GetObjectClass(thiz);
    jfieldID mGuid = env->GetFieldID(clazz, "mGuid", "Ljava/lang/String;");
    jfieldID mName = env->GetFieldID(clazz, "mName", "Ljava/lang/String;");
    jstring jguid = env->NewStringUTF(peer_guid.c_str());
    jstring jname = env->NewStringUTF(peer_name.c_str());
    env->SetObjectField(thiz, mGuid, jguid);
    env->SetObjectField(thiz, mName, jname);
    ALOGI("Pairing server completed");
    return (*got_pairing ? JNI_TRUE : JNI_FALSE);
}

static void native_pairing_cancel(JNIEnv* /* env */,
                                  jclass /* clazz */) {
    if (sServer != nullptr) {
        sServer.reset();
    }
}

// ----------------------------------------------------------------------------

static const JNINativeMethod gWirelessDebuggingManagerMethods[] = {
    /* name, signature, funcPtr */
    { "native_keystore_init", "()Z",
            (void*) native_keystore_init },
    { "native_keystore_remove_guid", "(Ljava/lang/String;)Z",
            (void*) native_keystore_remove_guid },
};
static const JNINativeMethod gPairingThreadMethods[] = {
    /* name, signature, funcPtr */
    { "native_pairing_start", "(Ljava/lang/String;)Z",
            (void*) native_pairing_start },
    { "native_pairing_cancel", "()V",
            (void*) native_pairing_cancel },
};

int register_android_server_WirelessDebuggingManager(JNIEnv* env) {
    int res = jniRegisterNativeMethods(env, "com/android/server/adb/WirelessDebuggingManager",
            gWirelessDebuggingManagerMethods, NELEM(gWirelessDebuggingManagerMethods));
    (void) res;  // Faked use when LOG_NDEBUG.
    LOG_FATAL_IF(res < 0, "Unable to register native methods.");

    res = jniRegisterNativeMethods(env, "com/android/server/adb/WirelessDebuggingManager$PairingThread",
            gPairingThreadMethods, NELEM(gPairingThreadMethods));
    (void) res;  // Faked use when LOG_NDEBUG.
    LOG_FATAL_IF(res < 0, "Unable to register native methods.");
    return 0;
}

} /* namespace android */
