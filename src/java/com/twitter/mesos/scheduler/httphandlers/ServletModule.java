package com.twitter.mesos.scheduler.httphandlers;

import javax.inject.Singleton;

import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import com.twitter.common.application.modules.LocalServiceRegistry;
import com.twitter.common.application.http.Registration;
import com.twitter.common.application.modules.LifecycleModule;
import com.twitter.common.base.ExceptionalCommand;
import com.twitter.common.net.pool.DynamicHostSet;
import com.twitter.common.net.pool.DynamicHostSet.MonitorException;
import com.twitter.mesos.scheduler.ClusterName;
import com.twitter.mesos.scheduler.CronJobManager;
import com.twitter.mesos.scheduler.LeaderRedirect;
import com.twitter.mesos.scheduler.SchedulerCore;
import com.twitter.mesos.scheduler.quota.QuotaManager;
import com.twitter.thrift.ServiceInstance;

/**
 * Binding module for scheduler HTTP servlets.
 *
 * @author William Farner
 */
public class ServletModule extends AbstractModule {
  @Override
  protected void configure() {
    requireBinding(SchedulerCore.class);
    requireBinding(CronJobManager.class);
    requireBinding(Key.get(String.class, ClusterName.class));
    requireBinding(QuotaManager.class);

    // Bindings required for the leader redirector.
    requireBinding(LocalServiceRegistry.class);
    requireBinding(Key.get(new TypeLiteral<DynamicHostSet<ServiceInstance>>() { }));

    Registration.registerServlet(binder(), "/slaves", Slaves.class, false);
    Registration.registerServlet(binder(), "/scheduler", SchedulerzHome.class, false);
    Registration.registerServlet(binder(), "/scheduler/role", SchedulerzRole.class, true);
    Registration.registerServlet(binder(), "/scheduler/job", SchedulerzJob.class, true);
    Registration.registerServlet(binder(), "/mname", Mname.class, false);

    // Static assets.
    Registration.registerHttpAsset(binder(), "/js/util.js", ServletModule.class,
        "assets/util.js", "application/javascript", true);
    Registration.registerHttpAsset(binder(), "/assets/thermos.png", ServletModule.class,
        "assets/thermos.png", "image/png", true);

    bind(LeaderRedirect.class).in(Singleton.class);
    LifecycleModule.bindStartupAction(binder(), RedirectMonitor.class);
  }

  static class RedirectMonitor implements ExceptionalCommand<MonitorException> {

    private final LeaderRedirect redirector;

    @Inject
    RedirectMonitor(LeaderRedirect redirector) {
      this.redirector = Preconditions.checkNotNull(redirector);
    }

    @Override public void execute() throws MonitorException {
      redirector.monitor();
    }
  }
}
