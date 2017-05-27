package me.armar.plugins.autorank.pathbuilder.requirement;

import me.armar.plugins.autorank.statsmanager.StatsPlugin;
import org.bukkit.entity.Player;

import me.armar.plugins.autorank.language.Lang;
import me.armar.plugins.autorank.util.AutorankTools;

public class BlocksMovedRequirement extends Requirement {

    BlocksMovedWrapper wrapper = null;

    @Override
    public String getDescription() {

        String desc = Lang.BLOCKS_MOVED_REQUIREMENT.getConfigValue(wrapper.getBlocksMoved() + "",
                wrapper.getMovementType());

        // Check if this requirement is world-specific
        if (this.isWorldSpecific()) {
            desc = desc.concat(" (in world '" + this.getWorld() + "')");
        }

        return desc;
    }

    @Override
    public String getProgress(final Player player) {

        final int progressBar = getStatsPlugin().getNormalStat(StatsPlugin.StatType.BLOCKS_MOVED,
                player.getUniqueId(),
                AutorankTools.makeStatsInfo("world", this.getWorld(), "moveType", wrapper.getRawMovementType()));

        return progressBar + "/" + wrapper.getBlocksMoved() + " (" + wrapper.getMovementType() + ")";
    }

    @Override
    public boolean meetsRequirement(final Player player) {

        if (!getStatsPlugin().isEnabled()) {
            return false;
        }

        final int count = getStatsPlugin().getNormalStat(StatsPlugin.StatType.BLOCKS_MOVED, player.getUniqueId(),
                AutorankTools.makeStatsInfo("world", this.getWorld(), "moveType", wrapper.getRawMovementType()));

        return count >= wrapper.getBlocksMoved();
    }

    @Override
    public boolean setOptions(final String[] options) {

        int blocksMoved = 0;
        int movementType = 0;

        if (options.length > 0) {
            blocksMoved = Integer.parseInt(options[0].trim());
        }
        if (options.length > 1) {
            movementType = Integer.parseInt(options[1].trim());
        }

        wrapper = new BlocksMovedWrapper(blocksMoved, movementType);

        return wrapper != null;
    }
}

class BlocksMovedWrapper {

    private int blocksMoved = 0;
    private String movementType = "";
    private int rawMovementType = 0;

    public BlocksMovedWrapper(final int blocksMoved, final int moveType) {
        this.blocksMoved = blocksMoved;
        this.movementType = getMovementString(moveType);
        this.rawMovementType = moveType;
    }

    public int getBlocksMoved() {
        return blocksMoved;
    }

    private String getMovementString(final int moveType) {
        switch (moveType) {
            case 0:
                return "by foot";
            case 1:
                return "by boat";
            case 2:
                return "by cart";
            case 3:
                return "by pig";
            case 4:
                return "by piggy-cart";
            case 5:
                return "by horse";
            default:
                return "by foot";
        }
    }

    public String getMovementType() {
        return movementType;
    }

    public int getRawMovementType() {
        return rawMovementType;
    }
}
