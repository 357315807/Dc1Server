package space.ponyo.dc1.server.server;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.ponyo.dc1.server.model.DataPool;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConnectionManager {
    public static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);
    private static final ConnectionManager instance = new ConnectionManager();
    public String token = "";

    public static ConnectionManager getInstance() {
        return instance;
    }

    private ConcurrentHashMap<String, DeviceConnection> mDeviceConnectionMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, PhoneConnection> mPhoneConnectionMap = new ConcurrentHashMap<>();

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    /**
     * 分发消息
     * @param channel 通道
     * @param msg 消息内容
     */
    public void dispatchMsg(Channel channel, String msg) {
        InetSocketAddress remoteAddress = (InetSocketAddress) (channel.remoteAddress());
        InetSocketAddress localAddress = (InetSocketAddress) (channel.localAddress());
        String ip = remoteAddress.getAddress().getHostAddress();
        int remotePort = remoteAddress.getPort();
        int localPort = localAddress.getPort();
        if (localPort == 8800) {
            executorService.execute(() -> {
                PhoneConnection pc = mPhoneConnectionMap.get(ip + ":" + remotePort);
                if (pc == null) {
                    logger.error("手机未上线 {}:{}", ip, localPort);
                } else {
                    pc.processMessage(msg);
                }
            });
        } else {
            executorService.execute(() -> {
                DeviceConnection dc = mDeviceConnectionMap.get(ip + ":" + remotePort);
                if (dc == null) {
                    logger.error("dc1设备未上线 {}:{}", ip, localPort);
                } else {
                    dc.processMessage(msg);
                }
            });
        }
    }

    public IConnection addChannel(Channel channel) {
        InetSocketAddress remoteAddress = (InetSocketAddress) (channel.remoteAddress());
        InetSocketAddress localAddress = (InetSocketAddress) (channel.localAddress());
        String ip = remoteAddress.getAddress().getHostAddress();
        int remotePort = remoteAddress.getPort();
        int localPort = localAddress.getPort();
        if (localPort == 8800) {
            //手机连接
            PhoneConnection connection = mPhoneConnectionMap.get(ip + ":" + remotePort);
            if (connection == null) {
                connection = new PhoneConnection();
                mPhoneConnectionMap.put(ip + ":" + remotePort, connection);
            }
            connection.setChannel(channel);
            return connection;
        } else {
            DeviceConnection connection = mDeviceConnectionMap.get(ip + ":" + remotePort);
            if (connection == null) {
                connection = new DeviceConnection();
                mDeviceConnectionMap.put(ip + ":" + remotePort, connection);
            }
            connection.setChannel(channel);
            return connection;
        }
    }

    public void removeChannel(Channel channel) {
        InetSocketAddress remoteAddress = (InetSocketAddress) (channel.remoteAddress());
        InetSocketAddress localAddress = (InetSocketAddress) (channel.localAddress());
        int remotePort = remoteAddress.getPort();
        String ip = remoteAddress.getAddress().getHostAddress();
        int localPort = localAddress.getPort();
        if (localPort == 8800) {
            //手机连接
            PhoneConnection phoneConnection = mPhoneConnectionMap.get(ip + ":" + remotePort);
            if (phoneConnection == null || phoneConnection.isActive()) {
                return;
            }
            mPhoneConnectionMap.remove(ip + ":" + remotePort);
        } else {
            DeviceConnection deviceConnection = mDeviceConnectionMap.get(ip + ":" + remotePort);
            if (deviceConnection == null || deviceConnection.isActive()) {
                return;
            }
            DataPool.offline(deviceConnection.getId());
            mDeviceConnectionMap.remove(ip + ":" + remotePort);
        }
    }

    public void setDc1Status(String id, String status) {
        mDeviceConnectionMap
                .values()
                .parallelStream()
                .filter(connection -> connection.getId().equals(id))
                .forEach(connection -> connection.setStatus(status));
    }

    /**
     * 推送设备最新数据
     */
    public void refreshPhoneDeviceData() {
        mPhoneConnectionMap
                .values()
                .parallelStream()
                .forEach(phoneConnection -> phoneConnection.processMessage("queryDevice " + token));
    }

    /**
     * 推送计划有更改
     *
     * @param planId
     */
    public void pushPlanDataChanged(String planId) {
        mPhoneConnectionMap
                .values()
                .parallelStream()
                .forEach(phoneConnection -> phoneConnection.pushMessage("planChanged " + planId));
    }
}
