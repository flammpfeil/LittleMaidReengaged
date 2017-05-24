package net.blacklab.lmr.entity.pathnavigate;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Blocks;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class MaidMoveNodeProcessor extends WalkNodeProcessor {

	protected boolean canSwim;

	@Override
	public void setCanSwim(boolean canSwimIn) {
		super.setCanSwim(canSwimIn);
		canSwim = canSwimIn;
	}

	@Override
	public PathPoint getStart() {
		if (canSwim && entity.isInWater()) {
			return this.openPoint(MathHelper.floor_double(entity.getEntityBoundingBox().minX), MathHelper.floor_double(entity.getEntityBoundingBox().minY + 0.5D), MathHelper.floor_double(entity.getEntityBoundingBox().minZ));
		}
		return super.getStart();
	}

	@Override
	public PathPoint getPathPointToCoords(double x, double y, double z) {
		if (canSwim && entity.isInWater()) {
			return this.openPoint(MathHelper.floor_double(x - (double)(entity.width / 2.0F)), MathHelper.floor_double(y + 0.5D), MathHelper.floor_double(z - (double)(entity.width / 2.0F)));
		}
		return super.getPathPointToCoords(x, y, z);
	}

	@Override
	public int findPathOptions(PathPoint[] pathOptions, PathPoint currentPoint, PathPoint targetPoint,
			float maxDistance) {

		if (canSwim && entity.isInWater()) {
			int i = 0;

			for (EnumFacing enumfacing : EnumFacing.values()) {
				PathPoint pathpoint = this.getSafePoint(entity, currentPoint.xCoord + enumfacing.getFrontOffsetX(), currentPoint.yCoord + enumfacing.getFrontOffsetY(), currentPoint.zCoord + enumfacing.getFrontOffsetZ());

				if (pathpoint != null && !pathpoint.visited && pathpoint.distanceTo(targetPoint) < maxDistance) {
					pathOptions[i++] = pathpoint;
				}
			}

			return i;
		}
		return super.findPathOptions(pathOptions, currentPoint, targetPoint, maxDistance);
	}

	/**
	 * Returns a point that the entity can safely move to
	 */
	private PathPoint getSafePoint(Entity entityIn, int x, int y, int z) {
		int i = -1;//this.func_176186_b(entityIn, x, y, z);
		return i == -1 ? this.openPoint(x, y, z) : null;
	}

/*
	private int func_176186_b(Entity entityIn, int x, int y, int z) {

		for (int i = x; i < x + this.field_176168_c; ++i) {
			for (int j = y; j < y + this.field_176165_d; ++j) {
				for (int k = z; k < z + this.field_176166_e; ++k) {
					Block block = entityIn.worldObj.getBlockState(new BlockPos(i, j, k)).getBlock();

					if (block.getMaterial() != Material.water) {
						return 0;
					}
				}
			}
		}

		return -1;
	}
*/
	//@Override
	public PathNodeType getPathNodeType(IBlockAccess blockaccessIn, int x, int y, int z)
	{
		PathNodeType pathnodetype = this.getPathNodeTypeRaw(blockaccessIn, x, y, z);

		if (pathnodetype == PathNodeType.OPEN && y >= 1)
		{
			Block block = blockaccessIn.getBlockState(new BlockPos(x, y - 1, z)).getBlock();
			PathNodeType pathnodetype1 = this.getPathNodeTypeRaw(blockaccessIn, x, y - 1, z);
			pathnodetype = pathnodetype1 != PathNodeType.WALKABLE && pathnodetype1 != PathNodeType.OPEN && pathnodetype1 != PathNodeType.WATER && pathnodetype1 != PathNodeType.LAVA ? PathNodeType.WALKABLE : PathNodeType.OPEN;

			if (pathnodetype1 == PathNodeType.DAMAGE_FIRE || block == Blocks.MAGMA)
			{
				pathnodetype = PathNodeType.DAMAGE_FIRE;
			}

			if (pathnodetype1 == PathNodeType.DAMAGE_CACTUS)
			{
				pathnodetype = PathNodeType.DAMAGE_CACTUS;
			}
		}

		BlockPos.PooledMutableBlockPos blockpos$pooledmutableblockpos = BlockPos.PooledMutableBlockPos.retain();

		if (pathnodetype == PathNodeType.WALKABLE)
		{
			for (int j = -1; j <= 1; ++j)
			{
				for (int i = -1; i <= 1; ++i)
				{
					if (j != 0 || i != 0)
					{
						Block block1 = blockaccessIn.getBlockState(blockpos$pooledmutableblockpos.setPos(j + x, y, i + z)).getBlock();

						if (block1 == Blocks.CACTUS)
						{
							pathnodetype = PathNodeType.DANGER_CACTUS;
						}
						else if (block1 == Blocks.FIRE)
						{
							pathnodetype = PathNodeType.DANGER_FIRE;
						}
					}
				}
			}
		}

		blockpos$pooledmutableblockpos.release();
		return pathnodetype;
	}

	private PathNodeType getPathNodeTypeRaw(IBlockAccess p_189553_1_, int p_189553_2_, int p_189553_3_, int p_189553_4_) {
		BlockPos blockpos = new BlockPos(p_189553_2_, p_189553_3_, p_189553_4_);
		IBlockState iblockstate = p_189553_1_.getBlockState(blockpos);
		Block block = iblockstate.getBlock();
		Material material = iblockstate.getMaterial();

		if (material == Material.AIR)
			return PathNodeType.OPEN;

		if (block == Blocks.TRAPDOOR || block == Blocks.IRON_TRAPDOOR || block == Blocks.WATERLILY)
			return PathNodeType.TRAPDOOR;

		if (block == Blocks.FIRE)
			return PathNodeType.DAMAGE_FIRE;

		if (block == Blocks.CACTUS)
			return PathNodeType.DAMAGE_CACTUS;

		if (block instanceof BlockDoor) {

			if (material == Material.WOOD && !iblockstate.getValue(BlockDoor.OPEN)) {
				IBlockState iblockstate2 = p_189553_1_.getBlockState(blockpos.down());
				Block block2 = iblockstate2.getBlock();
				if (block2 instanceof BlockDoor && material == Material.WOOD && iblockstate2.getValue(BlockDoor.OPEN))
					return PathNodeType.DOOR_OPEN;
				else
					return PathNodeType.DOOR_WOOD_CLOSED;
			}
			if (material == Material.IRON && !iblockstate.getValue(BlockDoor.OPEN)) {
				IBlockState iblockstate2 = p_189553_1_.getBlockState(blockpos.down());
				Block block2 = iblockstate2.getBlock();
				if (block2 instanceof BlockDoor && material == Material.IRON && iblockstate2.getValue(BlockDoor.OPEN))
					return PathNodeType.DOOR_OPEN;
				else
					return PathNodeType.DOOR_IRON_CLOSED;
			}

			if (iblockstate.getValue(BlockDoor.OPEN))
				return PathNodeType.DOOR_OPEN;
		}

		if (block instanceof BlockRailBase)
			return PathNodeType.RAIL;

		if((block instanceof BlockFence) || (block instanceof BlockWall) || ((block instanceof BlockFenceGate) && !iblockstate.getValue(BlockFenceGate.OPEN)))
			return PathNodeType.FENCE;

		if (material == Material.WATER)
			return PathNodeType.WATER;
		if (material == Material.LAVA)
			return PathNodeType.LAVA;

		if (block.isPassable(p_189553_1_, blockpos))
			return PathNodeType.OPEN;

		return PathNodeType.BLOCKED;

	}
}
