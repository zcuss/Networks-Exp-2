package io.github.sefiraat.networks.utils.datatypes;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.github.sefiraat.networks.network.stackcaches.QuantumCache;
import io.github.sefiraat.networks.utils.Keys;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nonnull;

public class PersistentQuantumStorageType implements PersistentDataType<PersistentDataContainer, QuantumCache> {

    public static final PersistentDataType<PersistentDataContainer, QuantumCache> TYPE = new PersistentQuantumStorageType();

    public static final NamespacedKey ITEM = Keys.newKey("item");
    public static final NamespacedKey AMOUNT = Keys.newKey("amount");
    public static final NamespacedKey MAX_AMOUNT = Keys.newKey("max_amount");
    public static final NamespacedKey VOID = Keys.newKey("void");

    @Override
    @Nonnull
    public Class<PersistentDataContainer> getPrimitiveType() {
        return PersistentDataContainer.class;
    }

    @Override
    @Nonnull
    public Class<QuantumCache> getComplexType() {
        return QuantumCache.class;
    }

    @Override
    @Nonnull
    public PersistentDataContainer toPrimitive(@Nonnull QuantumCache complex, @Nonnull PersistentDataAdapterContext context) {
        PersistentDataContainer container = context.newPersistentDataContainer();
        container.set(ITEM, DataType.ITEM_STACK, complex.getItemStack());
        container.set(AMOUNT, DataType.INTEGER, complex.getAmount());
        container.set(MAX_AMOUNT, DataType.INTEGER, complex.getLimit());
        container.set(VOID, DataType.BOOLEAN, complex.isVoidExcess());
        return container;
    }

    @Override
    @Nonnull
    public QuantumCache fromPrimitive(@Nonnull PersistentDataContainer primitive, @Nonnull PersistentDataAdapterContext context) {
        ItemStack item = primitive.get(ITEM, DataType.ITEM_STACK);
        int amount = primitive.get(AMOUNT, DataType.INTEGER);
        int limit = primitive.get(MAX_AMOUNT, DataType.INTEGER);
        boolean voidExcess = primitive.get(VOID, DataType.BOOLEAN);

        return new QuantumCache(item, amount, limit, voidExcess);
    }
}
