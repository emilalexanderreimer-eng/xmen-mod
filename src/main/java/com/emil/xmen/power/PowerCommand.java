package com.emil.xmen.power;

import com.emil.xmen.ability.PowerBehaviors;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /power             show your power
 * /power list [level] list powers
 * /power list working list the powers that already work
 * /power set <power> change your power (creative only)
 * /power random      roll a new random power (creative only)
 */
public final class PowerCommand {
    private PowerCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("power")
                .executes(PowerCommand::info)
                .then(Commands.literal("list")
                        .executes(ctx -> listLevels(ctx.getSource()))
                        .then(Commands.literal("working").executes(ctx -> listWorking(ctx.getSource())))
                        .then(Commands.argument("level", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        java.util.Arrays.stream(PowerLevel.values())
                                                .map(l -> l.displayName().toLowerCase()), builder))
                                .executes(ctx -> listLevel(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "level")))))
                .then(Commands.literal("set")
                        .then(Commands.argument("power", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(Powers.ids(), builder))
                                .executes(PowerCommand::set)))
                .then(Commands.literal("random").executes(PowerCommand::random)));
    }

    private static int info(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Power power = PlayerPowers.get(player);
        if (power == null) {
            ctx.getSource().sendFailure(Component.literal("You don't have a power yet."));
            return 0;
        }
        String basedOn = power.basedOn().isEmpty() ? "" : " (like " + power.basedOn() + ")";
        ctx.getSource().sendSuccess(() -> Component.literal("Your power: ").withStyle(ChatFormatting.YELLOW)
                .append(power.title()).append(Component.literal(basedOn).withStyle(ChatFormatting.GRAY)), false);
        ctx.getSource().sendSuccess(() -> Component.literal(power.description() + " ("
                + power.type().displayName() + ", " + power.category().displayName() + ")")
                .withStyle(ChatFormatting.GRAY), false);
        if (!PowerBehaviors.isImplemented(power.id())) {
            ctx.getSource().sendSuccess(() -> Component.literal("This power doesn't work yet. It comes in a later update.")
                    .withStyle(ChatFormatting.DARK_GRAY), false);
        }
        return 1;
    }

    private static int listLevels(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(Powers.count() + " powers. Use /power list <level> to see them:")
                .withStyle(ChatFormatting.YELLOW), false);
        for (PowerLevel level : PowerLevel.values()) {
            source.sendSuccess(() -> Component.literal(level.displayName() + ": " + Powers.ofLevel(level).size()
                    + " powers, " + level.chancePercent() + "% chance").withStyle(level.color()), false);
        }
        return 1;
    }

    private static int listWorking(CommandSourceStack source) {
        StringBuilder names = new StringBuilder();
        int count = 0;
        for (Power power : Powers.all()) {
            if (PowerBehaviors.isImplemented(power.id())) {
                if (!names.isEmpty()) {
                    names.append(", ");
                }
                names.append(power.id());
                count++;
            }
        }
        int shown = count;
        source.sendSuccess(() -> Component.literal(shown + " powers work so far: ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(names.toString()).withStyle(ChatFormatting.WHITE)), false);
        return 1;
    }

    private static int listLevel(CommandSourceStack source, String levelName) {
        PowerLevel level = null;
        for (PowerLevel l : PowerLevel.values()) {
            if (l.displayName().equalsIgnoreCase(levelName)) {
                level = l;
            }
        }
        if (level == null) {
            source.sendFailure(Component.literal("Unknown level: " + levelName));
            return 0;
        }
        StringBuilder names = new StringBuilder();
        for (Power power : Powers.ofLevel(level)) {
            if (!names.isEmpty()) {
                names.append(", ");
            }
            names.append(power.id());
        }
        PowerLevel shown = level;
        source.sendSuccess(() -> Component.literal(shown.displayName() + " powers: ").withStyle(shown.color())
                .append(Component.literal(names.toString()).withStyle(ChatFormatting.WHITE)), false);
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (!checkCreative(ctx.getSource(), player)) {
            return 0;
        }
        String id = StringArgumentType.getString(ctx, "power");
        Power power = Powers.get(id);
        if (power == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown power: " + id));
            return 0;
        }
        PlayerPowers.set(player, power);
        ctx.getSource().sendSuccess(() -> Component.literal("Your power is now ").withStyle(ChatFormatting.YELLOW)
                .append(power.title()), false);
        return 1;
    }

    private static int random(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (!checkCreative(ctx.getSource(), player)) {
            return 0;
        }
        Power power = Powers.roll(player.getRandom());
        PlayerPowers.set(player, power);
        ctx.getSource().sendSuccess(() -> Component.literal("Your new power is ").withStyle(ChatFormatting.YELLOW)
                .append(power.title()), false);
        return 1;
    }

    private static boolean checkCreative(CommandSourceStack source, ServerPlayer player) {
        if (!player.isCreative()) {
            source.sendFailure(Component.literal("You can only change your power in creative mode."));
            return false;
        }
        return true;
    }
}
