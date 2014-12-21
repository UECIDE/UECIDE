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


#include <Grapher.h>

Grapher::Grapher(Stream *dev) {
    _dev = dev;
}

Grapher::Grapher() {
    _dev = &Serial;
}

// Initialize any hardware here, not in the constructor.  You cannot
// guarantee the execution order of constructors, but you can guarantee
// when the begin member function is executed.
void Grapher::begin() {
    _dev->println("R");
    _dev->println("R");
    _dev->println("R");
}

void Grapher::setSize(uint16_t w, uint16_t h) {
    _width = w;
    _height = h;
    _dev->print("S");
    _dev->print(_width);
    _dev->print(":");
    _dev->println(_height);
}

void Grapher::setMargins(uint16_t t, uint16_t r, uint16_t b, uint16_t l) {
    _top = t;
    _right = r;
    _bottom = b;
    _left = l;
    _dev->print("M");
    _dev->print(_top);
    _dev->print(":");
    _dev->print(_right);
    _dev->print(":");
    _dev->print(_bottom);
    _dev->print(":");
    _dev->println(_left);
}

void Grapher::setTopMargin(uint16_t m) {
    setMargins(m, _right, _bottom, _left);
}

void Grapher::setRightMargin(uint16_t m) {
    setMargins(_top, m, _bottom, _left);
}

void Grapher::setBottomMargin(uint16_t m) {
    setMargins(_top, _right, m, _left);
}

void Grapher::setLeftMargin(uint16_t m) {
    setMargins(_top, _right, _bottom, m);
}

void Grapher::setBackground(uint8_t r, uint8_t g, uint8_t b) {
    _dev->print("B");
    _dev->print(r);
    _dev->print(":");
    _dev->print(g);
    _dev->print(":");
    _dev->println(b);
}

void Grapher::setForeground(uint8_t r, uint8_t g, uint8_t b) {
    _dev->print("F");
    _dev->print(r);
    _dev->print(":");
    _dev->print(g);
    _dev->print(":");
    _dev->println(b);
}

void Grapher::setYAxis(float low, float high, float step) {
    _dev->print("Y");
    _dev->print(low);
    _dev->print(":");
    _dev->print(high);
    _dev->print(":");
    _dev->println(step);
}

uint8_t Grapher::addSeries(const char *name, uint8_t r, uint8_t g, uint8_t b) {
    return addSeries(name, r, g, b, 2);
}

uint8_t Grapher::addSeries(const char *name, uint8_t r, uint8_t g, uint8_t b, uint8_t w) {
    _data[_series] = 0;
    _dev->print("A");
    _dev->print(name);
    _dev->print(":");
    _dev->print(r);
    _dev->print(":");
    _dev->print(g);
    _dev->print(":");
    _dev->print(b);
    _dev->print(":");
    _dev->println(w);
    uint8_t s = _series;
    _series++;
    return s;
}

void Grapher::setValue(uint8_t series, float v) {
    if (series < _series) {
        _data[series] = v;
    }
}

void Grapher::update() {
    _dev->print("V");
    for (int i = 0; i < _series; i++) {
        if (i != 0) {
            _dev->print(":");
        }
        _dev->print(_data[i]);
    }
    _dev->println();
}
