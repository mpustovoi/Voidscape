package tamaized.voidscape.entity;

import net.minecraft.world.entity.Entity;

public interface IEthereal {

	/**
	 * @return true to skip {@link tamaized.voidscape.capability.Insanity#tick(Entity)}
	 */
	default boolean insanityImmunity() {
		return true;
	}

}
