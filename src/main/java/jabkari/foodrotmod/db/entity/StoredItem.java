package jabkari.foodrotmod.db.entity;

import jakarta.persistence.*;
import java.util.Arrays;
import java.util.Objects;

@Entity
@Table(name = "stored_items", indexes = {
        @Index(name = "idx_storeditem_iceboxid", columnList = "ice_box_id") // Index FK for performance
})
public class StoredItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ice_box_id", nullable = false)
    private IceBoxData iceBox;

    @Column(name = "slot_index", nullable = false)
    private int slotIndex;

    @Lob // Large Object Binary - suitable for most databases
    @Column(name = "item_nbt_data", columnDefinition = "BLOB", nullable = false) // Explicit BLOB type
    private byte[] itemNbtData;

    public StoredItem() {}

    public StoredItem(IceBoxData iceBox, int slotIndex, byte[] itemNbtData) {
        this.iceBox = iceBox;
        this.slotIndex = slotIndex;
        this.itemNbtData = itemNbtData;
    }

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public IceBoxData getIceBox() { return iceBox; }
    // Setter also updates the other side of the relationship (useful)
    public void setIceBox(IceBoxData iceBox) {
        IceBoxData oldIceBox = this.iceBox;
        this.iceBox = iceBox;
        if (!sameAsFormer(iceBox)) { // Avoid redundant operations
            if (oldIceBox != null) oldIceBox.removeItem(this);
            if (iceBox != null) iceBox.addItem(this); // Add to new collection if not already there
        }
    }
    public int getSlotIndex() { return slotIndex; }
    public void setSlotIndex(int slotIndex) { this.slotIndex = slotIndex; }
    public byte[] getItemNbtData() { return itemNbtData; }
    public void setItemNbtData(byte[] itemNbtData) { this.itemNbtData = itemNbtData; }

    // Helper for bidirectional relationship management
    private boolean sameAsFormer(IceBoxData newIceBox) {
        return Objects.equals(this.iceBox, newIceBox);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoredItem that = (StoredItem) o;
        // Compare by primary key if available and non-zero
        return id != null && id != 0L && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        // Use primary key for hash code if available, otherwise rely on Object default
        return id != null && id != 0L ? Objects.hash(id) : super.hashCode();
    }

    @Override
    public String toString() {
        return "StoredItem{" +
                "id=" + id +
                ", iceBoxId=" + (iceBox != null ? iceBox.getId() : "null") +
                ", slotIndex=" + slotIndex +
                ", itemNbtData=" + Arrays.toString(itemNbtData) +
                '}';
    }
}