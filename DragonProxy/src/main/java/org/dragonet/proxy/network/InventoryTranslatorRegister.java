/*
 * GNU LESSER GENERAL PUBLIC LICENSE
 *                       Version 3, 29 June 2007
 *
 * Copyright (C) 2007 Free Software Foundation, Inc. <http://fsf.org/>
 * Everyone is permitted to copy and distribute verbatim copies
 * of this license document, but changing it is not allowed.
 *
 * You can view LICENCE file for details. 
 *
 * @author The Dragonet Team
 */
package org.dragonet.proxy.network;

import java.util.HashMap;
import java.util.Map;

import org.dragonet.proxy.DragonProxy;
import org.dragonet.proxy.network.cache.CachedWindow;
import org.dragonet.proxy.network.translator.InventoryTranslator;
import org.dragonet.proxy.network.translator.inv.ChestWindowTranslator;
import org.spacehq.mc.protocol.data.game.entity.metadata.Position;
import org.spacehq.mc.protocol.data.game.window.WindowType;
import org.spacehq.mc.protocol.packet.ingame.client.window.ClientCloseWindowPacket;
import org.spacehq.mc.protocol.packet.ingame.server.window.ServerOpenWindowPacket;
import org.spacehq.mc.protocol.packet.ingame.server.window.ServerSetSlotPacket;
import org.spacehq.mc.protocol.packet.ingame.server.window.ServerWindowItemsPacket;
import org.spacehq.packetlib.packet.Packet;

import cn.nukkit.inventory.InventoryType;
import net.marfgamer.jraknet.RakNetPacket;
import sul.protocol.pocket101.play.ContainerSetContent;
import sul.protocol.pocket101.types.Slot;
import sul.utils.Item;

import org.dragonet.inventory.PEInventorySlot;

public final class InventoryTranslatorRegister {

    public final static int[] HOTBAR_CONSTANTS = new int[]{36, 37, 38, 39, 40, 41, 42, 43, 44};

    public static Object[] sendPlayerInventory(ClientConnection session) {
        CachedWindow win = session.getWindowCache().getPlayerInventory();
        //Translate and send
        ContainerSetContent ret = new ContainerSetContent();
        ret.window = (byte) InventoryType.PLAYER.getNetworkType();
        ret.slots = new Slot[45];
        for (int i = 9; i < win.slots.length; i++) {
            //TODO: Add NBT support
            if (win.slots[i] != null) {
            	try {
            		Item item = PEInventorySlot.fromItemStack(win.slots[i]);
                	ret.slots[i - 9] = new Slot(item.id, item.meta, new byte[0]);
            	} catch (NullPointerException e)	{
            		ret.slots[i - 9] = new Slot(0, 0, new byte[0]);
            	}
            }
        }
        for (int i = 36; i < 45; i++) {
            ret.slots[i] = ret.slots[i - 9];    //Duplicate
        }
        
        for (int i = 0; i < 45; i++)	{
        	if (ret.slots[i] == null)	{
        		ret.slots[i] = new Slot(0, 0, new byte[0]);
        	}
        }
        ret.hotbar = HOTBAR_CONSTANTS;

        //TODO: Add armor support
        return new RakNetPacket[]{new RakNetPacket(ret.encode())};
    }

    // PC Type => PE Translator
    private final static Map<WindowType, InventoryTranslator> TRANSLATORS = new HashMap<>();

    static {
        TRANSLATORS.put(WindowType.CHEST, new ChestWindowTranslator());
    }

    public static void closeOpened(ClientConnection session, boolean byServer) {
        /*if (session.getDataCache().containsKey(CacheKey.WINDOW_OPENED_ID)) {
            //There is already a window opened
            int id = (int) session.getDataCache().remove(CacheKey.WINDOW_OPENED_ID);
            if (!byServer) {
                session.getDownstream().send(new ClientCloseWindowPacket(id));
            }
            if (session.getDataCache().containsKey(CacheKey.WINDOW_BLOCK_POSITION)) {
                //Already a block was replaced to Chest, reset it
                session.sendFakeBlock(
                        ((Position) session.getDataCache().get(CacheKey.WINDOW_BLOCK_POSITION)).getX(),
                        ((Position) session.getDataCache().get(CacheKey.WINDOW_BLOCK_POSITION)).getY(),
                        ((Position) session.getDataCache().remove(CacheKey.WINDOW_BLOCK_POSITION)).getZ(),
                        1, //Set to stone since we don't know what it was, server will correct it once client interacts it
                        0);
            }
            if (byServer) {
                ContainerClosePacket pkClose = new ContainerClosePacket();
                pkClose.windowid = (byte) (id & 0xFF);
                session.sendPacket(pkClose, true);
            }
        }*/
    }

    public static void open(ClientConnection session, ServerOpenWindowPacket win) {
        /*closeOpened(session, true);
        if (TRANSLATORS.containsKey(win.getType())) {
            CachedWindow cached = new CachedWindow(win.getWindowId(), win.getType(), 36 + win.getSlots());
            session.getWindowCache().cacheWindow(cached);
            TRANSLATORS.get(win.getType()).open(session, cached);

            Packet[] items = session.getWindowCache().getCachedPackets(win.getWindowId());
            for (Packet item : items) {
                if (item != null) {
                    if (ServerWindowItemsPacket.class.isAssignableFrom(item.getClass())) {
                        updateContent(session, (ServerWindowItemsPacket) item);
                    } else {
                        updateSlot(session, (ServerSetSlotPacket) item);
                    }
                }
            }
        } else {
            //Not supported
            session.getDownstream().send(new ClientCloseWindowPacket(win.getWindowId()));
        }*/
    }

    public static void updateSlot(ClientConnection session, ServerSetSlotPacket packet) {
        /*if (packet.getWindowId() == 0) {
            return;   //We don't process player inventory updates here. 
        }
        if (!session.getDataCache().containsKey(CacheKey.WINDOW_OPENED_ID) || !session.getWindowCache().hasWindow(packet.getWindowId())) {
            session.getDownstream().send(new ClientCloseWindowPacket(packet.getWindowId()));
            return;
        }
        int openedId = (int) session.getDataCache().get(CacheKey.WINDOW_OPENED_ID);
        if (packet.getWindowId() != openedId) {
            //Hmm
            closeOpened(session, true);
            session.getDownstream().send(new ClientCloseWindowPacket(packet.getWindowId()));
            return;
        }
        CachedWindow win = session.getWindowCache().get(openedId);
        DragonProxy.getLogger().info("WIN=" + win.slots.length + ", REQ_SLOT=" + packet.getSlot());
        if (win.size <= packet.getSlot()) {
            session.getDownstream().send(new ClientCloseWindowPacket(packet.getWindowId()));
            return;
        }
        InventoryTranslator t = TRANSLATORS.get(win.pcType);
        if (t == null) {
            session.getDownstream().send(new ClientCloseWindowPacket(packet.getWindowId()));
            return;
        }
        win.slots[packet.getSlot()] = packet.getItem(); //Update here
        t.updateSlot(session, win, packet.getSlot());*/
    }

    public static void updateContent(ClientConnection session, ServerWindowItemsPacket packet) {
        /*if (packet.getWindowId() == 0) {
            return;   //We don't process player inventory updates here. 
        }
        if (!session.getDataCache().containsKey(CacheKey.WINDOW_OPENED_ID) || !session.getWindowCache().hasWindow(packet.getWindowId())) {
            session.getDownstream().send(new ClientCloseWindowPacket(packet.getWindowId()));
            return;
        }
        int openedId = (int) session.getDataCache().get(CacheKey.WINDOW_OPENED_ID);
        if (packet.getWindowId() != openedId) {
            //Hmm
            closeOpened(session, true);
            return;
        }

        CachedWindow win = session.getWindowCache().get(openedId);
        InventoryTranslator t = TRANSLATORS.get(win.pcType);
        if (t == null) {
            session.getDownstream().send(new ClientCloseWindowPacket(packet.getWindowId()));
            return;
        }
        win.slots = packet.getItems();
        t.updateContent(session, win);*/
    }
}
