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
package org.spongepowered.common.service.permission;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.command.source.RemoteSource;
import org.spongepowered.api.service.context.ServiceContext;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.world.World;
import org.spongepowered.common.SpongeImpl;

import java.net.InetAddress;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * A context calculator handling world contexts.
 */
public class SpongeContextCalculator implements ContextCalculator<Subject> {
    private final LoadingCache<RemoteSource, Set<ServiceContext>> remoteIpCache = buildAddressCache(ServiceContext.REMOTE_IP_KEY,
                                                                                             input -> input.getConnection().getAddress().getAddress());

    private final LoadingCache<RemoteSource, Set<ServiceContext>> localIpCache = buildAddressCache(ServiceContext.LOCAL_IP_KEY,
                                                                                            input -> input.getConnection().getVirtualHost().getAddress());

    private LoadingCache<RemoteSource, Set<ServiceContext>> buildAddressCache(final String contextKey, final Function<RemoteSource, InetAddress> function) {
        return CacheBuilder.newBuilder()
            .weakKeys()
            .build(new CacheLoader<RemoteSource, Set<ServiceContext>>() {
                @Override
                public Set<ServiceContext> load(RemoteSource key) throws Exception {
                    ImmutableSet.Builder<ServiceContext> builder = ImmutableSet.builder();
                    final InetAddress addr = checkNotNull(function.apply(key), "addr");
                    builder.add(new ServiceContext(contextKey, addr.getHostAddress()));
                    for (String set : Maps.filterValues(SpongeImpl.getGlobalConfig().getConfig().getIpSets(), input -> {
                        return input.apply(addr);
                    }).keySet()) {
                        builder.add(new ServiceContext(contextKey, set));
                    }
                    return builder.build();
                }
            });
    }

    @Override
    public void accumulateContexts(Subject subject, Set<ServiceContext> accumulator) {
        Optional<CommandSource> subjSource = subject.getCommandSource();
        if (subjSource.isPresent()) {
            CommandSource source = subjSource.get();
            if (source instanceof Locatable) {
                World currentExt = ((Locatable) source).getWorld();
                accumulator.add(currentExt.getContext());
                accumulator.add((currentExt.getDimension().getContext()));
            }
            if (source instanceof RemoteSource) {
                RemoteSource rem = (RemoteSource) source;
                accumulator.addAll(this.remoteIpCache.getUnchecked(rem));
                accumulator.addAll(this.localIpCache.getUnchecked(rem));
                accumulator.add(new ServiceContext(ServiceContext.LOCAL_PORT_KEY, String.valueOf(rem.getConnection().getVirtualHost().getPort())));
                accumulator.add(new ServiceContext(ServiceContext.LOCAL_HOST_KEY, rem.getConnection().getVirtualHost().getHostName()));
            }
        }

    }

    @Override
    public boolean matches(ServiceContext context, Subject subject) {
        Optional<CommandSource> subjSource = subject.getCommandSource();
        if (subjSource.isPresent()) {
            CommandSource source = subjSource.get();
            if (source instanceof Locatable && context.getType().equals(ServiceContext.WORLD_KEY)) {
                Locatable located = (Locatable) source;
                if (context.getType().equals(ServiceContext.WORLD_KEY)) {
                    return located.getWorld().getContext().equals(context);
                } else if (context.getType().equals(ServiceContext.DIMENSION_KEY)) {
                    return located.getWorld().getDimension().getContext().equals(context);
                }
            }
            if (source instanceof RemoteSource) {
                RemoteSource remote = (RemoteSource) source;
                if (context.getType().equals(ServiceContext.LOCAL_HOST_KEY)) {
                    return context.getValue().equals(remote.getConnection().getVirtualHost().getHostName());
                } else if (context.getType().equals(ServiceContext.LOCAL_PORT_KEY)) {
                    return context.getValue().equals(String.valueOf(remote.getConnection().getVirtualHost().getPort()));
                } else if (context.getType().equals(ServiceContext.LOCAL_IP_KEY)) {
                    return this.localIpCache.getUnchecked(remote).contains(context);
                } else if (context.getType().equals(ServiceContext.REMOTE_IP_KEY)) {
                    return this.remoteIpCache.getUnchecked(remote).contains(context);
                }
            }
        }
        return false;
    }
}
