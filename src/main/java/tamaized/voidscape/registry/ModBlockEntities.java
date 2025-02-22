package tamaized.voidscape.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import tamaized.regutil.RegUtil;
import tamaized.regutil.RegistryClass;
import tamaized.voidscape.block.entity.DefuserBlockEntity;
import tamaized.voidscape.block.entity.LiquifierBlockEntity;

import java.util.function.Supplier;

@SuppressWarnings("ConstantConditions")
public class ModBlockEntities implements RegistryClass {

	@Override
	public void init(IEventBus bus) {

	}

	private static final DeferredRegister<BlockEntityType<?>> REGISTERY = RegUtil.create(Registries.BLOCK_ENTITY_TYPE);

	public static final Supplier<BlockEntityType<LiquifierBlockEntity>> LIQUIFIER = REGISTERY
			.register("liquifier", () -> BlockEntityType.Builder.of(LiquifierBlockEntity::new, ModBlocks.MACHINE_LIQUIFIER.get()).build(null));

	public static final Supplier<BlockEntityType<DefuserBlockEntity>> DEFUSER = REGISTERY
			.register("defuser", () -> BlockEntityType.Builder.of(DefuserBlockEntity::new, ModBlocks.MACHINE_DEFUSER.get()).build(null));

}
