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
package org.jenkinsci.plugins.restservicescheduler;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import hudson.model.Queue;
import jenkins.model.Jenkins;

import org.jenkinsci.plugins.externalscheduler.ExternalScheduler;
import org.jenkinsci.plugins.externalscheduler.NodeAssignments;
import org.jenkinsci.plugins.externalscheduler.StateProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ExternalScheduler.class, RestScheduler.class, Jenkins.class})
public class RemoteUpdaterTest {

    @Mock private ExternalScheduler externalScheduler;
    @Mock private PluginScheduler pluginScheduler;
    @Mock private RestScheduler restScheduler;
    @Mock private Jenkins jenkins;
    @Mock private Queue queue;

    private PluginScheduler.RemoteUpdater updater;

    @Before
    public void setUp() {

        MockitoAnnotations.initMocks(this);

        updater = new PluginScheduler.RemoteUpdater(externalScheduler);

        PowerMockito.mockStatic(Jenkins.class);
        when(Jenkins.getInstance()).thenReturn(jenkins);
        when(jenkins.getQueue()).thenReturn(queue);
    }

    @After
    public void tearDown() {

        verifyNoMoreInteractions(restScheduler);
    }

    @Test(expected = IllegalArgumentException.class)
    public void doNotInstantiateWithoutPlugin() {

        new PluginScheduler.RemoteUpdater(null);
    }

    @Test
    public void fetchAndSend() throws Exception {

        final NodeAssignments currentSolution = NodeAssignments.builder().assign(0, "master").build();
        final NodeAssignments newSolution = NodeAssignments.builder().assign(1, "slave").build();

        when(externalScheduler.activeScheduler()).thenReturn(pluginScheduler);
        Whitebox.setInternalState(PluginScheduler.class, "restScheduler", restScheduler);
        when(externalScheduler.currentSolution()).thenReturn(currentSolution);

        when(restScheduler.solution()).thenReturn(newSolution);

        updater.doRun();

        verify(restScheduler).solution();
        verify(restScheduler).queue(any(StateProvider.class), same(currentSolution));
        verify(queue).scheduleMaintenance();
    }
}
