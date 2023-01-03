package com.mkrooted.unrealmcmgmt;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.gitlab4j.api.Constants;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.Pager;
import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.Pipeline;
import org.gitlab4j.api.models.PipelineFilter;
import org.gitlab4j.api.models.PipelineStatus;
import org.slf4j.Logger;

import java.util.List;

public class GitlabPipelinesUI {
    public static class Params {
        // drawing
        public Block background = Blocks.SANDSTONE;
        public Block line = Blocks.COAL_BLOCK;
        public Block pad = Blocks.BLACK_STAINED_GLASS;

        // statuses
        public Block success = Blocks.EMERALD_BLOCK;
        public Block inprogress = Blocks.GOLD_BLOCK;
        public Block failure = Blocks.REDSTONE_BLOCK;
        public Block na = Blocks.GRAVEL;
    }

    public static final Params defaultParams = new Params();

    // =======
    private static final Logger LOGGER = LogUtils.getLogger();
    private Params params;

    // Origin is a lower left corner
    private BlockPos origin;
    private Level level;
    private Player player;

    GitlabPipelinesUI(Player player, Level level, BlockPos origin, Params params) {
        this.origin = origin;
        this.params = params;
        this.level = level;
        this.player = player;
    }

    // ========

    public void showBranches(List<Branch> branches) {
        BlockPos pos = origin;
        Vec3i normal = Direction.fromYRot(player.getYHeadRot()).getNormal().multiply(-1);
        Vec3i right = Direction.fromYRot(player.getYHeadRot()).getClockWise().getNormal();

        level.setBlock(pos, params.pad.defaultBlockState(), 0);
        level.setBlock(pos.above(1), params.pad.defaultBlockState(), 0);
        level.setBlock(pos.above(2), params.pad.defaultBlockState(), 0);
        pos = pos.offset(right);

        for (Branch branch : branches) {
            pos = showBranch(pos, branch);
            level.setBlock(pos, params.pad.defaultBlockState(), 0);
            level.setBlock(pos.above(1), params.pad.defaultBlockState(), 0);
            level.setBlock(pos.above(2), params.pad.defaultBlockState(), 0);
        }
    }

    private BlockState block_fromStatus(PipelineStatus s) {
        Block block = switch (s) {
            case FAILED -> params.failure;
            case PENDING, RUNNING -> params.inprogress;
            case SUCCESS -> params.success;
            default -> params.na;
        };
        return block_faceToPlayer(block.defaultBlockState());
    }

    private BlockState block_faceToPlayer(BlockState in) { return in.setValue(BlockStateProperties.FACING, Direction.fromYRot(180 - player.getYHeadRot())); }
    private boolean setBlockText(BlockPos pos, String text1, String text2, String text3, String text4) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity == null || entity.getType() != BlockEntityType.SIGN) return false;
        CompoundTag tag = entity.getUpdateTag();
        if (text1 != null) tag.putString("Text1", text1);
        if (text2 != null) tag.putString("Text2", text2);
        if (text3 != null) tag.putString("Text3", text3);
        if (text4 != null) tag.putString("Text4", text4);
        entity.load(tag);
        return true;
    }
    private BlockPos showBranch(BlockPos pos, Branch branch) {
        try {
            PipelineFilter filter = new PipelineFilter()
                    .withRef(branch.getName())
                    .withOrderBy(Constants.PipelineOrderBy.ID)
                    .withSort(Constants.SortOrder.DESC);
            Pager<Pipeline> pipelines = UnrealMgmtMod.gitLabApi.getPipelineApi().getPipelines(UnrealMgmtMod.projectId, filter, 1);
            Pipeline pipeline = pipelines.current().get(0);

            String ref = branch.getName();
            Long pid = pipeline.getId();
            String status = pipeline.getStatus().toString();

            BlockState bg = block_faceToPlayer(params.background.defaultBlockState());
            Vec3i normal = Direction.fromYRot(player.getYHeadRot()).getNormal().multiply(-1);
            Vec3i right = Direction.fromYRot(player.getYHeadRot()).getClockWise().getNormal();


            level.setBlock(pos, bg, 0);
            level.setBlock(pos.offset(right), bg, 0);
//            level.setBlock(pos.offset(right.multiply(2)), bg, 0);
            level.setBlock(pos.above(1), bg, 0);
            level.setBlock(pos.above(1).offset(right), block_fromStatus(pipeline.getStatus()), 0);
//            level.setBlock(pos.above(1).offset(right.multiply(2)), bg, 0);
            level.setBlock(pos.above(2), bg, 0);
            level.setBlock(pos.above(2).offset(right), bg, 0);
//            level.setBlock(pos.above(2).offset(right.multiply(2)), bg, 0);

            level.setBlock(pos.above(1).offset(right).offset(normal), block_faceToPlayer(Blocks.OAK_WALL_SIGN.defaultBlockState()), 0);
            setBlockText(pos.above(1).offset(normal), ref, "-", status, "");
            level.setBlock(pos.offset(right).offset(normal), block_faceToPlayer(Blocks.OAK_WALL_SIGN.defaultBlockState()), 0);
            setBlockText(pos.offset(normal), "pipeline id:", pid.toString(), "", "");

            return pos.offset(right.multiply(2));


        } catch (GitLabApiException | IndexOutOfBoundsException e) {
            LOGGER.error(String.format("Error while getting pipelines for branch %s", branch.getName()), e);
        }

        return pos;
    }

}
