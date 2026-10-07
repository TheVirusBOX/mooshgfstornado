package com.moosh.tornados;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(TornadoMod.MODID)
public class TornadoMod {
    public static final String MODID = "mooshgfstornado";
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final RegistryObject<EntityType<TornadoEntity>> TORNADO = ENTITIES.register("tornado",
            () -> EntityType.Builder.<TornadoEntity>of(TornadoEntity::new, MobCategory.MISC)
                    .sized(2f, 4f).fireImmune().clientTrackingRange(32).updateInterval(2).build("tornado"));

    @SuppressWarnings("unchecked")
    public static final RegistryObject<EntityType<DominatorEntity>>[] DOMINATORS = new RegistryObject[4];
    @SuppressWarnings("unchecked")
    public static final RegistryObject<Item>[] EGGS = new RegistryObject[4];
    private static final int[][] EGG_COLORS = {{0xB01818, 0x202020}, {0x4A2A1A, 0x101010}, {0x101010, 0xC02020}, {0x101010, 0x2060FF}};

    static {
        for (int i = 0; i < 4; i++) {
            final int lvl = i + 1;
            DOMINATORS[i] = ENTITIES.register("dominator" + lvl,
                    () -> EntityType.Builder.<DominatorEntity>of((t, l) -> new DominatorEntity(t, l, lvl), MobCategory.MISC)
                            .sized(2.6f, 1.7f).fireImmune().clientTrackingRange(16).build("dominator" + lvl));
            final int idx = i;
            EGGS[i] = ITEMS.register("dominator" + lvl + "_spawn_egg",
                    () -> new ForgeSpawnEggItem(() -> (EntityType<? extends Mob>) DOMINATORS[idx].get(), EGG_COLORS[idx][0], EGG_COLORS[idx][1], new Item.Properties()));
        }
    }

    public TornadoMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener(TornadoMod::tabs);
    }

    private static void tabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.SPAWN_EGGS) for (RegistryObject<Item> egg : EGGS) e.accept(egg.get());
    }
}
