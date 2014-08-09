/*
 * Copyright (C) 2011 Klaus Reimer <k@ailis.de>
 * See LICENSE.md for licensing information.
 */

package org.usb4java.javax;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.usb.UsbAbortException;
import javax.usb.UsbConst;
import javax.usb.UsbControlIrp;
import javax.usb.UsbEndpoint;
import javax.usb.UsbEndpointDescriptor;
import javax.usb.UsbException;
import javax.usb.UsbIrp;
import javax.usb.UsbShortPacketException;

import org.usb4java.DeviceHandle;
import org.usb4java.LibUsb;

/**
 * A queue for USB I/O request packets.
 * 
 * @author Klaus Reimer (k@ailis.de)
 */
final class IrpQueue extends AbstractIrpQueue<UsbIrp>
{
    /** The USB pipe. */
    private final Pipe pipe;

    /**
     * Constructor.
     * 
     * @param pipe
     *            The USB pipe
     */
    IrpQueue(final Pipe pipe)
    {
        super(pipe.getDevice());
        this.pipe = pipe;
    }

    @Override
    protected void finishIrp(final UsbIrp irp)
    {
        this.pipe.sendEvent(irp);
    }

    @Override
    protected void processIrp(final UsbIrp irp) throws UsbException
    {
        final UsbEndpoint endpoint = this.pipe.getUsbEndpoint();
        final byte direction = endpoint.getDirection();
        final byte type = endpoint.getType();
        if (type == UsbConst.ENDPOINT_TYPE_CONTROL)
        {
            processControlIrp((UsbControlIrp) irp);
            return;
        }

        switch (direction)
        {
            case UsbConst.ENDPOINT_DIRECTION_OUT:
                irp.setActualLength(write(irp.getData(), irp.getOffset(),
                    irp.getLength()));
                if (irp.getActualLength() < irp.getLength()
                    && !irp.getAcceptShortPacket())
                {
                    throw new UsbShortPacketException();
                }
                break;

            case UsbConst.ENDPOINT_DIRECTION_IN:
                irp.setActualLength(read(irp.getData(), irp.getOffset(),
                    irp.getLength()));
                if (irp.getActualLength() < irp.getLength()
                    && !irp.getAcceptShortPacket())
                {
                    throw new UsbShortPacketException();
                }
                break;

            default:
                throw new UsbException("Invalid direction: "
                    + direction);
        }
    }

    /**
     * Returns the USB endpoint descriptor.
     * 
     * @return The USB endpoint descriptor.
     */
    private UsbEndpointDescriptor getEndpointDescriptor()
    {
        return this.pipe.getUsbEndpoint().getUsbEndpointDescriptor();
    }

    /**
     * Reads bytes from an interrupt endpoint into the specified data array.
     * 
     * @param data
     *            The data array to write the read bytes to.
     * @param offset
     *            The offset in the data array to write the read bytes to.
     * @param len
     *            The number of bytes to read.
     * @throws UsbException
     *             When transfer fails.
     * @return The number of read bytes.
     */
    private int read(final byte[] data, final int offset, final int len)
        throws UsbException
    {
        final UsbEndpointDescriptor descriptor = getEndpointDescriptor();
        final byte type = this.pipe.getUsbEndpoint().getType();
        final DeviceHandle handle = getDevice().open();
        int read = 0;
        while (read < len)
        {
            final int size =
                Math.min(len - read, descriptor.wMaxPacketSize() & 0xffff);
            final ByteBuffer buffer = ByteBuffer.allocateDirect(size);
            final int result = transfer(handle, descriptor, type, buffer);
            buffer.rewind();
            buffer.get(data, offset + read, result);
            read += result;

            // Short packet detected, aborting
            if (result < size) break;
        }
        return read;
    }

    /**
     * Writes the specified bytes to a interrupt endpoint.
     * 
     * @param data
     *            The data array with the bytes to write.
     * @param offset
     *            The offset in the data array to write.
     * @param len
     *            The number of bytes to write.
     * @throws UsbException
     *             When transfer fails.
     * @return The number of written bytes.
     */
    private int write(final byte[] data, final int offset, final int len)
        throws UsbException
    {
        final UsbEndpointDescriptor descriptor = getEndpointDescriptor();
        final byte type = this.pipe.getUsbEndpoint().getType();
        final DeviceHandle handle = getDevice().open();
        int written = 0;
        while (written < len)
        {
            final int size =
                Math.min(len - written, descriptor.wMaxPacketSize() & 0xffff);
            final ByteBuffer buffer = ByteBuffer.allocateDirect(size);
            buffer.put(data, offset + written, size);
            buffer.rewind();
            final int result = transfer(handle, descriptor, type, buffer);
            written += result;

            // Short packet detected, aborting
            if (result < size) break;
        }
        return written;
    }

    /**
     * Transfers data from or to the device.
     * 
     * @param handle
     *            The device handle.
     * @param descriptor
     *            The endpoint descriptor.
     * @param type
     *            The endpoint type.
     * @param buffer
     *            The data buffer.
     * @return The number of transferred bytes.
     * @throws UsbException
     *             When data transfer fails.
     */
    private int transfer(final DeviceHandle handle,
        final UsbEndpointDescriptor descriptor, final int type,
        final ByteBuffer buffer) throws UsbException
    {
        final byte address = descriptor.bEndpointAddress();
        final boolean in = this.pipe.getUsbEndpoint().getDirection() ==
            UsbConst.ENDPOINT_DIRECTION_IN;
        if (type == UsbConst.ENDPOINT_TYPE_BULK)
        {
            return transferBulk(handle, address, in, buffer);
        }
        else if (type == UsbConst.ENDPOINT_TYPE_INTERRUPT)
        {
            return transferInterrupt(handle, address, in, buffer);
        }
        else
        {
            throw new UsbException("Unsupported endpoint type: " + type);
        }
    }

    /**
     * Transfers bulk data from or to the device.
     * 
     * @param handle
     *            The device handle.
     * @param address
     *            The endpoint address.
     * @param in
     *            If bulk-in transfer.
     * @param buffer
     *            The data buffer.
     * @return The number of transferred bytes.
     * @throws UsbException
     *             When data transfer fails.
     */
    private int transferBulk(final DeviceHandle handle, final byte address,
        final boolean in, final ByteBuffer buffer) throws UsbException
    {
        final IntBuffer transferred = IntBuffer.allocate(1);
        int result;
        do
        {
            result = LibUsb.bulkTransfer(handle, address, buffer,
                transferred, getConfig().getTimeout());
            if (result == LibUsb.ERROR_TIMEOUT && isAborting())
                throw new UsbAbortException();
        }
        while (in && result == LibUsb.ERROR_TIMEOUT);
        if (result < 0)
        {
            throw ExceptionUtils.createPlatformException(
                "Transfer error on bulk endpoint", result);
        }
        return transferred.get(0);
    }

    /**
     * Transfers interrupt data from or to the device.
     * 
     * @param handle
     *            The device handle.
     * @param address
     *            The endpoint address.
     * @param in
     *            If interrupt-in transfer.
     * @param buffer
     *            The data buffer.
     * @return The number of transferred bytes.
     * @throws UsbException
     *             When data transfer fails.
     */
    private int transferInterrupt(final DeviceHandle handle,
        final byte address, final boolean in, final ByteBuffer buffer)
        throws UsbException
    {
        final IntBuffer transferred = IntBuffer.allocate(1);
        int result;
        do
        {
            result = LibUsb.interruptTransfer(handle, address, buffer,
                transferred, getConfig().getTimeout());
            if (result == LibUsb.ERROR_TIMEOUT && isAborting())
                throw new UsbAbortException();
        }
        while (in && result == LibUsb.ERROR_TIMEOUT);
        if (result < 0)
        {
            throw ExceptionUtils.createPlatformException(
                "Transfer error on interrupt endpoint", result);
        }
        return transferred.get(0);
    }
}
