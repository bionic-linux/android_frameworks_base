/* frameworks/base/include/utils/FontEngineManager.cpp
**
** Copyright (c) 1989-2010, Bitstream Inc. and others.  All Rights
** Reserved.
**
** THIS SOFTWARE IS PROVIDED BY BITSTREAM INC. "AS IS" AND ANY EXPRESS
** OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
** WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
** DSICLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR
** ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL EXPLEMPLARY OR CONSEQUENTIAL
** DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
** OR SERVICES, LOSS OF USE, DATA OR PROFITS, OR BUSINESS INTERRUPTION)
** HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
** STRICT LIABILITY OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
** ANY WAY OUR OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
** OF SUCH DAMAGE.
*/

#include <utils/FontEngineManager.h>

#include <dlfcn.h>
#include <sys/types.h>
#include <dirent.h>
#include <assert.h>

/* #define FEM_ENABLE_LOG */

#ifdef FEM_ENABLE_LOG
/* fprintf, FILE */
#include <stdio.h>

static FILE * fplog = NULL;
static int logIndex = 0;

#define FEM_STARTLOG fplog = fopen("/data/femlog.txt", "a");
#define FEM_LOG(__s, ...) \
        FEM_STARTLOG \
        fprintf(fplog, /*"[%d]"*/__s, /*logIndex++,*/ __VA_ARGS__); \
        FEM_ENDLOG
#define FEM_SLOG(__s) \
        FEM_STARTLOG \
        fprintf(fplog, /*"[%d]"*/__s/*, logIndex++*/); \
        FEM_ENDLOG
#define FEM_ENDLOG fclose(fplog);
#else
#define FEM_STARTLOG
#define FEM_LOG(__s, ...)
#define FEM_SLOG(__s)
#define FEM_ENDLOG
#endif /* FEM_ENABLE_LOG */

#define MAX_PATH_LEN 1024

/* Font engine libraries are decidedly in the system partition. */
#define ANDROID_FONT_ENGINE_PATH "/system/lib/fontengines/"

/* Entry point to font engine plugin */
#define GET_FONT_ENGINE_INSTANCE "getFontEngineInstance"

FontEngineManager* FontEngineManager::pFEMInst = NULL;
typedef int (*direntAlphaSort)(const dirent**, const dirent**);

static int dummyMethod(const struct dirent *unused)
{
	return 1;
}/* end dummyMethod */

FontEngineManager::FontEngineManager()
    : engineCount(0), pFontEngineList(NULL), pFontEngineInfoArr(NULL)
{
    const char*      path = ANDROID_FONT_ENGINE_PATH;
    struct dirent**  eps;
    int              numEntries;

    FEM_SLOG("FontEngineManager::FontEngineManager\n");

    numEntries = scandir(path, &eps, dummyMethod, (direntAlphaSort)alphasort);
    if (numEntries >= 0) {
        char  filePath[MAX_PATH_LEN];
        int   length, index;

        length = 0;
        while (path[length]) {
            filePath[length] = path[length];
            length++;
        }

        assert(length < MAX_PATH_LEN);

        pFontEngineInfoArr = (FontEngineInfoArrPtr)calloc(sizeof(FontEngineInfoPtr), numEntries + 1);
        assert(pFontEngineInfoArr);

        for (index = 0; index < numEntries; index ++) {
            int i = length;
            int j = 0;

            while (eps[index]->d_name[j]) {
                filePath[i] = eps[index]->d_name[j];
                i++;
                j++;
            }

            assert(i < MAX_PATH_LEN);
            i = i - 3;
            if ( filePath[i] == '.' && filePath[i + 1] == 's' && filePath[i + 2] == 'o' ) {
                void*                      handle = NULL;
                const char*                entryMethodName = GET_FONT_ENGINE_INSTANCE;
                getFontEngineInstanceType  getFontEngineInstancePtr = NULL;
                FontEngine*                inst = NULL;

                FEM_LOG("FontEngineManager::FontEngineManager, filePath : %s, engineCount : %d\n", filePath, engineCount);

                handle = dlopen(filePath, RTLD_LAZY);
                getFontEngineInstancePtr = (getFontEngineInstanceType)dlsym(handle, entryMethodName);
                inst = getFontEngineInstancePtr();
                if (inst) {
                    FontEngineNode*  node = (FontEngineNode *)malloc(sizeof(FontEngineNode));
                    node->next = this->pFontEngineList;
                    node->inst = inst;
                    this->pFontEngineList = node;

                    pFontEngineInfoArr[engineCount] = (FontEngineInfoPtr)malloc(sizeof(FontEngineInfo));
                    assert(pFontEngineInfoArr[engineCount]);
                    pFontEngineInfoArr[engineCount]->name = strdup(inst->getName());

                    engineCount++;
                    FEM_LOG("FontEngineManager::FontEngineManager, successfully loaded %s font engine, engineCount : %d\n", filePath, engineCount);
                }/* end if */
            }/* end if */
        }/* end for */
    }/* end if */

    FEM_SLOG("FontEngineManager::FontEngineManager, returning...\n");
}/* end method constructor */

FontEngineManager::~FontEngineManager()
{
    register FontEngineInfoArrPtr  pTempFontEngineInfoArr = this->pFontEngineInfoArr;
    register FontEngineInfoPtr     pFontEngineInfo = *pTempFontEngineInfoArr;

    register FontEngineNode*       node = this->pFontEngineList;
    register FontEngineNode*       tempNode = NULL;

    while (pFontEngineInfo) {
        free((void*)pFontEngineInfo->name);
        free(pFontEngineInfo);

        pTempFontEngineInfoArr++;
        pFontEngineInfo = *pTempFontEngineInfoArr;
    }/* end while */

    free(pFontEngineInfoArr);

    while (node) {
        tempNode = node;
        node = node->next;
        free(tempNode);
    }/* end while */
}/* end method destructor */

/* Returns a singleton instance to a font engine manager. */
FontEngineManager& FontEngineManager::getInstance()
{
    if (!pFEMInst)
    {
      FEM_SLOG("FontEngineManager::getInstance\n");
      pFEMInst = new FontEngineManager();
    }

    return *pFEMInst;
}/* end method getInstance */

/*
   FontEngine list is traversed and a request for font scaler is performed
   against a font engine. The API returns immediately if the font scaler
   is successfully created; a request for font scaler creation is made
   to the next font engine in the list otherwise.
*/
FontScaler* FontEngineManager::createFontScalerContext(const FontScalerInfo& desc)
{
    register FontEngineNode*  node = this->pFontEngineList;
    FontScaler*  pFontScalerContext = NULL;

    FEM_SLOG("FontEngineManager::createFontScalerContext\n");

    while (node != NULL) {
        pFontScalerContext = node->inst->createFontScalerContext(desc);
        if (pFontScalerContext) {
            FEM_SLOG("FontEngineManager::createFontScalerContext, successfully created font scaler\n");
            return pFontScalerContext;
        }/* end if */
        node = node->next;
    }/* end while */

    FEM_SLOG("FontEngineManager::createFontScalerContext, returning NULL\n");
    return NULL;
}/* end method createFontScalerContext */

FontEngine* FontEngineManager::getFontEngine(const char name[])
{
    register FontEngineNode*  node = this->pFontEngineList;

    while (node != NULL) {
        if ( ! strcmp(name, node->inst->getName()) ) {
            return node->inst;
        }/* end if */

        node = node->next;
    }/* end while */

    return NULL;
}/* end method getFontEngine */

size_t FontEngineManager::getFontNameAndStyle(const char path[], char name[], size_t length, FontStyle* style)
{
    register FontEngineNode*  node = this->pFontEngineList;
    size_t count;

    while (node != NULL) {
        count = node->inst->getFontNameAndStyle(path, name, length, style);

        if (count) {
            return count;
        }/* end if */

        node = node->next;
    }/* end while */

    return 0;
}/* end method getFontNameAndStyle */

size_t FontEngineManager::getFontNameAndStyle(const void* buffer, const uint32_t bufferLength, char name[], size_t length, FontStyle*  style)
{
    register FontEngineNode*  node = this->pFontEngineList;
    size_t count;

    while (node != NULL) {
        count = node->inst->getFontNameAndStyle(buffer, bufferLength, name, length, style);

        if (count) {
            return count;
        }/* end if */

        node = node->next;
    }/* end while */

    return 0;
}/* end method getFontNameAndStyle */

bool FontEngineManager::isFontSupported(const char path[], bool isLoad)
{
    register FontEngineNode*  node = this->pFontEngineList;

    while (node != NULL) {
        if (node->inst->isFontSupported(path, isLoad)) {
            return true;
        }/* end if */

        node = node->next;
    }/* end while */

    return false;
}

bool FontEngineManager::isFontSupported(const void* buffer, const uint32_t bufferLength)
{
    register FontEngineNode*  node = this->pFontEngineList;

    while (node != NULL) {
        if (node->inst->isFontSupported(buffer, bufferLength)) {
            return true;
        }/* end if */

        node = node->next;
    }/* end while */

    return false;
}

