package us.to.midensthings.dynamicTools.util;

import us.to.midensthings.dynamicTools.tools.ToolType;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

public class TypeSets {

    public static Set<ToolType> onlyTools = Set.of(ToolType.AXE,ToolType.HOE,ToolType.SHOVEL,ToolType.PICKAXE);
    public static Set<ToolType> onlyWeapons = Set.of(ToolType.AXE,ToolType.SWORD,ToolType.MACE,ToolType.SPEAR);
    public static Set<ToolType> onlyArmor = Set.of(); //TODO: add armors when armors are added
    public static Set<ToolType> noArmor = EnumSet.allOf(ToolType.class); //TODO: remove armors when armors are added
    public static Set<ToolType> all = EnumSet.allOf(ToolType.class);

}
