/*
 * The MIT License
 *
 * Copyright (c) 2012 Red Hat, Inc.
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
package org.jenkinsci.plugins.restservicescheduler.json;

import java.lang.reflect.Type;

import org.jenkinsci.plugins.externalscheduler.NodeAssignments;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * Deserialize incoming json to {@link NodeAssignments}
 *
 * @author ogondza
 */
public final class NodeAssignmentsDeserializer implements JsonDeserializer<NodeAssignments> {

    public NodeAssignments deserialize(
            final JsonElement json,
            final Type typeOfT,
            final JsonDeserializationContext context
    ) throws JsonParseException {

        final NodeAssignments.Builder builder = NodeAssignments.builder();

        final JsonArray items = json.getAsJsonObject().getAsJsonArray("solution");
        for (final JsonElement o: items) {

            final JsonObject item = o.getAsJsonObject();

            final int itemId = item.getAsJsonPrimitive("id").getAsInt();
            final String nodeName = deserilizeNodeName(item.getAsJsonPrimitive("node").getAsString());
            builder.assign(itemId, nodeName);
        }

        return builder.build();
    }

    private String deserilizeNodeName(final String jsonNodeName) {

        return Translator.NOT_ASSIGNED.equals(jsonNodeName)
                ? null
                : jsonNodeName
        ;
    }
}
