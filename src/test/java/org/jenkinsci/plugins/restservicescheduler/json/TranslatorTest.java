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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.queue.CauseOfBlockage;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedSet;

import org.jenkinsci.plugins.externalscheduler.ItemMock;
import org.jenkinsci.plugins.externalscheduler.NodeAssignments;
import org.jenkinsci.plugins.externalscheduler.NodeMockFactory;
import org.jenkinsci.plugins.externalscheduler.StateProviderMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Node.class, Computer.class, Queue.BuildableItem.class})
public class TranslatorTest {

    private static final Translator SERIALIZER = new Translator();

    private final NodeMockFactory nodeFactory = new NodeMockFactory();

    private final List<Node> nodes = new ArrayList<Node>();

    private String fixture(final String filename) {

        final InputStream stream = this.getClass().getResourceAsStream(filename);
        Scanner scanner = new Scanner(stream, "UTF-8").useDelimiter("\\A");
        if (scanner.hasNext()) return scanner.next().trim();

        throw new RuntimeException("Unable to load " + filename);
    }

    @Test
    public void deserializeScore() {

        assertThat(-1, equalTo(SERIALIZER.extractScore(getScoreMessage(-1)).get()));
        assertThat(0, equalTo(SERIALIZER.extractScore(getScoreMessage(0)).get()));
        assertThat(1, equalTo(SERIALIZER.extractScore(getScoreMessage(1)).get()));
    }

    private String getScoreMessage(final long score) {

        return "{\"score\": " + new Long(score).toString() + "}";
    }

    @Test
    public void deserializeSeveralItems() {


        final NodeAssignments assignments = SERIALIZER.extractAssignments(
                fixture("solution.json")
        );

        assertEquals(2, assignments.size());
        assertEquals("slave1", assignments.nodeName(1));
        assertEquals(null, assignments.nodeName(2));
    }

    @Test
    public void serializeSingleItem() {

        final String actual = SERIALIZER.buildQuery(
                new StateProviderMock(singleItem(), nodes),
                NodeAssignments.empty()
        );

        assertEquals(fixture("singleItem.queue.json"), actual);
    }

    private List<Queue.BuildableItem> singleItem() {

        final List<Queue.BuildableItem> items = ItemMock.list();
        final Set<Node> nodes = nodeFactory.set();

        nodes.add(nodeFactory.node("master", 2, 1));

        items.add(ItemMock.create(nodes, 2, "Single queue item", 3));

        return items;
    }

    @Test
    public void serializeSeveralItems() {

        final String actual = SERIALIZER.buildQuery(
                new StateProviderMock(severalItems(), nodes),
                NodeAssignments.builder()
                        .assign(4, "slave2")
                        .build()
        );

        assertEquals(fixture("severalItems.queue.json"), actual);
    }

    private List<Queue.BuildableItem> severalItems() {

        final List<Queue.BuildableItem> items = ItemMock.list();

        SortedSet<Node> nodes = nodeFactory.set();

        nodes.add(nodeFactory.node("master", 2, 1));

        items.add(ItemMock.create(nodes, 2, "Single queue item", 3));

        nodes = nodeFactory.set();

        nodes.add(nodeFactory.node("slave1", 7, 7));
        nodes.add(nodeFactory.node("slave2", 1, 0));

        items.add(ItemMock.create(nodes, 4, "raven_eap", 5));

        return items;
    }

    @Test
    public void serializeUnlabeledItem() {

        nodes.add(nodeFactory.node("slave_2:1", 2, 1));
        nodes.add(nodeFactory.node("slave_1:2", 1, 2));

        final String actual = SERIALIZER.buildQuery(
                new StateProviderMock(unlabeledItem(), nodes),
                NodeAssignments.empty()
        );

        assertEquals(fixture("unlabeledItem.queue.json"), actual);
    }

    private List<Queue.BuildableItem> unlabeledItem() {

        final List<Queue.BuildableItem> items = ItemMock.list();

        items.add(ItemMock.create(null, 2, "Unlabeled item", 3));

        return items;
    }

    @Test
    public void doNotOfferExclusiveNode() {

        final Node regular = nodeFactory.node("regular", 1, 1);
        final Node exclusive = mock(Node.class);
        when(exclusive.canTake(any(Queue.BuildableItem.class)))
            .thenReturn(new CauseOfBlockage() {

                @Override
                public String getShortDescription() {
                    return "COB";
                }
            })
        ;

        nodes.add(regular);
        nodes.add(exclusive);

        final Queue.BuildableItem item = ItemMock.create(
                new HashSet<Node>(nodes), 42, "item", 1
        );

        final JsonArray queue = serialize(Arrays.asList(item), this.nodes, NodeAssignments.empty());

        final JsonArray nodes = queue.get(0).getAsJsonObject().get("nodes").getAsJsonArray();

        assertThat(nodes.size(), equalTo(1));

        final String nodeName = nodes.get(0).getAsJsonObject().get("name").getAsString();
        assertThat(nodeName, equalTo("regular"));
    }

    @Test
    public void doNotOfferOfflineNode() {

        final NodeAssignments solution = NodeAssignments.builder()
                .assign(1, "offline")
                .build()
        ;

        final Node offline = nodeFactory.node("offline", 1, 1);

        when(offline.toComputer().isOffline()).thenReturn(true);

        nodes.add(offline);

        final Queue.BuildableItem item = ItemMock.create(
                new HashSet<Node>(nodes), 1, "item", 1
        );

        final JsonArray queue = serialize(Arrays.asList(item), this.nodes, solution);

        assertThat(queue.size(), equalTo(1));

        final JsonObject queueItem = queue.get(0).getAsJsonObject();

        assertThat(queueItem.get("nodes").getAsJsonArray().size(), equalTo(0));

        assertThat(queueItem.get("assigned"), instanceOf(JsonNull.class));
    }

    private JsonArray serialize(
            final List<Queue.BuildableItem> items,
            final List<Node> nodes,
            final NodeAssignments solution
    ) {

        final String actual = SERIALIZER.buildQuery(
                new StateProviderMock(items, nodes),
                solution
        );

        return new JsonParser().parse(actual).getAsJsonObject().get("queue").getAsJsonArray();
    }
}
