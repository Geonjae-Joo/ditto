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
package org.eclipse.ditto.signals.commands.things.modify;

import static org.eclipse.ditto.model.base.common.ConditionChecker.checkNotNull;

import java.util.Objects;
import java.util.function.Predicate;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.eclipse.ditto.json.JsonFactory;
import org.eclipse.ditto.json.JsonField;
import org.eclipse.ditto.json.JsonFieldDefinition;
import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.json.JsonObjectBuilder;
import org.eclipse.ditto.json.JsonPointer;
import org.eclipse.ditto.model.base.common.HttpStatusCode;
import org.eclipse.ditto.model.base.headers.DittoHeaders;
import org.eclipse.ditto.model.base.json.FieldType;
import org.eclipse.ditto.model.base.json.JsonParsableCommandResponse;
import org.eclipse.ditto.model.base.json.JsonSchemaVersion;
import org.eclipse.ditto.model.things.ThingId;
import org.eclipse.ditto.signals.commands.base.AbstractCommandResponse;
import org.eclipse.ditto.signals.commands.base.CommandResponseJsonDeserializer;

/**
 * Response to a {@link DeleteFeatureDefinition} command.
 */
@Immutable
@JsonParsableCommandResponse(type = DeleteFeatureDefinitionResponse.TYPE)
public final class DeleteFeatureDefinitionResponse extends AbstractCommandResponse<DeleteFeatureDefinitionResponse>
        implements ThingModifyCommandResponse<DeleteFeatureDefinitionResponse> {

    /**
     * Type of this response.
     */
    public static final String TYPE = TYPE_PREFIX + DeleteFeatureDefinition.NAME;

    static final JsonFieldDefinition<String> JSON_FEATURE_ID =
            JsonFactory.newStringFieldDefinition("featureId", FieldType.REGULAR, JsonSchemaVersion.V_1,
                    JsonSchemaVersion.V_2);

    private final ThingId thingId;
    private final String featureId;

    private DeleteFeatureDefinitionResponse(final ThingId thingId, final String featureId,
            final DittoHeaders dittoHeaders) {

        super(TYPE, HttpStatusCode.NO_CONTENT, dittoHeaders);
        this.thingId = thingId;
        this.featureId = checkNotNull(featureId, "Feature ID");
    }

    /**
     * Creates a response to a {@link DeleteFeatureDefinition} command.
     *
     * @param thingId the Thing ID of the deleted Feature Definition.
     * @param featureId the {@code Feature}'s ID whose Definition were deleted.
     * @param dittoHeaders the headers of the preceding command.
     * @return the response.
     * @throws NullPointerException if any argument is {@code null}.
     * @deprecated Thing ID is now typed. Use
     * {@link #of(org.eclipse.ditto.model.things.ThingId, String, org.eclipse.ditto.model.base.headers.DittoHeaders)}
     * instead.
     */
    @Deprecated
    public static DeleteFeatureDefinitionResponse of(final String thingId, final String featureId,
            final DittoHeaders dittoHeaders) {

        return of(ThingId.of(thingId), featureId, dittoHeaders);
    }

    /**
     * Creates a response to a {@link DeleteFeatureDefinition} command.
     *
     * @param thingId the Thing ID of the deleted Feature Definition.
     * @param featureId the {@code Feature}'s ID whose Definition were deleted.
     * @param dittoHeaders the headers of the preceding command.
     * @return the response.
     * @throws NullPointerException if any argument is {@code null}.
     */
    public static DeleteFeatureDefinitionResponse of(final ThingId thingId, final String featureId,
            final DittoHeaders dittoHeaders) {

        return new DeleteFeatureDefinitionResponse(thingId, featureId, dittoHeaders);
    }

    /**
     * Creates a response to a {@link DeleteFeatureDefinition} command from a JSON string.
     *
     * @param jsonString the JSON string of which the response is to be created.
     * @param dittoHeaders the headers of the preceding command.
     * @return the response.
     * @throws NullPointerException if any argument is {@code null}.
     * @throws IllegalArgumentException if {@code jsonString} is empty.
     * @throws org.eclipse.ditto.json.JsonParseException if the passed in {@code jsonString} was not in the expected
     * format.
     * @throws org.eclipse.ditto.json.JsonMissingFieldException if the parsed {@code jsonString} did not contain any of
     * the required fields
     * <ul>
     *     <li>{@link ThingModifyCommandResponse.JsonFields#JSON_THING_ID} or</li>
     *     <li>{@link #JSON_FEATURE_ID}.</li>
     * </ul>
     * @throws org.eclipse.ditto.model.things.ThingIdInvalidException if the parsed thing ID did not comply to
     * {@link org.eclipse.ditto.model.base.entity.id.RegexPatterns#ID_REGEX}.
     */
    public static DeleteFeatureDefinitionResponse fromJson(final String jsonString, final DittoHeaders dittoHeaders) {
        return fromJson(JsonFactory.newObject(jsonString), dittoHeaders);
    }

    /**
     * Creates a response to a {@link DeleteFeatureDefinition} command from a JSON object.
     *
     * @param jsonObject the JSON object of which the response is to be created.
     * @param dittoHeaders the headers of the preceding command.
     * @return the response.
     * @throws NullPointerException any argument is {@code null}.
     * @throws org.eclipse.ditto.json.JsonParseException if the passed in {@code jsonObject} was not in the expected
     * format.
     * @throws org.eclipse.ditto.json.JsonMissingFieldException if {@code jsonObject} did not contain any of the
     * required fields
     * <ul>
     *     <li>{@link ThingModifyCommandResponse.JsonFields#JSON_THING_ID} or</li>
     *     <li>{@link #JSON_FEATURE_ID}.</li>
     * </ul>
     * @throws org.eclipse.ditto.model.things.ThingIdInvalidException if the parsed thing ID did not comply to
     * {@link org.eclipse.ditto.model.base.entity.id.RegexPatterns#ID_REGEX}.
     */
    public static DeleteFeatureDefinitionResponse fromJson(final JsonObject jsonObject,
            final DittoHeaders dittoHeaders) {

        return new CommandResponseJsonDeserializer<DeleteFeatureDefinitionResponse>(TYPE, jsonObject).deserialize(
                statusCode -> {
                    final String extractedThingId =
                            jsonObject.getValueOrThrow(ThingModifyCommandResponse.JsonFields.JSON_THING_ID);
                    final ThingId thingId = ThingId.of(extractedThingId);
                    final String extractedFeatureId = jsonObject.getValueOrThrow(JSON_FEATURE_ID);

                    return of(thingId, extractedFeatureId, dittoHeaders);
                });
    }

    @Override
    public ThingId getThingEntityId() {
        return thingId;
    }

    /**
     * Returns the {@code Feature}'s ID whose Definition was deleted.
     *
     * @return the Feature's ID.
     */
    public String getFeatureId() {
        return featureId;
    }

    @Override
    public JsonPointer getResourcePath() {
        return JsonFactory.newPointer("/features/" + featureId + "/definition");
    }

    @Override
    protected void appendPayload(final JsonObjectBuilder jsonObjectBuilder, final JsonSchemaVersion schemaVersion,
            final Predicate<JsonField> thePredicate) {

        final Predicate<JsonField> predicate = schemaVersion.and(thePredicate);
        jsonObjectBuilder.set(ThingModifyCommandResponse.JsonFields.JSON_THING_ID, thingId.toString(), predicate);
        jsonObjectBuilder.set(JSON_FEATURE_ID, featureId, predicate);
    }

    @Override
    public DeleteFeatureDefinitionResponse setDittoHeaders(final DittoHeaders dittoHeaders) {
        return of(thingId, featureId, dittoHeaders);
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DeleteFeatureDefinitionResponse that = (DeleteFeatureDefinitionResponse) o;
        return that.canEqual(this) && Objects.equals(thingId, that.thingId)
                && Objects.equals(featureId, that.featureId) && super.equals(o);
    }

    @Override
    protected boolean canEqual(@Nullable final Object other) {
        return other instanceof DeleteFeatureDefinitionResponse;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), thingId, featureId);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + super.toString() + ", thingId=" + thingId + ", featureId=" +
                featureId + "]";
    }

}
