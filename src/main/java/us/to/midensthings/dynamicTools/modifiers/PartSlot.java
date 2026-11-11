package us.to.midensthings.dynamicTools.modifiers;

/***
 * Which part of a tool a material is used in: the primary part (head, blade or armor frame), the secondary part
 * (handle or armor core) or the tertiary part (binding). Modifiers can be limited to materials in some of these.
 */
public enum PartSlot {
    PRIMARY,
    SECONDARY,
    TERTIARY;

    /***
     * @return the slot of the part at index 0-2 of a tool type's parts (primary, secondary, tertiary)
     */
    public static PartSlot fromIndex(int index) {
        return values()[index];
    }
}
