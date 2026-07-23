package com.drultralux.betterboilers.util;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.io.File;

public class BBConfig {

    public File configFolder;
    private Configuration configuration;

    public static int ticksToBoil = 200;
    public static int steamPerBoil = 800;
    public static int heatPerFireboxBlockTick = 20;
    public static int furnaceHeatCapacityPerBlock = 4000;
    public static int heatPerSourceFaceTick = 20;
    public static int heatSinkCapacity = 4000;
    public static int heatSinkActiveGraceTicks = 20;
    public static int tankHeatCapacityPerBlock = 4000;
    public static int tankHeatConsumedPerTickPerBlock = 20;
    public static int heatPerSteamUnit = 4;
    public static int heatExtractPerTickPerBlock = 20;
    public static int heatInsertPerTickPerBlock = 20;

    public static int rotorBaseCount = 4;
    public static int steamBaseUse = 40;
    public static int turbineOut = 400;

    public static int defaultMaxMultiblock = 1000;
    public static boolean debugLoggingEnabled = false;
    public static int maxLogsPerTick = 20;
    public static int maxLogQueueSize = 500;
    public static int logDedupWindowTicks = 200;
    public static int maxBlocksScannedPerTick = 64;
    public static double ironCoilEfficiency = 1.0;
    public static double goldCoilEfficiency = 1.5;
    public static double turbineBaseCapturePerBlade = 2.0;
    public static int mBPerOpenTurbineSlot = 1000;
    public static int maxTurbineArmLength = 8;
    public static int maxTurbineShaftLength = 64;
    public static int maxTurbineSliceAirBlocks = 400;

    public static int pipeRebuildBlocksPerTick = 256;
    public static int pipeDistributeBudgetPerTick = 64;
    public static int fluidPipeCapacityPerSegment = 1000;
    public static int fluidPipeMaxTransferPerTick = 4000;
    public static int energyPipeCapacityPerSegment = 4000;
    public static int energyPipeMaxTransferPerTick = 4000;
    public static int heatPipeCapacityPerSegment = 1000;
    public static int heatPipeMaxTransferPerTick = 100000;

    private BBConfig(File configFile) {
        this.configuration = new Configuration(configFile);
        this.configFolder = configFile.getParentFile();
    }

    private void loadConfig() {
        configuration.load();

        Property p;

        p = configuration.get("BoilerUsage", "ticksToBoil", ticksToBoil,
                "The amount of ticks needed for one boiler cycle, sans calculation. Actual value will be 200/(<number of firebox blocks> * <number of active fuel sources>.");
        ticksToBoil = p.getInt();

        p = configuration.get("BoilerUsage", "steamPerBoil", steamPerBoil,
                "The amount of steam produced per boiler cycle. Water cost will always be 2x the resulting steam.");
        steamPerBoil = p.getInt();

        p = configuration.get("BoilerUsage", "heatPerFireboxBlockTick", heatPerFireboxBlockTick,
                "Heat generated per tick by each firebox block that currently has active fuel burning in its slot.");
        heatPerFireboxBlockTick = p.getInt();

        p = configuration.get("BoilerUsage", "furnaceHeatCapacityPerBlock", furnaceHeatCapacityPerBlock,
                "Heat storage capacity contributed by each block in a furnace controller's multiblock. Total capacity is this times the block count.");
        furnaceHeatCapacityPerBlock = p.getInt();

        p = configuration.get("BoilerUsage", "heatPerSourceFaceTick", heatPerSourceFaceTick,
                "Heat generated per tick, per adjacent face, by a heat sink block touching a valid natural heat source (Magma Block, Lava source block).");
        heatPerSourceFaceTick = p.getInt();

        p = configuration.get("BoilerUsage", "heatSinkCapacity", heatSinkCapacity,
                "Heat storage capacity of a single heat sink block.");
        heatSinkCapacity = p.getInt();

        p = configuration.get("BoilerUsage", "heatSinkActiveGraceTicks", heatSinkActiveGraceTicks,
                "How many ticks a heat sink stays visually lit after it last actually generated heat, so constant fast extraction (drained back to 0 immediately) does not read as inactive.");
        heatSinkActiveGraceTicks = p.getInt();

        p = configuration.get("BoilerUsage", "tankHeatCapacityPerBlock", tankHeatCapacityPerBlock,
                "Heat storage capacity contributed by each block in a tank controller's multiblock (the water/steam tank). Total capacity is this times the block count.");
        tankHeatCapacityPerBlock = p.getInt();

        p = configuration.get("BoilerUsage", "tankHeatConsumedPerTickPerBlock", tankHeatConsumedPerTickPerBlock,
                "Maximum heat a tank controller may consume per tick, per block, to convert water into steam.");
        tankHeatConsumedPerTickPerBlock = p.getInt();

        p = configuration.get("BoilerUsage", "heatPerSteamUnit", heatPerSteamUnit,
                "Heat consumed by a tank controller to convert 1 mB of water into 1 mB of steam.");
        heatPerSteamUnit = p.getInt();

        p = configuration.get("BoilerUsage", "heatExtractPerTickPerBlock", heatExtractPerTickPerBlock,
                "Maximum heat a furnace controller can export per tick, per firebox block, independent of how much it has stored. More connected pipes/faces only help once this scales up (more firebox blocks) - this is the real throughput limit, not total heat capacity.");
        heatExtractPerTickPerBlock = p.getInt();

        p = configuration.get("BoilerUsage", "heatInsertPerTickPerBlock", heatInsertPerTickPerBlock,
                "Maximum heat a tank controller can accept per tick, per tank block, independent of total heat capacity. This is the real acceptance-rate limit, scaling with tank size rather than being capped by a single block's worth of faces.");
        heatInsertPerTickPerBlock = p.getInt();

        p = configuration.get("TurbineUsage", "rotorBaseCount", rotorBaseCount,
                "base amount of rotors used to calculate a turbine's diminishing returns. RF generation will always be 2x the steam cost.");
        rotorBaseCount = p.getInt();

        p = configuration.get("TurbineUsage", "steamBaseUse", steamBaseUse,
                "The amount of steam consumed in a turbine with rotorBaseCount rotors. Used to calculate diminishing returns. RF generation will always be 2x the steam cost.");
        steamBaseUse = p.getInt();

        p = configuration.get("TurbineUsage", "turbineOut", turbineOut,
                "How much RF/T the turbine power tap can transfer.");
        turbineOut = p.getInt();

        p = configuration.get("Multiblock", "defaultMaxMultiblock", defaultMaxMultiblock,
                "The maximum amount of blocks that can be added to a standard multiblock. Some controllers may have different maxima. Includes all of the multiblock's components.");
        defaultMaxMultiblock = p.getInt();

        p = configuration.get("Multiblock", "maxBlocksScannedPerTick", maxBlocksScannedPerTick,
                "How many blocks the multiblock scanner is allowed to check per tick. Higher values validate large structures faster but risk a bigger single-tick cost; lower values spread the cost more but take longer to fully validate huge structures.");
        maxBlocksScannedPerTick = p.getInt();

        p = configuration.get("TurbineUsage", "ironCoilEfficiency", ironCoilEfficiency,
                "Output efficiency multiplier for Iron Coils. Baseline tier.");
        ironCoilEfficiency = p.getDouble();

        p = configuration.get("TurbineUsage", "goldCoilEfficiency", goldCoilEfficiency,
                "Output efficiency multiplier for Gold Coils. Higher tier than Iron.");
        goldCoilEfficiency = p.getDouble();

        p = configuration.get("TurbineUsage", "turbineBaseCapturePerBlade", turbineBaseCapturePerBlade,
                "Base steam-capture value contributed by each valid blade arm, before coil efficiency is applied. Scales overall turbine output up or down independent of coil tier ratios.");
        turbineBaseCapturePerBlade = p.getDouble();

        p = configuration.get("TurbineUsage", "mBPerOpenTurbineSlot", mBPerOpenTurbineSlot,
                "How much steam tank capacity (in mB) each open, uncovered slot around a 2-blade rotor stage contributes.");
        mBPerOpenTurbineSlot = p.getInt();

        p = configuration.get("TurbineUsage", "maxTurbineArmLength", maxTurbineArmLength,
                "Maximum length, in blocks, a single rotor blade arm is allowed to extend from the shaft.");
        maxTurbineArmLength = p.getInt();

        p = configuration.get("TurbineUsage", "maxTurbineShaftLength", maxTurbineShaftLength,
                "Maximum total length, in blocks, the turbine's shaft is allowed to run from Controller to Power Tap.");
        maxTurbineShaftLength = p.getInt();

        p = configuration.get("TurbineUsage", "maxTurbineSliceAirBlocks", maxTurbineSliceAirBlocks,
                "Safety cap on how many air blocks a single rotor slice's flood-fill can find before it's considered unsealed or excessively oversized, failing the whole turbine.");
        maxTurbineSliceAirBlocks = p.getInt();

        p = configuration.get("Logging", "debugLoggingEnabled", debugLoggingEnabled,
                "If true, debug and trace level log messages are recorded. If false, they are discarded immediately at zero cost.");
        debugLoggingEnabled = p.getBoolean();

        p = configuration.get("Logging", "maxLogsPerTick", maxLogsPerTick,
                "Maximum number of queued log entries written to the log file per tick. Prevents a burst of logging from spamming the log file all at once.");
        maxLogsPerTick = p.getInt();

        p = configuration.get("Logging", "maxLogQueueSize", maxLogQueueSize,
                "Maximum number of log entries allowed to sit in the queue awaiting output. If exceeded, the oldest queued entries are dropped and a single summary warning reports how many were lost.");
        maxLogQueueSize = p.getInt();

        p = configuration.get("Logging", "logDedupWindowTicks", logDedupWindowTicks,
                "How many ticks an identical repeated log message is suppressed for before being collapsed into a single '[repeated Nx]' summary line.");
        logDedupWindowTicks = p.getInt();

        p = configuration.get("PipeNetworks", "pipeRebuildBlocksPerTick", pipeRebuildBlocksPerTick,
                "Total pipe blocks (across all in-progress network rebuilds combined) that may be visited per tick. A rebuild that exceeds this resumes next tick instead of spiking the tick it started on.");
        pipeRebuildBlocksPerTick = p.getInt();

        p = configuration.get("PipeNetworks", "pipeDistributeBudgetPerTick", pipeDistributeBudgetPerTick,
                "Maximum number of active pipe networks (per type) that run their distribute() step per tick. A round-robin cursor ensures none starve if there are more active networks than this budget allows in one tick.");
        pipeDistributeBudgetPerTick = p.getInt();

        p = configuration.get("PipeNetworks", "fluidPipeCapacityPerSegment", fluidPipeCapacityPerSegment,
                "mB of fluid capacity contributed by each pipe segment in a fluid network. Total network capacity is this times the network's member count.");
        fluidPipeCapacityPerSegment = p.getInt();

        p = configuration.get("PipeNetworks", "fluidPipeMaxTransferPerTick", fluidPipeMaxTransferPerTick,
                "Maximum mB a single fluid network may pull in and push out per tick, combined, regardless of network size.");
        fluidPipeMaxTransferPerTick = p.getInt();

        p = configuration.get("PipeNetworks", "energyPipeCapacityPerSegment", energyPipeCapacityPerSegment,
                "RF of energy capacity contributed by each pipe segment in an energy network. Total network capacity is this times the network's member count.");
        energyPipeCapacityPerSegment = p.getInt();

        p = configuration.get("PipeNetworks", "energyPipeMaxTransferPerTick", energyPipeMaxTransferPerTick,
                "Maximum RF a single energy network may pull in and push out per tick, combined, regardless of network size.");
        energyPipeMaxTransferPerTick = p.getInt();

        p = configuration.get("PipeNetworks", "heatPipeCapacityPerSegment", heatPipeCapacityPerSegment,
                "Heat unit capacity contributed by each pipe segment in a heat network. Total network capacity is this times the network's member count. No heat sources/sinks exist yet - this is transport-layer prep for the future heat system.");
        heatPipeCapacityPerSegment = p.getInt();

        p = configuration.get("PipeNetworks", "heatPipeMaxTransferPerTick", heatPipeMaxTransferPerTick,
                "Safety ceiling on how much heat a single network may move per tick, combined, regardless of network size - the real per-tick limit now comes from the source/sink's own scaled rate (heatExtractPerTickPerBlock / heatInsertPerTickPerBlock), so this should rarely be the actual bottleneck unless set unreasonably low.");
        heatPipeMaxTransferPerTick = p.getInt();

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    public static BBConfig createConfig(FMLPreInitializationEvent event) {
        // Move config file if it exists.
        File bbFolder = new File(event.getModConfigurationDirectory(), "betterboilers");
        bbFolder.mkdirs();
        if (event.getSuggestedConfigurationFile().exists()) {
            event.getSuggestedConfigurationFile().renameTo(new File(bbFolder, "betterboilers.cfg"));
        }

        BBConfig config = new BBConfig(new File(bbFolder, "betterboilers.cfg"));
        config.loadConfig();
        return config;
    }
}