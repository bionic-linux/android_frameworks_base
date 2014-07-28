/*
 * Copyright (C) 2014 The Android Open Source Project
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
#include <dlfcn.h>
#include <stdio.h>
#include <NativeBridge.h>
#include <cutils/properties.h>


namespace android {

// Default library name for native-brdige
static const char* const kDefaultNativeBridge = "libnativebridge.so";

// Property that defines the library name of native-bridge
static const char* const kPropNativeBridge = "persist.native.bridge";

// Property that enables native-bridge
static const char* const kPropEnableNativeBridge = "persist.enable.native.bridge";

// The symbol name exposed by native-bridge with the type of NativeBridgeCallbacks
static const char* const kNativeBridgeInterfaceSymbol = "NativeBridgeItf";

// AndroidRuntime callbacks to native-bridge. Reserved.
struct NativeBridgeRuntimeCallbacks;

// Native-bridge interfaces to native-activity
struct NativeBridgeCallbacks {
    // Initialize native-bridge. Native-bridge's internal implementation must ensure MT safety
    // and that native-bridge is initialized only once. OK to call this interface for already
    // initialized native-bridge.
    //
    // Parameters:
    //   rt_cbs [IN] the pointer to NativeBridgeRuntimeCallbacks callbacks
    // Returns:
    //   TRUE for initialization success, FALSE for initialization fail.
    bool (*initialize)(NativeBridgeRuntimeCallbacks* rt_cbs);
    // Load a shared library that is supported by the native-bridge.
    //
    // Parameters:
    //   libpath [IN] path to the shared library
    //   flag [IN] the stardard RTLD_XXX defined in bionic dlfcn.h
    // Returns:
    //   The opaque handle of shared library if sucessful, otherwise NULL
    void* (*loadLibrary)(const char* libpath, int flag);
    // Get a native-bridge trampoline for specified native method. The trampoline has same
    // sigature as the native method.
    //
    // Parameters:
    //   handle [IN] the handle returned from loadLibrary
    //   shorty [IN] short descriptor of native method
    //   len [IN] length of shorty
    // Returns:
    //   address of trampoline of successful, otherwise NULL
    void* (*getTrampoline)(void* handle, const char* name, const char* shorty, uint32_t len);
    // Check whether native library is valid and is for an ABI that is supported by native-bridge.
    //
    // Parameters:
    //   libpath [IN] path to the shared library
    // Returns:
    //   TRUE if library is supported by native-bridge, FALSE otherwise
    bool (*isSupported)(const char* libpath);
};

bool
NativeBridge::Initialize()
{
    Mutex::Autolock l(lock_);
    if (!initialized_) {
        const char* libnb_path = kDefaultNativeBridge;
        char propBuf[PROP_VALUE_MAX];
        property_get(kPropEnableNativeBridge, propBuf, "false");
        if (strcmp(propBuf, "true") != 0)
            return false;

        // If prop persist.native.bridge set, overwrite the default name
        int name_len = property_get(kPropNativeBridge, propBuf, kDefaultNativeBridge);
        if (name_len > 0)
            libnb_path = propBuf;

        void* handle = dlopen(libnb_path, RTLD_LAZY);
        if (handle == NULL)
            return false;

        callbacks_ = reinterpret_cast<NativeBridgeCallbacks*>(dlsym(handle, kNativeBridgeInterfaceSymbol));
        if (callbacks_ == NULL) {
            dlclose(handle);
            return false;
        }

        initialized_ = callbacks_->initialize(NULL);
    }
    return initialized_;
}

void*
NativeBridge::LoadLibrary(const char* libpath, int flag)
{
    if (Initialize())
        return callbacks_->loadLibrary(libpath, flag);
    return NULL;
}

void*
NativeBridge::GetTrampoline(void* handle, const char* name, const char* shorty, uint32_t len)
{
    if (Initialize())
        return callbacks_->getTrampoline(handle, name, shorty, len);
    return NULL;

}

bool
NativeBridge::IsSupported(const char* libpath)
{
    if (Initialize())
        return callbacks_->isSupported(libpath);
    return false;
}

bool NativeBridge::initialized_ = false;
NativeBridgeCallbacks* NativeBridge::callbacks_ = NULL;
Mutex NativeBridge::lock_;

};  // namespace android
