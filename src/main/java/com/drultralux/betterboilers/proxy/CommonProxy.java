package com.drultralux.betterboilers.proxy;

import net.minecraft.item.Item;
import net.minecraft.util.text.translation.I18n;

public class CommonProxy {

    public void preInit() {}

    public void registerItemRenderer(Item item, int meta, String id) {}

    public String i18nFormat(String key, Object[] format) {
        return I18n.translateToLocalFormatted(key, format);
    }

    public boolean i18nContains(String key) {
        return I18n.canTranslate(key);
    }
}