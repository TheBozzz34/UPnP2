package xyz.necrozma.upnpplugin.net;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.util.Enumeration;
import java.util.LinkedList;

abstract class GatewayFinder {
    private static final String[] SEARCH_MESSAGES;

    static {
        LinkedList<String> m = new LinkedList<String>();
        for (String type : new String[] { "urn:schemas-upnp-org:device:InternetGatewayDevice:1", "urn:schemas-upnp-org:service:WANIPConnection:1", "urn:schemas-upnp-org:service:WANPPPConnection:1" })
            m.add("M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nST: " + type + "\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\n\r\n");
        SEARCH_MESSAGES = m.<String>toArray(new String[0]);
    }

    private class GatewayListener extends Thread {
        private Inet4Address ip;

        private String req;

        public GatewayListener(Inet4Address ip, String req) {
            setName("WaifUPnP - Gateway Listener");
            this.ip = ip;
            this.req = req;
        }

        public void run() {
            boolean foundgw = false;
            Gateway gw = null;
            try {
                byte[] req = this.req.getBytes();
                DatagramSocket s = new DatagramSocket(new InetSocketAddress(this.ip, 0));
                s.send(new DatagramPacket(req, req.length, new InetSocketAddress("239.255.255.250", 1900)));
                s.setSoTimeout(3000);
                while (true) {
                    try {
                        DatagramPacket recv = new DatagramPacket(new byte[1536], 1536);
                        s.receive(recv);
                        gw = new Gateway(recv.getData(), this.ip, recv.getAddress());
                        String extIp = gw.getExternalIP();
                        if (extIp != null && !extIp.equalsIgnoreCase("0.0.0.0")) {
                            GatewayFinder.this.gatewayFound(gw);
                            foundgw = true;
                        }
                    } catch (SocketTimeoutException t) {
                        break;
                    } catch (Throwable throwable) {}
                }
            } catch (Throwable throwable) {}
            if (!foundgw && gw != null)
                GatewayFinder.this.gatewayFound(gw);
        }
    }

    private LinkedList<GatewayListener> listeners = new LinkedList<GatewayListener>();

    public GatewayFinder() {
        for (Inet4Address ip : getLocalIPs()) {
            for (String req : SEARCH_MESSAGES) {
                GatewayListener l = new GatewayListener(ip, req);
                l.start();
                this.listeners.add(l);
            }
        }
    }

    public boolean isSearching() {
        for (GatewayListener l : this.listeners) {
            if (l.isAlive())
                return true;
        }
        return false;
    }

    private static Inet4Address[] getLocalIPs() {
        LinkedList<Inet4Address> ret = new LinkedList<Inet4Address>();
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                try {
                    NetworkInterface iface = ifaces.nextElement();
                    if (!iface.isUp() || iface.isLoopback() || iface.isVirtual() || iface.isPointToPoint())
                        continue;
                    Enumeration<InetAddress> addrs = iface.getInetAddresses();
                    if (addrs == null)
                        continue;
                    while (addrs.hasMoreElements()) {
                        InetAddress addr = addrs.nextElement();
                        if (addr instanceof Inet4Address)
                            ret.add((Inet4Address)addr);
                    }
                } catch (Throwable throwable) {}
            }
        } catch (Throwable throwable) {}
        return ret.<Inet4Address>toArray(new Inet4Address[0]);
    }

    public abstract void gatewayFound(Gateway paramGateway);
}

