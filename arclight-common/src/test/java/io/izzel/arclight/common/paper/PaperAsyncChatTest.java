/* SPDX-License-Identifier: GPL-3.0-or-later */
package io.izzel.arclight.common.paper;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperAsyncChatTest {

    @Test
    void anUnchangedMessageStaysAsItWas() {
        AsyncPlayerChatEvent legacy = legacy("hello");
        Component original = PaperJoinMessages.read("hello");
        PaperAsyncChat.apply(legacy, original, event(legacy, original));
        assertEquals("hello", legacy.getMessage());
        assertFalse(legacy.isCancelled());
    }

    @Test
    void aChangedComponentIsWrittenBackAsASectionString() {
        AsyncPlayerChatEvent legacy = legacy("hello");
        Component original = PaperJoinMessages.read("hello");
        AsyncChatEvent paper = event(legacy, original);
        paper.message(Component.text("A", NamedTextColor.RED));
        PaperAsyncChat.apply(legacy, original, paper);
        assertEquals("§cA", legacy.getMessage());
    }

    @Test
    void theRendererIsWrittenBackForTheLegacyBroadcast() {
        AsyncPlayerChatEvent legacy = legacy("hello");
        Component original = PaperJoinMessages.read("hello");
        AsyncChatEvent paper = event(legacy, original);
        paper.renderer((source, displayName, message, viewer) -> Component.text("seen"));
        PaperAsyncChat.apply(legacy, original, paper);
        assertEquals("seen", legacy.getMessage());
        assertFalse(legacy.isCancelled());
    }

    @Test
    void aCancelledLegacyEventDoesNotFirePaper() {
        AsyncPlayerChatEvent legacy = legacy("hello");
        legacy.setCancelled(true);
        AtomicBoolean called = new AtomicBoolean();
        PaperAsyncChat.fire(plugins(called, null), legacy, true);
        assertFalse(called.get());
        assertEquals("hello", legacy.getMessage());
    }

    @Test
    void viewersFollowTheRecipientSetAndCanSend() {
        AtomicReference<String> sent = new AtomicReference<>();
        Player ada = player(sent);
        AsyncPlayerChatEvent legacy = new AsyncPlayerChatEvent(false, ada, "hello", new HashSet<>(Set.of(ada)));
        AtomicBoolean called = new AtomicBoolean();
        PaperAsyncChat.fire(plugins(called, sent), legacy, true);
        assertTrue(called.get());
        assertEquals("hello", legacy.getMessage());
    }

    @Test
    void cancellationReachesTheStringEvent() {
        AsyncPlayerChatEvent legacy = legacy("hello");
        Component original = PaperJoinMessages.read("hello");
        AsyncChatEvent paper = event(legacy, original);
        paper.setCancelled(true);
        PaperAsyncChat.apply(legacy, original, paper);
        assertTrue(legacy.isCancelled());
        assertEquals("hello", legacy.getMessage());
    }

    private static AsyncPlayerChatEvent legacy(String message) {
        return new AsyncPlayerChatEvent(false, player(), message, new HashSet<>());
    }

    private static AsyncChatEvent event(AsyncPlayerChatEvent legacy, Component original) {
        return new AsyncChatEvent(false, legacy.getPlayer(), Set.of(),
            (source, displayName, message, viewer) -> message, original, original, null);
    }

    private static PluginManager plugins(AtomicBoolean called, AtomicReference<String> sent) {
        return (PluginManager) Proxy.newProxyInstance(PluginManager.class.getClassLoader(),
            new Class<?>[] {PluginManager.class}, (proxy, method, args) -> {
                if ("callEvent".equals(method.getName()) && args[0] instanceof AsyncChatEvent event) {
                    called.set(true);
                    Player ada = event.getPlayer();
                    assertTrue(event.viewers().contains(ada));
                    Audience viewer = event.viewers().iterator().next();
                    assertTrue(event.viewers().contains(viewer));
                    if (sent != null) {
                        viewer.sendMessage(Component.text("hi"));
                        assertEquals("hi", sent.get());
                    }
                    assertTrue(event.viewers().remove(ada));
                    assertFalse(event.viewers().contains(ada));
                }
                Class<?> type = method.getReturnType();
                if (type == boolean.class) {
                    return false;
                }
                if (type == int.class) {
                    return 0;
                }
                return null;
            });
    }

    private static Player player() {
        return player(null);
    }

    private static Player player(AtomicReference<String> sent) {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[] {Player.class},
            (proxy, method, args) -> {
                if ("getUniqueId".equals(method.getName())) {
                    return new UUID(0L, 1L);
                }
                if ("getName".equals(method.getName()) || "getDisplayName".equals(method.getName())) {
                    return "Ada";
                }
                if ("sendMessage".equals(method.getName()) && sent != null && args != null && args.length == 1
                    && args[0] instanceof String text) {
                    sent.set(text);
                    return null;
                }
                Class<?> type = method.getReturnType();
                if (type == boolean.class) {
                    return false;
                }
                if (type == int.class) {
                    return 0;
                }
                if (type == long.class) {
                    return 0L;
                }
                return null;
            });
    }
}
