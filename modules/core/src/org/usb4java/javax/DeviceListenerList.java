/*
 * Copyright (C) 2011 Klaus Reimer <k@ailis.de>
 * See LICENSE.md for licensing information.
 */

package org.usb4java.javax;

import javax.usb.event.UsbDeviceDataEvent;
import javax.usb.event.UsbDeviceErrorEvent;
import javax.usb.event.UsbDeviceEvent;
import javax.usb.event.UsbDeviceListener;

/**
 * USB device listener list.
 * 
 * @author Klaus Reimer (k@ailis.de)
 */
final class DeviceListenerList extends
    EventListenerList<UsbDeviceListener> implements UsbDeviceListener
{
    /**
     * Constructs a new USB device listener list.
     */
    DeviceListenerList()
    {
        super();
    }
    
    @Override
    public UsbDeviceListener[] toArray()
    {
        return getListeners().toArray(
            new UsbDeviceListener[getListeners().size()]);
    }

    @Override
    public void usbDeviceDetached(final UsbDeviceEvent event)
    {
        for (final UsbDeviceListener listener: toArray())
        {
            listener.usbDeviceDetached(event);
        }
    }

    @Override
    public void errorEventOccurred(final UsbDeviceErrorEvent event)
    {
        for (final UsbDeviceListener listener: toArray())
        {
            listener.errorEventOccurred(event);
        }
    }

    @Override
    public void dataEventOccurred(final UsbDeviceDataEvent event)
    {
        for (final UsbDeviceListener listener: toArray())
        {
            listener.dataEventOccurred(event);
        }
    }
}
