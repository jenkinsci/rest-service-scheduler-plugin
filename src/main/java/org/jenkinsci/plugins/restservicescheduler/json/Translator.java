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

import org.jenkinsci.plugins.externalscheduler.NodeAssignments;
import org.jenkinsci.plugins.externalscheduler.StateProvider;
import org.jenkinsci.plugins.restservicescheduler.Score;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Translate objects to JSON and back
 *
 * @author ogondza
 */
public final class Translator {

    /*package*/ static final String NOT_ASSIGNED = "not-assigned";

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(NodeAssignments.class, new NodeAssignmentsDeserializer())
            .registerTypeAdapter(Snapshot.class, new SnapshotSerializer())
            .setPrettyPrinting()
            .serializeNulls()
            .create()
    ;

    /**
     * Extract score message
     *
     * @param score JSON score
     */
    public Score extractScore(final String score) {

        return gson.fromJson(score, Score.class);
    }

    /**
     * Extract assignments from solution message
     *
     * @param solution JSON solution
     * @return Updated assignments
     */
    public NodeAssignments extractAssignments(final String solution) {

        return gson.fromJson(solution, NodeAssignments.class);
    }

    /**
     * Serialize current state as JSON
     *
     * @param stateProvider Current state
     * @param assignments Latest assignments
     * @return JSON query
     */
    public String buildQuery(
            final StateProvider stateProvider, final NodeAssignments assignments
    ) {

        return gson.toJson(new Snapshot(stateProvider, assignments));
    }

    /*package*/ static final class Snapshot {

        private final NodeAssignments assignments;
        private final StateProvider stateProvider;

        public Snapshot(final StateProvider stateProvider, final NodeAssignments assignments) {

            if (assignments == null) throw new AssertionError("nodeAssignments is null");
            if (stateProvider == null) throw new AssertionError("stateProvider is null");

            this.stateProvider = stateProvider;
            this.assignments = assignments;
        }

        public NodeAssignments assignments() {

            return assignments;
        }

        public StateProvider stateProvider() {

            return stateProvider;
        }
    }
}
