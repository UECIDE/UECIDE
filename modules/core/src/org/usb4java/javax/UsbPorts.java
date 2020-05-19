/*
 * Copyright (C) 2011 Klaus Reimer <k@ailis.de>
 * See LICENSE.md for licensing information.
 */

package org.usb4java.javax;

import java.util.List;

import javax.usb.UsbDevice;
import javax.usb.UsbPort;

/**
 * A list of USB ports.
 * 
 * @author Klaus Reimer (k@ailis.de)
 * @param <P>
 *            The USB port type.
 * @param <D>
 *            The USB device type.
 */
interface UsbPorts<P extends UsbPort, D extends UsbDevice>
{
    /**
     * Returns the number of ports.
     * 
     * @return The number of ports.
     */
    byte getNumberOfPorts();

    /**
     * Returns the ports.
     * 
     * @return The ports.
     */
    List<P> getUsbPorts();

    /**
     * Returns the USB port with the specified port number.
     * 
     * @param number
     *            The USB port number.
     * @return The USB port or null if no such port.
     */
    P getUsbPort(final byte number);

    /**
     * Returns the attached USB devices.
     * 
     * @return The attached USB devices.
     */
    List<D> getAttachedUsbDevices();

    /**
     * Checks if the specified device is attached to one of the ports.
     * 
     * @param device
     *            The device to search.
     * @return True if device is connected, false if not.
     */
    boolean isUsbDeviceAttached(D device);

    /**
     * Connects a new device to this hub.
     * 
     * @param device
     *            The device to add to this hub.
     */
    void connectUsbDevice(D device);

    /**
     * Disconnects the specified device from the hub.
     * 
     * @param device
     *            The device to disconnected from the hub.
     */
    void disconnectUsbDevice(D device);
}
