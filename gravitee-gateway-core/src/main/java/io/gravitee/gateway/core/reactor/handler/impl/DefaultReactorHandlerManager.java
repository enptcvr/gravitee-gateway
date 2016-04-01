/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.core.reactor.handler.impl;

import io.gravitee.gateway.core.reactor.handler.ReactorHandler;
import io.gravitee.gateway.core.reactor.handler.ReactorHandlerFactory;
import io.gravitee.gateway.core.reactor.handler.ReactorHandlerManager;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Constructor;
import java.util.*;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class DefaultReactorHandlerManager implements ReactorHandlerManager, ApplicationContextAware {

    private ApplicationContext applicationContext;
    private Collection<ReactorHandlerFactory> reactorHandlerFactories;

    public ReactorHandler create(Object item) {
        if (reactorHandlerFactories == null) {
            reactorHandlerFactories = (Collection) getSpringFactoriesInstances(ReactorHandlerFactory.class);
        }

        return reactorHandlerFactories.iterator().next().create(item);
    }

    private <T> Collection<? extends T> getSpringFactoriesInstances(Class<T> type) {
        return getSpringFactoriesInstances(type, new Class<?>[] {});
    }

    private <T> Collection<? extends T> getSpringFactoriesInstances(Class<T> type,
                                                                    Class<?>[] parameterTypes, Object... args) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        // Use names and ensure unique to protect against duplicates
        Set<String> names = new LinkedHashSet<>(
                SpringFactoriesLoader.loadFactoryNames(type, classLoader));
        List<T> instances = createSpringFactoriesInstances(type, parameterTypes,
                classLoader, args, names);
        AnnotationAwareOrderComparator.sort(instances);
        return instances;
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> createSpringFactoriesInstances(Class<T> type,
                                                       Class<?>[] parameterTypes, ClassLoader classLoader, Object[] args,
                                                       Set<String> names) {
        List<T> instances = new ArrayList<>(names.size());
        for (String name : names) {
            try {
                Class<?> instanceClass = ClassUtils.forName(name, classLoader);
                Assert.isAssignable(type, instanceClass);
                Constructor<?> constructor = instanceClass
                        .getDeclaredConstructor(parameterTypes);
                T instance = (T) BeanUtils.instantiateClass(constructor, args);
                ((AbstractApplicationContext) applicationContext)
                        .getBeanFactory().autowireBean(instance);
                instances.add(instance);
            }
            catch (Throwable ex) {
                throw new IllegalArgumentException(
                        "Cannot instantiate " + type + " : " + name, ex);
            }
        }
        return instances;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
