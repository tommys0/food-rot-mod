package jabkari.foodrotmod.db.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "ice_boxes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"world_key", "pos_x", "pos_y", "pos_z"})
})
public class IceBoxData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "world_key", nullable = false, length = 128) // Increased length for safety
    private String worldKey; // e.g., "minecraft:overworld"

    @Column(name = "pos_x", nullable = false)
    private int posX;

    @Column(name = "pos_y", nullable = false)
    private int posY;

    @Column(name = "pos_z", nullable = false)
    private int posZ;

    @OneToMany(mappedBy = "iceBox", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("slotIndex ASC") // Keep items ordered by slot
    private List<StoredItem> items = new ArrayList<>();

    public IceBoxData() {}

    public IceBoxData(String worldKey, int posX, int posY, int posZ) {
        this.worldKey = worldKey;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
    }

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getWorldKey() { return worldKey; }
    public void setWorldKey(String worldKey) { this.worldKey = worldKey; }
    public int getPosX() { return posX; }
    public void setPosX(int posX) { this.posX = posX; }
    public int getPosY() { return posY; }
    public void setPosY(int posY) { this.posY = posY; }
    public int getPosZ() { return posZ; }
    public void setPosZ(int posZ) { this.posZ = posZ; }
    public List<StoredItem> getItems() { return items; }
    public void setItems(List<StoredItem> items) { this.items = items; }

    // Helper to manage bidirectional relationship
    public void addItem(StoredItem item) {
        if (item != null) {
            items.add(item);
            item.setIceBox(this);
        }
    }
    public void removeItem(StoredItem item) {
        if (item != null) {
            items.remove(item);
            item.setIceBox(null);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IceBoxData that = (IceBoxData) o;
        // Base equality on logical key: world and position
        return posX == that.posX && posY == that.posY && posZ == that.posZ && Objects.equals(worldKey, that.worldKey);
    }

    @Override
    public int hashCode() {
        // Base hash code on logical key
        return Objects.hash(worldKey, posX, posY, posZ);
    }

    @Override
    public String toString() {
        return "IceBoxData{" +
                "id=" + id +
                ", worldKey='" + worldKey + '\'' +
                ", posX=" + posX +
                ", posY=" + posY +
                ", posZ=" + posZ +
                ", itemCount=" + (items != null ? items.size() : 0) +
                '}';
    }
}