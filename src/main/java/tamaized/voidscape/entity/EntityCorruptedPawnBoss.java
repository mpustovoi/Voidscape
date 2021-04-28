package tamaized.voidscape.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.BossInfo;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerBossInfo;
import net.minecraft.world.server.ServerWorld;
import tamaized.voidscape.Voidscape;
import tamaized.voidscape.entity.ai.AITask;
import tamaized.voidscape.entity.ai.IInstanceEntity;
import tamaized.voidscape.registry.ModDamageSource;
import tamaized.voidscape.registry.ModEntities;
import tamaized.voidscape.turmoil.SubCapability;
import tamaized.voidscape.world.Instance;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EntityCorruptedPawnBoss extends EntityCorruptedPawn implements IInstanceEntity {

	private static final Vector3d[] TENTACLE_POSITIONS = new Vector3d[]{new Vector3d(36.5, 60, 0.5), new Vector3d(27.5, 60, 9.5), new Vector3d(18.5, 60, 18.5), new Vector3d(9.5, 60, 9.5), new Vector3d(0.5, 60, 0.5), new Vector3d(9.5, 60, -9.5), new Vector3d(18.5, 60, -18.5), new Vector3d(27.5, 60, -9.5)};
	private final ServerBossInfo bossEvent = (ServerBossInfo) (new ServerBossInfo(this.getDisplayName(), BossInfo.Color.PURPLE, BossInfo.Overlay.PROGRESS)).setDarkenScreen(true);
	public boolean beStill = false;
	private AITask<EntityCorruptedPawnBoss> ai;
	private Instance.InstanceType type;
	private int aiTick;
	private Entity lockonTarget;
	private List<Entity> tentacles = new ArrayList<>();
	private List<Integer> tentacleIndicies = new ArrayList<>();

	public EntityCorruptedPawnBoss(World level) {
		this(ModEntities.CORRUPTED_PAWN_BOSS.get(), level);
	}

	public EntityCorruptedPawnBoss(EntityType<? extends EntityCorruptedPawn> p_i48577_1_, World p_i48577_2_) {
		super(p_i48577_1_, p_i48577_2_);
		forcedLoading = true;
		setNoGravity(true);
	}

	@Override
	public void startSeenByPlayer(ServerPlayerEntity player) {
		super.startSeenByPlayer(player);
		this.bossEvent.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayerEntity player) {
		super.stopSeenByPlayer(player);
		this.bossEvent.removePlayer(player);
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		return super.hurt(source, ModDamageSource.check(ModDamageSource.ID_VOIDIC, source) ? amount : amount * 0.01F);
	}

	@Override
	public void lookAt(Entity entityIn, float maxYawIncrease, float maxPitchIncrease) {
		super.lookAt(entityIn, maxYawIncrease, maxPitchIncrease);
		setYHeadRot(yRot);
	}

	@Override
	public void tick() {
		if (!level.isClientSide())
			setDeltaMovement(Vector3d.ZERO);
		bossEvent.setPercent(this.getHealth() / this.getMaxHealth());
		if (getTarget() != null && getTarget().getCapability(SubCapability.CAPABILITY).map(cap -> cap.get(Voidscape.subCapTurmoilTracked).map(data -> data.incapacitated).orElse(false)).orElse(false)) {
			getCapability(SubCapability.CAPABILITY_AGGRO).ifPresent(cap -> cap.remove(getTarget()));
			setTarget(null);
		}
		if (!level.isClientSide() && ai != null && getTarget() != null) {
			ai = ai.handle(this);
			lookAt(getTarget(), 10F, 10F);
			if (!beStill) {
				if (distanceTo(getTarget()) > 4) {
					Vector3d angle = getLookAngle().scale(0.25F);
					move(MoverType.SELF, new Vector3d(angle.x(), 0, angle.z()));
				}
			}
		}
		if (!level.isClientSide() && getTarget() == null) {
			Entity closest = null;
			for (PlayerEntity p : level.getEntitiesOfClass(PlayerEntity.class, getBoundingBox().inflate(20F), e -> true)) {
				if (closest == null || distanceTo(p) < distanceTo(closest))
					closest = p;
			}
			if (closest != null)
				lookAt(closest, 10F, 10F);
		}
		super.tick();
	}

	@Override
	public void readAdditionalSaveData(CompoundNBT tag) {
		super.readAdditionalSaveData(tag);
		type = Instance.InstanceType.fromOrdinal(tag.getCompound(Voidscape.MODID).getInt("instance") - 1);
	}

	@Override
	public void addAdditionalSaveData(CompoundNBT tag) {
		super.addAdditionalSaveData(tag);
		CompoundNBT nbt = new CompoundNBT();
		if (type != null)
			nbt.putInt("instance", type.ordinal() + 1);
		tag.put(Voidscape.MODID, nbt);
	}

	private void doTeleportParticles() {
		if (level instanceof ServerWorld)
			for (int i = 0; i < 50; i++) {
				double x = random.nextFloat() * (getBoundingBox().maxX - getBoundingBox().minX) + getBoundingBox().minX;
				double y = random.nextFloat() * (getBoundingBox().maxY - getBoundingBox().minY) + getBoundingBox().minY;
				double z = random.nextFloat() * (getBoundingBox().maxZ - getBoundingBox().minZ) + getBoundingBox().minZ;
				((ServerWorld) level).sendParticles(ParticleTypes.SQUID_INK, x, y, z, 0, 0, 0, 0, 0);
			}
	}

	private void teleportHome() {
		doTeleportParticles();
		moveTo(getRestrictCenter().getX() + 0.5F, getRestrictCenter().getY(), getRestrictCenter().getZ() + 0.5F);
		if (!level.isClientSide())
			level.playSound(null, this.xo, this.yo, this.zo, SoundEvents.ENDERMAN_TELEPORT, this.getSoundSource(), 4F, 0.25F + random.nextFloat() * 0.5F);
		doTeleportParticles();
	}

	@Override
	@SuppressWarnings("ConstantConditions")
	public void initInstanceType(Instance.InstanceType type) {
		this.type = type;
		float hp = 20;
		switch (type) {
			case Unrestricted:
				hp = 400;
				(ai = new AITask.PendingAITask<>((boss, ai) -> {
					if (!boss.isInvulnerable()) {
						boss.setInvulnerable(true);
						boss.beStill = true;
						boss.markCasting(true);
						int t1 = random.nextInt(8);
						int t2 = t1;
						while (t2 == t1)
							t2 = random.nextInt(8);
						boss.disableTentacles(t1 | t2);
						tentacleIndicies.add(t1);
						tentacleIndicies.add(t2);
						boss.teleportHome();
						for (int t : tentacleIndicies) {
							Vector3d pos = TENTACLE_POSITIONS[t];
							EntityCorruptedPawnTentacle tentacle = new EntityCorruptedPawnTentacle(level, this, pos).setHealth(25).explodes(65 * 20, 100F);
							tentacles.add(tentacle);
							level.addFreshEntity(tentacle);
						}
					} else {
						tentacles.removeIf(t -> !t.isAlive());
						if (tentacles.isEmpty()) {
							int t = 0;
							for (Integer i : tentacleIndicies)
								t |= i;
							tentacleIndicies.clear();
							boss.enableTentacles(t);
							boss.markCasting(false);
							boss.setInvulnerable(false);
							ai.finish();
						}
					}
				}, boss -> boss.getHealth() / boss.getMaxHealth() <= 0.75F)).
						next(new AITask.PendingAITask<>((boss, ai) -> {
							ai.finish();
						}, boss -> boss.getHealth() / boss.getMaxHealth() <= 0.5F)).
						next(new AITask.PendingAITask<>((boss, ai) -> {
							ai.finish();
						}, boss -> boss.getHealth() / boss.getMaxHealth() <= 0.25F)).
						next(new AITask.RandomAITask<>()).
						next(new AITask.RepeatedAITask<>((boss, ai) -> { // Auto Attack
							if (boss.aiTick == 0) {
								if (boss.tickCount % 60 == 0)
									boss.aiTick = boss.tickCount + 20 * 3;
							} else if (boss.tickCount >= boss.aiTick) {
								boss.aiTick = 0;
								boss.getTarget().hurt(DamageSource.indirectMagic(boss, boss), 3);
								boss.getTarget().hurt(ModDamageSource.VOIDIC_WITH_ENTITY.apply(boss), 3);
								ai.finish();
							}
						})).
						next(new AITask.RandomAITask.ChanceAITask<EntityCorruptedPawnBoss>(rand -> rand.nextInt(5) == 0).
								next(new AITask.RepeatedAITask<>((boss, ai) -> { // Tank Buster
									if (boss.aiTick == 0) {
										if (boss.tickCount % 60 == 0) {
											boss.beStill = true;
											boss.aiTick = boss.tickCount + 20 * 6;
											boss.lockonTarget = boss.getTarget();
										}
									} else if (boss.tickCount >= boss.aiTick) {
										boss.aiTick = 0;
										if (boss.lockonTarget == null || !boss.lockonTarget.isAlive())
											boss.lockonTarget = boss.getTarget();
										boss.lockonTarget.hurt(DamageSource.indirectMagic(boss, boss), 6);
										boss.lockonTarget.hurt(ModDamageSource.VOIDIC_WITH_ENTITY.apply(boss), 6);
										boss.beStill = false;
										ai.finish();
									}
								})));
				break;
			case Normal:
				remove();
				break;
			case Insane:
				remove();
				break;
		}
		Objects.requireNonNull(getAttribute(Attributes.MAX_HEALTH)).addPermanentModifier(new AttributeModifier("Instanced Health", hp - 20, AttributeModifier.Operation.ADDITION));
		setHealth(getMaxHealth());
	}
}
