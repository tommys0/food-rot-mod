package jabkari.foodrotmod.db;

import jabkari.foodrotmod.Foodrotmod;
import jabkari.foodrotmod.db.entity.IceBoxData;
import jabkari.foodrotmod.db.entity.StoredItem;
import jakarta.persistence.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Function;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class DatabaseService {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static EntityManagerFactory emf;

    public static void initialize() {
        if (emf != null && emf.isOpen()) {
            LOGGER.warn("DatabaseService already initialized.");
            return;
        }
        try {
            emf = Persistence.createEntityManagerFactory("foodrot-pu");
            LOGGER.info("DatabaseService initialized successfully using persistence unit 'foodrot-pu'.");
        } catch (Exception e) {
            LOGGER.error("!!! CRITICAL: Failed to initialize DatabaseService (JPA/Hibernate) !!!", e);
            LOGGER.error("!!! FoodRotMod database features will be DISABLED. Check persistence.xml and dependencies. !!!");
            emf = null;
        }
    }

    public static void shutdown() {
        if (emf != null && emf.isOpen()) {
            emf.close();
            LOGGER.info("DatabaseService shut down.");
            emf = null;
        }
    }

    public static boolean isInitialized() {
        return emf != null && emf.isOpen();
    }

    private static void executeTransaction(Consumer<EntityManager> operation) {
        if (!isInitialized()) return;
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();
            operation.accept(em);
            em.getTransaction().commit();
        } catch (Exception e) {
            LOGGER.error("Database transaction failed:", e);
            if (em != null && em.getTransaction().isActive()) {
                try {
                    em.getTransaction().rollback();
                    LOGGER.info("Transaction rolled back.");
                } catch (Exception rbEx) {
                    LOGGER.error("Transaction rollback failed:", rbEx);
                }
            }
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    private static <R> R executeRead(Function<EntityManager, R> operation, R defaultValue) {
        if (!isInitialized()) return defaultValue;
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            return operation.apply(em);
        } catch (Exception e) {
            LOGGER.error("Database read operation failed:", e);
            return defaultValue;
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    public static Optional<IceBoxData> findIceBoxWithItems(Level level, BlockPos pos) {
        if (level == null || pos == null) return Optional.empty();
        return findIceBoxWithItems(level.dimension().location().toString(), pos.getX(), pos.getY(), pos.getZ());
    }

    public static Optional<IceBoxData> findIceBoxWithItems(String worldKey, int x, int y, int z) {
        return executeRead(em -> {
            try {
                TypedQuery<IceBoxData> query = em.createQuery(
                        "SELECT ib FROM IceBoxData ib LEFT JOIN FETCH ib.items WHERE ib.worldKey = :worldKey AND ib.posX = :x AND ib.posY = :y AND ib.posZ = :z",
                        IceBoxData.class
                );
                query.setParameter("worldKey", worldKey);
                query.setParameter("x", x);
                query.setParameter("y", y);
                query.setParameter("z", z);
                return Optional.of(query.getSingleResult());
            } catch (NoResultException e) {
                return Optional.empty();
            }
        }, Optional.empty());
    }

    private static Optional<IceBoxData> findIceBoxInternal(String worldKey, int x, int y, int z, EntityManager em) {
        try {
            TypedQuery<IceBoxData> query = em.createQuery(
                    "SELECT ib FROM IceBoxData ib WHERE ib.worldKey = :worldKey AND ib.posX = :x AND ib.posY = :y AND ib.posZ = :z",
                    IceBoxData.class
            );
            query.setParameter("worldKey", worldKey);
            query.setParameter("x", x);
            query.setParameter("y", y);
            query.setParameter("z", z);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public static void saveOrUpdateIceBox(Level level, BlockPos pos, List<ItemStack> currentItems, HolderLookup.Provider provider) {
        if (level == null || pos == null || currentItems == null || provider == null) return;
        String worldKey = level.dimension().location().toString();
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        executeTransaction(em -> {
            IceBoxData iceBox = findIceBoxInternal(worldKey, x, y, z, em)
                    .orElseGet(() -> new IceBoxData(worldKey, x, y, z));

            List<StoredItem> existingDbItems = new ArrayList<>(iceBox.getItems());
            iceBox.getItems().clear();

            for (int i = 0; i < currentItems.size(); i++) {
                ItemStack stack = currentItems.get(i);
                if (!stack.isEmpty()) {
                    try {
                        byte[] nbtData = serializeItemStack(stack, provider);
                        if (nbtData != null) {
                            StoredItem existingItem = findAndRemove(existingDbItems, i);
                            if (existingItem != null) {
                                existingItem.setItemNbtData(nbtData);
                                existingItem.setIceBox(iceBox);
                                iceBox.addItem(existingItem);
                            } else {
                                StoredItem newItem = new StoredItem(iceBox, i, nbtData);
                                iceBox.addItem(newItem);
                            }
                        }
                    } catch (IOException e) {
                        LOGGER.error("Failed to serialize ItemStack for slot {} at {}/{},{},{}", i, worldKey, x, y, z, e);
                    }
                }
            }
            if (iceBox.getId() == null) {
                em.persist(iceBox);
            } else {
                em.merge(iceBox);
            }
        });
    }

    private static StoredItem findAndRemove(List<StoredItem> list, int slotIndex) {
        StoredItem found = null;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getSlotIndex() == slotIndex) {
                found = list.remove(i);
                break;
            }
        }
        return found;
    }

    public static void removeIceBox(Level level, BlockPos pos) {
        if (level == null || pos == null) return;
        String worldKey = level.dimension().location().toString();
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        executeTransaction(em -> {
            findIceBoxInternal(worldKey, x, y, z, em).ifPresent(iceBox -> {
                LOGGER.debug("Removing IceBoxData with ID: {} at {}/{},{},{}", iceBox.getId(), worldKey, x, y, z);
                em.remove(iceBox);
            });
        });
    }

    public static List<ItemStack> loadItemsForIceBox(Level level, BlockPos pos, int inventorySize, HolderLookup.Provider provider) {
        List<ItemStack> items = NonNullList.withSize(inventorySize, ItemStack.EMPTY);
        if (level == null || pos == null || provider == null) return items;

        findIceBoxWithItems(level, pos).ifPresent(iceBoxData -> {
            for (StoredItem storedItem : iceBoxData.getItems()) {
                if (storedItem.getSlotIndex() >= 0 && storedItem.getSlotIndex() < inventorySize) {
                    try {
                        ItemStack stack = deserializeItemStack(storedItem.getItemNbtData(), provider);
                        items.set(storedItem.getSlotIndex(), stack);
                    } catch (IOException e) {
                        LOGGER.error("Failed to deserialize ItemStack for slot {} at {}/{},{},{}",
                                storedItem.getSlotIndex(), iceBoxData.getWorldKey(), iceBoxData.getPosX(), iceBoxData.getPosY(), iceBoxData.getPosZ(), e);
                    } catch (Exception e) {
                        LOGGER.error("Critical error deserializing ItemStack NBT for slot {} at {}/{},{},{}. Data might be corrupted.",
                                storedItem.getSlotIndex(), iceBoxData.getWorldKey(), iceBoxData.getPosX(), iceBoxData.getPosY(), iceBoxData.getPosZ(), e);
                    }
                } else {
                    LOGGER.warn("Loaded item with invalid slot index {} (max {}) for IceBox at {}/{},{},{}",
                            storedItem.getSlotIndex(), inventorySize-1, iceBoxData.getWorldKey(), iceBoxData.getPosX(), iceBoxData.getPosY(), iceBoxData.getPosZ());
                }
            }
        });
        return items;
    }

    public static boolean isLocationAnIceBox(Level level, BlockPos pos) {
        if (level == null || pos == null || !isInitialized()) return false;
        String worldKey = level.dimension().location().toString();
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        return executeRead(em -> {
            try {
                TypedQuery<Long> query = em.createQuery(
                        "SELECT COUNT(ib.id) FROM IceBoxData ib WHERE ib.worldKey = :worldKey AND ib.posX = :x AND ib.posY = :y AND ib.posZ = :z",
                        Long.class
                );
                query.setParameter("worldKey", worldKey);
                query.setParameter("x", x);
                query.setParameter("y", y);
                query.setParameter("z", z);
                query.setMaxResults(1);
                return query.getSingleResult() > 0;
            } catch(Exception e) {
                LOGGER.error("Error checking if location is IceBox {} at {},{},{}", worldKey, x, y, z, e);
                return false;
            }
        }, false);
    }

    @Nullable
    private static byte[] serializeItemStack(ItemStack stack, HolderLookup.Provider provider) throws IOException {
        if (stack == null || stack.isEmpty()) return null;
        CompoundTag nbt = stack.save(provider, new CompoundTag());
        if (nbt == null || nbt.isEmpty()) return null;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        NbtIo.writeCompressed(nbt, baos);
        return baos.toByteArray();
    }

    private static ItemStack deserializeItemStack(byte[] data, HolderLookup.Provider provider) throws IOException {
        if (data == null || data.length == 0) return ItemStack.EMPTY;
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        CompoundTag nbt = null;
        try {
            nbt = NbtIo.readCompressed(bais, NbtAccounter.create(0x200000L));
        } catch (IOException e) {
            LOGGER.error("IOException reading compressed NBT data, data might be corrupted.", e);
            return ItemStack.EMPTY;
        } catch (Exception e) {
            LOGGER.error("Unexpected exception reading compressed NBT data.", e);
            return ItemStack.EMPTY;
        }

        if (nbt == null) {
            LOGGER.warn("Read compressed NBT resulted in null tag.");
            return ItemStack.EMPTY;
        }

        return ItemStack.parseOptional(provider, nbt);
    }

    public static CompletableFuture<Void> saveOrUpdateIceBoxAsync(Level level, BlockPos pos, List<ItemStack> currentItems, HolderLookup.Provider provider) {
        if (!isInitialized()) return CompletableFuture.completedFuture(null);
        String worldKey = level.dimension().location().toString();
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        List<ItemStack> itemsCopy = new ArrayList<>(currentItems.size());
        for (ItemStack stack : currentItems) {
            itemsCopy.add(stack != null ? stack.copy() : ItemStack.EMPTY);
        }

        return CompletableFuture.runAsync(() -> {
            saveOrUpdateIceBox(level, pos, itemsCopy, provider);
        }, ForkJoinPool.commonPool());
    }

    public static CompletableFuture<Void> removeIceBoxAsync(Level level, BlockPos pos) {
        if (!isInitialized()) return CompletableFuture.completedFuture(null);
        String worldKey = level.dimension().location().toString();
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        return CompletableFuture.runAsync(() -> removeIceBox(level, pos), ForkJoinPool.commonPool());
    }

    private static void testConnection() {
        executeRead(em -> {
            try {
                Query query = em.createNativeQuery("SELECT 1");
                query.getSingleResult();
                LOGGER.info("Database connection test successful.");
                return true;
            } catch (Exception e) {
                LOGGER.error("Database connection test failed!", e);
                return false;
            }
        }, false);
    }
}