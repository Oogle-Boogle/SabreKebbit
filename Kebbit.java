package com.Bing_Bong.scripts.SabreKebbit;

import com.Bing_Bong.scripts.SabreKebbit.ai.AI;
import com.Bing_Bong.scripts.SabreKebbit.ai.KebbitConfig;
import com.runemate.game.api.client.ClientUI;
import com.runemate.game.api.hybrid.local.hud.interfaces.Bank;
import com.runemate.game.api.hybrid.region.Players;
import com.runemate.game.api.script.framework.LoopingBot;
import com.runemate.ui.setting.annotation.open.SettingsProvider;
import lombok.Getter;

public class Kebbit extends LoopingBot {

    public AI ai;

    @Getter
    @SettingsProvider(updatable = true)
    private KebbitConfig config;

    @Override
    public void onLoop() {

        final var player = Players.getLocal();
        if (player == null) {
            return;
        }

        if (Bank.isOpen()) {
            Bank.close(true);
        }
        ai.execute();
    }

    @Override
    public void onStart(String... args) {
        ClientUI.showAlert(ClientUI.AlertLevel.INFO, "You can join BB Discord to find out more information - <a href=\"https://discord.gg/fX5byan2mJ\">Click here</a> ");
        this.ai = new AI(this);
        this.setLoopDelay(300, 800);
    }
}
