/* frameworks/base/include/utils/FontEngineManager.h
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
#ifndef __FONTENGINEMANAGER_HEADER__
#define __FONTENGINEMANAGER_HEADER__

#ifdef __cplusplus
extern "C" {
#endif
#include <stdint.h>
#include <stdlib.h>

    unsigned long streamRead(void*           stream,
                                 unsigned long   offset,
                                 unsigned char*  buffer,
                                 unsigned long   count );
    void streamClose(void*  stream);
#ifdef __cplusplus
}/* end extern "C" */
#endif

#ifdef FEM_UNUSED
#undef FEM_UNUSED
#endif
#define FEM_UNUSED(x) ((void)(x))

class FontEngine;

/* 32 bit signed integer used to represent fractions values with 16 bits to the right of the decimal point */
typedef int32_t FEM16Dot16;
#define FEMOne16Dot16          (1 << 16)

/* 32 bit signed integer used to represent fractions values with 6 bits to the right of the decimal point */
typedef int32_t FEM26Dot6;
#define FEMOne26Dot6           (1 << 6)

typedef FontEngine* (*getFontEngineInstanceType)();
typedef void (*releaseFontEngineInstanceType)(FontEngine*);

/** Style specifies the intrinsic style attributes of a given typeface */
typedef enum
{
    STYLE_NORMAL     = 0,
    STYLE_BOLD       = 0x1,
    STYLE_ITALIC     = 0x2,
    STYLE_BOLDITALIC = 0x3
}FontStyle;

typedef enum
{
    ALIAS_MONOCHROME = 0,  /* 1 bit per pixel */
    ALIAS_GRAYSCALE  = 1,  /* 8 bit per pixel */
    ALIAS_LCD_H      = 2,  /* 4 bytes per pixel : a/r/g/b */
    ALIAS_LCD_V      = 3   /* 4 bytes per pixel : a/r/g/b */
}AliasMode;

typedef enum
{
    HINTING_NONE   = 0,
    HINTING_LIGHT  = 1,
    HINTING_NORMAL = 2,
    HINTING_FULL   = 3
}Hinting;

typedef enum
{
    CAN_RENDER_MONO   = 0,
    CAN_RENDER_GRAY   = 0x1,
    CAN_RENDER_LCD_H  = 0x2,
    CAN_RENDER_LCD_V  = 0x4,
    CAN_RENDER_LCD    = 0x6
} EngineCapability;

/** \struct FontMetrics

    This struct provides font metrics information.
*/
struct FontMetrics
{
    FEM16Dot16    fTop;           /* The greatest distance above the baseline for any glyph (will be <= 0) */
    FEM16Dot16    fAscent;        /* The recommended distance above the baseline (will be <= 0) */
    FEM16Dot16    fDescent;       /* The recommended distance below the baseline (will be >= 0) */
    FEM16Dot16    fBottom;        /* The greatest distance below the baseline for any glyph (will be >= 0) */
    FEM16Dot16    fLeading;       /* The recommended distance to add between lines of text (will be >= 0) */
    FEM16Dot16    fAvgCharWidth;  /* the average character width (>= 0) */
    FEM16Dot16    fXMin;          /* The minimum bounding box x value for all glyphs */
    FEM16Dot16    fXMax;          /* The maximum bounding box x value for all glyphs */
    FEM16Dot16    fXHeight;       /* the height of an 'x' in px, or 0 if no 'x' in face */
};/* end class FontMetrics */

/** \class GlyphMetrics

    This class represents infomation for a single glyph. A glyph is the visual
    representation of one or more characters. Many different glyphs can be
    used to represent a single character or combination of characters.

    Metrics available through GlyphMetrics are the components of the advance,
    the visual bounds, and the left and right side bearings.

    Glyphs for a rotated font, or obtained from after applying a rotation to
    the glyph, can have advances that contain both X and Y components. Usually
    the advance only has one component.

    The advance of a glyph is the distance from the glyph's origin to the
    origin of the next glyph along the baseline, which is either vertical
    or horizontal.
*/
class GlyphMetrics
{
public:
    GlyphMetrics()
        : lsbDelta(0), rsbDelta(0),
           width(0), height(0),
           fAdvanceX(0), fAdvanceY(0),
           left(0), top(0)
    {}

    ~GlyphMetrics() {}

    /** Call this to set all of the metrics fields to 0 (e.g. if the scaler
        encounters an error measuring a glyph). Note: this does not alter the
        fImage, fPath, fID, fMaskFormat fields.
     */
    void clear()
    {
        rsbDelta = 0; lsbDelta = 0;
        width = 0; height = 0;
        fAdvanceX = 0; fAdvanceY = 0;
        top = 0; left = 0;
    }

    int8_t      lsbDelta;   /* The difference between hinted and unhinted
                               left side bearing while autohinting is active.
                               Zero otherwise. */

    int8_t      rsbDelta;   /* The difference between hinted and unhinted
                               right side bearing while autohinting is active.
                               Zero otherwise. */

    uint16_t    width;     /* The width of the glyph. */

    uint16_t    height;     /* The height of the glyph. */

    FEM16Dot16  fAdvanceX;  /* The horizontal distance from the glyph's origin
                               to the origin of the next glyph along the
                               baseline. */

    FEM16Dot16  fAdvanceY;  /* The vertical distance from the glyph's origin
                               to the origin of the next glyph along the
                               baseline. */

    FEM26Dot6   left;       /* The horizontal offset from the origin (baseline). */

    FEM26Dot6   top;        /* The vertical offset from the origin (baseline),
                               using the y downwards convention. */
};/* end class GlyphMetrics */

enum Flags
{
    FEM_DevKernText_Flag        = 0x01,
    FEM_Hinting_Flag            = 0x06,
    FEM_EmbeddedBitmapText_Flag = 0x08,
    FEM_Embolden_Flag           = 0x10,
};

/** \struct FontMetrics

    This struct provides font scaler information. This information is passed
    to the font engine plugin to create a font scaler.
*/
struct FontScalerInfo
{
    uint32_t        fontID;

    bool            subpixelPositioning;
    AliasMode       maskFormat;           /* mono, gray, lcd */

    /*
      bit 4     : embeddedbitmap
      bit 3     : emboldening
      bit 1 & 2 : hinting
      bit 0     : kerning

    */
    uint8_t         flags;

    FEM16Dot16      fScaleX, fScaleY;
    FEM16Dot16      fSkewX, fSkewY;
    const uint8_t*  pBuffer;              /* font file buffer */

    void*           pStream;              /* font input stream */

    const char*     pPath;                /* system path to font file */
    size_t          pathSz;               /* font file path length */
    size_t          size;                 /* buffer size if (pBuffer != NULL); input stream size otherwise */
};/* end class FontScalerInfo */

/** \class GlyphOutline

    This class provides outline information for a glyph.
*/
class GlyphOutline
{
private:
    GlyphOutline();

public:
    GlyphOutline(int16_t nOtlnPts, int16_t nContours)
        : contourCount(nContours), pointCount(nOtlnPts),
           x(NULL), y(NULL), contours(NULL), flags(NULL)
    {

        x = (FEM26Dot6*)malloc(((pointCount + pointCount) * sizeof(FEM26Dot6)) + (nContours * sizeof(int16_t)) + (pointCount * sizeof(uint8_t)));
        y = (FEM26Dot6*)&x[pointCount];
        contours = (int16_t*)&y[pointCount];
        flags = (uint8_t*)&contours[nContours];
    }

    ~GlyphOutline()
    {
        free(x);
    }

    int16_t   contourCount;  /* number of contours in glyph */
    int16_t   pointCount;    /* number of points in the glyph */

    FEM26Dot6  *x, *y;       /* actual points in device coordinates */

    int16_t*  contours;      /* contours end point */
    uint8_t*  flags;         /* the points flags */
}; /* end class GlyphOutline */

/** \class FontScaler

    Font Scaler Interface; each plugin will provide its own implementation.
*/
class FontScaler
{
public:
    FontScaler() {}
    virtual ~FontScaler() {}

    /** Returns the number of glyphs in the font.
    */
    virtual uint32_t getGlyphCount() const = 0;

    /** Returns the glyph index of the character code (unicode value) in the
        font. The glyph index is a number from 0 to n-1, assuming the font
        contains n number of glyphs. Glyph Index will be zero if unicode is
        not present in the font.
        @param charUniCode    character code (unicode).
        @return the glyph index for the given character code.
    */
    virtual uint16_t getCharToGlyphID(int32_t charUniCode) = 0;

    /** Returns the character code (unicode value) of the glyph index in the
        font. character code will be zero if glyph index is not present in the
        font.
        @param glyphID    glyph index.
        @return the character code (unicode value) of the given glyph index.
    */
    virtual int32_t getGlyphIDToChar(uint16_t glyphID) = 0;

    /** Returns the advance measure for the glyph, given the glyph index.
        @param glyphID    glyph index.
        @param fracX      horizontal factional pen delta; normally set to
                          zero. Use it with non-zero values if you are also
                          using fractional character positioning.
        @param fracY      vertical factional pen delta; normally set to
                          zero. Use it with non-zero values if you are also
                          using fractional character positioning.
        @return the advance measure for the given glyph.
    */
    virtual GlyphMetrics getGlyphAdvance(uint16_t glyphID, FEM16Dot16 fracX, FEM16Dot16 fracY) = 0;

    /** Returns the metrics for the glyph, given the glyph index.
        @param glyphID    glyph index.
        @param fracX      horizontal factional pen delta; normally set to
                          zero. Use it with non-zero values if you are also
                          using fractional character positioning.
        @param fracY      vertical factional pen delta; normally set to
                          zero. Use it with non-zero values if you are also
                          using fractional character positioning.
        @return the metrics for the given glyph.
    */
    virtual GlyphMetrics getGlyphMetrics(uint16_t glyphID, FEM16Dot16 fracX, FEM16Dot16 fracY) = 0;

    /** Render the specified glyph into a 'buffer' (user allocated) of given rowBytes and height.
        @param glyphID    glyph index.
        @param fracX      horizontal factional pen delta; normally set to
                          zero. Use it with non-zero values if you are also
                          using fractional character positioning.
        @param fracY      vertical factional pen delta; normally set to
                          zero. Use it with non-zero values if you are also
                          using fractional character positioning.
        @param rowBytes   buffer's row bytes.
        @param width      buffer's width.
        @param height     buffer height.
        @param buffer     user allocated buffer.
    */
    virtual void getGlyphImage(uint16_t glyphID, FEM16Dot16 fracX, FEM16Dot16 fracY, uint32_t rowBytes, uint16_t width, uint16_t height, uint8_t *buffer) = 0;

    /** Returns the font wide metrics.
        @param mX    If not null, returns the horizontal font wide metric.
        @param mY    If not null, returns the vertical font wide metric.
    */
    virtual void getFontMetrics(FontMetrics* mX, FontMetrics* mY) = 0;

    /** Returns the outline for the glyph, given the glyph id. User should
        delete the GlyphOutline object when done.
        @param glyphID    glyph index.
        @param fracX      horizontal factional pen delta; normally set to
                          zero. Use it with non-zero values if you are also
                          using fractional character positioning.
        @param fracY      vertical factional pen delta; normally set to
                          zero. Use it with non-zero values if you are also
                          using fractional character positioning.
        @return the outline for the given glyph.
    */
    virtual GlyphOutline* getGlyphOutline(uint16_t glyphID, FEM16Dot16 fracX, FEM16Dot16 fracY) = 0;
};/* end class FontScaler */

/** \class FontEngine

    Font Engine Interface; each plugin will provide its implementation.
*/
class FontEngine
{
public:
    /** constructor
    */
    FontEngine() {}

    /** Destructor
    */
    virtual ~FontEngine() {}

    /** Returns font engine name.
    */
    virtual const char* getName() const = 0;

    /** Returns font engine's capabilities.
        @param desc    The information about the font scaler.
    */
    virtual EngineCapability getCapabilities(FontScalerInfo& desc) const = 0;

    /** Creates and returns font scaler on sucess; null otherwise.
        @param desc    The information about the font scaler.
    */
    virtual FontScaler* createFontScalerContext(const FontScalerInfo& desc) = 0;

    /** Given system path of the font file; returns the font name, name's length and
        style.
        @param path      The system path to font file.
        @param name      If not null, returns name of the specifed font path.
                         The returned name is 'length' byte long.
        @param length    The maximum space allocated in path (by the caller).
        @param style     If not null, return style of the specified font.
        @return on success, length of font name; zero otherwise.
    */
    virtual size_t getFontNameAndStyle(const char path[], char name[], size_t length, FontStyle* style) = 0;

    /** Given data of font file in buffer; returns the font name, name's length and
        style.
        @param buffer          The font file buffer.
        @param bufferLength    Length of the buffer.
        @param name            If not null, returns name of the specifed font path.
                               The returned name is 'length' byte long.
        @param length          The maximum space allocated in path (by the caller).
        @param style           If not null, return style of the specified font.
        @return on success, length of font name; zero otherwise.
    */
    virtual size_t getFontNameAndStyle(const void* buffer, const uint32_t bufferLength, char name[], size_t length, FontStyle*  style) = 0;

    /** Given system path of the font file; returns 'true' if the font format is
        supported; 'false' otherwise.
        @param path      The system path to font file.
        @param isLoad    If 'true' font file will be loaded i.e. font object
                         (sfnt etc.) will be created to determine the font
                         file support.
                         If 'false' file extension will be checked against
                         the predefined list of engine supported font formats
                         to determine the font file support.
    */
    virtual bool isFontSupported(const char path[], bool isLoad) = 0;

    /** Given data of font file in buffer; returns 'true' if the font format is
        supported; 'false' otherwise.
        @param buffer    The font file buffer.
        @param isLoad    If 'true' font file will be loaded i.e. font object
                         (sfnt etc.) will be created to determine the font
                         file support.
                         If 'false' file extension will be checked against
                         the predefined list of engine supported font formats
                         to determine the font file support.
    */
    virtual bool isFontSupported(const void* buffer, const uint32_t bufferLength) = 0;

    /** Given system path of the font file; returns the number of font units
        per em.
        @param path    The system path to font file.
        @return the number of font units per em or 0 on error.
    */
    virtual uint32_t getFontUnitsPerEm(const char path[]) = 0;

    /** Given font data in buffer; returns the number of font units per em.
        @param buffer          The font file buffer.
        @param bufferLength    Length of the buffer.
        @return the number of font units per em or 0 on error.
    */
    virtual uint32_t getFontUnitsPerEm(const void* buffer, const uint32_t bufferLength) = 0;
};

/** \struct FontEngineInfo

    This struct provides information about a font engine implementation.
*/
struct FontEngineInfo
{
    const char*  name;
}; /* end struct FontEngineInfo */

typedef FontEngineInfo*          FontEngineInfoPtr;
typedef FontEngineInfo**         FontEngineInfoArrPtr;

class FontEngineManager
{
public:
    /** Returns a singleton instance to a font engine manager.
    */
    static FontEngineManager& getInstance();

    /** Returns font scaler instance.
        @param desc    The information about the font scaler.
    */
    FontScaler* createFontScalerContext(const FontScalerInfo& desc);

    /** Returns the count of available font engines.
    */
    size_t getFontEngineCount() const { return engineCount; }

    /** Given a font engine name; returns its instance.
    */
    FontEngine* getFontEngine(const char name[]);

    /** Returns a list all available font engines. Font engine manager is
        resposible to free it.
    */
    const FontEngineInfoArrPtr listFontEngines() { return pFontEngineInfoArr; }

    /** Given system path of the font file; returns the fone name, name's length and
        style.
        @param path      The system path to font file.
        @param name      If not null, returns name of the specifed font path.
                         The returned name is 'length' byte long.
        @param length    The maximum space allocated in path (by the caller).
        @param style     If not null, return style of the specified font.
        @return on success, length of font name; zero otherwise.
    */
    size_t getFontNameAndStyle(const char path[], char name[], size_t length, FontStyle* style);

    /** Given font data in buffer; returns its name, name's length and
        style.
        @param buffer          The font file buffer.
        @param bufferLength    Length of the buffer.
        @param name            If not null, returns name of the specifed font path.
                               The returned name is 'length' byte long.
        @param length          The maximum space allocated in path (by the caller).
        @param style           If not null, return style of the specified font.
        @return on success, length of font name; zero otherwise.
    */
    size_t getFontNameAndStyle(const void* buffer, const uint32_t bufferLength, char name[], size_t length, FontStyle*  style);

    /** Given system path of the font file; returns 'true' if the font format is
        supported; 'false' otherwise.
        @param path      The system path to font file.
        @param isLoad    If 'true' font file will be loaded i.e. font object
                         (sfnt etc.) will be created to determine the font
                         file support.
                         If 'false' file extension will be checked against
                         the predefined list of engine supported font formats
                         to determine the font file support.
    */
    bool isFontSupported(const char path[], bool isLoad);

    /** Given font data in buffer; returns 'true' if the font format is
        supported; 'false' otherwise.
        @param buffer    The font file buffer.
        @param isLoad    If 'true' font file will be loaded i.e. font object
                         (sfnt etc.) will be created to determine the font
                         file support.
                         If 'false' file extension will be checked against
                         the predefined list of engine supported font formats
                         to determine the font file support.
    */
    bool isFontSupported(const void* buffer, const uint32_t bufferLength);

    /** Given system path of the font file; returns the number of font units
        per em.
        @param path    The system path to font file.
        @return the number of font units per em or 0 on error.
    */
    uint32_t getFontUnitsPerEm(const char path[]);

    /** Given font data in buffer; returns the number of font units per em.
        @param buffer          The font file buffer.
        @param bufferLength    Length of the buffer.
        @return the number of font units per em or 0 on error.
    */
    uint32_t getFontUnitsPerEm(const void* buffer, const uint32_t bufferLength);

private:
    typedef struct FontEngineNode_t
    {
        struct FontEngineNode_t*  next;
        FontEngine*               inst;
    } FontEngineNode;

    size_t                     engineCount;         /* No. of available font engines */
    FontEngineNode*            pFontEngineList;     /* All available font engines */

    FontEngineInfoArrPtr       pFontEngineInfoArr;  /* All available font engines info */

    static FontEngineManager*  pFEMInst;            /* Pointer to singleton font engine manager's instance */

    FontEngineManager();
    ~FontEngineManager();

    FontEngineManager(const FontEngineManager &);
    FontEngineManager& operator=(const FontEngineManager &);
};/* end class FontEngineManager */

#endif /* __FONTENGINEMANAGER_HEADER__ */

