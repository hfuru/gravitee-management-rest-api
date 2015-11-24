package io.gravitee.management.security.provider.spring;

import io.gravitee.management.security.provider.AuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class AuthenticationProviderBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(AuthenticationProviderBeanFactoryPostProcessor.class);

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        Set<String> authenticationProviders = new HashSet<>(
                SpringFactoriesLoader.loadFactoryNames(AuthenticationProvider.class, beanFactory.getBeanClassLoader()));

            LOGGER.info("\tFound {} {} implementation(s)",
                    authenticationProviders.size(), AuthenticationProvider.class.getSimpleName());

            DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) beanFactory;

            for (String authenticationProviderClass : authenticationProviders) {
                try {
                    Class<?> instanceClass = ClassUtils.forName(authenticationProviderClass, beanFactory.getBeanClassLoader());
                    Assert.isAssignable(AuthenticationProvider.class, instanceClass);

                    AuthenticationProvider authenticationProviderInstance =
                            createInstance((Class<AuthenticationProvider>) instanceClass);

                    LOGGER.info("Registering an authentication provider {} [{}]", instanceClass.getSimpleName(),
                            authenticationProviderInstance.type());

                    defaultListableBeanFactory.registerBeanDefinition(authenticationProviderInstance.getClass().getName(),
                            new RootBeanDefinition(authenticationProviderInstance.getClass().getName()));
                } catch (Exception ex) {
                    LOGGER.error("Unable to instantiate authentication provider: {}", ex);
                    throw new IllegalStateException("Unable to instantiate authentication provider: " + authenticationProviderClass, ex);
                }
            }
    }

    private <T> T createInstance(Class<T> clazz) throws Exception {
        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            LOGGER.error("Unable to instantiate class: {}", ex);
            throw ex;
        }
    }
}