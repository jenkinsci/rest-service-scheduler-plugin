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

import hudson.model.Computer;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.Queue.BuildableItem;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jenkinsci.plugins.restservicescheduler.json.Translator.Snapshot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/*package*/ final class SnapshotSerializer implements JsonSerializer<Snapshot> {

    private final Gson gson = new GsonBuilder()
            .registerTypeHierarchyAdapter(Queue.BuildableItem.class, this.new QueueItemSerializer())
            .registerTypeHierarchyAdapter(Node.class, this.new NodeSerializer())
            .setPrettyPrinting()
            .serializeNulls()
            .create()
    ;

    private Snapshot snapshot;

    public JsonElement serialize(
            final Snapshot src,
            final Type typeOfSrc,
            final JsonSerializationContext context
    ) {

        this.snapshot = src;

        final JsonObject jsonQueue = new JsonObject();

        final JsonElement queue = gson.toJsonTree(src.stateProvider().getQueue());
        jsonQueue.add("queue", queue);

        return jsonQueue;
    }

    private final class QueueItemSerializer implements JsonSerializer<Queue.BuildableItem> {

        public JsonElement serialize(
                final BuildableItem item,
                final Type typeOfSrc,
                final JsonSerializationContext context
        ) {

            final JsonObject json = new JsonObject();

            json.addProperty("id", item.id);
            json.addProperty("priority", priority(item));
            json.addProperty("inQueueSince", item.getInQueueSince());
            json.addProperty("name", item.task.getDisplayName());

            final List<Node> assignableNodes = assignableNodes(item);
            json.add("nodes", gson.toJsonTree(assignableNodes));

            json.addProperty("assigned", assignedNode(item, assignableNodes));

            return json;
        }

        private int priority(final Queue.BuildableItem item) {

            return 50;
        }

        private List<Node> assignableNodes(final Queue.BuildableItem item) {

            final Label label = item.getAssignedLabel();

            final Collection<Node> nodeCandidates = (label != null && label.getNodes() != null)
                    ? label.getNodes()
                    : snapshot.stateProvider().getNodes()
            ;

            final List<Node> nodes = new ArrayList<Node>(nodeCandidates.size());
            for(final Node node: nodeCandidates) {

                if (nodeApplicable(item, node)) {

                    nodes.add(node);
                }
            }

            return nodes;
        }

        private boolean nodeApplicable(Queue.BuildableItem item, final Node node) {

            return isOnline(node) && node.canTake(item) == null;
        }

        private boolean isOnline(final Node node) {

            final Computer computer = node.toComputer();
            return computer != null && !computer.isOffline() && computer.isAcceptingTasks();
        }

        private String assignedNode(final Queue.BuildableItem item, final List<Node> nodes) {

            final String assignedTo = snapshot.assignments().nodeName(item);

            for (final Node node: nodes) {

                if (getName(node).equals(assignedTo)) return assignedTo;
            }

            // currently assigned node is no longer assignable
            return null;
        }
    }

    private final class NodeSerializer implements JsonSerializer<Node> {

        public JsonElement serialize(
                final Node src, final Type typeOfSrc, final JsonSerializationContext context
        ) {

            final JsonObject node = new JsonObject();
            node.addProperty("name", getName(src));
            node.addProperty("executors", src.getNumExecutors());
            node.addProperty("freeExecutors", src.toComputer().countIdle());

            return node;
        }
    }

    private String getName(final Node node) {

        return node.getSelfLabel().toString();
    }
}
