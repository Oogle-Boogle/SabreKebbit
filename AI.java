package com.Bing_Bong.scripts.SabreKebbit.ai;

import com.Bing_Bong.scripts.SabreKebbit.Kebbit;
import com.runemate.game.api.client.ClientUI;
import com.runemate.game.api.hybrid.Environment;
import com.runemate.game.api.hybrid.entities.*;
import com.runemate.game.api.hybrid.input.Mouse;
import com.runemate.game.api.hybrid.local.Camera;
import com.runemate.game.api.hybrid.local.hud.InteractablePoint;
import com.runemate.game.api.hybrid.local.hud.interfaces.Bank;
import com.runemate.game.api.hybrid.local.hud.interfaces.Equipment;
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory;
import com.runemate.game.api.hybrid.location.Area;
import com.runemate.game.api.hybrid.location.Coordinate;
import com.runemate.game.api.hybrid.region.Banks;
import com.runemate.game.api.hybrid.region.GameObjects;
import com.runemate.game.api.hybrid.region.Players;
import com.runemate.game.api.hybrid.util.calculations.Distance;
import com.runemate.game.api.hybrid.util.calculations.Random;
import com.runemate.game.api.hybrid.web.WebPath;
import com.runemate.game.api.hybrid.web.WebPathRequest;
import com.runemate.game.api.script.Execution;
import com.runemate.ui.DefaultUI;
import javafx.beans.property.SimpleStringProperty;
import lombok.extern.log4j.Log4j2;

/**
 * Use this video to help guide - https://www.youtube.com/watch?v=_9d3hiNgMP0
 * Wiki - https://oldschool.runescape.wiki/w/Sabre-toothed_kebbit
 */

@Log4j2(topic = "Kebbit")
public class AI {

    Kebbit bot;
    public State state;
    public Player player;
    public SimpleStringProperty status;
    private static final String KNIFE = "Knife";

    public static final Coordinate BANKAREA = new Coordinate(2725, 3491, 0);

    public static final Coordinate HUNTERAREA = new Coordinate(2721, 3773, 0);

    public enum State {
        WALK, CUT, HUNT, BANK, END
    }

    public AI(Kebbit bot) {
        this.bot = bot;
        this.status = new SimpleStringProperty();
    }

    public void execute() {
        final var player = Players.getLocal();
        if (player == null) {
            return;
        }
        if (state == null) {
            log.info("State == null.");
            if (Inventory.contains(KNIFE)) {
                state = State.CUT;
            } else if (Inventory.contains("Logs")){
                state = State.HUNT;
            } else {
                state = State.BANK;
            }
        }

        switch (state) {
            case WALK:
                walk();
                break;
            case CUT:
                cut();
                break;
            case HUNT:
                hunt();
                break;
            case BANK:
                bank();
                break;
            case END:
                end();
                break;
        }
    }

    void walk() {
        GameObject bankBooth = GameObjects.newQuery().names("Bank booth").results().nearest();
        WebPath pathToBank = WebPathRequest.builder().setUsingTeleports(true).setDestination(BANKAREA).build();
        WebPath pathToHunter = WebPathRequest.builder().setUsingTeleports(true).setDestination(HUNTERAREA).build();

        log.info("Reached walk.");
        final var player = Players.getLocal();
        if (player == null) {
            return;
        }

        if (!Equipment.contains(bot.getConfig().axe().getGameName())) {
            DefaultUI.setStatus("We are not wearing the correct axe. - Walking to the bank.");
            if (pathToBank != null) {
                pathToBank.step();
                Execution.delayUntil(() -> bankBooth.isVisible(), 2000);
                state = State.BANK;
            }
        }

        if (!Inventory.contains("Kebbit teeth")) {
            if (pathToHunter != null) {
                pathToHunter.step();
                Execution.delayUntil(() -> pathToHunter == null, 3000);
            }
            state = State.CUT;
        }

        if (Inventory.contains("Kebbit teeth")) {
            DefaultUI.setStatus("Walking back to bank to deposit.");
            if (pathToBank != null) {
                pathToBank.step();
                Execution.delayUntil(() -> pathToBank == null, 3000);
            }
            state = State.BANK;
        }
    }

    void cut() {
        Camera.setZoom(0, 0);
        log.info("Reached cut.");

        GameObject TREE = GameObjects.newQuery().names("Dead tree").actions("Chop down").results().nearest();

        if (Inventory.contains("Logs")) {
            state = State.HUNT;
        }

        if (TREE == null) {
            DefaultUI.setStatus("Tree == null. Waiting for one to spawn.");
            return;
        }

        if (TREE.interact("Chop down")) {
            Execution.delayUntil(() -> Inventory.contains("Logs"), 2000);
        }

        if (TREE.isVisible()) {
            if (Random.nextBoolean()) {
                mouseWheelTurnTo(TREE.getPosition());
                Execution.delayWhile(Camera::isTurning, 3000);
            }
        }
    }

    void hunt() {
        GameObject TRAP = GameObjects.newQuery().names("Boulder").actions("Set-trap").results().nearest();
        GameObject BOULDER = GameObjects.newQuery().names("Boulder").results().first();
        GameObject deadFall = GameObjects.newQuery().names("Deadfall").actions("Dismantle").results().first();

        if (BOULDER != null) {
            if (BOULDER.interact("Check")) {
                Execution.delayUntil(() -> !player.isMoving() && !(player.getAnimationId() == -1) || !deadFall.isVisible(), 2000);
                return;
            }
        }

        /**
         * Safety check - If the bot accidently or player drops knife, we must end the bot.
         */
        if (!Inventory.contains(KNIFE)) {
            DefaultUI.setStatus("Knife has been dropped - we're stopping the bot.");
            end();
            return;
        }

        if (!Inventory.contains("Logs")) {
            state = State.CUT;
        }

        if (Inventory.contains("Logs") && Inventory.contains(KNIFE)) {
            if (TRAP.interact("Set-trap")) {

            }
        }
        state = State.CUT;
    }

    /**
     * Bank - If we have Kebbit teeth in inventory - deposit.
     * If we do not contain axe, knife or teleports back to rellekka, we need to withdraw or end bot.
     */

    void bank() {
        GameObject bankBooth = GameObjects.newQuery().names("Bank booth").results().nearest();
        WebPath pathToBank = WebPathRequest.builder().setUsingTeleports(true).setDestination(BANKAREA).build();

        if (Inventory.contains(KNIFE) && Equipment.contains(bot.getConfig().axe().getGameName())) {
            state = State.WALK;
        }

        if (bankBooth != null && bankBooth.isVisible()) {
            bankBooth.interact("Bank");
            Execution.delayUntil(() -> player.isMoving() && Bank.isOpen(), 1800);
        }

        if (Bank.isOpen()) {
            if (!Bank.contains(KNIFE) && Inventory.contains(KNIFE)) {

            }

            if (!Inventory.contains(KNIFE) && Bank.contains(KNIFE)) {
                Bank.withdraw(KNIFE, 1);
                Execution.delayUntil(() -> Inventory.contains(KNIFE));
            }

            if (!Equipment.contains(bot.getConfig().axe().getGameName())) {
                Bank.withdraw(bot.getConfig().axe().getGameName(), 1);
                Execution.delayUntil(() -> Inventory.contains(bot.getConfig().axe().getGameName()), 1800);
            }

            if (Inventory.contains("Kebbit teeth")) {
                DefaultUI.setStatus("Depositing Kebbit teeth...");
                Bank.deposit("Kebbit teeth", Inventory.getQuantity("Kebbit teeth"));
                Execution.delayUntil(() -> !Inventory.contains("Kebbit teeth"), 1800);
            }
            if (Inventory.contains(bot.getConfig().axe().getGameName())) {
                DefaultUI.setStatus("Equipping selected axe...");
                Inventory.getItems(bot.getConfig().axe().getGameName());
                Inventory.getSelectedItem().click();
                Execution.delay(3000, 5000);
            }
        }

        final var banks = Banks.getLoaded().first();
        if (banks == null || Distance.between(player, banks) > 30) {
            log.info("Going closer to a bank.");
            if (pathToBank != null) {
                pathToBank.step();
            } else if (Distance.between(banks, player) < 15) {
                log.info("Camera turning to " + banks);
                Camera.turnTo(banks);
                return;
            }
        }
        log.info("Opening bank{} ", banks);
        Bank.open();
    }

    /**
     * End bot.
     */
    void end() {
        ClientUI.showAlert(ClientUI.AlertLevel.INFO, "You can join BB Discord to find out more information - <a href=\"https://discord.gg/fX5byan2mJ\">Click here</a> ");
        String msg = "The bot has completed all tasks. Ended session.";
        DefaultUI.setStatus(msg);
        end();
    }

    private static void mouseWheelTurnTo(Coordinate Tc) {
        final var player = Players.getLocal();
        if (player == null) {
            return;
        }

        if (Tc == null) return;
        Coordinate Pc = player.getPosition();

        int Dx = Tc.getX() - Pc.getX();
        int Dy = Tc.getY() - Pc.getY();
        int Yaw = Camera.getYaw();

        int Beta = (int) (Math.atan2(-Dx, Dy) * 180 / Math.PI);
        if (Beta < 0) Beta = 360 + Beta;

        int deltaYaw = Beta - Yaw;

        if (deltaYaw > 180) {
            deltaYaw = deltaYaw - 360;
        } else if (deltaYaw < -180) {
            deltaYaw = deltaYaw + 360;
        }

        int deltaMouseMoveX = (int) (-deltaYaw * 2.5);

        Area hoverArea = new Area.Circular(player.getPosition(), 3);
        hoverArea.getRandomCoordinate().hover();

        Mouse.press(Mouse.Button.WHEEL);
        Mouse.move(new InteractablePoint((int) (Mouse.getPosition().getX() + deltaMouseMoveX), (int) (Mouse.getPosition().getY() + Random.nextInt(-10, 10))));
        Mouse.release(Mouse.Button.WHEEL);
    }

}
