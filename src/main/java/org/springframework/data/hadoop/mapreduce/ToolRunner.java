/*
 * Copyright 2011-2012 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.hadoop.mapreduce;

import java.util.List;
import java.util.concurrent.Callable;

import org.apache.hadoop.util.Tool;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.CollectionUtils;

/**
 * Wrapper around {@link org.apache.hadoop.util.ToolRunner} allowing for an easier configuration and execution
 * of {@link Tool}  instances inside Spring.
 * Optionally returns the execution result (as an int per {@link Tool#run(String[])}).
 * <p/>Note by default, the runner is configured to execute at startup. One can customize this behaviour through {@link #setRunAtStartup(boolean)}/
 * <p/>This class is a factory bean - if {@link #setRunAtStartup(boolean)} is set to false, then the action (namely the execution of the Tool) is postponed by the call
 * to {@link #getObject()}.
 * 
 * @author Costin Leau
 */
public class ToolRunner extends ToolExecutor implements Callable<Integer>, InitializingBean, BeanFactoryAware {

	private boolean runAtStartup = false;

	private List<String> preActions;
	private List<String> postActions;
	private BeanFactory beanFactory;

	@Override
	public Integer call() throws Exception {
		invoke(preActions);
		Integer result = runCode();
		invoke(postActions);
		return result;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();

		if (runAtStartup) {
			call();
		}
	}

	/**
	 * Indicates whether the tool should run at container startup (the default) or not.
	 *
	 * @param runAtStartup The runAtStartup to set.
	 */
	public void setRunAtStartup(boolean runAtStartup) {
		this.runAtStartup = runAtStartup;
	}

	/**
	 * Beans to be invoked before running the action.
	 * 
	 * @param beans
	 */
	public void setPreAction(String... beans) {
		this.preActions = CollectionUtils.arrayToList(beans);
	}

	/**
	 * Beans to be invoked after running the action.
	 * 
	 * @param beans
	 */
	public void setPostAction(String... beans) {
		this.postActions = CollectionUtils.arrayToList(beans);
	}

	private void invoke(List<String> beans) {
		if (beanFactory != null) {
			if (!CollectionUtils.isEmpty(beans)) {
				for (String bean : beans) {
					beanFactory.getBean(bean);
				}
			}
		}
		else {
			log.warn("No beanFactory set - cannot invoke pre/post actions [" + beans + "]");
		}
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}
}