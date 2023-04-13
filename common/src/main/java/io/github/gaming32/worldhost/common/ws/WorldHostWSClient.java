package io.github.gaming32.worldhost.common.ws;

import io.github.gaming32.worldhost.common.WorldHostCommon;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Session;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Future;

public class WorldHostWSClient implements AutoCloseable {
    private final Session session;

    private boolean authenticated;

    private UUID connectionId = WorldHostCommon.CONNECTION_ID;
    private String baseIp = "";
    private int basePort;

    public WorldHostWSClient(URI serverUri) throws DeploymentException, IOException {
        session = ContainerProvider.getWebSocketContainer().connectToServer(WorldHostClientEndpoint.class, serverUri);
    }

    public Future<Void> authenticate(UUID userUuid) {
        authenticated = true;
        final ByteBuffer bb = ByteBuffer.allocate(32);
        bb.putLong(userUuid.getMostSignificantBits());
        bb.putLong(userUuid.getLeastSignificantBits());
        bb.putLong(connectionId.getMostSignificantBits());
        bb.putLong(connectionId.getLeastSignificantBits());
        bb.flip();
        return session.getAsyncRemote().sendBinary(bb);
    }

    private void ensureAuthenticated() {
        if (!authenticated) {
            throw new IllegalStateException("Attempted to communicate with server before authenticating.");
        }
    }

    public void listOnline(Collection<UUID> friends) {
        ensureAuthenticated();
        session.getAsyncRemote().sendObject(new WorldHostC2SMessage.ListOnline(friends));
    }

    public void publishedWorld(Collection<UUID> friends) {
        ensureAuthenticated();
        session.getAsyncRemote().sendObject(new WorldHostC2SMessage.PublishedWorld(friends));
    }

    public void closedWorld(Collection<UUID> friends) {
        ensureAuthenticated();
        session.getAsyncRemote().sendObject(new WorldHostC2SMessage.ClosedWorld(friends));
    }

    public void friendRequest(UUID friend) {
        ensureAuthenticated();
        session.getAsyncRemote().sendObject(new WorldHostC2SMessage.FriendRequest(friend));
    }

    public void queryRequest(Collection<UUID> friends) {
        ensureAuthenticated();
        session.getAsyncRemote().sendObject(new WorldHostC2SMessage.QueryRequest(friends));
    }

    public void requestJoin(UUID friend) {
        ensureAuthenticated();
        session.getAsyncRemote().sendObject(new WorldHostC2SMessage.RequestJoin(friend));
    }

    public void proxyS2CPacket(long connectionId, byte[] data) {
        ensureAuthenticated();
        session.getAsyncRemote().sendObject(new WorldHostC2SMessage.ProxyS2CPacket(connectionId, data));
    }

    public void proxyDisconnect(long connectionId) {
        ensureAuthenticated();
        session.getAsyncRemote().sendObject(new WorldHostC2SMessage.ProxyDisconnect(connectionId));
    }

    public UUID getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(UUID connectionId) {
        this.connectionId = connectionId;
    }

    public String getBaseIp() {
        return baseIp;
    }

    public void setBaseIp(String baseIp) {
        this.baseIp = baseIp;
    }

    public int getBasePort() {
        return basePort;
    }

    public void setBasePort(int basePort) {
        this.basePort = basePort;
    }

    @Override
    public void close() throws IOException {
        session.close();
    }
}
