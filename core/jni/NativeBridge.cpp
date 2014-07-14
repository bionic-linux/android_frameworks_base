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

bool      NativeBridge::initialized_ = false;
nb_itf_t* NativeBridge::nb_itf_ = NULL;
Mutex     NativeBridge::lock_;

bool
NativeBridge::init()
{
    if (initialized_) {
        return true;
    } else {
        Mutex::Autolock l(lock_);
        if (!initialized_) {
            const char* libnb_path = DEFAULT_NATIVE_BRIDGE;
            char propBuf[PROP_VALUE_MAX];
            property_get(PROP_ENABLE_NAIVE_BRIDGE, propBuf, "false");
            if (strcmp(propBuf, "true") != 0)
                return false;

            // If prop persist.native.bridge set, overwrite the default name
            int name_len = property_get(PROP_NATIVE_BRIDGE, propBuf, DEFAULT_NATIVE_BRIDGE);
            if (name_len > 0)
                libnb_path = propBuf;

            void* handle = dlopen(libnb_path, RTLD_LAZY);
            if (handle == NULL)
                return false;

            nb_itf_t* nb_itf = (nb_itf_t*)dlsym(handle, NATIVE_BRIDGE_ITF);
            if (nb_itf == NULL) {
                dlclose(handle);
                return false;
            }
            nb_itf_ = nb_itf;

            nb_itf_->initialize(NULL);
            initialized_ = true;
        }
    }
    return initialized_;
}

void*
NativeBridge::loadLibrary(const char* libpath, int flag)
{
    if (init())
        return nb_itf_->loadLibrary(libpath, flag);
    return NULL;
}

void*
NativeBridge::getTrampoline(void* handle, const char* name, const char* shorty, uint32_t len)
{
    if (init())
        return nb_itf_->getTrampoline(handle, name, shorty, len);
    return NULL;

}

bool
NativeBridge::isSupported(const char* libpath)
{
    if (init())
        return nb_itf_->isSupported(libpath);
    return false;
}

};  // namespace android
