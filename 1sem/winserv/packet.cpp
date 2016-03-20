#include "packet.h"
#include <assert.h>
#include <iostream>

using namespace std;

void resetPacket(Packet * pack)
{
	pack->magix = MAGIX_NUMBER;
	pack->code = 0;
	pack->dataSize = 0;
}

void fillPacket(Packet * pack, void * data, size_t len)
{
	if (len != 0)
	{
		pack->dataSize = len;
		memcpy(pack->data, data, len);
	}
}

bool sendPacket(SOCKET sock, int code)
{
	char buf[HEADER_SIZE];
	Packet * pack = (Packet*)buf;

	resetPacket(pack);
	pack->code = code;

	int res = send(sock, buf,HEADER_SIZE,0);

	if (res == SOCKET_ERROR)
	{
		cout << "Sending packet fail (" << WSAGetLastError() <<" error)" << endl;
		return false;
	}
	else if (res != HEADER_SIZE)
	{
		cout << "Sending packet fail (was send " << res <<" bytes instead of " << HEADER_SIZE << ")" << endl;
		return false;
	}

	return true;
}

bool sendPacket(SOCKET sock, Packet * pack)
{
	int res = send(sock, (char*)pack,HEADER_SIZE+pack->dataSize,0);
	if (res == SOCKET_ERROR)
	{
		cout << "Sending packet fail (" << WSAGetLastError() <<" error)" << endl;
		return false;
	}
	else if (res != HEADER_SIZE+pack->dataSize)
	{
		cout << "Sending packet fail (was send " << res <<" bytes instead of " << HEADER_SIZE+pack->dataSize << ")" << endl;
		return false;
	}

	return true;
}

int waitForPacket(SOCKET sock, Packet * pack)
{
	// wait for data
	int res = recv(sock,(char*)pack,sizeof(Packet),0);
	if (res>0)
	{
		// check size
		if (res < HEADER_SIZE)
		{
			cout << "Received packet too small" << endl;
			return -1;
		}

		// check magix number
		if ( pack->magix != MAGIX_NUMBER )
		{
			cout << "Received not correct packet" << endl;
			return -1;
		}

		// check received data size
		if (res != HEADER_SIZE+pack->dataSize)
		{
			cout << "Received data not equal to packet size" << endl;
			return -1;
		}

		return 1;
	}
	else if (res==0)
	{
		// connection closed
		cout << "Connection from server was closed" << endl;
		return 0;
	}
	else
	{
		// error
		int err_code = WSAGetLastError();
		if (err_code == WSAETIMEDOUT)
		{
			cout << "Timeout error" << endl;
		}
		else
		{
			cout << "Error while receive packet " << err_code << endl;
		}
		return -1;
	}

}
