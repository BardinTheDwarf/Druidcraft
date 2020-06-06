package com.vulp.druidcraft.entities;

import com.vulp.druidcraft.entities.AI.goals.*;
import com.vulp.druidcraft.events.EventFactory;
import com.vulp.druidcraft.pathfinding.FlyingFishPathNavigator;
import com.vulp.druidcraft.registry.ParticleRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.controller.MovementController;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.IWorld;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

@SuppressWarnings("unchecked")
public class DreadfishEntity extends TameableFlyingMonsterEntity
{
    private static final Predicate<LivingEntity> isPlayer;
    private static final DataParameter<Integer> SMOKE_COLOR = EntityDataManager.createKey(DreadfishEntity.class, DataSerializers.VARINT);
    private static final Map<DyeColor, int[]> DYE_COLOR_MAP = new HashMap<>();
    private FlyingFishPathNavigator navigator;
    private DyeColor smokeColor = null;

    static {
        DYE_COLOR_MAP.put(DyeColor.BLACK, new int[]{15, 15, 15});
        DYE_COLOR_MAP.put(DyeColor.RED, new int[]{255, 50, 40});
        DYE_COLOR_MAP.put(DyeColor.GREEN, new int[]{15, 150, 45});
        DYE_COLOR_MAP.put(DyeColor.BROWN, new int[]{130, 70, 45});
        DYE_COLOR_MAP.put(DyeColor.BLUE, new int[]{30, 60, 225});
        DYE_COLOR_MAP.put(DyeColor.PURPLE, new int[]{135, 45, 245});
        DYE_COLOR_MAP.put(DyeColor.CYAN, new int[]{20, 125, 130});
        DYE_COLOR_MAP.put(DyeColor.LIGHT_GRAY, new int[]{160, 160, 155});
        DYE_COLOR_MAP.put(DyeColor.GRAY, new int[]{90, 90, 90});
        DYE_COLOR_MAP.put(DyeColor.PINK, new int[]{255, 115, 170});
        DYE_COLOR_MAP.put(DyeColor.LIME, new int[]{135, 250, 35});
        DYE_COLOR_MAP.put(DyeColor.YELLOW, new int[]{240, 240, 50});
        DYE_COLOR_MAP.put(DyeColor.LIGHT_BLUE, new int[]{50, 200, 255});
        DYE_COLOR_MAP.put(DyeColor.MAGENTA, new int[]{230, 65, 170});
        DYE_COLOR_MAP.put(DyeColor.ORANGE, new int[]{240, 135, 30});
        DYE_COLOR_MAP.put(DyeColor.WHITE, new int[]{215, 215, 215});
    }

    public DreadfishEntity(EntityType<? extends TameableFlyingMonsterEntity> type, World worldIn)
    {
        super(type, worldIn);
        this.navigator = (FlyingFishPathNavigator) this.createNavigator(worldIn);
        this.setTamed(false);
        this.moveController = new DreadfishEntity.MoveHelperController(this);
    }

    @Override
    protected void registerGoals()
    {
        super.registerGoals();
        this.sitGoal = new SitGoalMonster(this);
        this.goalSelector.addGoal(1, this.sitGoal);
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 3.0, true));
        this.goalSelector.addGoal(3, new LookAtGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.addGoal(4, new FollowOwnerGoalMonster(this, 5.0D, 10.0F, 2.0F));
        this.goalSelector.addGoal(5, new DreadfishEntity.SwimGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoalMonster(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoalMonster(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new NonTamedTargetGoalMonster(this, PlayerEntity.class, false, isPlayer));
        this.targetSelector.addGoal(5, new NonTamedTargetGoalMonster(this, IronGolemEntity.class, false));
    }

    @Override
    protected void registerAttributes()
    {
        super.registerAttributes();
        this.getAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.975d);
    }

    @Override
    protected void registerData() {
        super.registerData();
        this.dataManager.register(SMOKE_COLOR, DyeColor.PURPLE.getId());
    }

    public static boolean placement(EntityType<?> type, IWorld worldIn, SpawnReason reason, BlockPos pos, Random randomIn) {
        Block block = worldIn.getBlockState(pos.down()).getBlock();
        return worldIn.getDifficulty() != Difficulty.PEACEFUL && isValidLightLevel(worldIn, pos, randomIn);
    }

    @Override
    protected float getStandingEyeHeight(Pose poseIn, EntitySize sizeIn) {
        return 0.2F;
    }

    @Override
    public boolean onLivingFall(float p_225503_1_, float p_225503_2_) {
        return false;
    }

    private static boolean isValidLightLevel(IWorld world, BlockPos pos, Random rand) {
        if (world.getLightFor(LightType.SKY, pos) > rand.nextInt(32)) {
            return false;
        } else {
            int i = world.getWorld().isThundering() ? world.getNeighborAwareLightSubtracted(pos, 10) : world.getLight(pos);
            return i <= rand.nextInt(8);
        }
    }

    @Override
    protected void updateFallState(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            Entity entity = source.getTrueSource();
            if (this.sitGoal != null) {
                this.sitGoal.setSitting(false);
            }

            if (entity != null && !(entity instanceof PlayerEntity) && !(entity instanceof AbstractArrowEntity)) {
                amount = (amount + 1.0F) / 2.0F;
            }

            return super.attackEntityFrom(source, amount);
        }
    }

    @Override
    public void writeAdditional(CompoundNBT compound) {
        super.writeAdditional(compound);
        compound.putBoolean("Hostile", this.isHostile());
        compound.putByte("SmokeColor", (byte)this.getSmokeColor().getId());
    }

    @Override
    public void readAdditional(CompoundNBT compound) {
        super.readAdditional(compound);
        this.setHostile(compound.getBoolean("Hostile"));
        if (compound.contains("SmokeColor", 99)) {
            this.setSmokeColor(DyeColor.byId(compound.getInt("SmokeColor")));
        }
    }

    @Override
    public boolean hasNoGravity() {
        return true;
    }

    @Override
    public CreatureAttribute getCreatureAttribute() {
        return CreatureAttribute.UNDEAD;
    }

    @Override
    protected PathNavigator createNavigator(World worldIn) {
        return new FlyingFishPathNavigator(this, worldIn);
    }

    @Override
    public void travel(Vec3d relative) {
        if (this.isServerWorld()) {
            this.moveRelative(0.01F, relative);
            this.move(MoverType.SELF, this.getMotion());
            this.setMotion(this.getMotion().scale(0.9D));
        } else {
            super.travel(relative);
        }
    }

    protected boolean execute() {
        return true;
    }

    static class SwimGoal extends RandomFlyingGoal {
        private final DreadfishEntity dreadfishEntity;

        public SwimGoal(DreadfishEntity dreadfishEntity) {
            super(dreadfishEntity, 0.8D, 40);
            this.dreadfishEntity = dreadfishEntity;
        }

        @Override
        public boolean shouldExecute() {
            return this.dreadfishEntity.execute() && super.shouldExecute();
        }
    }


    static class MoveHelperController extends MovementController {
        private final DreadfishEntity dreadfishEntity;

        MoveHelperController(DreadfishEntity dreadfishEntity) {
            super(dreadfishEntity);
            this.dreadfishEntity = dreadfishEntity;
        }

        @Override
        public void tick() {

            this.dreadfishEntity.setMotion(this.dreadfishEntity.getMotion().add(0.0D, 0.0D, 0.0D));

            if (this.action == Action.MOVE_TO && !this.dreadfishEntity.getNavigator().noPath()) {
                double d0 = this.posX - this.dreadfishEntity.getPosX();
                double d1 = this.posY - this.dreadfishEntity.getPosY();
                double d2 = this.posZ - this.dreadfishEntity.getPosZ();
                double d3 = (double) MathHelper.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                d1 /= d3;
                float f = (float)(MathHelper.atan2(d2, d0) * 57.2957763671875D) - 90.0F;
                this.dreadfishEntity.rotationYaw = this.limitAngle(this.dreadfishEntity.rotationYaw, f, 90.0F);
                this.dreadfishEntity.renderYawOffset = this.dreadfishEntity.rotationYaw;
                float f1 = (float)(this.speed * this.dreadfishEntity.getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getValue());
                this.dreadfishEntity.setAIMoveSpeed(MathHelper.lerp(1.5F, this.dreadfishEntity.getAIMoveSpeed(), f1));
                this.dreadfishEntity.setMotion(this.dreadfishEntity.getMotion().add(0.0D, (double)this.dreadfishEntity.getAIMoveSpeed() * d1 * 0.01D, 0.0D));
            } else {
                this.dreadfishEntity.setAIMoveSpeed(0.0F);
            }
        }
    }

    @Override
    public void setTamed(boolean tamed) {
        super.setTamed(tamed);
        if (tamed) {
            this.getAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(24.0D);
        } else {
            this.getAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(16.0D);
        }

        this.getAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(4.0D);
    }

    @Override
    public void setAttackTarget(@Nullable LivingEntity entitylivingbaseIn) {
        super.setAttackTarget(entitylivingbaseIn);
        if (entitylivingbaseIn == null) {
            this.setHostile(false);
        } else if (!this.isTamed()) {
            this.setHostile(true);
        }

    }

    public int[] getSmokeColorArray () {
        return DYE_COLOR_MAP.getOrDefault(getSmokeColor(), new int[]{0, 0, 0});
    }

    public DyeColor getSmokeColor() {
        if (smokeColor == null) {
            smokeColor = DyeColor.byId(this.dataManager.get(SMOKE_COLOR));
        }
        return smokeColor;
    }

    public void setSmokeColor(DyeColor smokeColor) {
        this.dataManager.set(SMOKE_COLOR, smokeColor.getId());
        this.smokeColor = smokeColor;
    }

    @Override
    public boolean processInteract(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getHeldItem(hand);
        Item item = itemstack.getItem();
        if (this.isTamed()) {
            if (!itemstack.isEmpty()) {
                if (item == Items.BONE && this.getHealth() < this.getMaxHealth()) {
                    if (!player.abilities.isCreativeMode) {
                        itemstack.shrink(1);
                    }

                    this.heal(4f);
                    return true;
                }

                else if (item instanceof DyeItem) {
                    DyeColor dyecolor = ((DyeItem) item).getDyeColor();
                    if (dyecolor != this.getSmokeColor()) {
                        this.setSmokeColor(dyecolor);
                        if (!player.abilities.isCreativeMode) {
                            itemstack.shrink(1);
                        }

                        return true;
                    }
                }
            }
            if (this.isOwner(player) && !this.world.isRemote && !(item == Items.BONE && item instanceof DyeItem)) {
                this.sitGoal.setSitting(!this.isSitting());
                this.isJumping = false;
                this.navigator.clearPath();
                this.setAttackTarget(null);
            }
        }
        else if (item == Items.PRISMARINE_CRYSTALS) {
            if (!player.abilities.isCreativeMode) {
                itemstack.shrink(1);
            }

            if (!this.world.isRemote) {
                if (this.rand.nextInt(3) == 0 && !EventFactory.onMonsterTame(this, player)) {
                    this.playTameEffect(true);
                    this.setTamedBy(player);
                    this.navigator.clearPath();
                    this.setAttackTarget(null);
                    this.sitGoal.setSitting(true);
                    this.setHealth(24.0F);
                    this.world.setEntityState(this, (byte)7);
                } else {
                    this.playTameEffect(false);
                    this.world.setEntityState(this, (byte)6);
                }
            }

            return true;
        }

        return super.processInteract(player, hand);
    }

    @Override
    public boolean shouldAttackEntity(LivingEntity target, LivingEntity owner) {
        if (!(target instanceof CreeperEntity)) {
            if (target instanceof TameableMonsterEntity) {
                TameableMonsterEntity monsterEntity = (TameableMonsterEntity) target;
                if (monsterEntity.isTamed() && monsterEntity.getOwner() == this.getOwner()) {
                    return false;
                }
            }

            if (target instanceof TameableEntity) {
                TameableEntity tameableEntity = (TameableEntity) target;
                if (tameableEntity.isTamed() && tameableEntity.getOwner() == this.getOwner()) {
                    return false;
                }
            }

            if (target instanceof PlayerEntity && owner instanceof PlayerEntity && !((PlayerEntity) owner).canAttackPlayer((PlayerEntity) target)) {
                return false;
            } else if (target instanceof AbstractHorseEntity && ((AbstractHorseEntity) target).isTame()) {
                return false;
            } else {
                return !(target instanceof CatEntity) || !((CatEntity) target).isTamed();
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean canBeLeashedTo(PlayerEntity player) {
        return !this.isHostile() && !this.isTamed();
    }

    static {
        isPlayer = (type) -> {
            EntityType<?> entitytype = type.getType();
            return entitytype == EntityType.PLAYER;
        };
    }

    public boolean isHostile() {
        return (this.dataManager.get(TAMED) & 2) != 0;
    }

    public void setHostile(boolean hostile) {
        byte b0 = this.dataManager.get(TAMED);
        if (hostile) {
            this.dataManager.set(TAMED, (byte)(b0 | 2));
        } else {
            this.dataManager.set(TAMED, (byte)(b0 & -3));
        }

    }

    @Override
    public void livingTick() {
        super.livingTick();
        if (this.world.isRemote) {
            int[] color = getSmokeColorArray();

            world.addParticle(ParticleRegistry.magic_smoke, false, this.getPosX(), this.getPosY() + (((rand.nextDouble() - 0.5) + 0.2) / 3) + 0.2, this.getPosZ() + (((rand.nextDouble() - 0.5) + 0.2) / 3), color[0] / 255.f, color[1] / 255.f, color[2] / 255.f);
        }

        if (!this.world.isRemote && this.getAttackTarget() == null && this.isHostile()) {
            this.setHostile(false);
        }
    }

    @Override
    public boolean isOnLadder() {
        return false;
    }
}