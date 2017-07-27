package cam72cam.immersiverailroading.entity;

import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.registry.DefinitionManager;
import cam72cam.immersiverailroading.entity.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.util.BufferUtil;
import cam72cam.immersiverailroading.util.Speed;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public abstract class EntityRollingStock extends Entity implements IEntityAdditionalSpawnData {
	protected String defID;

	public EntityRollingStock(World world, String defID) {
		super(world);

		this.defID = defID;

		super.preventEntitySpawning = true;
		super.isImmuneToFire = true;
		super.entityCollisionReduction = 1F;
		super.ignoreFrustumCheck = true;
	}

	protected EntityRollingStockDefinition getDefinition() {
		return DefinitionManager.getDefinition(defID);
	}

	/*
	 * 
	 * Data RW for Spawn and Entity Load
	 */

	@Override
	public void readSpawnData(ByteBuf additionalData) {
		defID = BufferUtil.readString(additionalData);
		rollingStockInit();
	}

	@Override
	public void writeSpawnData(ByteBuf buffer) {
		BufferUtil.writeString(buffer, defID);
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbttagcompound) {
		nbttagcompound.setString("defID", defID);
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
		defID = nbttagcompound.getString("defID");
		rollingStockInit();
	}

	/**
	 * Fired after we have a definitionID. Here is where you construct objects
	 * based on the rolling stock definition
	 */
	protected void rollingStockInit() {
	}

	@Override
	protected void entityInit() {
	}

	/*
	 * Player Interactions
	 */
	@Override
	public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
		return false;
	}

	@Override
	public boolean canBeCollidedWith() {
		// Needed for right click, probably a forge or MC bug
		return true;
	}

	@Override
	public boolean attackEntityFrom(DamageSource damagesource, float amount) {
		if (world.isRemote) {
			return false;
		}
		if (damagesource.isCreativePlayer()) {
			this.setDead();
			world.removeEntity(this);
			return false;
		}

		if (damagesource.getTrueSource() instanceof EntityPlayer && !damagesource.isProjectile()) {
			this.setDead();
			world.removeEntity(this);
			return false;
		}
		return false;
	}

	@Override
	public boolean canBePushed() {
		return false;
	}

	/*
	 * Helpers
	 */

	public Speed getCurrentSpeed() {
		return Speed.fromMinecraft(MathHelper.sqrt(motionX * motionX + motionZ * motionZ));
	}

	public void sendToObserving(IMessage packet) {
		ImmersiveRailroading.net.sendToAllAround(packet,
				new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, ImmersiveRailroading.ENTITY_SYNC_DISTANCE));
	}

	public void render(double x, double y, double z, float entityYaw, float partialTicks) {
		if (this.getDefinition() != null) {
			this.getDefinition().render(this, x, y, z, entityYaw, partialTicks);
		} else {
			this.getEntityWorld().removeEntity(this);
		}
	}
}