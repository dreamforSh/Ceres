# Ceres

## Overview

Ceres is a high-performance network optimization mod designed for Minecraft .It aims to reduce network traffic, lower latency, and enhance multiplayer gaming experiences. Through various methods, Ceres significantly decreases the data volume transmitted between Minecraft clients and servers, making it particularly suitable for poor network conditions or large-scale server environments.  
It features a native implementation of Velocity's Netty framework and integrates the high-performance libdeflate compression library.

## Acknowledgments to Velocity and Pluto

## Key Features

### Packet Compression
1. Compresses only packets exceeding a threshold to avoid overhead for small packets  
2. Balances compression ratio and performance as needed  
3. Automatically uses raw data if compression yields larger sizes  

### Duplicate Packet Filtering
- **Smart Detection**: Identifies and filters redundant network packets  
- **Type Filtering**: Configurable filtering for position, chunk, and entity packets  
- **Safety Mechanisms**: Timeout and max consecutive duplicates ensure game state synchronization  

### Network Optimization Modes
- **Vanilla Mode**: Uses Forge's networking system for maximum compatibility  
- **Modern Mode**: Leverages Netty directly for stronger optimizations (may conflict with some mods)  

### Packet Batching
- **Intelligent Batching**: Merges small packets into larger ones to reduce overhead  
- **Configurable Latency**: Adjust batching delays to balance responsiveness and efficiency  
- **Priority Handling**: Ensures critical packets are never delayed  

## Performance Impact

In our tests, Ceres excels across various network conditions:

| Scenario                  | Traffic Reduction | Latency Improvement |
|---------------------------|-------------------|---------------------|
| High-entity-density areas | Up to 75%        | 15-30%             |
| Large redstone machines   | Up to 60%        | 10-25%             |
| Rapid chunk exploration   | Up to 50%        | 5-20%              |
| Multiplayer PVP           | Up to 40%        | 10-15%             |

| Feature          | JAVA     | LIBDEFLATE       |
|-------------------|----------|------------------|
| CPU Usage         | Moderate | Low              |
| Compression Ratio | Good     | Excellent        |
| Compatibility     | Best     | Requires native  |
| Performance       | Standard | 2-4x faster      |

## Configuration Options

Ceres offers extensive configuration:

### General Settings
- Optimization mode (Vanilla/Modern)  
- Packet compression toggle  
- Compression threshold/level  
- Duplicate filtering settings  

### Client Settings
- Client optimization toggle  
- Batching latency/size  
- Network stats display  

### Server Settings
- Server optimization toggle  
- Server batching parameters  
- Chunk update optimizations  

``` 
/ceres compression stats     - Show compression statistics  
/ceres compression reset     - Reset stats  
/ceres compression engine <type> - Switch compression engine  
/ceres compression benchmark - Run performance tests  
```

## Compatibility

Ceres is designed for compatibility with most Minecraft mods. Vanilla Mode ensures maximum compatibility, while Modern Mode prioritizes optimizations.  

If encountering issues, try switching to Vanilla Mode or disabling specific optimizations.  

### Notes  
- May conflict with other network optimization mods  
- LIBDEFLATE requires supported OS/architecture  

## Installation

1. Ensure Minecraft 1.19.2 and compatible Forge are installed  
2. Download the Ceres mod file  
3. Place the file in Minecraft's `mods` folder  
4. Launch the game – Ceres auto-configures upon startup  

## Performance Monitoring

Ceres includes built-in monitoring tools:  
1. Press F3 to open the debug screen  
2. Locate `[Ceres]` entries showing optimization stats  
3. Stats include compression ratios, filtered packets, and network traffic  

## FAQ

**Q: Does Ceres require installation on both server and client?**  
A: For best results, install on both. Client-only installation provides partial optimizations.  

**Q: Does Ceres affect game stability?**  
A: Ceres includes multiple safeguards to ensure stability. Disable specific features if issues arise.  

**Q: Why can't I see network stats?**  
A: Enable `showNetworkStats` in config and check the F3 debug screen.  

## Contributions & Support

Ceres is open-source. We welcome code contributions and suggestions. Please submit issues or PRs via our GitHub repository.
