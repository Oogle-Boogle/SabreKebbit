package com.Bing_Bong.scripts.SabreKebbit.ai;

import com.runemate.ui.setting.annotation.open.*;
import com.runemate.ui.setting.open.*;


@SettingsGroup(group = "Kebbit")
public interface KebbitConfig extends Settings {

    @SettingsSection(title = "Axes", description = "Axe settings", order = 1)
    String axeSettings = "axeSettings";

    @Setting(key = "axe", title = "Axe", section = axeSettings, order = 1)
    default KebbitAxe axe() {
        return KebbitAxe.BRONZE_AXE;
    }

}
