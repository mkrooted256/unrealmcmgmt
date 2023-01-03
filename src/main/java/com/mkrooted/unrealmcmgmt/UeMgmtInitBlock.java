package com.mkrooted.unrealmcmgmt;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Branch;
import org.slf4j.Logger;

import java.util.List;

public class UeMgmtInitBlock extends Block {
    private static final Component CONTAINER_TITLE = Component.literal("Gitlab connection setup");
    private static final Logger LOGGER = LogUtils.getLogger();

    public UeMgmtInitBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    public InteractionResult use(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
//            player.openMenu(blockState.getMenuProvider(level, blockPos));
//            player.awardStat(Stats.INTERACT_WITH_CRAFTING_TABLE);

            try {
                List<Branch> branches =  UnrealMgmtMod.gitLabApi.getRepositoryApi().getBranches(UnrealMgmtMod.projectId);
                // Build gitlab interface next to the block
                GitlabPipelinesUI ui = new GitlabPipelinesUI(player, level, blockPos, GitlabPipelinesUI.defaultParams);
                ui.showBranches(branches);

            } catch (GitLabApiException e) {
                LOGGER.error("Error getting gitlab branches.");
            }

            return InteractionResult.CONSUME;
        }
    }

//    public MenuProvider getMenuProvider(BlockState p_52240_, Level p_52241_, BlockPos p_52242_) {
//        return new SimpleMenuProvider((p_52229_, p_52230_, p_52231_) -> {
//            return new CraftingMenu(p_52229_, p_52230_, ContainerLevelAccess.create(p_52241_, p_52242_));
//        }, CONTAINER_TITLE);
//    }
}