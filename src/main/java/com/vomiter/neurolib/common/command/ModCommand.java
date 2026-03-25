package com.vomiter.neurolib.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.vomiter.neurolib.NeuroLib;
import com.vomiter.neurolib.common.entity.loot.LootMatchSpec;
import com.vomiter.neurolib.common.entity.loot.LootTableContainsHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;

import java.util.Set;
import java.util.function.Supplier;

public class ModCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
            Commands.literal("neuro")
            .then(Commands.literal("debug")
            .executes(ctx -> {
                ctx.getSource().sendSuccess(
                    () -> Component.literal(Boolean.toString(LootTableContainsHelper.entityDefaultLootTableContains(
                            ctx.getSource().getServer(),
                            EntityType.PIG,
                            new Supplier<LootMatchSpec>() {
                                @Override
                                public LootMatchSpec get() {
                                    NeuroLib.LOGGER.info(ItemTags.MEAT.location().toString());
                                    return new LootMatchSpec(Set.of(), ItemTags.MEAT.location().toString());
                                }
                            }
                    ))), false);
                    return 1;
            }))
        );

    }
}
