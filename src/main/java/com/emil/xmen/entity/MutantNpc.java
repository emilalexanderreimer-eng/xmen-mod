package com.emil.xmen.entity;

import java.util.function.Supplier;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

/**
 * A real X-Men character (or villain) with its own skin. Which one it is comes from the "Character" value; the
 * name, health and special moves come from {@link com.emil.xmen.world.MutantMobs} like for every other structure
 * mob. Friendly characters walk around, chat and fight monsters; hostile ones go after players.
 * Characters that sell things (Professor X) open a trading screen.
 */
public class MutantNpc extends PathfinderMob implements Merchant {
    private static final EntityDataAccessor<String> CHARACTER =
            SynchedEntityData.defineId(MutantNpc.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> HOSTILE =
            SynchedEntityData.defineId(MutantNpc.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SITTING =
            SynchedEntityData.defineId(MutantNpc.class, EntityDataSerializers.BOOLEAN);

    /** Set by MutantMobs for characters that trade. Only lives on the server. */
    public static Supplier<MerchantOffers> professorOffers = MerchantOffers::new;

    private MerchantOffers offers;
    private net.minecraft.core.BlockPos home;
    private Player customer;

    public MutantNpc(EntityType<? extends MutantNpc> type, Level level) {
        super(type, level);
        if (getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARACTER, "");
        builder.define(HOSTILE, false);
        builder.define(SITTING, false);
    }

    public String getCharacter() {
        return entityData.get(CHARACTER);
    }

    public void setCharacter(String value) {
        entityData.set(CHARACTER, value);
    }

    public boolean isHostile() {
        return entityData.get(HOSTILE);
    }

    public void setHostile(boolean value) {
        entityData.set(HOSTILE, value);
    }

    /** Professor X sits in his wheelchair and doesn't walk around. */
    public boolean isSitting() {
        return entityData.get(SITTING);
    }

    public void setSitting(boolean value) {
        entityData.set(SITTING, value);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Character", getCharacter());
        tag.putBoolean("Hostile", isHostile());
        tag.putBoolean("Sitting", isSitting());
        if (home != null) {
            tag.putLong("Home", home.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setCharacter(tag.getString("Character"));
        setHostile(tag.getBoolean("Hostile"));
        setSitting(tag.getBoolean("Sitting"));
        if (tag.contains("Home")) {
            home = net.minecraft.core.BlockPos.of(tag.getLong("Home"));
        }
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15, true) {
            @Override
            public boolean canUse() {
                return !isSitting() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !isSitting() && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(2, new OpenDoorGoal(this, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6) {
            @Override
            public boolean canUse() {
                return !isSitting() && super.canUse();
            }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10, 0.05f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        // Villains attack players. Heroes protect people from monsters and only hit back at monsters.
        targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return isHostile() && super.canUse();
            }
        });
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                target -> target instanceof Player player && !player.isCreative() && !player.isSpectator()) {
            @Override
            public boolean canUse() {
                return isHostile() && super.canUse();
            }
        });
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
                target -> target instanceof Enemy && !(target instanceof MutantNpc)) {
            @Override
            public boolean canUse() {
                return !isHostile() && !isSitting() && super.canUse();
            }
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && !hasRestriction()) {
            // heroes stay around the room they live in instead of wandering off into the world
            if (home == null) {
                home = blockPosition();
            }
            if (!isHostile()) {
                restrictTo(home, 10);
            }
        }
        LivingEntity target = getTarget();
        if (!level().isClientSide && !isHostile() && target instanceof Player) {
            setTarget(null);
        }
        if (customer != null && (!customer.isAlive() || customer.distanceToSqr(this) > 64)) {
            setTradingPlayer(null);
        }
    }

    /** Players can't (accidentally) beat up the heroes. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!isHostile() && source.getEntity() instanceof Player player && !player.isCreative()) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    // ------------------------------------------------------------------ trading (Professor X)
    public boolean trades() {
        return "professor_x".equals(getCharacter());
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (trades() && !isHostile() && hand == InteractionHand.MAIN_HAND && !player.isShiftKeyDown()) {
            if (!level().isClientSide && customer == null) {
                setTradingPlayer(player);
                openTradingScreen(player, getDisplayName(), 1);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void setTradingPlayer(Player player) {
        customer = player;
    }

    @Override
    public Player getTradingPlayer() {
        return customer;
    }

    @Override
    public MerchantOffers getOffers() {
        if (offers == null) {
            offers = professorOffers.get();
        }
        return offers;
    }

    @Override
    public void overrideOffers(MerchantOffers value) {
        offers = value;
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        playSound(SoundEvents.VILLAGER_YES, 0.6f, 0.8f);
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    @Override
    public boolean isClientSide() {
        return level().isClientSide;
    }
}
