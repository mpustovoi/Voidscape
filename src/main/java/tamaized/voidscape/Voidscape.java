package tamaized.voidscape;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.IndirectEntityDamageSource;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.progress.StartupMessageManager;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tamaized.voidscape.network.NetworkMessages;
import tamaized.voidscape.registry.ModAttributes;
import tamaized.voidscape.registry.ModBlocks;
import tamaized.voidscape.registry.ModDamageSource;
import tamaized.voidscape.registry.RegUtil;
import tamaized.voidscape.turmoil.Insanity;
import tamaized.voidscape.turmoil.SubCapability;
import tamaized.voidscape.turmoil.Turmoil;
import tamaized.voidscape.turmoil.TurmoilStats;
import tamaized.voidscape.turmoil.skills.TurmoilSkill;
import tamaized.voidscape.world.InstanceChunkGenerator;
import tamaized.voidscape.world.VoidChunkGenerator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Mod(Voidscape.MODID)
public class Voidscape {

	public static final String MODID = "voidscape";

	public static final Logger LOGGER = LogManager.getLogger(MODID);

	public static final SimpleChannel NETWORK = NetworkRegistry.ChannelBuilder.
			named(new ResourceLocation(MODID, MODID)).
			clientAcceptedVersions(s -> true).
			serverAcceptedVersions(s -> true).
			networkProtocolVersion(() -> "1").
			simpleChannel();
	public static final RegistryKey<World> WORLD_KEY_VOID = RegistryKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(MODID, "void"));
	public static final SubCapability.ISubCap.SubCapKey<Turmoil> subCapTurmoilData = SubCapability.AttachedSubCap.register(Turmoil.class, Turmoil::new);
	public static final SubCapability.ISubCap.SubCapKey<Insanity> subCapInsanity = SubCapability.AttachedSubCap.register(Insanity.class, Insanity::new);
	public static final SubCapability.ISubCap.SubCapKey<TurmoilStats> subCapTurmoilStats = SubCapability.AttachedSubCap.register(TurmoilStats.class, TurmoilStats::new);

	public Voidscape() {
		StartupMessageManager.addModMessage("Loading Turmoil");
		TurmoilSkill.classload();
		IEventBus busMod = FMLJavaModLoadingContext.get().getModEventBus();
		IEventBus busForge = MinecraftForge.EVENT_BUS;
		RegUtil.register(busMod);
		busMod.addListener((Consumer<FMLCommonSetupEvent>) event -> {
			NetworkMessages.register(NETWORK);
			Registry.register(Registry.CHUNK_GENERATOR, new ResourceLocation(MODID, "void"), VoidChunkGenerator.codec);
			Registry.register(Registry.CHUNK_GENERATOR, new ResourceLocation(MODID, "instance"), InstanceChunkGenerator.codec);
			CapabilityManager.INSTANCE.register(SubCapability.ISubCap.class, new SubCapability.ISubCap.Storage() {
			}, SubCapability.AttachedSubCap::new);
		});
		busForge.addListener((Consumer<FMLServerStartingEvent>) event ->

				event.getServer().getCommands().getDispatcher().register(LiteralArgumentBuilder.<CommandSource>literal("voidscape").
						then(VoidCommands.Debug.register()))

		);
		busForge.addListener((Consumer<TickEvent.PlayerTickEvent>) event -> {
			if (event.player.level != null && !event.player.level.isClientSide() && checkForVoidDimension(event.player.level) && event.player.tickCount % 30 == 0 && event.player.getRandom().nextFloat() <= 0.20F) {
				final int dist = 64;
				final int rad = dist / 2;
				final Supplier<Integer> exec = () -> event.player.getRandom().nextInt(dist) - rad;
				BlockPos dest = event.player.blockPosition().offset(exec.get(), exec.get(), exec.get());
				if (event.player.level.getBlockState(dest).equals(Blocks.BEDROCK.defaultBlockState()))
					event.player.level.setBlockAndUpdate(dest, ModBlocks.VOIDIC_CRYSTAL_ORE.get().defaultBlockState());
			}
		});
		busForge.addListener((Consumer<LivingHurtEvent>) event -> {
			if (ModDamageSource.check(ModDamageSource.ID_VOIDIC, event.getSource()))
				event.setAmount((float) Math.max(0, event.getAmount() - event.getEntityLiving().getAttributeValue(ModAttributes.VOIDIC_RES.get())));
			else if (meleeOrArrowSource(event.getSource())) {
				Entity e = event.getSource() instanceof IndirectEntityDamageSource ? event.getSource().getEntity() : event.getSource().getDirectEntity();
				if (e instanceof LivingEntity) {
					LivingEntity attacker = (LivingEntity) e;
					float dmg = (float) attacker.getAttributeValue(ModAttributes.VOIDIC_DMG.get());
					if (dmg > 0) {
						event.getEntity().invulnerableTime = 0;
						event.getEntity().hurt(ModDamageSource.VOIDIC_WITH_ENTITY.apply(attacker), dmg);
					}
				}
			}
		});
	}

	private static boolean meleeOrArrowSource(DamageSource source) {
		if (!(source instanceof EntityDamageSource))
			return false;
		switch (source.getMsgId()) {
			case "player":
			case "mob":
			case "arrow":
				return true;
			default:
				return false;
		}
	}

	public static boolean checkForVoidDimension(World world) {
		return world.dimension().location().equals(WORLD_KEY_VOID.location());
	}

	public static ServerWorld getWorld(World world, RegistryKey<World> dest) {
		return Objects.requireNonNull(Objects.requireNonNull(world.getServer()).getLevel(dest));
	}

	@Nonnull
	@SuppressWarnings("ConstantConditions")
	public static <T> T getNull() {
		return null;
	}

	public static RayTraceResult getHitResultFromEyes(LivingEntity entity, Predicate<Entity> predicate, double range) {
		Vector3d vector3d = entity.getEyePosition(1F);
		Vector3d vector3d1 = entity.getViewVector(1.0F);
		Vector3d vector3d2 = vector3d.add(vector3d1.x * range, vector3d1.y * range, vector3d1.z * range);
		AxisAlignedBB axisalignedbb = entity.getBoundingBox().expandTowards(vector3d1.scale(range)).inflate(1.0D, 1.0D, 1.0D);
		RayTraceResult raytraceresult = entity.level.clip(new RayTraceContext(vector3d, vector3d2, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, entity));
		if (raytraceresult.getType() != RayTraceResult.Type.MISS) {
			vector3d2 = raytraceresult.getLocation();
		}
		RayTraceResult ray = getEntityHitResult(entity, vector3d, vector3d2, axisalignedbb, predicate, range * range);
		return ray == null ? raytraceresult : ray;
	}

	@Nullable
	private static EntityRayTraceResult getEntityHitResult(Entity shooter, Vector3d startVec, Vector3d endVec, AxisAlignedBB boundingBox, Predicate<Entity> filter, double distance) {
		World world = shooter.level;
		double d0 = distance;
		Entity entity = null;
		Vector3d vector3d = null;

		for (Entity entity1 : world.getEntities(shooter, boundingBox, filter)) {
			AxisAlignedBB axisalignedbb = entity1.getBoundingBox().inflate((double) entity1.getPickRadius());
			Optional<Vector3d> optional = axisalignedbb.clip(startVec, endVec);
			if (axisalignedbb.contains(startVec)) {
				if (d0 >= 0.0D) {
					entity = entity1;
					vector3d = optional.orElse(startVec);
					d0 = 0.0D;
				}
			} else if (optional.isPresent()) {
				Vector3d vector3d1 = optional.get();
				double d1 = startVec.distanceToSqr(vector3d1);
				if (d1 < d0 || d0 == 0.0D) {
					if (entity1.getRootVehicle() == shooter.getRootVehicle() && !entity1.canRiderInteract()) {
						if (d0 == 0.0D) {
							entity = entity1;
							vector3d = vector3d1;
						}
					} else {
						entity = entity1;
						vector3d = vector3d1;
						d0 = d1;
					}
				}
			}
		}

		return entity == null ? null : new EntityRayTraceResult(entity, vector3d);
	}

}
