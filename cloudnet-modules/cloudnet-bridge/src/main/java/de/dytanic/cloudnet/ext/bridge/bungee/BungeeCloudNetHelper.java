/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.ext.bridge.bungee;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.PluginInfo;
import de.dytanic.cloudnet.ext.bridge.bungee.event.BungeePlayerFallbackEvent;
import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.proxy.BridgeProxyHelper;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public final class BungeeCloudNetHelper {

  /**
   * @deprecated use {@link BridgeProxyHelper#getCachedServiceInfoSnapshot(String)} or {@link
   * BridgeProxyHelper#cacheServiceInfoSnapshot(ServiceInfoSnapshot)}
   */
  @Deprecated
  public static final Map<String, ServiceInfoSnapshot> SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION = BridgeProxyHelper.SERVICE_CACHE;
  private static int lastOnlineCount = -1;

  private BungeeCloudNetHelper() {
    throw new UnsupportedOperationException();
  }

  public static int getLastOnlineCount() {
    return lastOnlineCount;
  }

  public static boolean isOnMatchingFallbackInstance(ProxiedPlayer player) {
    String currentServer = player.getServer() == null ? null : player.getServer().getInfo().getName();

    if (currentServer != null) {
      ServiceInfoSnapshot currentService = BridgeProxyHelper.getCachedServiceInfoSnapshot(currentServer);

      if (currentService != null) {
        return BridgeProxyHelper.filterPlayerFallbacks(
          player.getUniqueId(),
          currentServer,
          player.getPendingConnection().getVirtualHost().getHostString(),
          player::hasPermission
        ).anyMatch(proxyFallback ->
          proxyFallback.getTask().equals(currentService.getServiceId().getTaskName()));
      }
    }

    return false;
  }

  public static boolean isFallbackServer(ServerInfo serverInfo) {
    if (serverInfo == null) {
      return false;
    }
    return BridgeProxyHelper.isFallbackService(serverInfo.getName());
  }

  public static Optional<ServerInfo> getNextFallback(ProxiedPlayer player, ServerInfo currentServer) {
    return BridgeProxyHelper.getNextFallback(
      player.getUniqueId(),
      currentServer != null ? currentServer.getName() : null,
      player.getPendingConnection().getVirtualHost().getHostString(),
      player::hasPermission
    ).map(serviceInfoSnapshot -> ProxyServer.getInstance().getPluginManager().callEvent(
      new BungeePlayerFallbackEvent(player, serviceInfoSnapshot, serviceInfoSnapshot.getName())
    )).map(BungeePlayerFallbackEvent::getFallbackName)
      .map(fallback -> ProxyServer.getInstance().getServerInfo(fallback));
  }

  public static CompletableFuture<ServiceInfoSnapshot> connectToFallback(ProxiedPlayer player, String currentServer) {
    return BridgeProxyHelper.connectToFallback(player.getUniqueId(),
      currentServer,
      player.getPendingConnection().getVirtualHost().getHostString(),
      player::hasPermission,
      serviceInfoSnapshot -> {
        BungeePlayerFallbackEvent event = new BungeePlayerFallbackEvent(player, serviceInfoSnapshot,
          serviceInfoSnapshot.getName());
        ProxyServer.getInstance().getPluginManager().callEvent(event);
        if (event.getFallbackName() == null) {
          return CompletableFuture.completedFuture(false);
        }

        ServerInfo serverInfo = ProxyServer.getInstance().getServerInfo(event.getFallbackName());
        if (serverInfo == null) {
          return CompletableFuture.completedFuture(false);
        }

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        player.connect(serverInfo, (result, error) -> future.complete(result && error == null));
        return future;
      }
    );
  }

  public static boolean isServiceEnvironmentTypeProvidedForBungeeCord(ServiceInfoSnapshot serviceInfoSnapshot) {
    Preconditions.checkNotNull(serviceInfoSnapshot);
    ServiceEnvironmentType currentServiceEnvironment = Wrapper.getInstance().getCurrentServiceInfoSnapshot()
      .getServiceId().getEnvironment();
    return (serviceInfoSnapshot.getServiceId().getEnvironment().isMinecraftJavaServer() && currentServiceEnvironment
      .isMinecraftJavaProxy())
      || (serviceInfoSnapshot.getServiceId().getEnvironment().isMinecraftBedrockServer() && currentServiceEnvironment
      .isMinecraftBedrockProxy());
  }

  public static void init() {
    BridgeProxyHelper.setMaxPlayers(ProxyServer.getInstance().getConfig().getPlayerLimit());
  }

  public static void initProperties(ServiceInfoSnapshot serviceInfoSnapshot) {
    Preconditions.checkNotNull(serviceInfoSnapshot);

    lastOnlineCount = ProxyServer.getInstance().getPlayers().size();

    serviceInfoSnapshot.getProperties()
      .append("Online", BridgeHelper.isOnline())
      .append("Version", ProxyServer.getInstance().getVersion())
      .append("Game-Version", ProxyServer.getInstance().getGameVersion())
      .append("Online-Count", ProxyServer.getInstance().getOnlineCount())
      .append("Max-Players", BridgeProxyHelper.getMaxPlayers())
      .append("Channels", ProxyServer.getInstance().getChannels())
      .append("BungeeCord-Name", ProxyServer.getInstance().getName())
      .append("Players",
        ProxyServer.getInstance().getPlayers().stream().map(proxiedPlayer -> new BungeeCloudNetPlayerInfo(
          proxiedPlayer.getUniqueId(),
          proxiedPlayer.getName(),
          proxiedPlayer.getServer() != null ? proxiedPlayer.getServer().getInfo().getName() : null,
          proxiedPlayer.getPing(),
          new HostAndPort(proxiedPlayer.getPendingConnection().getAddress())
        )).collect(Collectors.toList()))
      .append("Plugins", ProxyServer.getInstance().getPluginManager().getPlugins().stream().map(plugin -> {
        PluginInfo pluginInfo = new PluginInfo(plugin.getDescription().getName(), plugin.getDescription().getVersion());

        pluginInfo.getProperties()
          .append("author", plugin.getDescription().getAuthor())
          .append("main-class", plugin.getDescription().getMain())
          .append("depends", plugin.getDescription().getDepends())
        ;

        return pluginInfo;
      }).collect(Collectors.toList()));
  }

  public static NetworkConnectionInfo createNetworkConnectionInfo(PendingConnection pendingConnection) {
    return BridgeHelper.createNetworkConnectionInfo(
      pendingConnection.getUniqueId(),
      pendingConnection.getName(),
      pendingConnection.getVersion(),
      new HostAndPort(pendingConnection.getAddress()),
      new HostAndPort(pendingConnection.getListener().getHost()),
      pendingConnection.isOnlineMode(),
      pendingConnection.isLegacy(),
      BridgeHelper.createOwnNetworkServiceInfo()
    );
  }

  public static ServerInfo createServerInfo(String name, InetSocketAddress address) {
    Preconditions.checkNotNull(name);
    Preconditions.checkNotNull(address);

    // with rakNet enabled to support bedrock servers on Waterdog
    if (Wrapper.getInstance().getCurrentServiceInfoSnapshot().getServiceId().getEnvironment()
      == ServiceEnvironmentType.WATERDOG) {
      try {
        Class<ProxyServer> proxyServerClass = ProxyServer.class;

        Method method = proxyServerClass.getMethod("constructServerInfo",
          String.class, SocketAddress.class, String.class, boolean.class, boolean.class, String.class);
        method.setAccessible(true);
        return (ServerInfo) method
          .invoke(ProxyServer.getInstance(), name, address, "CloudNet provided serverInfo", false, true, "default");
      } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
        Wrapper.getInstance().getLogger()
          .log(LogLevel.ERROR, "Unable to enable rakNet, although using Waterdog: ", exception);
      }
    }

    return ProxyServer.getInstance().constructServerInfo(name, address, "CloudNet provided serverInfo", false);
  }
}
