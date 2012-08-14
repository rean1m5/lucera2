/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.catssoftware.util.concurrent;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.log4j.Logger;


/**
 * @author NB4L1
 */
public final class L2RejectedExecutionHandler implements RejectedExecutionHandler
{
	private static final Logger _log = Logger.getLogger(L2RejectedExecutionHandler.class);

	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor)
	{
		if (executor.isShutdown())
			return;

		_log.warn(r + " from " + executor, new RejectedExecutionException());

		if (Thread.currentThread().getPriority() > Thread.NORM_PRIORITY)
			new Thread(r).start();
		else
			r.run();
	}
}