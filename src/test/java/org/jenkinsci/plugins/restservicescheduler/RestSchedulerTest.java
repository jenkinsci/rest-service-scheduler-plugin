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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.net.MalformedURLException;
import java.net.URL;

import javax.ws.rs.core.MediaType;

import org.jenkinsci.plugins.restservicescheduler.RestScheduler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

@RunWith(PowerMockRunner.class)
@PrepareForTest(URL.class)
public class RestSchedulerTest {

    private URL serviceUrl;
    private Client client;

    private RestScheduler pp;

    @Before
    public void setUp() throws MalformedURLException, InterruptedException, SchedulerException {

        client = mock(Client.class);

        serviceUrl = mock(URL.class);
        when(serviceUrl.toString()).thenReturn("Fake url");

        useMeaningFullInfo();

        pp = new RestScheduler(serviceUrl, client);
    }

    private void useMeaningFullInfo() {

        WebResource r = mock(WebResource.class);
        WebResource.Builder rb = mock(WebResource.Builder.class);
        when(r.accept(MediaType.TEXT_PLAIN)).thenReturn(rb);

        when(client.resource(Mockito.endsWith("/info"))).thenReturn(r);

        when(rb.get(String.class)).thenReturn("info: hudson-queue-planning: Planner Mock");
    }

    @Test(expected = IllegalStateException.class)
    public void getScoreFromNotStarted() throws SchedulerException {

        pp.score();
    }

    @Test(expected = IllegalStateException.class)
    public void getSolutionFromNotStarted() throws SchedulerException {

        pp.solution();
    }

    @Test(expected = IllegalStateException.class)
    public void getScoreFromStopped() throws SchedulerException {

        pp.stop();
        pp.score();
    }

    @Test(expected = IllegalStateException.class)
    public void getSolutionFromStopped() throws SchedulerException {

        pp.stop();
        pp.solution();
    }

    @Test(expected = IllegalArgumentException.class)
    public void getInstanctWithoutUrl() throws SchedulerException {

        new RestScheduler(null);
    }

    @Test
    public void getUrl() throws MalformedURLException {

        assertSame(serviceUrl, pp.remoteUrl());
    }

    @Test
    public void checkName() {

        assertEquals("hudson-queue-planning: Planner Mock", pp.name());
    }
}
