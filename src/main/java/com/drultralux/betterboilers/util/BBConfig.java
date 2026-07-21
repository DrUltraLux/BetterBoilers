package com.drultralux.betterboilers.util;

import com.drultralux.betterboilers.BetterBoilers;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.io.File;

public class BBConfig {

    public File configFolder;
    private Configuration configuration;

    public static int ticksToBoil = 200;
    public static int steamPerBoil = 800;
    public static double pumpMultiplier = 1;
    public static int pumpDrain = 500;

    public static int rotorBaseCount = 4;
    public static int steamBaseUse = 40;
    public static int turbineOut = 400;

    public static int defaultMaxMultiblock = 1000;
    public static int defaultMinMultiblock = 8;
    public static boolean debugLoggingEnabled = false;
    public static int maxLogsPerTick = 20;
    public static int maxLogQueueSize = 500;
    public static int logDedupWindowTicks = 200;
    public static int maxBlocksScannedPerTick = 64;
    public static double ironCoilEfficiency = 1.0;
    public static double goldCoilEfficiency = 1.5;
    public static int mBPerOpenTurbineSlot = 1000;
    public static int maxTurbineArmLength = 8;
    public static int maxTurbineShaftLength = 64;
    public static int maxTurbineSliceAirBlocks = 400;

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

        p = configuration.get("BoilerUsage", "pumpMultiplier", pumpMultiplier,
                "The multiplier for how much steam is produced per tick with a pump. Steam production calculated by <number of firebox blocks> * <number of active fuel sources> * <standard steam/tick> * <this multiplier>.");
        pumpMultiplier = p.getDouble();

        p = configuration.get("BoilerUsage", "pumpDrain", pumpDrain,
                "How much steam a pump will auto-output a tick. Can still be extracted from faster with a machine.");
        pumpDrain = p.getInt();

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

        p = configuration.get("Multiblock", "defaultMinMultiblock", defaultMinMultiblock,
                "The minimum amount of blocks that can be added to a standard multiblock. Some controllers may have different minima. Includes all of the multiblock's components. Set to 0 for no minimum.");
        defaultMinMultiblock = p.getInt();

        p = configuration.get("Multiblock", "maxBlocksScannedPerTick", maxBlocksScannedPerTick,
                "How many blocks the multiblock scanner is allowed to check per tick. Higher values validate large structures faster but risk a bigger single-tick cost; lower values spread the cost more but take longer to fully validate huge structures.");
        maxBlocksScannedPerTick = p.getInt();

        p = configuration.get("TurbineUsage", "ironCoilEfficiency", ironCoilEfficiency,
                "Output efficiency multiplier for Iron Coils. Baseline tier.");
        ironCoilEfficiency = p.getDouble();

        p = configuration.get("TurbineUsage", "goldCoilEfficiency", goldCoilEfficiency,
                "Output efficiency multiplier for Gold Coils. Higher tier than Iron.");
        goldCoilEfficiency = p.getDouble();

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