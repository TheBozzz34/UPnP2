package xyz.necrozma.upnpplugin;

import io.papermc.lib.PaperLib;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.necrozma.upnpplugin.net.UPnP;

import java.util.logging.Logger;

public class UpnpPlugin extends JavaPlugin {

  Logger logger = this.getLogger();

  private final int PORT = this.getServer().getPort();

  @Override
  public void onEnable() {
    PaperLib.suggestPaper(this);

    saveDefaultConfig();

    try {
      if (UPnP.isUPnPAvailable()) { //is UPnP available?
        if (UPnP.isMappedTCP(PORT)) { //is the port already mapped?
          logger.warning("UPnP port forwarding not enabled: port is already mapped");
        } else if (UPnP.openPortTCP(PORT)) { //try to map port
          logger.finest("UPnP port forwarding enabled");
        } else {
          logger.severe("UPnP port forwarding failed");
        }
      } else {
        logger.severe("UPnP is not available");
      }
    } catch (Throwable t) {
      logger.severe(t.getLocalizedMessage());
    }

  }
  @Override
  public void onDisable() {
    UPnP.closePortTCP(PORT);
  }
}
