package MomentumMayhem.systems;

import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static MomentumMayhem.game.GameManager.getWorld;

public class ItemBuilder {
    private final ItemStack stack;

    public ItemBuilder(Item item, int count) {
        this.stack = new ItemStack(item, count);
    }

    public ItemBuilder name(String name, Formatting color) {
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).formatted(color));
        return this;
    }

    public <T> ItemBuilder withComponent(ComponentType<T> type, T value) {
        stack.set(type, value);
        return this;
    }

    public ItemBuilder withEnchant(RegistryKey<Enchantment> enchantment, int level) {
        Registry<Enchantment> registry = getWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        stack.addEnchantment(registry.getOrThrow(enchantment), level);
        return this;
    }

    public ItemBuilder maxDura(int amount) {
        stack.set(DataComponentTypes.MAX_DAMAGE, amount);
        return this;
    }

    public ItemStack build() {
        return this.stack;
    }
}