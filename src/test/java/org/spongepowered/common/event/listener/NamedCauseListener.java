/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.event.listener;

import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.filter.cause.ContextValue;
import org.spongepowered.common.event.EventFilterTest;

public class NamedCauseListener {

    public boolean namedCauseListenerCalled;
    public boolean namedCauseListenerCalledInc;
    public boolean namedCauseListenerCalledEx;

    @Listener
    public void namedCauseListener(EventFilterTest.SubEvent event, @ContextValue(EventContext.OWNER) BlockState state) {
        this.namedCauseListenerCalled = true;
    }

    @Listener
    public void namedCauseListenerInclude(EventFilterTest.SubEvent event,
            @ContextValue(value = EventContext.OWNER, typeFilter = BlockState.class) Object state) {
        this.namedCauseListenerCalledInc = true;
    }

    @Listener
    public void namedCauseListenerExclude(EventFilterTest.SubEvent event,
            @ContextValue(value = EventContext.OWNER, typeFilter = BlockState.class, inverse = true) Object state) {
        this.namedCauseListenerCalledEx = true;
    }

}
