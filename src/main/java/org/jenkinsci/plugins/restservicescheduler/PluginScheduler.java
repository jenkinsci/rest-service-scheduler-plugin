/*
 * The MIT License
 *
 * Copyright (c) 2013 Red Hat, Inc.
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

import hudson.Extension;
import hudson.model.PeriodicWork;
import hudson.util.FormValidation;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;

import org.jenkinsci.plugins.externalscheduler.ExternalScheduler;
import org.jenkinsci.plugins.externalscheduler.NodeAssignments;
import org.jenkinsci.plugins.externalscheduler.Scheduler;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class PluginScheduler extends Scheduler {

    private final static Logger LOGGER = Logger.getLogger(
            PluginScheduler.class.getName()
    );

    private static RemoteUpdater updater;
    private static RestScheduler restScheduler;

    private final String serverUrl;

    @DataBoundConstructor
    public PluginScheduler(final String serverUrl) {

        this.serverUrl = serverUrl;

        if (restScheduler != null) {

            try {
                restScheduler.stop();
            } catch (SchedulerException ex) {

                LOGGER.log(Level.INFO, "Failed stopping REST service scheduler at " + restScheduler.remoteUrl());
            }

            restScheduler = null;
        }

        restScheduler = startScheduler(serverUrl);
    }

    public String getServerUrl() {

        return serverUrl;
    }

    @Override
    public NodeAssignments solution() {

        return updater.currentAssignments;
    }

    public static RestScheduler startScheduler(final String url) {

        try {

            final RestScheduler restScheduler = getRestScheduler(url);
            restScheduler.queue(stateProvider(), NodeAssignments.empty());
            return restScheduler;
        } catch (MalformedURLException ex) {

            LOGGER.log(Level.INFO, "Failed starting REST service scheduler at " + url, ex);
        } catch (SchedulerException ex) {

            LOGGER.log(Level.INFO, "Failed starting REST service scheduler at " + url, ex);
        }

        return null;
    }

    private static RestScheduler getRestScheduler(final String serverUrl) throws MalformedURLException, SchedulerException {

        return new RestScheduler(new URL(serverUrl));
    }

    @Extension
    public static class Descriptor extends Scheduler.Descriptor {

        @Override
        public String getDisplayName() {

            return "Delegate scheduling to REST service";
        }

        public FormValidation doCheckServerUrl(@QueryParameter String serverUrl) {

            try {

                return FormValidation.ok(getRestScheduler(serverUrl).name());
            } catch(MalformedURLException ex) {

                return FormValidation.error(ex, "It is not URL");
            } catch(SchedulerException ex) {

                return FormValidation.warning(ex, "Server seems down or it is not an External scheduler REST service");
            }
        }
    }

    @Extension
    public static RemoteUpdater instantiateUpdater() {

        if (updater != null) throw new IllegalStateException(
                "Updater already initialized"
        );

        final ExternalScheduler external = Jenkins.getInstance().getPlugin(ExternalScheduler.class);
        updater = new RemoteUpdater(external);
        if (updater.getScheduler() != null) {

            restScheduler = startScheduler(updater.getScheduler().getServerUrl());
        }
        return updater;
    }

    public static class RemoteUpdater extends PeriodicWork {

        private final ExternalScheduler plugin;

        private NodeAssignments currentAssignments;

        public RemoteUpdater(final ExternalScheduler plugin) {

            if (plugin == null) throw new IllegalArgumentException("No plugin provided");

            this.plugin = plugin;
        }

        @Override
        public long getInitialDelay() {

            return 0;
        }

        @Override
        public long getRecurrencePeriod() {

            return 5 * 1000;
        }

        @Override
        protected void doRun() {

            final PluginScheduler scheduler = getScheduler();

            if (scheduler == null || PluginScheduler.restScheduler == null) return;

            try {

                currentAssignments = fetchSolution(scheduler);
                sendQueue(scheduler);
            } catch (SchedulerException ex) {

                // Rest Scheduler's sanity has been questioned.
                // Dispatcher will find this out sooner or later.
            }
        }

        private PluginScheduler getScheduler() {

            final Scheduler scheduler = plugin.activeScheduler();
            if (!(scheduler instanceof PluginScheduler)) return null;

            return (PluginScheduler) scheduler;
        }

        private NodeAssignments fetchSolution(final PluginScheduler scheduler) throws SchedulerException {

            final NodeAssignments oldSolution = plugin.currentSolution();

            final NodeAssignments solution = PluginScheduler.restScheduler.solution();

            if (queueUpdateNeeded(oldSolution, solution)) {

                Jenkins.getInstance().getQueue().scheduleMaintenance();
            }

            return solution;
        }

        private boolean queueUpdateNeeded(
                final NodeAssignments oldSolution, final NodeAssignments newSolution
        ) {

            return oldSolution == null
                    ? newSolution != null
                    : !oldSolution.equals(newSolution)
            ;
        }

        private boolean sendQueue(final PluginScheduler scheduler) throws SchedulerException {

            return PluginScheduler.restScheduler.queue(stateProvider(), plugin.currentSolution());
        }
    }
}
