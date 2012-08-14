package ru.catssoftware.loginserver.mmocore;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public final class TCPSocket implements ISocket
{
	private final Socket _socket;

	TCPSocket(Socket socket)
	{
		_socket = socket;
	}

	public void close() throws IOException
	{
		_socket.close();
	}

	public ReadableByteChannel getReadableByteChannel()
	{
		return _socket.getChannel();
	}

	public WritableByteChannel getWritableByteChannel()
	{
		return _socket.getChannel();
	}

	public InetAddress getInetAddress()
	{
		return _socket.getInetAddress();
	}

	public int getPort()
	{
		return _socket.getPort();
	}
}