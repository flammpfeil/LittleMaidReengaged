package net.blacklab.lmr.entity.mode;

import net.blacklab.lib.minecraft.vector.VectorUtil;
import net.blacklab.lmr.achievements.AchievementsLMRE;
import net.blacklab.lmr.entity.EntityLittleMaid;
import net.blacklab.lmr.inventory.InventoryLittleMaid;
import net.blacklab.lmr.util.EnumSound;
import net.blacklab.lmr.util.TriggerSelect;
import net.blacklab.lmr.util.helper.MaidHelper;
import net.minecraft.block.Block;
import net.minecraft.block.material.MaterialLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class EntityMode_TorchLayer extends EntityModeBase {

	public static final int mmode_Torcher = 0x0020;


	public EntityMode_TorchLayer(EntityLittleMaid pEntity) {
		super(pEntity);
		isAnytimeUpdate = true;
	}

	@Override
	public int priority() {
		return 6200;
	}

	@Override
	public void init() {
		/* langファイルに移動
		ModLoader.addLocalization("littleMaidMob.mode.Torcher", "Torcher");
		ModLoader.addLocalization("littleMaidMob.mode.F-Torcher", "F-Torcher");
		ModLoader.addLocalization("littleMaidMob.mode.D-Torcher", "D-Torcher");
		ModLoader.addLocalization("littleMaidMob.mode.T-Torcher", "T-Torcher");
		*/
		TriggerSelect.appendTriggerItem(null, "Torch", "");
	}

	@Override
	public void addEntityMode(EntityAITasks pDefaultMove, EntityAITasks pDefaultTargeting) {
		// Torcher:0x0020
		EntityAITasks[] ltasks = new EntityAITasks[2];
		ltasks[0] = pDefaultMove;
		ltasks[1] = pDefaultTargeting;

		owner.addMaidMode(ltasks, "Torcher", mmode_Torcher);
	}

	@Override
	public boolean changeMode(EntityPlayer pentityplayer) {
		ItemStack litemstack = owner.getHandSlotForModeChange();
		if (litemstack != null) {
			if (litemstack.getItem() == Item.getItemFromBlock(Blocks.TORCH) || TriggerSelect.checkTrigger(owner.getMaidMasterUUID(), "Torch", litemstack.getItem())) {
				owner.setMaidMode("Torcher");
				if (pentityplayer != null) {
					pentityplayer.addStat(AchievementsLMRE.ac_TorchLayer);
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean setMode(int pMode) {
		switch (pMode) {
		case mmode_Torcher :
			owner.setBloodsuck(false);
			owner.aiAttack.setEnable(false);
			owner.aiShooting.setEnable(false);
			return true;
		}

		return false;
	}

	@Override
	public int getNextEquipItem(int pMode) {
		int li;
		if ((li = super.getNextEquipItem(pMode)) >= 0) {
			return li;
		}

		ItemStack litemstack;

		// モードに応じた識別判定、速度優先
		switch (pMode) {
		case mmode_Torcher :
			for (li = 0; li < owner.maidInventory.getSizeInventory(); li++) {
				litemstack = owner.maidInventory.getStackInSlot(li);
				if (litemstack == null) continue;

				// 松明
				if (isTriggerItem(pMode, litemstack)) {
					return li;
				}
			}
			break;
		}

		return -1;
	}

	@Override
	protected boolean isTriggerItem(int pMode, ItemStack par1ItemStack) {
		if (par1ItemStack == null) {
			return false;
		}
		return par1ItemStack.getItem() == Item.getItemFromBlock(Blocks.TORCH) || TriggerSelect.checkTrigger(owner.getMaidMasterUUID(), "Torch", par1ItemStack.getItem());
	}

	@Override
	public boolean checkItemStack(ItemStack pItemStack) {
		return isTriggerItem(owner.getMaidModeInt(), pItemStack);
	}

	@Override
	public boolean isSearchBlock() {
		return !owner.isMaidWait()&&(owner.getCurrentEquippedItem()!=null);
	}

	@Override
	public boolean shouldBlock(int pMode) {
		return !(owner.getCurrentEquippedItem() == null);
	}

	protected int getBlockLighting(int px, int py, int pz) {
		World worldObj = owner.worldObj;
		//離れすぎている
		if (!MaidHelper.isTargetReachable(owner, new Vec3d(px, py, pz), 0)) return 15;

		BlockPos targetPos = new BlockPos(px, py, pz);
		if (!owner.isMaidWait()) {
			int a = worldObj.getLight(targetPos,true);
			return a;
		}
		return 15;
	}

	@Override
	public boolean checkBlock(int pMode, int px, int py, int pz) {
		if (!super.checkBlock(pMode, px, py, pz)) return false;

		// アイテムを置けない場合
		Item heldItem = owner.getHeldItem(EnumHand.MAIN_HAND).getItem();
		if (heldItem instanceof ItemBlock) {
			if (!canPlaceItemBlockOnSide(owner.worldObj, px, py - 1, pz, EnumFacing.UP, owner.maidAvatar, owner.getHeldItem(EnumHand.MAIN_HAND), (ItemBlock) heldItem)) {
				return false;
			}
		}

		int v = getBlockLighting(px, py, pz);
		if (v < 8 && VectorUtil.canBlockBeSeen(owner, px, py - 1, pz, true, true, true) && !owner.isMaidWait()) {
			if (owner.getNavigator().tryMoveToXYZ(px, py, pz, 1.0F) ) {
				//owner.playLittleMaidSound(LMM_EnumSound.findTarget_D, true);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean executeBlock(int pMode, int px, int py, int pz) {
		ItemStack lis = owner.getCurrentEquippedItem();
		if (lis == null) return false;

		if(lis.getItem()!=Item.getItemFromBlock(Blocks.TORCH)) return false;

		int li = lis.stackSize;
		// TODO:当たり判定をどうするか
		if (lis.onItemUse(owner.maidAvatar, owner.worldObj, new BlockPos(px, py - 1, pz), EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F) == EnumActionResult.SUCCESS) {
			owner.setSwing(10, EnumSound.installation, false);
			owner.addMaidExperience(0.32f);
			if (owner.maidAvatar.capabilities.isCreativeMode) {
				lis.stackSize = li;
			}
			if (lis.stackSize <= 0) {
				owner.maidInventory.setInventoryCurrentSlotContents(null);
				owner.getNextEquipItem();
			}
		}
		return false;
	}

	public boolean canPlaceItemBlockOnSide(World par1World, int par2, int par3, int par4, EnumFacing par5,
			EntityPlayer par6EntityPlayer, ItemStack par7ItemStack, ItemBlock pItemBlock) {
		// TODO:マルチ対策用、ItemBlockから丸パクリバージョンアップ時は確認すること
		Block var8 = par1World.getBlockState(new BlockPos(par2, par3, par4)).getBlock();

		if (Block.isEqualTo(var8, Blocks.SNOW)) {
			par5 = EnumFacing.UP;
		} else if (!Block.isEqualTo(var8, Blocks.VINE) && !Block.isEqualTo(var8, Blocks.TALLGRASS) &&
				!Block.isEqualTo(var8, Blocks.DEADBUSH)) {
			if (par5 == EnumFacing.DOWN) {
				--par3;
			}
			if (par5 == EnumFacing.UP) {
				++par3;
			}
			if (par5 == EnumFacing.NORTH) {
				--par4;
			}
			if (par5 == EnumFacing.SOUTH) {
				++par4;
			}
			if (par5 == EnumFacing.WEST) {
				--par2;
			}
			if (par5 == EnumFacing.EAST) {
				++par2;
			}
		}

		IBlockState iState = par1World.getBlockState(new BlockPos(par2, par3, par4));
		if (iState.getMaterial() instanceof MaterialLiquid) {
			return false;
		}

		return par1World.canBlockBePlaced(Block.getBlockFromItem(pItemBlock), new BlockPos(par2, par3, par4), false, par5, (Entity)null, par7ItemStack);
	}

	@Override
	public void updateAITick(int pMode) {
		// トーチの設置
/*
		if (pMode == mmode_Torcher && owner.getNextEquipItem()) {
			ItemStack lis = owner.getCurrentEquippedItem();
			int lic = lis.stackSize;
			Item lii = lis.getItem();
			World lworld = owner.worldObj;

			// 周囲を検索
			int lxx = MathHelper.floor_double(owner.posX);
			int lyy = MathHelper.floor_double(owner.posY);
			int lzz = MathHelper.floor_double(owner.posZ);
			//			mod_LMM_littleMaidMob.Debug("torch-s: %d, %d, %d", lxx, lyy, lzz);
			int ll = 8;
			int ltx = lxx, lty = lyy, ltz = lzz;
			int lil[] = {lyy, lyy - 1, lyy + 1};
			owner.getAvatarIF().getValue();
			for (int x = -1; x < 2; x++) {
				for (int z = -1; z < 2; z++) {
					for (int lyi : lil) {
						int lv = getBlockLighting(lxx + x, lyi, lzz + z);
						if (ll > lv && lii instanceof ItemBlock &&
								canPlaceItemBlockOnSide(lworld, lxx + x, lyi - 1, lzz + z, EnumFacing.UP, owner.maidAvatar, lis, (ItemBlock)lii)
								&& canBlockBeSeen(lxx + x, lyi - 1, lzz + z, true, false, true)) {
//						if (ll > lv && lworld.getBlockMaterial(lxx + x, lyi - 1, lzz + z).isSolid()
//								&& (lworld.getBlockMaterial(lxx + x, lyi, lzz + z) == Material.air
//								|| lworld.getBlockId(lxx + x, lyi, lzz + z) == Block.snow.blockID)
//								&& canBlockBeSeen(lxx + x, lyi - 1, lzz + z, true, false, true)) {
							ll = lv;
							ltx = lxx + x;
							lty = lyi - 1;
							ltz = lzz + z;
//							mod_LMM_littleMaidMob.Debug("torch: %d, %d, %d: %d", ltx, lty, ltz, lv);
						}
					}
				}
			}

			if (ll < 8 && lis.onItemUse(owner.maidAvatar, owner.worldObj, new BlockPos(ltx, lty, ltz), EnumFacing.UP, 0.5F, 1.0F, 0.5F)) {
//				mod_LMM_littleMaidMob.Debug("torch-inst: %d, %d, %d: %d", ltx, lty, ltz, ll);
				owner.setSwing(10, LMM_EnumSound.installation, false);
				owner.getNavigator().clearPathEntity();
				if (owner.maidAvatar.capabilities.isCreativeMode) {
					lis.stackSize = lic;
				}
				if (lis.stackSize <= 0) {
					owner.maidInventory.setInventoryCurrentSlotContents(null);
					owner.getNextEquipItem();
				}
			}

		}
*/
	}

	@Override
	public void onWarp() {
		Path pathEntity = owner.getNavigator().getPath();
		if (pathEntity == null) return;
		PathPoint destination = pathEntity.getFinalPathPoint();
		if (!checkBlock(owner.getMaidModeInt(), destination.xCoord, destination.yCoord, destination.zCoord)) {
			owner.getNavigator().clearPathEntity();
		}
	}

}
