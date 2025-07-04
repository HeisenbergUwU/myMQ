package io.github.heisenberguwu.myrocketmq.common.message;

import java.io.Serializable;

public class MessageQueue implements Comparable<MessageQueue>, Serializable {
    private static final long serialVersionUID = 6191200464116433425L;
    private String topic;
    private String brokerName;
    private int queueId; //

    public MessageQueue() {

    }


    public MessageQueue(MessageQueue other) {
        this.topic = other.topic;
        this.brokerName = other.brokerName;
        this.queueId = other.queueId;
    }

    public MessageQueue(String topic, String brokerName, int queueId) {
        this.topic = topic;
        this.brokerName = brokerName;
        this.queueId = queueId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public int getQueueId() {
        return queueId;
    }

    public void setQueueId(int queueId) {
        this.queueId = queueId;
    }

    @Override
    public String toString() {
        return "MessageQueue [topic=" + topic + ", brokerName=" + brokerName + ", queueId=" + queueId + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((brokerName == null) ? 0 : brokerName.hashCode());
        result = prime * result + queueId;
        result = prime * result + ((topic == null) ? 0 : topic.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) // 对比内存地址
        {
            return true;
        }
        if (obj == null) // 空对象
            return false;
        if (this.getClass() != obj.getClass())  // 不是同一类
        {
            return false;
        }
        MessageQueue other = (MessageQueue) obj;
        // brokerName 
        if (brokerName == null) {
            if (other.brokerName != null)
                return false;
        } else if (!brokerName.equals(other.brokerName)) {
            return false;
        }
        if (queueId != other.queueId) // 这是基本类型。
            return false;
        // topic
        if (topic == null) {
            if (other.topic != null)
                return false;
        } else if (!topic.equals(other.topic))
            return false;
        return true;
    }

    @Override
    public int compareTo(MessageQueue o) {
        // 函数内部作用域 - pName scope can see
        {
            int result = this.topic.compareTo(o.topic);
            if (result != 0) {
                return result;
            }
        }
        {
            int result = this.brokerName.compareTo(o.brokerName);
            if (result != 0) {
                return result;
            }
        }

        return this.queueId - o.queueId;
    }
}