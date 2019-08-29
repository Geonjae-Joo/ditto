/*
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.ditto.signals.commands.live.query;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

import org.eclipse.ditto.model.things.Thing;
import org.eclipse.ditto.signals.commands.base.CommandResponse;
import org.eclipse.ditto.signals.commands.live.base.LiveCommandAnswer;
import org.eclipse.ditto.signals.commands.live.query.RetrieveThingLiveCommandAnswerBuilder.ResponseFactory;
import org.eclipse.ditto.signals.commands.things.ThingErrorResponse;
import org.eclipse.ditto.signals.commands.things.exceptions.ThingNotAccessibleException;
import org.eclipse.ditto.signals.commands.things.query.RetrieveThingResponse;

/**
 * A mutable builder with a fluent API for creating a {@link LiveCommandAnswer} for a {@link RetrieveThingLiveCommand}.
 */
@ParametersAreNonnullByDefault
@NotThreadSafe
final class RetrieveThingLiveCommandAnswerBuilderImpl
        extends AbstractLiveCommandAnswerBuilder<RetrieveThingLiveCommand, ResponseFactory>
        implements RetrieveThingLiveCommandAnswerBuilder {

    private RetrieveThingLiveCommandAnswerBuilderImpl(final RetrieveThingLiveCommand command) {
        super(command);
    }

    /**
     * Returns a new instance of {@code RetrieveThingLiveCommandAnswerBuilderImpl}.
     *
     * @param command the command to build an answer for.
     * @return the instance.
     * @throws NullPointerException if {@code command} is {@code null}.
     */
    public static RetrieveThingLiveCommandAnswerBuilderImpl newInstance(final RetrieveThingLiveCommand command) {
        return new RetrieveThingLiveCommandAnswerBuilderImpl(command);
    }

    @Override
    protected CommandResponse doCreateResponse(
            final Function<ResponseFactory, CommandResponse<?>> createResponseFunction) {
        return createResponseFunction.apply(new ResponseFactoryImpl());
    }

    @ParametersAreNonnullByDefault
    @Immutable
    private final class ResponseFactoryImpl implements ResponseFactory {

        @Nonnull
        @Override
        public RetrieveThingResponse retrieved(final Thing thing) {
            return RetrieveThingResponse.of(command.getThingEntityId(), thing, command.getDittoHeaders());
        }

        @Nonnull
        @Override
        public ThingErrorResponse thingNotAccessibleError() {
            return errorResponse(command.getThingEntityId(), ThingNotAccessibleException.newBuilder(command.getThingEntityId())
                    .dittoHeaders(command.getDittoHeaders())
                    .build());
        }
    }

}
