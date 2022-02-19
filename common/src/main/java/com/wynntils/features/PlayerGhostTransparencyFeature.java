/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features;

import com.google.common.collect.ImmutableList;
import com.wynntils.core.features.Feature;
import com.wynntils.mc.event.LivingEntityRenderTranslucentCheckEvent;
import com.wynntils.wc.utils.WynnPlayerUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerGhostTransparencyFeature extends Feature {
    @Override
    protected void init(ImmutableList.Builder<Condition> conditions) {}

    @Override
    protected boolean onEnable() {
        return true;
    }

    @Override
    protected void onDisable() {}

    @SubscribeEvent
    public void onTranslucentCheck(LivingEntityRenderTranslucentCheckEvent e) {
        if (e.getEntity() instanceof Player player) {
            // TODO make this variable a setting
            if (WynnPlayerUtils.isPlayerGhost(player)) {
                e.setTranslucence(0.75f);
            }
        }
    }
}