package io.github.heisenberguwu.myrocketmq.common;

import io.github.heisenberguwu.myrocketmq.common.annotation.ImportantField;
import io.github.heisenberguwu.myrocketmq.common.constant.LoggerName;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class BrokerIdentity {
    private static final String DEFAULT_CLUSTER_NAME = "DefaultCluster";

    protected static final Logger LOGGER = LoggerFactory.getLogger(LoggerName.COMMON_LOGGER_NAME);

    private static String localHostName;

    static {
        try {
            localHostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            LOGGER.error("Failed to obtain the host name", e);
        }
    }

    // load it after the localHostName is initialized
    public static final BrokerIdentity BROKER_CONTAINER_IDENTITY = new BrokerIdentity(true);

    @ImportantField
    private String brokerName = defaultBrokerName();
    @ImportantField
    private String brokerClusterName = DEFAULT_CLUSTER_NAME;
    @ImportantField
    private volatile long brokerId = MixAll.MASTER_ID;

    private boolean isBrokerContainer = false;

    // Do not set it manually, it depends on the startup mode
    // Broker start by BrokerStartup is false, start or add by BrokerContainer is true
    private boolean isInBrokerContainer = false;

    public BrokerIdentity() {
    }

    public BrokerIdentity(boolean isBrokerContainer) {
        this.isBrokerContainer = isBrokerContainer;
    }

    public BrokerIdentity(String brokerClusterName, String brokerName, long brokerId) {
        this.brokerName = brokerName;
        this.brokerClusterName = brokerClusterName;
        this.brokerId = brokerId;
    }

    public BrokerIdentity(String brokerClusterName, String brokerName, long brokerId, boolean isInBrokerContainer) {
        this.brokerName = brokerName;
        this.brokerClusterName = brokerClusterName;
        this.brokerId = brokerId;
        this.isInBrokerContainer = isInBrokerContainer;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(final String brokerName) {
        this.brokerName = brokerName;
    }

    public String getBrokerClusterName() {
        return brokerClusterName;
    }

    public void setBrokerClusterName(final String brokerClusterName) {
        this.brokerClusterName = brokerClusterName;
    }

    public long getBrokerId() {
        return brokerId;
    }

    public void setBrokerId(final long brokerId) {
        this.brokerId = brokerId;
    }

    public boolean isInBrokerContainer() {
        return isInBrokerContainer;
    }

    public void setInBrokerContainer(boolean inBrokerContainer) {
        isInBrokerContainer = inBrokerContainer;
    }

    private String defaultBrokerName() {
        return StringUtils.isEmpty(localHostName) ? "DEFAULT_BROKER" : localHostName;
    }

    public String getCanonicalName() {
        return isBrokerContainer ? "BrokerContainer" : String.format("%s_%s_%d", brokerClusterName, brokerName,
                brokerId);
    }

    public String getIdentifier() {
        return "#" + getCanonicalName() + "#";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final BrokerIdentity identity = (BrokerIdentity) o;

        return new EqualsBuilder()
                .append(brokerId, identity.brokerId)
                .append(brokerName, identity.brokerName)
                .append(brokerClusterName, identity.brokerClusterName)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(brokerName)
                .append(brokerClusterName)
                .append(brokerId)
                .toHashCode();
    }
}
