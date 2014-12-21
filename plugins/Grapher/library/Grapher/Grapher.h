/*
 * Copyright (c) , Majenko Technologies
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met:
 * 
 *  1. Redistributions of source code must retain the above copyright notice, 
 *     this list of conditions and the following disclaimer.
 * 
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *      and/or other materials provided with the distribution.
 * 
 *  3. Neither the name of Majenko Technologies nor the names of its contributors may be used
 *     to endorse or promote products derived from this software without 
 *     specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE 
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL 
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, 
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE 
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


#ifndef _GRAPHER_H
#define _GRAPHER_H

#if (ARDUINO >= 100) 
# include <Arduino.h>
#else
# include <WProgram.h>
#endif

#define MAX_SERIES 10

class Grapher {
    private:
        // Private functions and variables here.  They can only be accessed
        // by functions within the class.
        Stream *_dev;
        uint16_t _width;
        uint16_t _height;
        uint16_t _top;
        uint16_t _right;
        uint16_t _bottom;
        uint16_t _left;
        float _data[MAX_SERIES];
        uint8_t _series;

    public:
        // Public functions and variables.  These can be accessed from
        // outside the class.
        Grapher();
        Grapher(Stream *dev);
        void begin();
        void setSize(uint16_t w, uint16_t h);
        void setMargins(uint16_t t, uint16_t r, uint16_t b, uint16_t l);
        void setTopMargin(uint16_t m);
        void setRightMargin(uint16_t m);
        void setBottomMargin(uint16_t m);
        void setLeftMargin(uint16_t m);
        void setBackground(uint8_t r, uint8_t g, uint8_t b);
        void setForeground(uint8_t r, uint8_t g, uint8_t b);
        void setYAxis(float low, float high, float step);
        uint8_t addSeries(const char *name, uint8_t r, uint8_t g, uint8_t b);
        uint8_t addSeries(const char *name, uint8_t r, uint8_t g, uint8_t b, uint8_t w);
        void setValue(uint8_t series, float v);
        void update();
};
#endif
