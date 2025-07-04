package io.github.heisenberguwu.myrocketmq.common.logging;

import org.apache.rocketmq.logging.ch.qos.logback.classic.ClassicConstants;
import org.apache.rocketmq.logging.ch.qos.logback.classic.LoggerContext;
import org.apache.rocketmq.logging.ch.qos.logback.classic.util.DefaultJoranConfigurator;
import org.apache.rocketmq.logging.ch.qos.logback.core.LogbackException;
import org.apache.rocketmq.logging.ch.qos.logback.core.joran.spi.JoranException;
import org.apache.rocketmq.logging.ch.qos.logback.core.status.InfoStatus;
import org.apache.rocketmq.logging.ch.qos.logback.core.status.StatusManager;
import org.apache.rocketmq.logging.ch.qos.logback.core.util.Loader;
import org.apache.rocketmq.logging.ch.qos.logback.core.util.OptionHelper;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DefaultJoranConfiguratorExt extends DefaultJoranConfigurator {
    // 默认的配置文件
    final public static String TEST_AUTOCONFIG_FILE = "rmq.logback-test.xml";
    final public static String AUTOCONFIG_FILE = "rmq.logback.xml";

    final public static String PROXY_AUTOCONFIG_FILE = "rmq.proxy.logback.xml";
    final public static String BROKER_AUTOCONFIG_FILE = "rmq.broker.logback.xml";

    final public static String NAMESRV_AUTOCONFIG_FILE = "rmq.namesrv.logback.xml";
    final public static String CONTROLLER_AUTOCONFIG_FILE = "rmq.controller.logback.xml";
    final public static String TOOLS_AUTOCONFIG_FILE = "rmq.tools.logback.xml";

    final public static String CLIENT_AUTOCONFIG_FILE = "rmq.client.logback.xml";

    private final List<String> configFiles;

    public DefaultJoranConfiguratorExt() {
        this.configFiles = new ArrayList<>();
        configFiles.add(TEST_AUTOCONFIG_FILE);
        configFiles.add(AUTOCONFIG_FILE);
        configFiles.add(PROXY_AUTOCONFIG_FILE);
        configFiles.add(BROKER_AUTOCONFIG_FILE);
        configFiles.add(NAMESRV_AUTOCONFIG_FILE);
        configFiles.add(CONTROLLER_AUTOCONFIG_FILE);
        configFiles.add(TOOLS_AUTOCONFIG_FILE);
        configFiles.add(CLIENT_AUTOCONFIG_FILE);
    }

    @Override
    public ExecutionStatus configure(LoggerContext loggerContext) {
        URL url = findURLOfDefaultConfigurationFile(true);
        if (url != null) {
            try {
                configureByResource(url);
            } catch (JoranException e) {
                e.printStackTrace();
            }
        }
        // skip other configurator on purpose.
        return ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY;
    }

    public void configureByResource(URL url) throws JoranException {
        if (url == null) {
            throw new IllegalArgumentException("URL argument cannot be null");
        }
        final String urlString = url.toString();
        if (urlString.endsWith("xml")) {
            JoranConfiguratorExt configurator = new JoranConfiguratorExt();
            configurator.setContext(context);
            configurator.doConfigure0(url);
        } else {
            throw new LogbackException(
                    "Unexpected filename extension of file [" + url + "]. Should be .xml");
        }
    }

    public URL findURLOfDefaultConfigurationFile(boolean updateStatus) {
        ClassLoader myClassLoader = Loader.getClassLoaderOfObject(this);
        URL url = findConfigFileURLFromSystemProperties(myClassLoader, updateStatus);
        if (url != null) {
            return url;
        }

        for (String configFile : configFiles) {
            url = getResource(configFile, myClassLoader, updateStatus);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    private URL findConfigFileURLFromSystemProperties(ClassLoader classLoader, boolean updateStatus) {
        String logbackConfigFile = OptionHelper.getSystemProperty(ClassicConstants.CONFIG_FILE_PROPERTY);
        if (logbackConfigFile != null) {
            URL result = null;
            try {
                result = new URL(logbackConfigFile);
                return result;
            } catch (MalformedURLException e) {
                // so, resource is not a URL:
                // attempt to get the resource from the class path
                result = Loader.getResource(logbackConfigFile, classLoader);
                if (result != null) {
                    return result;
                }
                File f = new File(logbackConfigFile);
                if (f.exists() && f.isFile()) {
                    try {
                        result = f.toURI().toURL();
                        return result;
                    } catch (MalformedURLException ignored) {
                    }
                }
            } finally {
                if (updateStatus) {
                    statusOnResourceSearch(logbackConfigFile, classLoader, result);
                }
            }
        }
        return null;
    }

    private URL getResource(String filename, ClassLoader myClassLoader, boolean updateStatus) {
        URL url = Loader.getResource(filename, myClassLoader);
        if (updateStatus) {
            statusOnResourceSearch(filename, myClassLoader, url);
        }
        return url;
    }

    private void statusOnResourceSearch(String resourceName, ClassLoader classLoader, URL url) {
        StatusManager sm = context.getStatusManager();
        if (url == null) {
            sm.add(new InfoStatus("Could NOT find resource [" + resourceName + "]", context));
        } else {
            sm.add(new InfoStatus("Found resource [" + resourceName + "] at [" + url.toString() + "]", context));
            multiplicityWarning(resourceName, classLoader);
        }
    }

    private void multiplicityWarning(String resourceName, ClassLoader classLoader) {
        Set<URL> urlSet = null;
        try {
            urlSet = Loader.getResources(resourceName, classLoader);
        } catch (IOException e) {
            addError("Failed to get url list for resource [" + resourceName + "]", e);
        }
        if (urlSet != null && urlSet.size() > 1) {
            addWarn("Resource [" + resourceName + "] occurs multiple times on the classpath.");
            for (URL url : urlSet) {
                addWarn("Resource [" + resourceName + "] occurs at [" + url.toString() + "]");
            }
        }
    }
}
