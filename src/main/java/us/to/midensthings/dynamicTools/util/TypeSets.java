package us.to.midensthings.dynamicTools.util;

import us.to.midensthings.dynamicTools.tools.ToolType;

import java.util.EnumSet;
import java.util.Set;

public class TypeSets {

    public static Set<ToolType> onlyTools = Set.of(ToolType.AXE,ToolType.HOE,ToolType.SHOVEL,ToolType.PICKAXE);
    public static Set<ToolType> onlyWeapons = Set.of(ToolType.AXE,ToolType.SWORD,ToolType.MACE,ToolType.SPEAR);
    public static Set<ToolType> onlyArmor = Set.of(ToolType.HELMET,ToolType.CHESTPLATE,ToolType.LEGGINGS,ToolType.BOOTS);
    public static Set<ToolType> noArmor = EnumSet.complementOf(EnumSet.of(ToolType.HELMET,ToolType.CHESTPLATE,ToolType.LEGGINGS,ToolType.BOOTS));
    public static Set<ToolType> all = EnumSet.allOf(ToolType.class);

}
