package ${package};

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.universAAL.middleware.container.ModuleContext;
import org.universAAL.middleware.container.osgi.uAALBundleContainer;
import org.universAAL.middleware.container.utils.LogUtils;

public class ProjectActivator implements BundleActivator {

	static ModuleContext context;

	public void start(BundleContext arg0) throws Exception {
		context = uAALBundleContainer.THE_CONTAINER
                .registerModule(new Object[] {arg0});
		LogUtils.logDebug(context, getClass(), "start", "Starting.");
		/*
		 * uAAL stuff
		 */

		LogUtils.logDebug(context, getClass(), "start", "Started.");
	}


	public void stop(BundleContext arg0) throws Exception {
		LogUtils.logDebug(context, getClass(), "stop", "Stopping.");
		/*
		 * close uAAL stuff
		 */

		LogUtils.logDebug(context, getClass(), "stop", "Stopped.");

	}

}
